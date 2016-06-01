/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine;

import net.devrieze.util.CollectionUtil;
import net.devrieze.util.ComparableHandle;
import net.devrieze.util.Handle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.Handles;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import nl.adaptivity.xml.XmlWriterUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import java.security.Principal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProcessInstance<T extends Transaction> implements HandleAware<ProcessInstance<T>>, SecureObject, XmlSerializable {

  public enum State {
    NEW,
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED;
  }

  public static class ProcessInstanceRef implements Handle<ProcessInstance>, XmlSerializable {

    private long mHandle;

    private long mProcessModel;

    private String mName;

    private String mUUID;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(final ProcessInstance processInstance) {
      setHandle(processInstance.getHandleValue());
      setProcessModel(processInstance.mProcessModel.getHandle());
      mName = processInstance.getName();
      if ((mName == null) || (mName.trim().length() == 0)) {
        mName = processInstance.mProcessModel.getName() + " instance " + mHandle;
      }
      mUUID = processInstance.getUUID()==null ? null : processInstance.getUUID().toString();
    }

    @Override
    public void serialize(@NotNull final XmlWriter out) throws XmlException {
      XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX);
      if (mHandle>=0) { out.attribute("", "handle", "", Long.toString(mHandle)); }
      XmlWriterUtil.writeAttribute(out, "processModel", mProcessModel);
      XmlWriterUtil.writeAttribute(out, "name", mName);
      XmlWriterUtil.writeAttribute(out, "uuid", mUUID);
      out.endTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX);
    }

    public void setHandle(final long handle) {
      mHandle = handle;
    }

    @Override
    public long getHandleValue() {
      return mHandle;
    }

    public void setProcessModel(final Handle<? extends ProcessModelImpl> processModel) {
      mProcessModel = processModel.getHandleValue();
    }

    public long getProcessModel() {
      return mProcessModel;
    }

    public String getName() {
      return mName;
    }

    public void setName(final String name) {
      mName = name;
    }

    public String getUUID() {
      return mUUID;
    }

    public void setUUID(String uUID) {
      mUUID = uUID;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModelImpl mProcessModel;

  private final Collection<ComparableHandle<? extends ProcessNodeInstance<T>>> mThreads;

  private final Collection<ComparableHandle<? extends ProcessNodeInstance<T>>> mFinishedNodes;

  private final Collection<ComparableHandle<? extends ProcessNodeInstance<T>>> mEndResults;

  private final HashMap<JoinImpl, ComparableHandle<? extends JoinInstance<T>>> mJoins;

  private long mHandle;

  private final ProcessEngine<T> mEngine;// XXX actually introduce a generic parameter for transactions

  private List<ProcessData> mInputs = new ArrayList<>();

  private List<ProcessData> mOutputs = new ArrayList<>();

  private final String mName;

  private final Principal mOwner;

  private State mState;

  private final UUID mUUid;

  @Deprecated
  ProcessInstance(final long handle, final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    this(Handles.<ProcessInstance<T>>handle(handle), owner, processModel, name, uUid, state, engine);
  }

  ProcessInstance(final Handle<ProcessInstance<T>> handle, final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    mHandle = handle.getHandleValue();
    mProcessModel = processModel;
    mOwner = owner;
    mUUid = uUid;
    mEngine = engine;
    mName =name;
    mState = state==null ? State.NEW : state;
    mThreads = new LinkedList<>();
    mJoins = new HashMap<>();
    mEndResults = new ArrayList<>();
    mFinishedNodes = new ArrayList<>();
  }

  public ProcessInstance(final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    mProcessModel = processModel;
    mName = name;
    mUUid = uUid;
    mEngine = engine;
    mThreads = new LinkedList<>();
    mOwner = owner;
    mJoins = new HashMap<>();
    mEndResults = new ArrayList<>();
    mFinishedNodes = new ArrayList<>();
    mState = state == null ? State.NEW : state;
  }

  synchronized void setChildren(T transaction, final Collection<? extends Handle<? extends ProcessNodeInstance<T>>> children) throws SQLException {
    mJoins.clear(); // TODO proper synchronization
    mThreads.clear();
    mFinishedNodes.clear();
    mEndResults.clear();

    List<ProcessNodeInstance<T>> nodes = new ArrayList<>();
    Set<ComparableHandle<? extends ProcessNodeInstance<T>>> threads = new TreeSet<>();

    for(Handle<? extends ProcessNodeInstance<T>> handle: children) {
      if (handle==null) { throw new NullPointerException(); }
      final ProcessNodeInstance inst = mEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      nodes.add(inst);
      threads.add(Handles.handle(handle));
      if (inst instanceof JoinInstance) {
        JoinInstance<T> joinInst = (JoinInstance<T>) inst;
        mJoins.put(joinInst.getNode(), joinInst.getHandle());
      }
    }

    for(ProcessNodeInstance<T> instance: nodes) {

      if (instance.getNode() instanceof EndNode) {
        final ComparableHandle<? extends ProcessNodeInstance<T>> handle = instance.getHandle();
        mEndResults.add(handle);
        threads.remove(handle);
      }

      final Collection<? extends ComparableHandle<? extends ProcessNodeInstance<T>>> preds = instance.getDirectPredecessors();
      if (preds!=null) {
        for(ComparableHandle<? extends ProcessNodeInstance<T>> pred:preds) {
          if (threads.contains(pred)) {
            mFinishedNodes.add(pred);
            threads.remove(pred);
          }
        }
      }

    }
    mThreads.addAll(threads);
  }

  synchronized void setThreads(T transaction, final Collection<? extends ComparableHandle<? extends ProcessNodeInstance<T>>> threads) throws SQLException {
    if (!CollectionUtil.hasNull(threads)) { throw new NullPointerException(); }
    mThreads.addAll(threads);
  }

  public synchronized void initialize(T transaction) throws SQLException {
    if (mState!=State.NEW || mThreads.size() > 0) {
      throw new IllegalStateException("The instance already appears to be initialised");
    }
    for (final StartNodeImpl node : mProcessModel.getStartNodes()) {
      final ProcessNodeInstance<T> instance = new ProcessNodeInstance<T>(node, null, this);
      ComparableHandle<? extends ProcessNodeInstance<T>>  handle   = mEngine.registerNodeInstance(transaction, instance);
      if (handle==null) { throw new NullPointerException(); }
      mThreads.add(handle);
    }
    mState = State.INITIALIZED;
    mEngine.updateStorage(transaction, this);
  }

  public synchronized void finish(T transaction) throws SQLException {
    int mFinished = getFinishedCount();
    if (mFinished >= mProcessModel.getEndNodeCount()) {
      // TODO mark and store results
      mState=State.FINISHED;
      mEngine.updateStorage(transaction, this);
      transaction.commit();
      mEngine.finishInstance(transaction, this);
    }
  }

  public synchronized ProcessNodeInstance<T> getNodeInstance(final T transaction, final Identifiable identifiable) throws SQLException {
    for (Handle<? extends ProcessNodeInstance<T>> handle: CollectionUtil.combine(mEndResults, mFinishedNodes, mThreads)) {
      ProcessNodeInstance<T> instance = mEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      if (identifiable.getId().equals(instance.getNode().getId())) {
        return instance;
      }
      Handle<? extends ProcessNodeInstance<T>> result = instance.getPredecessor(transaction, identifiable.getId());
      if (result!=null) { return mEngine.getNodeInstance(transaction, result, SecurityProvider.SYSTEMPRINCIPAL); }
    }
    return null;
  }

  private int getFinishedCount() {
    return mEndResults.size();
  }

  @NotNull
  public synchronized JoinInstance<T> getJoinInstance(final T transaction, final JoinImpl join, final ComparableHandle<? extends ProcessNodeInstance<T>> predecessor) throws SQLException {
    synchronized (mJoins) {
      ComparableHandle<? extends JoinInstance<T>> joinHandle = mJoins.get(join);
      JoinInstance<T>                   result     = joinHandle==null ? null : (JoinInstance) getEngine().getNodeInstance(transaction, joinHandle, SecurityProvider.SYSTEMPRINCIPAL);
      if (result == null) {
        final Collection<ComparableHandle<? extends ProcessNodeInstance<T>>> predecessors = new ArrayList<>(join.getPredecessors().size());
        predecessors.add(predecessor);
        result = new JoinInstance<T>(transaction, join, predecessors, this);
        ComparableHandle<JoinInstance<T>> resultHandle = getEngine().registerNodeInstance(transaction, result);
        mJoins.put(join, resultHandle);
      } else {
        result.addPredecessor(transaction, predecessor);
      }
      return result;
    }
  }

  public synchronized void removeJoin(final JoinInstance j) {
    mJoins.remove(j.getNode());
  }

  public synchronized long getHandleValue() {
    return mHandle;
  }

  @Override
  public synchronized Handle<? extends ProcessInstance<T>> getHandle() {
    return Handles.handle(mHandle);
  }

  @Override
  public synchronized void setHandleValue(final long handleValue) {
    mHandle = handleValue;
  }

  public String getName() {
    return mName;
  }

  public Principal getOwner() {
    return mOwner;
  }

  public UUID getUUID() {
    return mUUid;
  }

  public ProcessEngine<T> getEngine() {
    return mEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public ProcessModelImpl getProcessModel() {
    return mProcessModel;
  }

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  public synchronized List<ProcessData> getInputs() {
    return mInputs;
  }

  public synchronized State getState() {
    return mState;
  }

  public synchronized void start(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final Node payload) throws SQLException {
    if (mState==null) {
      initialize(transaction);
    }
    ArrayList<ComparableHandle<? extends ProcessNodeInstance<T>>> threads = new ArrayList<>(mThreads);
    if (threads.size() == 0) {
      throw new IllegalStateException("No starting nodes in process");
    }
    mInputs = mProcessModel.toInputs(payload);
    for (final Handle<? extends ProcessNodeInstance<T>> hnode : threads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      provideTask(transaction, messageService, node);
    }
    mState = State.STARTED;
    mEngine.updateStorage(transaction, this);
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance. */
  public void reinitialize(T transaction) {
    // TODO Auto-generated method stub

  }

  public synchronized void provideTask(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.provideTask(transaction, messageService)) {
      takeTask(transaction, messageService, node);
    }
  }

  public synchronized void takeTask(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.takeTask(transaction, messageService)) {
      startTask(transaction, messageService, node);
    }
  }

  public synchronized void startTask(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.startTask(transaction, messageService)) {
      finishTask(transaction, messageService, node, null);
    }
  }

  public synchronized void finishTask(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node, final Node resultPayload) throws SQLException {
    if (node.getState() == NodeInstanceState.Complete) {
      throw new IllegalStateException("Task was already complete");
    }
    node.finishTask(transaction, resultPayload);
    // Make sure the finish is recorded.
    transaction.commit();

    handleFinishedState(transaction, messageService, node);

  }

  private synchronized void handleFinishedState(final T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node) throws SQLException {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.getNode() instanceof EndNode) {
        mEndResults.add(node.getHandle());
        mThreads.remove(node);
        finish(transaction);
      } else {
        startSuccessors(transaction, messageService, node);
      }
    } catch (RuntimeException|SQLException e) {
      transaction.rollback();
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e);
    }
  }

  private synchronized void startSuccessors(T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance<T> predecessor) throws SQLException {
    if (! mFinishedNodes.contains(predecessor)) {
      mFinishedNodes.add(predecessor.getHandle());
    }
    mThreads.remove(predecessor);

    final List<ProcessNodeInstance<?>> startedTasks = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    final List<JoinInstance<T>> joinsToEvaluate = new ArrayList<>();
    for (final Identifiable successorNode : predecessor.getNode().getSuccessors()) {
      final ProcessNodeInstance<T> instance = createProcessNodeInstance(transaction, predecessor, mProcessModel.getNode(successorNode));
      final ComparableHandle<? extends ProcessNodeInstance<T>> instanceHandle = instance.getHandle();
      if (instance instanceof JoinInstance) {
        if (! mThreads.contains(instanceHandle)) { mThreads.add(instanceHandle); }
        joinsToEvaluate.add((JoinInstance<T>) instance);
        continue;
      } else {
        mThreads.add(instanceHandle);
        startedTasks.add(instance);
        mEngine.registerNodeInstance(transaction, instance);
      }
    }
    // Commit the registration of the follow up nodes before starting them.
    transaction.commit();
    for (final ProcessNodeInstance task : startedTasks) {
      provideTask(transaction, messageService, task);
    }
    for(final ProcessNodeInstance<T> join:joinsToEvaluate) {
      startTask(transaction, messageService, join);
      if (join.getState().isFinal()) {

      }
    }
  }

  private synchronized ProcessNodeInstance<T> createProcessNodeInstance(T transaction, final ProcessNodeInstance<T> predecessor, final ExecutableProcessNode node) throws SQLException {
    if (node instanceof JoinImpl) {
      final JoinImpl join = (JoinImpl) node;
      return getJoinInstance(transaction, join, predecessor.getHandle());
    } else {
      return new ProcessNodeInstance(node, predecessor.getHandle(), this);
    }
  }

  public synchronized void failTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node, final Throwable cause) throws SQLException {
    node.failTask(transaction, cause);
  }

  public synchronized void cancelTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, T, ProcessNodeInstance<T>> messageService, final ProcessNodeInstance node) throws SQLException {
    node.cancelTask(transaction);
  }

  public synchronized Collection<ProcessNodeInstance<T>> getActivePredecessorsFor(T transaction, final JoinImpl join) throws
          SQLException {
    final ArrayList<ProcessNodeInstance<T>> activePredecesors = new ArrayList<>(Math.min(join.getPredecessors().size(), mThreads.size()));
    for (final Handle<? extends ProcessNodeInstance<T>> hnode : mThreads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      if (node.getNode().isPredecessorOf(join)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance<T>>> getDirectSuccessors(T transaction, final ProcessNodeInstance<T> predecessor) throws SQLException {
    final ArrayList<Handle<? extends ProcessNodeInstance<T>>> result = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    for (final Handle<? extends ProcessNodeInstance<T>> hcandidate : mThreads) {
      ProcessNodeInstance<T> candidate = getEngine().getNodeInstance(transaction, hcandidate, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, candidate, predecessor.getHandle());
    }
    return result;
  }

  private synchronized void addDirectSuccessor(T transaction, final ArrayList<Handle<? extends ProcessNodeInstance<T>>> result, ProcessNodeInstance<T> candidate, final Handle<? extends ProcessNodeInstance<T>> predecessor) throws SQLException {
    // First look for this node, before diving into it's children
    for (final Handle<? extends ProcessNodeInstance<T>> node : candidate.getDirectPredecessors()) {
      if (node.getHandleValue() == predecessor.getHandleValue()) {
        result.add(candidate.getHandle());
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final Handle<? extends ProcessNodeInstance<T>> hnode : candidate.getDirectPredecessors()) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      ProcessNodeInstance<T> node = candidate.getProcessInstance().getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, node, predecessor);
    }
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance<T>>> getActive() {
    return new ArrayList<>(mThreads);
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance<T>>> getFinished() {
    return new ArrayList<>(mFinishedNodes);
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance<T>>> getResults() {
    return new ArrayList<>(mEndResults);
  }

  @Override
  public synchronized void serialize(XmlWriter out) throws XmlException {
    //
    XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX);
    try {
      XmlWriterUtil.writeAttribute(out, "handle", mHandle<0 ? null : Long.toString(mHandle));
      XmlWriterUtil.writeAttribute(out, "name", mName);
      XmlWriterUtil.writeAttribute(out, "processModel", Long.toString(getProcessModel().getHandleValue()));
      XmlWriterUtil.writeAttribute(out, "owner", mOwner.getName());
      XmlWriterUtil.writeAttribute(out, "state", mState.name());

      XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "inputs", null);
      try {
        for(ProcessData input:mInputs) {
          input.serialize(out);
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "inputs", null);
      }

      XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "outputs", null);
      try {
        for(ProcessData output:mOutputs) {
          output.serialize(out);
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "outputs", null);
      }

      try(T transaction = getEngine().startTransaction()) {

        if (mThreads.size() > 0) {
          try {
            XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "active", null);
            for (Handle<? extends ProcessNodeInstance<T>> active : mThreads) {
              writeActiveNodeRef(transaction, out, active);
            }
          } finally {
            out.endTag(Constants.PROCESS_ENGINE_NS, "active", null);
          }
        }
        if (mFinishedNodes.size() > 0) {
          try {
            XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "finished", null);
            for (Handle<? extends ProcessNodeInstance<T>> finished : mFinishedNodes) {
              writeActiveNodeRef(transaction, out, finished);
            }
          } finally {
            out.endTag(Constants.PROCESS_ENGINE_NS, "finished", null);
          }
        }
        if (mEndResults.size() > 0) {
          try {
            XmlWriterUtil.smartStartTag(out, Constants.PROCESS_ENGINE_NS, "endresults", null);
            for (Handle<? extends ProcessNodeInstance<T>> result : mEndResults) {
              writeResultNodeRef(transaction, out, result);
            }
          } finally {
            out.endTag(Constants.PROCESS_ENGINE_NS, "endresults", null);
          }
        }
        transaction.commit();
      } catch (SQLException e) {
        throw new XmlException(e);
      }
    } finally {
      out.endTag(Constants.PROCESS_ENGINE_NS, "processInstance", null);
    }

  }

  private void writeActiveNodeRef(T transaction, XmlWriter out, Handle<? extends ProcessNodeInstance<T>> handleNodeInstance) throws
          XmlException, SQLException {
    ProcessNodeInstance nodeInstance = getEngine().getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL);
    out.startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    try {
      writeNodeRefCommon(out, nodeInstance);
    } finally{
      out.endTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    }
  }

  private void writeResultNodeRef(T transaction, XmlWriter out, Handle<? extends ProcessNodeInstance<T>> handleNodeInstance) throws
          XmlException, SQLException {
    ProcessNodeInstance nodeInstance = getEngine().getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL);
    out.startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    try {
      writeNodeRefCommon(out, nodeInstance);
      out.startTag(Constants.PROCESS_ENGINE_NS, "results", null);
      try {
        List<ProcessData> results = nodeInstance.getResults();
        for(ProcessData result:results) {
          result.serialize(out);
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "results", null);
      }
    } finally{
      out.endTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    }
  }

  private static void writeNodeRefCommon(XmlWriter out, ProcessNodeInstance<?> nodeInstance) throws XmlException {
    out.attribute(null, "nodeid", null, nodeInstance.getNode().getId());
    out.attribute(null, "handle", null, Long.toString(nodeInstance.getHandleValue()));
    out.attribute(null, "state", null, nodeInstance.getState().toString());
    if (nodeInstance.getState() == NodeInstanceState.Failed) {
      final Throwable failureCause = nodeInstance.getFailureCause();
      final String value = failureCause==null? "<unknown>" : failureCause.getClass().getName()+": "+failureCause.getMessage();
      out.attribute(null, "failureCause", null, value);
    }

  }

  void setInputs(List<ProcessData> inputs) {
    mInputs.clear();
    mInputs.addAll(inputs);
  }

  public void setOutputs(List<ProcessData> outputs) {
    mOutputs.clear();
    mOutputs.addAll(outputs);
  }

  /**
   * Trigger the instance to reactivate pending tasks.
   * @param transaction The database transaction to use
   * @param messageService The message service to use for messenging.
   */
  public void tickle(T transaction, IMessageService<?, T, ProcessNodeInstance<T>> messageService) {
    List<ComparableHandle<? extends ProcessNodeInstance<T>>> threads = new ArrayList<>(mThreads); // make a copy as the list may be changed due to tickling.
    for(Handle<? extends ProcessNodeInstance<T>> handle: threads) {
      try {
        getEngine().tickleNode(transaction, handle);
        ProcessNodeInstance<T> instance = getEngine().getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
        final NodeInstanceState instanceState   = instance.getState();
        if (instanceState.isFinal()) {
          handleFinishedState(transaction, messageService, instance);
        }
      } catch (SQLException e) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when tickling process instance", e);
      }
    }
    if (mThreads.isEmpty()) {
      try {
        finish(transaction);
      } catch (SQLException e) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when trying to finish a process instance as result of tickling", e);
      }
    }
  }

}
