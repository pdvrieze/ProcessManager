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

import net.devrieze.util.HandleMap.Handle;
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
public class ProcessNodeInstance implements IProcessNodeInstance<ProcessNodeInstance>, SecureObject {

  public static class Factory implements XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Override
    public XmlProcessNodeInstance deserialize(final XmlReader in) throws XmlException {
      return XmlProcessNodeInstance.deserialize(in);
    }
  }


  private final ExecutableProcessNode mNode;

  private final List<ProcessData> mResults = new ArrayList<>();

  private Collection<Handle<? extends ProcessNodeInstance>> mPredecessors;

  private NodeInstanceState mState = NodeInstanceState.Pending;

  private long mHandle = -1;

  private final ProcessInstance mProcessInstance;

  private Throwable mFailureCause;

  public ProcessNodeInstance(final ExecutableProcessNode node, final Handle<? extends ProcessNodeInstance> predecessor, final ProcessInstance processInstance) {
    super();
    mNode = node;
    if (predecessor==null) {
      if (node instanceof StartNode) {
        mPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      mPredecessors = Collections.<Handle<? extends ProcessNodeInstance>>singletonList(predecessor);
    }
    mProcessInstance = processInstance;
    if ((predecessor == null) && !(node instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ExecutableProcessNode node, final Collection<? extends Handle<? extends ProcessNodeInstance>> predecessors, final ProcessInstance processInstance) {
    super();
    mNode = node;
    mPredecessors = new ArrayList<>(predecessors);
    mProcessInstance = processInstance;
    if (((mPredecessors == null) || (mPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(final Transaction transaction, ExecutableProcessNode node, ProcessInstance processInstance, NodeInstanceState state) throws SQLException {
    mNode = node;
    mProcessInstance = processInstance;
    mState = state;
    mPredecessors = resolvePredecessors(transaction, processInstance, node);
    if (((mPredecessors == null) || (mPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  public ProcessNodeInstance(final Transaction transaction, final ProcessEngine processEngine, final XmlProcessNodeInstance nodeInstance) throws SQLException {
    this(transaction, processEngine.getProcessInstance(transaction, Handles.<ProcessInstance>handle(nodeInstance.getProcessInstance()),SecurityProvider.SYSTEMPRINCIPAL)
                                   .getProcessModel().getNode(nodeInstance.getNodeId()), processEngine.getProcessInstance(transaction, Handles.<ProcessInstance>handle(nodeInstance.getProcessInstance()), SecurityProvider.SYSTEMPRINCIPAL), nodeInstance.getState());
  }

  private Collection<Handle<? extends ProcessNodeInstance>> resolvePredecessors(final Transaction transaction, final ProcessInstance processInstance, final ExecutableProcessNode node) throws SQLException {
    List<Handle<? extends ProcessNodeInstance>> result = new ArrayList<>();
    for (Identifiable pred : node.getPredecessors()) {
      result.add(processInstance.getNodeInstance(transaction, pred));
    }
    return result;
  }

  public void setFailureCause(final String failureCause) {
    mFailureCause = new Exception(failureCause);
    mFailureCause.setStackTrace(new StackTraceElement[0]); // wipe the stacktrace, it is irrelevant
  }

  public static ProcessNodeInstance deserialize(final Transaction transaction, final ProcessEngine processEngine, XmlReader in) throws XmlException {
    try {
      return new ProcessNodeInstance(transaction, processEngine, XmlProcessNodeInstance.deserialize(in));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public <T extends Transaction> void tickle(final Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService) {
    try {
      switch (getState()) {
        case FailRetry:
        case Pending:
          mProcessInstance.provideTask(transaction, messageService, this);
          break;
        default:
          // ignore
      }
    } catch (SQLException e) {
      Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when tickling process instance", e);
    }

  }

  public ExecutableProcessNode getNode() {
    return mNode;
  }

  public List<ProcessData> getResults() {
    return mResults;
  }

  @Override
  public ProcessData getResult(Transaction transaction, String name) throws SQLException {
    for(ProcessData result:getResults()) {
      if (name.equals(result.getName())) {
        return result;
      }
    }
    return null;
  }

  public List<ProcessData> getDefines(Transaction transaction) throws SQLException {
    ArrayList<ProcessData> result = new ArrayList<>();
    for(IXmlDefineType define: mNode.getDefines()) {
      ProcessData data = define.apply(transaction, this);
      result.add(data);
    }
    return result;
  }

  public Collection<Handle<? extends ProcessNodeInstance>> getDirectPredecessors() {
    return mPredecessors;
  }

  public void setDirectPredecessors(Collection<Handle<? extends ProcessNodeInstance>> predecessors) {
    if (predecessors==null || predecessors.isEmpty()) {
      mPredecessors = Collections.emptyList();
    } else {
      mPredecessors = new ArrayList<>(predecessors);
    }
  }

  @Override
  public ProcessNodeInstance getPredecessor(Transaction transaction, String nodeName) throws SQLException {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for(Handle<? extends ProcessNodeInstance> hpred: mPredecessors) {
      ProcessNodeInstance instance = getProcessInstance().getEngine().getNodeInstance(transaction, hpred, SecurityProvider.SYSTEMPRINCIPAL);
      if (nodeName.equals(instance.getNode().getId())) {
        return instance;
      } else {
        ProcessNodeInstance result = instance.getPredecessor(transaction, nodeName);
        if (result!=null) { return result; }
      }
    }
    return null;
  }

  public Throwable getFailureCause() {
    return mFailureCause;
  }

  @Override
  public NodeInstanceState getState() {
    return mState;
  }

  @Override
  public void setState(Transaction transaction, final NodeInstanceState newState) throws SQLException {
    if ((mState != null) && (mState.compareTo(newState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + mState + " new:" + newState);
    }
    mState = newState;
    mProcessInstance.getEngine().updateStorage(transaction, this);
  }

  @Override
  public void setHandle(final long handle) {
    mHandle = handle;
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  @Override
  public <U> boolean provideTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
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
  public <U> boolean takeTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    final boolean result = mNode.takeTask(messageService, this);
    setState(transaction, NodeInstanceState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    final boolean startTask = mNode.startTask(messageService, this);
    setState(transaction, NodeInstanceState.Started);
    return startTask;
  }

  @Override
  public void finishTask(Transaction transaction, final Node resultPayload) throws SQLException {
    for(IXmlResultType resultType: (Collection<? extends IXmlResultType>) getNode().getResults()) {
      mResults.add(resultType.apply(resultPayload));
    } //TODO ensure this is stored
    setState(transaction, NodeInstanceState.Complete);// This triggers a database store. So do it after setting the results
  }

  @Override
  public void cancelTask(Transaction transaction) throws SQLException {
    setState(transaction, NodeInstanceState.Cancelled);
  }

  @Override
  public String toString() {
    return mNode.getClass().getSimpleName() + " (" + mState + ")";
  }

  public ProcessInstance getProcessInstance() {
    return mProcessInstance;
  }

  @Override
  public void failTask(Transaction transaction, final Throwable cause) throws SQLException {
    mFailureCause = cause;
    setState(transaction, mState == NodeInstanceState.Pending ? NodeInstanceState.FailRetry : NodeInstanceState.Failed);
  }

  @Override
  public void failTaskCreation(Transaction transaction, final Throwable cause) throws SQLException {
    mFailureCause = cause;
    setState(transaction, NodeInstanceState.FailRetry);
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param results the new results.
   */
  void setResult(List<ProcessData> results) {
    mResults.clear();
    mResults.addAll(results);
  }

  public void instantiateXmlPlaceholders(Transaction transaction, Source source, final Result result) throws
          SQLException, XmlException {
    instantiateXmlPlaceholders(transaction, source, true);
  }

  public void instantiateXmlPlaceholders(final Transaction transaction, final XmlReader in, final XmlWriter out, final boolean removeWhitespace) throws
          XmlException, SQLException {
    List<ProcessData> defines = getDefines(transaction);
    PETransformer transformer = PETransformer.create(new ProcessNodeInstanceContext(this, defines, mState == NodeInstanceState.Complete), removeWhitespace);
    transformer.transform(in, XmlWriterUtil.filterSubstream(out));
  }

  public CompactFragment instantiateXmlPlaceholders(final Transaction transaction, final Source source, final boolean removeWhitespace) throws
          SQLException, XmlException {
    XmlReader in = XmlStreaming.newReader(source);
    return instantiateXmlPlaceholders(transaction, in, removeWhitespace);
  }

  @NotNull
  public WritableCompactFragment instantiateXmlPlaceholders(final Transaction transaction, final XmlReader in, final boolean removeWhitespace) throws
          XmlException, SQLException {
    CharArrayWriter caw = new CharArrayWriter();

    XmlWriter writer = XmlStreaming.newWriter(caw, true);
    instantiateXmlPlaceholders(transaction, in, writer, removeWhitespace);
    writer.close();
    return new WritableCompactFragment(Collections.<Namespace>emptyList(), caw.toCharArray());
  }

  private static Logger getLogger() {
    Logger logger = Logger.getLogger(nl.adaptivity.process.engine.processModel.ProcessNodeInstance.class.getName());
    return logger;
  }

  public XmlProcessNodeInstance toSerializable(final Transaction transaction) throws SQLException, XmlException {
    XmlProcessNodeInstance xmlNodeInst = new XmlProcessNodeInstance();
    xmlNodeInst.setState(mState);
    xmlNodeInst.setHandle(mHandle);

    if (mNode instanceof Activity) {
      Activity<?, ?> act = (Activity<?, ?>) mNode;
      IXmlMessage message = act.getMessage();
      try {
        XmlReader in = XMLFragmentStreamReader.from(message.getMessageBody());
        xmlNodeInst.setBody(instantiateXmlPlaceholders(transaction, in, true));
      } catch (XmlException e) {
        getLogger().log(Level.WARNING, "Error processing body", e);
        throw new RuntimeException(e);
      }
    }

    xmlNodeInst.setProcessInstance(mProcessInstance.getHandle());

    xmlNodeInst.setNodeId(mNode.getId());

    if (mPredecessors!=null && mPredecessors.size()>0) {
      List<Long> predecessors = xmlNodeInst.getPredecessors();
      for(Handle<? extends ProcessNodeInstance> h: mPredecessors) {
        predecessors.add(Long.valueOf(h.getHandle()));
      }
    }

    xmlNodeInst.setResults(getResults());

    return xmlNodeInst;
  }

  public void serialize(final Transaction transaction, final XmlWriter out) throws XmlException {
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
      for (Handle<? extends ProcessNodeInstance> predecessor: mPredecessors) {
        XmlWriterUtil.writeSimpleElement(out, XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, Long.toString(predecessor.getHandle()));
      }
    }
    if (mResults!=null) {
      for (ProcessData result: mResults) {
        result.serialize(out);
      }
    }
    if (mNode instanceof Activity) {
      XmlWriterUtil.smartStartTag(out, XmlProcessNodeInstance.BODY_ELEMENTNAME);
      XmlReader in = XMLFragmentStreamReader.from(((Activity) mNode).getMessage().getMessageBody());
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
