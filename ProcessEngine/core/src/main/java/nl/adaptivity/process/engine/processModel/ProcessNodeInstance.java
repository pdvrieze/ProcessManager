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

package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.ComparableHandle;
import net.devrieze.util.Handle;
import net.devrieze.util.Handles;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.PETransformer;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.Namespace;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import java.io.CharArrayWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlDeserializer(ProcessNodeInstance.Factory.class)
public class ProcessNodeInstance<T extends Transaction> implements IProcessNodeInstance<T, ProcessNodeInstance<T>>, SecureObject {

  public static class Factory implements XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Override
    public XmlProcessNodeInstance deserialize(final XmlReader reader) throws XmlException {
      return XmlProcessNodeInstance.deserialize(reader);
    }
  }


  private final ExecutableProcessNode mNode;

  private final List<ProcessData> mResults = new ArrayList<>();

  private List<ComparableHandle<? extends ProcessNodeInstance<T>>> mPredecessors;

  private NodeInstanceState mState = NodeInstanceState.Pending;

  private long mHandle = -1;

  private final ProcessInstance mProcessInstance;

  private Throwable mFailureCause;

  public ProcessNodeInstance(final ExecutableProcessNode node, final ComparableHandle<? extends ProcessNodeInstance<T>> predecessor, final ProcessInstance<T> processInstance) {
    super();
    mNode = node;
    if (predecessor==null) {
      if (node instanceof StartNode) {
        mPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      mPredecessors = Collections.<ComparableHandle<? extends ProcessNodeInstance<T>>>singletonList(predecessor);
    }
    mProcessInstance = processInstance;
    if ((predecessor == null) && !(node instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ExecutableProcessNode node, final Collection<? extends ComparableHandle<? extends ProcessNodeInstance<T>>> predecessors, final ProcessInstance processInstance) {
    super();
    mNode = node;
    mPredecessors = new ArrayList<>(predecessors);
    mProcessInstance = processInstance;
    if (((mPredecessors == null) || (mPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  protected ProcessNodeInstance(final ExecutableProcessNode node, final Collection<? extends ComparableHandle<? extends ProcessNodeInstance<T>>> predecessors, final ProcessInstance processInstance, final NodeInstanceState state) {
    super();
    mNode = node;
    mPredecessors = new ArrayList<>(predecessors);
    mProcessInstance = processInstance;
    mState = state;
    if (((mPredecessors == null) || (mPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(final T transaction, final ExecutableProcessNode node, final ProcessInstance<T> processInstance, final NodeInstanceState state) throws SQLException {
    mNode = node;
    mProcessInstance = processInstance;
    mState = state;
    mPredecessors = resolvePredecessors(transaction, processInstance, node);
    if (((mPredecessors == null) || (mPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  public ProcessNodeInstance(final T transaction, final ProcessEngine<T> processEngine, final XmlProcessNodeInstance nodeInstance) throws SQLException {
    this(transaction, processEngine.getProcessInstance(transaction, Handles.<ProcessInstance<T>>handle(nodeInstance.getProcessInstance()),SecurityProvider.SYSTEMPRINCIPAL)
                                   .getProcessModel().getNode(nodeInstance.getNodeId()), processEngine.getProcessInstance(transaction, Handles.<ProcessInstance<T>>handle(nodeInstance.getProcessInstance()), SecurityProvider.SYSTEMPRINCIPAL), nodeInstance.getState());
  }

  private List<ComparableHandle<? extends ProcessNodeInstance<T>>> resolvePredecessors(final T transaction, final ProcessInstance<T> processInstance, final ExecutableProcessNode node) throws SQLException {
    final List<ComparableHandle<? extends ProcessNodeInstance<T>>> result = new ArrayList<>();
    for (final Identifiable pred : node.getPredecessors()) {
      final ProcessNodeInstance nodeInstance = processInstance.getNodeInstance(transaction, pred);
      if (nodeInstance!=null) { result.add(nodeInstance.getHandle()); }
    }
    return result;
  }

  public void ensurePredecessor(final ComparableHandle<? extends ProcessNodeInstance<T>> handle) {
    if(! hasDirectPredecessor(handle)) {
      mPredecessors.add(handle);
    }
  }

  public void setFailureCause(final String failureCause) {
    mFailureCause = new Exception(failureCause);
    mFailureCause.setStackTrace(new StackTraceElement[0]); // wipe the stacktrace, it is irrelevant
  }

  public static ProcessNodeInstance deserialize(final Transaction transaction, final ProcessEngine processEngine, final XmlReader in) throws XmlException {
    try {
      return new ProcessNodeInstance(transaction, processEngine, XmlProcessNodeInstance.deserialize(in));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void tickle(final T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    switch (getState()) {
      case FailRetry:
      case Pending:
        mProcessInstance.provideTask(transaction, messageService, this);
        break;
      default:
        // ignore
    }
  }

  public ExecutableProcessNode getNode() {
    return mNode;
  }

  public List<ProcessData> getResults() {
    return mResults;
  }

  @Override
  public ProcessData getResult(final Transaction transaction, final String name) throws SQLException {
    for(final ProcessData result:getResults()) {
      if (name.equals(result.getName())) {
        return result;
      }
    }
    return null;
  }

  public List<ProcessData> getDefines(final T transaction) throws SQLException {
    final ArrayList<ProcessData> result = new ArrayList<>();
    for(final IXmlDefineType define: mNode.getDefines()) {
      final ProcessData data = define.apply(transaction, this);
      result.add(data);
    }
    return result;
  }

  private boolean hasDirectPredecessor(final Handle<? extends ProcessNodeInstance<?>> handle) {
    for (final Handle<? extends ProcessNodeInstance<?>> pred : mPredecessors) {
      if (pred.getHandleValue() == handle.getHandleValue()) {
        return true;
      }
    }
    return false;
  }

  public Collection<ComparableHandle<? extends ProcessNodeInstance<T>>> getDirectPredecessors() {
    return mPredecessors;
  }

  public Collection<? extends ProcessNodeInstance<T>> resolvePredecessors(final T transaction) throws SQLException {
    final List<ProcessNodeInstance<T>> result = new ArrayList<>(mPredecessors.size());
    for (int i = 0; i < mPredecessors.size(); i++) {
      final Handle<? extends ProcessNodeInstance<T>> handle       = mPredecessors.get(i);
      final ProcessNodeInstance<T>                nodeInstance = getProcessInstance().getEngine().getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
      mPredecessors.set(i, nodeInstance.getHandle());
      result.add(nodeInstance);
    }
    return result;
  }

  public void setDirectPredecessors(final T transaction, final Collection<? extends ComparableHandle<? extends ProcessNodeInstance<T>>> predecessors) throws SQLException {
    if (predecessors==null || predecessors.isEmpty()) {
      mPredecessors = Collections.emptyList();
    } else {
      mPredecessors = new ArrayList<>(predecessors);
    }
    mProcessInstance.getEngine().updateStorage(transaction, this);
  }

  public Handle<? extends ProcessNodeInstance<T>> getPredecessor(final T transaction, final String nodeName) throws SQLException {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for(final Handle<? extends ProcessNodeInstance<T>> hpred: mPredecessors) {
      final ProcessNodeInstance<T> instance = getProcessInstance().getEngine().getNodeInstance(transaction, hpred, SecurityProvider.SYSTEMPRINCIPAL);
      if (nodeName.equals(instance.getNode().getId())) {
        return instance.getHandle();
      } else {
        final Handle<? extends ProcessNodeInstance<T>> result = instance.getPredecessor(transaction, nodeName);
        if (result!=null) { return result; }
      }
    }
    return null;
  }

  @Override
  public ProcessNodeInstance<T> resolvePredecessor(final T transaction, final String nodeName) throws SQLException {
    Handle<? extends ProcessNodeInstance<T>> handle = getPredecessor(transaction, nodeName);
    return getProcessInstance().getEngine().getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
  }

  public Throwable getFailureCause() {
    return mFailureCause;
  }

  @Override
  public NodeInstanceState getState() {
    return mState;
  }

  @Override
  public void setState(final Transaction transaction, final NodeInstanceState newState) throws SQLException {
    if ((mState != null) && (mState.compareTo(newState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + mState + " new:" + newState);
    }
    mState = newState;
    mProcessInstance.getEngine().updateStorage(transaction, this);
  }

  public long getHandleValue() {
    return mHandle;
  }

  @Override
  public void setHandleValue(final long handleValue) {
    mHandle = handleValue;
  }

  @Override
  public ComparableHandle<? extends ProcessNodeInstance<T>> getHandle() {
    return Handles.handle(mHandle);
  }

  @Override
  public <U> boolean provideTask(final T transaction, final IMessageService<U, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    try {
      final boolean result = mNode.provideTask(transaction, messageService, this);
      if (result) { // the task must be automatically taken. Mostly this is false and we don't set the state.
        setState(transaction, NodeInstanceState.Sent);
      }
      return result;
    } catch (RuntimeException e) {
      // TODO later move failretry to fail
//      if (mState!=TaskState.FailRetry) {
        failTaskCreation(transaction, e);
//      }
      throw e;
    }
  }

  @Override
  public <U> boolean takeTask(final T transaction, final IMessageService<U, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    final boolean result = mNode.takeTask(messageService, this);
    setState(transaction, NodeInstanceState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(final T transaction, final IMessageService<U, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    final boolean startTask = mNode.startTask(messageService, this);
    setState(transaction, NodeInstanceState.Started);
    return startTask;
  }

  @Override
  public void finishTask(final Transaction transaction, final Node resultPayload) throws SQLException {
    for(final IXmlResultType resultType: (Collection<? extends IXmlResultType>) getNode().getResults()) {
      mResults.add(resultType.apply(resultPayload));
    } //TODO ensure this is stored
    setState(transaction, NodeInstanceState.Complete);// This triggers a database store. So do it after setting the results
  }

  @Override
  public void cancelTask(final Transaction transaction) throws SQLException {
    setState(transaction, NodeInstanceState.Cancelled);
  }

  @Override
  public void tryCancelTask(final T transaction) throws SQLException {
    try {
      setState(transaction, NodeInstanceState.Cancelled);
    } catch (IllegalArgumentException e) {
      getLogger().log(Level.WARNING, "Task could not be cancelled");
    }
  }

  @Override
  public String toString() {
    return mNode.getClass().getSimpleName() + " (" + mState + ")";
  }

  public ProcessInstance<T> getProcessInstance() {
    return mProcessInstance;
  }

  @Override
  public void failTask(final Transaction transaction, final Throwable cause) throws SQLException {
    mFailureCause = cause;
    setState(transaction, mState == NodeInstanceState.Pending ? NodeInstanceState.FailRetry : NodeInstanceState.Failed);
  }

  @Override
  public void failTaskCreation(final Transaction transaction, final Throwable cause) throws SQLException {
    mFailureCause = cause;
    setState(transaction, NodeInstanceState.FailRetry);
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param results the new results.
   */
  void setResult(final List<ProcessData> results) {
    mResults.clear();
    mResults.addAll(results);
  }

  public void instantiateXmlPlaceholders(final T transaction, final Source source, final Result result) throws
          SQLException, XmlException {
    instantiateXmlPlaceholders(transaction, source, true);
  }

  public void instantiateXmlPlaceholders(final T transaction, final XmlReader in, final XmlWriter out, final boolean removeWhitespace) throws
          XmlException, SQLException {
    final List<ProcessData> defines     = getDefines(transaction);
    final PETransformer     transformer = PETransformer.create(new ProcessNodeInstanceContext(this, defines, mState == NodeInstanceState.Complete), removeWhitespace);
    transformer.transform(in, XmlWriterUtil.filterSubstream(out));
  }

  public CompactFragment instantiateXmlPlaceholders(final T transaction, final Source source, final boolean removeWhitespace) throws
          SQLException, XmlException {
    final XmlReader in = XmlStreaming.newReader(source);
    return instantiateXmlPlaceholders(transaction, in, removeWhitespace);
  }

  @NotNull
  public WritableCompactFragment instantiateXmlPlaceholders(final T transaction, final XmlReader in, final boolean removeWhitespace) throws
          XmlException, SQLException {
    final CharArrayWriter caw = new CharArrayWriter();

    final XmlWriter writer = XmlStreaming.newWriter(caw, true);
    instantiateXmlPlaceholders(transaction, in, writer, removeWhitespace);
    writer.close();
    return new WritableCompactFragment(Collections.<Namespace>emptyList(), caw.toCharArray());
  }

  private static Logger getLogger() {
    final Logger logger = Logger.getLogger(ProcessNodeInstance.class.getName());
    return logger;
  }

  public XmlProcessNodeInstance toSerializable(final T transaction) throws SQLException, XmlException {
    final XmlProcessNodeInstance xmlNodeInst = new XmlProcessNodeInstance();
    xmlNodeInst.setState(mState);
    xmlNodeInst.setHandle(mHandle);

    if (mNode instanceof Activity) {
      final Activity<?, ?> act     = (Activity<?, ?>) mNode;
      final IXmlMessage    message = act.getMessage();
      try {
        final XmlReader in = XMLFragmentStreamReader.from(message.getMessageBody());
        xmlNodeInst.setBody(instantiateXmlPlaceholders(transaction, in, true));
      } catch (XmlException e) {
        getLogger().log(Level.WARNING, "Error processing body", e);
        throw new RuntimeException(e);
      }
    }

    xmlNodeInst.setProcessInstance(mProcessInstance.getHandleValue());

    xmlNodeInst.setNodeId(mNode.getId());

    if (mPredecessors!=null && mPredecessors.size()>0) {
      final List<Handle<? extends IProcessNodeInstance<?,?>>> predecessors = xmlNodeInst.getPredecessors();
      predecessors.addAll(mPredecessors);
    }

    xmlNodeInst.setResults(getResults());

    return xmlNodeInst;
  }

  @Override
  public void serialize(final T transaction, final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, XmlProcessNodeInstance.ELEMENTNAME);
    if (mState!=null) {
      XmlWriterUtil.writeAttribute(out, "state", mState.name());
    }
    if (mProcessInstance!=null) {
      XmlWriterUtil.writeAttribute(out, "processinstance", mProcessInstance.getHandle());
    }
    if (mHandle!=-1) {
      XmlWriterUtil.writeAttribute(out, "handle", mHandle);
    }
    if (mNode!=null) {
      XmlWriterUtil.writeAttribute(out, "nodeid", mNode.getId());
    }
    if (mPredecessors!=null) {
      for (final Handle<? extends ProcessNodeInstance> predecessor: mPredecessors) {
        XmlWriterUtil.writeSimpleElement(out, XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, Long.toString(predecessor.getHandleValue()));
      }
    }
    if (mResults!=null) {
      for (final ProcessData result: mResults) {
        result.serialize(out);
      }
    }
    if (mNode instanceof Activity) {
      XmlWriterUtil.smartStartTag(out, XmlProcessNodeInstance.BODY_ELEMENTNAME);
      final XmlReader in = XMLFragmentStreamReader.from(((Activity) mNode).getMessage().getMessageBody());
      try {
        instantiateXmlPlaceholders(transaction, in, out, true);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      XmlWriterUtil.endTag(out, XmlProcessNodeInstance.BODY_ELEMENTNAME);
    }
    XmlWriterUtil.endTag(out, XmlProcessNodeInstance.ELEMENTNAME);
  }

}
