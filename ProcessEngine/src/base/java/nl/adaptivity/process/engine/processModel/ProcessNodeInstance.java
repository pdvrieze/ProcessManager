package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.PETransformer;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.exec.XmlProcessNodeInstance.Body;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.Namespace;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import java.io.CharArrayWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProcessNodeInstance implements IProcessNodeInstance<ProcessNodeInstance>, SecureObject {

  private final ExecutableProcessNode mNode;

  private List<ProcessData> mResults = new ArrayList<>();

  private Collection<Handle<? extends ProcessNodeInstance>> mPredecessors;

  private TaskState mState = TaskState.Pending;

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

  ProcessNodeInstance(ExecutableProcessNode node, ProcessInstance processInstance, TaskState state) {
    mNode = node;
    mProcessInstance = processInstance;
    mState = state;
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
    for(IXmlDefineType define: (Collection<? extends IXmlDefineType>) mNode.getDefines()) {
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
  public TaskState getState() {
    return mState;
  }

  @Override
  public void setState(Transaction transaction, final TaskState newState) throws SQLException {
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
        setState(transaction, TaskState.Sent);
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
    setState(transaction, TaskState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    final boolean startTask = mNode.startTask(messageService, this);
    setState(transaction, TaskState.Started);
    return startTask;
  }

  @Override
  public void finishTask(Transaction transaction, final Node resultPayload) throws SQLException {
    for(IXmlResultType resultType: (Collection<? extends IXmlResultType>) getNode().getResults()) {
      mResults.add(resultType.apply(resultPayload));
    } //TODO ensure this is stored
    setState(transaction, TaskState.Complete);// This triggers a database store. So do it after setting the results
  }

  @Override
  public void cancelTask(Transaction transaction) throws SQLException {
    setState(transaction, TaskState.Cancelled);
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
    setState(transaction, TaskState.Failed);
  }

  @Override
  public void failTaskCreation(Transaction transaction, final Throwable cause) throws SQLException {
    mFailureCause = cause;
    setState(transaction, TaskState.FailRetry);
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
    PETransformer transformer = PETransformer.create(new ProcessNodeInstanceContext(this, defines, mState== TaskState.Complete), removeWhitespace);
    transformer.transform(in, XmlUtil.stripMetatags(out));
  }

  public CompactFragment instantiateXmlPlaceholders(final Transaction transaction, final Source source, final boolean removeWhitespace) throws
          SQLException, XmlException {
    XmlReader in = XmlStreaming.newReader(source);
    return instantiateXmlPlaceholders(transaction, in, removeWhitespace);
  }

  @NotNull
  public CompactFragment instantiateXmlPlaceholders(final Transaction transaction, final XmlReader in, final boolean removeWhitespace) throws
          XmlException, SQLException {
    CharArrayWriter caw = new CharArrayWriter();

    XmlWriter writer = XmlStreaming.newWriter(caw, true);
    instantiateXmlPlaceholders(transaction, in, writer, removeWhitespace);
    writer.close();
    return new CompactFragment(Collections.<Namespace>emptyList(), caw.toCharArray());
  }

  private static Logger getLogger() {
    Logger logger = Logger.getLogger(nl.adaptivity.process.engine.processModel.ProcessNodeInstance.class.getName());
    return logger;
  }

  public XmlProcessNodeInstance toXmlNode(Transaction transaction) throws SQLException {
    XmlProcessNodeInstance xmlNodeInst = new XmlProcessNodeInstance();
    xmlNodeInst.setState(mState);
    xmlNodeInst.setHandle(mHandle);

    if (mNode instanceof Activity) {
      Activity<?, ?> act = (Activity<?, ?>) mNode;
      IXmlMessage message = act.getMessage();
      try {
        XmlReader in = XMLFragmentStreamReader.from(message.getMessageBody());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document document = dbf.newDocumentBuilder().newDocument();
        final DOMResult transformResult = new DOMResult(document);

        XmlWriter out = XmlStreaming.newWriter(transformResult);

        instantiateXmlPlaceholders(transaction, in, out, true /** TODO should this be stripped? */);
        xmlNodeInst.setBody(new Body(document.getDocumentElement())); // XXX don't use DOM anymore.
      } catch (ParserConfigurationException | XmlException e) {
        getLogger().log(Level.WARNING, "Error processing body", e);
        throw new RuntimeException(e);
      }
    }

    xmlNodeInst.setProcessinstance(mProcessInstance.getHandle());

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

}
