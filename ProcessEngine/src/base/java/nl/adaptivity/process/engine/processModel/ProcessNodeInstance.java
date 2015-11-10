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
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.Namespace;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXResult;

import java.io.CharArrayWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProcessNodeInstance implements IProcessNodeInstance<ProcessNodeInstance>, SecureObject {

  private final ProcessNodeImpl aNode;

  private List<ProcessData> aResults = new ArrayList<>();

  private Collection<Handle<? extends ProcessNodeInstance>> aPredecessors;

  private TaskState aState = TaskState.Pending;

  private long aHandle = -1;

  private final ProcessInstance aProcessInstance;

  private Throwable aFailureCause;

  public ProcessNodeInstance(final ProcessNodeImpl node, final Handle<? extends ProcessNodeInstance> predecessor, final ProcessInstance processInstance) {
    super();
    aNode = node;
    if (predecessor==null) {
      if (node instanceof StartNode) {
        aPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      aPredecessors = Collections.<Handle<? extends ProcessNodeInstance>>singletonList(predecessor);
    }
    aProcessInstance = processInstance;
    if ((predecessor == null) && !(node instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ProcessNodeImpl node, final Collection<? extends Handle<? extends ProcessNodeInstance>> predecessors, final ProcessInstance processInstance) {
    super();
    aNode = node;
    aPredecessors = new ArrayList<>(predecessors);
    aProcessInstance = processInstance;
    if (((aPredecessors == null) || (aPredecessors.size()==0)) && !(node instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(ProcessNodeImpl node, ProcessInstance processInstance, TaskState state) {
    aNode = node;
    aProcessInstance = processInstance;
    aState = state;
  }

  public <T extends Transaction> void tickle(final Transaction transaction, final IMessageService<?, ProcessNodeInstance> messageService) {
    try {
      switch (getState()) {
        case FailRetry:
        case Pending:
          aProcessInstance.provideTask(transaction, messageService, this);
          break;
        default:
          // ignore
      }
    } catch (SQLException e) {
      Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error when tickling process instance", e);
    }

  }

  public ProcessNodeImpl getNode() {
    return aNode;
  }

  public List<ProcessData> getResults() {
    return aResults;
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
    for(IXmlDefineType define: (Collection<? extends IXmlDefineType>) aNode.getDefines()) {
      ProcessData data = define.apply(transaction, this);
      result.add(data);
    }
    return result;
  }

  public Collection<Handle<? extends ProcessNodeInstance>> getDirectPredecessors() {
    return aPredecessors;
  }

  public void setDirectPredecessors(Collection<Handle<? extends ProcessNodeInstance>> predecessors) {
    if (predecessors==null || predecessors.isEmpty()) {
      aPredecessors = Collections.emptyList();
    } else {
      aPredecessors = new ArrayList<>(predecessors);
    }
  }

  @Override
  public ProcessNodeInstance getPredecessor(Transaction transaction, String nodeName) throws SQLException {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for(Handle<? extends ProcessNodeInstance> hpred: aPredecessors) {
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
    return aFailureCause;
  }

  @Override
  public TaskState getState() {
    return aState;
  }

  @Override
  public void setState(Transaction transaction, final TaskState newState) throws SQLException {
    if ((aState != null) && (aState.compareTo(newState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + aState + " new:" + newState);
    }
    aState = newState;
    aProcessInstance.getEngine().updateStorage(transaction, this);
  }

  @Override
  public void setHandle(final long handle) {
    aHandle = handle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public <U> boolean provideTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    try {
      final boolean result = aNode.provideTask(transaction, messageService, this);
      if (result) { // the task must be automatically taken. Mostly this is false and we don't set the state.
        setState(transaction, TaskState.Sent);
      }
      return result;
    } catch (RuntimeException e) {
      // TODO later move failretry to fail
//      if (aState!=TaskState.FailRetry) {
        failTaskCreation(transaction, e);
//      }
      throw e;
    }
  }

  @Override
  public <U> boolean takeTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    final boolean result = aNode.takeTask(messageService, this);
    setState(transaction, TaskState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(Transaction transaction, final IMessageService<U, ProcessNodeInstance> messageService) throws SQLException {
    final boolean startTask = aNode.startTask(messageService, this);
    setState(transaction, TaskState.Started);
    return startTask;
  }

  @Override
  public void finishTask(Transaction transaction, final Node resultPayload) throws SQLException {
    for(IXmlResultType resultType: (Collection<? extends IXmlResultType>) getNode().getResults()) {
      aResults.add(resultType.apply(resultPayload));
    } //TODO ensure this is stored
    setState(transaction, TaskState.Complete);// This triggers a database store. So do it after setting the results
  }

  @Override
  public void cancelTask(Transaction transaction) throws SQLException {
    setState(transaction, TaskState.Cancelled);
  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName() + " (" + aState + ")";
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  @Override
  public void failTask(Transaction transaction, final Throwable cause) throws SQLException {
    aFailureCause = cause;
    setState(transaction, TaskState.Failed);
  }

  @Override
  public void failTaskCreation(Transaction transaction, final Throwable cause) throws SQLException {
    aFailureCause = cause;
    setState(transaction, TaskState.FailRetry);
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param results the new results.
   */
  void setResult(List<ProcessData> results) {
    aResults.clear();
    aResults.addAll(results);
  }

  public void instantiateXmlPlaceholders(Transaction transaction, Source source, final Result result) throws XMLStreamException, SQLException {
    instantiateXmlPlaceholders(transaction, source, true);
  }

  public CompactFragment instantiateXmlPlaceholders(final Transaction transaction, final Source source, final boolean removeWhitespace) throws
          SQLException, XMLStreamException {
    CharArrayWriter caw = new CharArrayWriter();
    List<ProcessData> defines = getDefines(transaction);
    PETransformer transformer = PETransformer.create(new ProcessNodeInstanceContext(this, defines, aState== TaskState.Complete), removeWhitespace);
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    XMLStreamWriter xsw = XmlUtil.stripMetatags(xof.createXMLStreamWriter(caw));
    transformer.transform(source, new StAXResult(xsw));
    xsw.close();
    return new CompactFragment(Collections.<Namespace>emptyList(), caw.toCharArray());
  }

  private static Logger getLogger() {
    Logger logger = Logger.getLogger(nl.adaptivity.process.engine.processModel.ProcessNodeInstance.class.getName());
    return logger;
  }

  public XmlProcessNodeInstance toXmlNode(Transaction transaction) throws SQLException {
    XmlProcessNodeInstance xmlNodeInst = new XmlProcessNodeInstance();
    xmlNodeInst.setState(aState);
    xmlNodeInst.setHandle(aHandle);

    if (aNode instanceof Activity<?>) {
      Activity<?> act = (Activity<?>) aNode;
      IXmlMessage message = act.getMessage();
      Source source = message.getBodySource();

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);

      Document document;
      try {
        document = dbf.newDocumentBuilder().newDocument();
      } catch (ParserConfigurationException e1) {
        throw new RuntimeException(e1);
      }
      final DOMResult transformResult = new DOMResult(document);
      try {
        instantiateXmlPlaceholders(transaction, source, transformResult);
        xmlNodeInst.setBody(new Body(document.getDocumentElement()));
      } catch (XMLStreamException e) {
        getLogger().log(Level.WARNING, "Error processing body", e);
        throw new RuntimeException(e);
      }
    }

    xmlNodeInst.setProcessinstance(aProcessInstance.getHandle());

    xmlNodeInst.setNodeId(aNode.getId());

    if (aPredecessors!=null && aPredecessors.size()>0) {
      List<Long> predecessors = xmlNodeInst.getPredecessors();
      for(Handle<? extends ProcessNodeInstance> h: aPredecessors) {
        predecessors.add(Long.valueOf(h.getHandle()));
      }
    }

    xmlNodeInst.setResults(getResults());

    return xmlNodeInst;
  }

}
