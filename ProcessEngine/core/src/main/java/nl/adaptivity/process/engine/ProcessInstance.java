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
import net.devrieze.util.HandleMap.ComparableHandle;
import net.devrieze.util.HandleMap.Handle;
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
import org.w3c.dom.Node;

import javax.xml.XMLConstants;

import java.security.Principal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProcessInstance implements HandleAware<ProcessInstance>, SecureObject, XmlSerializable {

  public enum State {
    NEW,
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED;
  }

  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    private long mHandle;

    private long mProcessModel;

    private String mName;

    private String mUUID;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(final ProcessInstance processInstance) {
      setHandle(processInstance.getHandle());
      setProcessModel(processInstance.mProcessModel.getHandle());
      mName = processInstance.getName();
      if ((mName == null) || (mName.trim().length() == 0)) {
        mName = processInstance.mProcessModel.getName() + " instance " + mHandle;
      }
      mUUID = processInstance.getUUID()==null ? null : processInstance.getUUID().toString();
    }

    public void setHandle(final long handle) {
      mHandle = handle;
    }

    @Override
    public long getHandle() {
      return mHandle;
    }

    public void setProcessModel(final long processModel) {
      mProcessModel = processModel;
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

  private final Collection<Handle<? extends ProcessNodeInstance>> mThreads;

  private final Collection<Handle<? extends ProcessNodeInstance>> mFinishedNodes;

  private final Collection<Handle<? extends ProcessNodeInstance>> mEndResults;

  private HashMap<JoinImpl, JoinInstance> mJoins;

  private long mHandle;

  private final ProcessEngine mEngine;// XXX actually introduce a generic parameter for transactions

  private List<ProcessData> mInputs = new ArrayList<>();

  private List<ProcessData> mOutputs = new ArrayList<>();

  private final String mName;

  private final Principal mOwner;

  private State mState;

  private final UUID mUUid;

  ProcessInstance(final long handle, final Principal owner, final ProcessModelImpl processModel, final String name, final UUID uUid, final State state, final ProcessEngine engine) {
    mHandle = handle;
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

  void setChildren(Transaction transaction, final Collection<? extends Handle<? extends ProcessNodeInstance>> children) throws SQLException {
    mThreads.clear();
    mFinishedNodes.clear();
    mEndResults.clear();

    List<ProcessNodeInstance> nodes = new ArrayList<>();
    TreeMap<ComparableHandle<? extends ProcessNodeInstance>,ProcessNodeInstance> threads = new TreeMap<>();

    for(Handle<? extends ProcessNodeInstance> handle: children) {
      final ProcessNodeInstance inst = mEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      nodes.add(inst);
      threads.put(Handles.handle(handle), inst);
    }

    for(ProcessNodeInstance instance: nodes) {

      if (instance.getNode() instanceof EndNode) {
        mEndResults.add(instance);
        threads.remove(Handles.handle(instance));
      }

      final Collection<Handle<? extends ProcessNodeInstance>> preds = instance.getDirectPredecessors();
      if (preds!=null) {
        for(Handle<? extends ProcessNodeInstance> pred:preds) {
          ComparableHandle<? extends ProcessNodeInstance> handle = Handles.handle(pred);
          if (threads.containsKey(handle)) {
            mFinishedNodes.add(threads.get(handle));
            threads.remove(handle);
          }
        }
      }

    }
    mThreads.addAll(threads.values());
  }

  void setThreads(Transaction transaction, final Collection<? extends Handle<? extends ProcessNodeInstance>> threads) throws SQLException {
    for(Handle<? extends ProcessNodeInstance> threadHeadHandle:threads) {
      mThreads.add(mEngine.getNodeInstance(transaction, threadHeadHandle, SecurityProvider.SYSTEMPRINCIPAL));
    }
  }

  public void initialize(Transaction transaction) throws SQLException {
    if (mState!=State.NEW || mThreads.size()>0) {
      throw new IllegalStateException("The instance already appears to be initialised");
    }
    for (final StartNodeImpl node : mProcessModel.getStartNodes()) {
      final ProcessNodeInstance instance = new ProcessNodeInstance(node, null, this);
      mEngine.registerNodeInstance(transaction, instance);
      mThreads.add(instance);
    }
    mState = State.INITIALIZED;
    mEngine.updateStorage(transaction, this);
  }

  public synchronized void finish(Transaction transaction) throws SQLException {
    int mFinished = getFinishedCount();
    if (mFinished >= mProcessModel.getEndNodeCount()) {
      // TODO mark and store results
      mState=State.FINISHED;
      mEngine.updateStorage(transaction, this);
      transaction.commit();
      mEngine.finishInstance(transaction, this);
    }
  }

  public ProcessNodeInstance getNodeInstance(final Transaction transaction, final Identifiable pred) throws SQLException {
    for (Handle<? extends ProcessNodeInstance> handle: CollectionUtil.combine(mEndResults, mFinishedNodes, mThreads)) {
      ProcessNodeInstance instance = mEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      if (pred.getId().equals(instance.getNode().getId())) {
        return instance;
      }
      ProcessNodeInstance result = instance.getPredecessor(transaction, pred.getId());
      if (result!=null) { return result; }
    }
    return null;
  }

  private int getFinishedCount() {
    return mEndResults.size();
  }

  public synchronized JoinInstance getJoinInstance(Transaction transaction, final JoinImpl join, final ProcessNodeInstance predecessor) throws SQLException {
    JoinInstance result = mJoins.get(join);
    if (result == null) {
      final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(join.getPredecessors().size());
      predecessors.add(predecessor);
      result = new JoinInstance(transaction, join, predecessors, this);
      mJoins.put(join, result);
    } else {
      result.addPredecessor(transaction, predecessor);
    }
    return result;
  }

  public synchronized void removeJoin(final JoinInstance j) {
    mJoins.remove(j.getNode());
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  @Override
  public void setHandle(final long handle) {

    mHandle = handle;
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

  public ProcessEngine getEngine() {
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
  public List<ProcessData> getInputs() {
    return mInputs;
  }

  public synchronized State getState() {
    return mState;
  }

  public synchronized void start(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final Node payload) throws SQLException {
    if (mState==null) {
      initialize(transaction);
    }
    if (mThreads.size() == 0) {
      throw new IllegalStateException("No starting nodes in process");
    }
    mInputs = mProcessModel.toInputs(payload);
    for (final Handle<? extends ProcessNodeInstance> hnode : mThreads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      provideTask(transaction, messageService, node);
    }
    mState = State.STARTED;
    mEngine.updateStorage(transaction, this);
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance. */
  public void reinitialize(Transaction transaction) {
    // TODO Auto-generated method stub

  }

  public synchronized void provideTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.provideTask(transaction, messageService)) {
      takeTask(transaction, messageService, node);
    }
  }

  public synchronized void takeTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.takeTask(transaction, messageService)) {
      startTask(transaction, messageService, node);
    }
  }

  public synchronized void startTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    if (node.startTask(transaction, messageService)) {
      finishTask(transaction, messageService, node, null);
    }
  }

  public synchronized void finishTask(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node, final Node resultPayload) throws SQLException {
    if (node.getState() == NodeInstanceState.Complete) {
      throw new IllegalStateException("Task was already complete");
    }
    node.finishTask(transaction, resultPayload);
    // Make sure the finish is recorded.
    transaction.commit();

    try {
      if (node.getNode() instanceof EndNode) {
        mEndResults.add(node);
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

  private void startSuccessors(Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance predecessor) throws SQLException {
    if (! mFinishedNodes.contains(predecessor)) {
      mFinishedNodes.add(predecessor);
    }
    mThreads.remove(predecessor);

    final List<ProcessNodeInstance> startedTasks = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    for (final Identifiable successorNode : predecessor.getNode().getSuccessors()) {
      final ProcessNodeInstance instance = createProcessNodeInstance(transaction, predecessor, mProcessModel.getNode(successorNode));
      if (instance instanceof JoinInstance && mThreads.contains(instance)) {
        continue;
      } else {
        mThreads.add(instance);
        startedTasks.add(instance);
        mEngine.registerNodeInstance(transaction, instance);
      }
    }
    // Commit the registration of the follow up nodes before starting them.
    transaction.commit();
    for (final ProcessNodeInstance task : startedTasks) {
      provideTask(transaction, messageService, task);
    }
  }

  private ProcessNodeInstance createProcessNodeInstance(Transaction transaction, final ProcessNodeInstance predecessor, final ExecutableProcessNode node) throws SQLException {
    if (node instanceof JoinImpl) {
      final JoinImpl join = (JoinImpl) node;
      if (mJoins == null) {
        mJoins = new HashMap<>();
      }
      JoinInstance instance = mJoins.get(join);
      if (instance == null) {

        final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(node.getPredecessors().size());
        predecessors.add(predecessor);
        instance = new JoinInstance(transaction, join, predecessors, this);
        mJoins.put(join, instance);
      } else {
        instance.addPredecessor(transaction, predecessor);
      }
      return instance;

    } else {
      return new ProcessNodeInstance(node, predecessor, this);
    }
  }

  public synchronized void failTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node, final Throwable cause) throws SQLException {
    node.failTask(transaction, cause);
  }

  public synchronized void cancelTask(Transaction transaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> messageService, final ProcessNodeInstance node) throws SQLException {
    node.cancelTask(transaction);
  }

  public synchronized Collection<ProcessNodeInstance> getActivePredecessorsFor(Transaction transaction, final JoinImpl join) throws
          SQLException {
    final ArrayList<ProcessNodeInstance> activePredecesors = new ArrayList<>(Math.min(join.getPredecessors().size(), mThreads.size()));
    for (final Handle<? extends ProcessNodeInstance> hnode : mThreads) {
      ProcessNodeInstance node = getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      if (node.getNode().isPredecessorOf(join)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance>> getDirectSuccessors(Transaction transaction, final ProcessNodeInstance predecessor) throws SQLException {
    final ArrayList<Handle<? extends ProcessNodeInstance>> result = new ArrayList<>(predecessor.getNode().getSuccessors().size());
    for (final Handle<? extends ProcessNodeInstance> hcandidate : mThreads) {
      ProcessNodeInstance candidate = getEngine().getNodeInstance(transaction, hcandidate, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, candidate, predecessor);
    }
    return result;
  }

  private void addDirectSuccessor(Transaction transaction, final ArrayList<Handle<? extends ProcessNodeInstance>> result, ProcessNodeInstance candidate, final Handle<? extends ProcessNodeInstance> predecessor) throws SQLException {
    // First look for this node, before diving into it's children
    for (final Handle<? extends ProcessNodeInstance> node : candidate.getDirectPredecessors()) {
      if (node.getHandle() == predecessor.getHandle()) {
        result.add(candidate);
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final Handle<? extends ProcessNodeInstance> hnode : candidate.getDirectPredecessors()) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      ProcessNodeInstance node = candidate.getProcessInstance().getEngine().getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(transaction, result, node, predecessor);
    }
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getActive() {
    return mThreads;
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getFinished() {
    return mFinishedNodes;
  }

  public Collection<? extends Handle<? extends ProcessNodeInstance>> getResults() {
    return mEndResults;
  }

  @Override
  public void serialize(XmlWriter out) throws XmlException {
    //
    if(out.getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      out.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    }
    out.startTag(Constants.PROCESS_ENGINE_NS, "processInstance", null);
    try {
      out.attribute(null, "handle", null, Long.toString(mHandle));
      out.attribute(null, "name", null, mName);
      out.attribute(null, "processModel", null, Long.toString(getProcessModel().getHandle()));
      out.attribute(null, "owner", null, mOwner.getName());
      out.attribute(null, "state", null, mState.name());

      out.startTag(Constants.PROCESS_ENGINE_NS, "inputs", null);
      try {
        for(ProcessData input:mInputs) {
          input.serialize(out);
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "inputs", null);
      }

      out.startTag(Constants.PROCESS_ENGINE_NS, "outputs", null);
      try {
        for(ProcessData output:mOutputs) {
          output.serialize(out);
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "outputs", null);
      }

      try(Transaction transaction = getEngine().startTransaction()) {

        if (mThreads.size() > 0) {
          try {
            out.startTag(Constants.PROCESS_ENGINE_NS, "active", null);
            for (Handle<? extends ProcessNodeInstance> active : mThreads) {
              writeActiveNodeRef(transaction, out, active);
            }
          } finally {
            out.endTag(Constants.PROCESS_ENGINE_NS, "active", null);
          }
        }
        if (mFinishedNodes.size() > 0) {
          try {
            out.startTag(Constants.PROCESS_ENGINE_NS, "finished", null);
            for (Handle<? extends ProcessNodeInstance> finished : mFinishedNodes) {
              writeActiveNodeRef(transaction, out, finished);
            }
          } finally {
            out.endTag(Constants.PROCESS_ENGINE_NS, "finished", null);
          }
        }
        if (mEndResults.size() > 0) {
          try {
            out.startTag(Constants.PROCESS_ENGINE_NS, "endresults", null);
            for (Handle<? extends ProcessNodeInstance> result : mEndResults) {
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

  private void writeActiveNodeRef(Transaction transaction, XmlWriter out, Handle<? extends ProcessNodeInstance> handleNodeInstance) throws
          XmlException, SQLException {
    ProcessNodeInstance nodeInstance = getEngine().getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL);
    out.startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    try {
      writeNodeRefCommon(out, nodeInstance);
    } finally{
      out.endTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null);
    }
  }

  private void writeResultNodeRef(Transaction transaction, XmlWriter out, Handle<? extends ProcessNodeInstance> handleNodeInstance) throws
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

  private static void writeNodeRefCommon(XmlWriter out, ProcessNodeInstance nodeInstance) throws XmlException {
    out.attribute(null, "nodeid", null, nodeInstance.getNode().getId());
    out.attribute(null, "handle", null, Long.toString(nodeInstance.getHandle()));
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
  public void tickle(Transaction transaction, IMessageService<?, ProcessNodeInstance> messageService) {
    for(Handle<? extends ProcessNodeInstance> handle: mThreads) {
      try {
        getEngine().tickleNode(transaction, handle);
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
