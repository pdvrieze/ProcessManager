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

  public ProcessNodeInstance(final ProcessNodeImpl pNode, final Handle<? extends ProcessNodeInstance> pPredecessor, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    if (pPredecessor==null) {
      if (pNode instanceof StartNode) {
        aPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      aPredecessors = Collections.<Handle<? extends ProcessNodeInstance>>singletonList(pPredecessor);
    }
    aProcessInstance = pProcessInstance;
    if ((pPredecessor == null) && !(pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ProcessNodeImpl pNode, final Collection<? extends Handle<? extends ProcessNodeInstance>> pPredecessors, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    aPredecessors = new ArrayList<>(pPredecessors);
    aProcessInstance = pProcessInstance;
    if (((aPredecessors == null) || (aPredecessors.size()==0)) && !(pNode instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(ProcessNodeImpl pNode, ProcessInstance pProcessInstance, TaskState pState) {
    aNode = pNode;
    aProcessInstance = pProcessInstance;
    aState = pState;
  }

  public <T extends Transaction> void tickle(final Transaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService) {
    try {
      switch (getState()) {
        case FailRetry:
        case Pending:
          aProcessInstance.provideTask(pTransaction, pMessageService, this);
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
  public ProcessData getResult(Transaction pTransaction, String pName) throws SQLException {
    for(ProcessData result:getResults()) {
      if (pName.equals(result.getName())) {
        return result;
      }
    }
    return null;
  }

  public List<ProcessData> getDefines(Transaction pTransaction) throws SQLException {
    ArrayList<ProcessData> result = new ArrayList<>();
    for(IXmlDefineType define: (Collection<? extends IXmlDefineType>) aNode.getDefines()) {
      ProcessData data = define.apply(pTransaction, this);
      result.add(data);
    }
    return result;
  }

  public Collection<Handle<? extends ProcessNodeInstance>> getDirectPredecessors() {
    return aPredecessors;
  }

  public void setDirectPredecessors(Collection<Handle<? extends ProcessNodeInstance>> pPredecessors) {
    if (pPredecessors==null || pPredecessors.isEmpty()) {
      aPredecessors = Collections.emptyList();
    } else {
      aPredecessors = new ArrayList<>(pPredecessors);
    }
  }

  @Override
  public ProcessNodeInstance getPredecessor(Transaction pTransaction, String pNodeName) throws SQLException {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for(Handle<? extends ProcessNodeInstance> hpred: aPredecessors) {
      ProcessNodeInstance instance = getProcessInstance().getEngine().getNodeInstance(pTransaction, hpred, SecurityProvider.SYSTEMPRINCIPAL);
      if (pNodeName.equals(instance.getNode().getId())) {
        return instance;
      } else {
        ProcessNodeInstance result = instance.getPredecessor(pTransaction, pNodeName);
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
  public void setState(Transaction pTransaction, final TaskState pNewState) throws SQLException {
    if ((aState != null) && (aState.compareTo(pNewState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + aState + " new:" + pNewState);
    }
    aState = pNewState;
    aProcessInstance.getEngine().updateStorage(pTransaction, this);
  }

  @Override
  public void setHandle(final long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public <U> boolean provideTask(Transaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    try {
      final boolean result = aNode.provideTask(pTransaction, pMessageService, this);
      if (result) { // the task must be automatically taken. Mostly this is false and we don't set the state.
        setState(pTransaction, TaskState.Sent);
      }
      return result;
    } catch (RuntimeException e) {
      // TODO later move failretry to fail
//      if (aState!=TaskState.FailRetry) {
        failTaskCreation(pTransaction, e);
//      }
      throw e;
    }
  }

  @Override
  public <U> boolean takeTask(Transaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    final boolean result = aNode.takeTask(pMessageService, this);
    setState(pTransaction, TaskState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(Transaction pTransaction, final IMessageService<U, ProcessNodeInstance> pMessageService) throws SQLException {
    final boolean startTask = aNode.startTask(pMessageService, this);
    setState(pTransaction, TaskState.Started);
    return startTask;
  }

  @Override
  public void finishTask(Transaction pTransaction, final Node pResultPayload) throws SQLException {
    for(IXmlResultType resultType: (Collection<? extends IXmlResultType>) getNode().getResults()) {
      aResults.add(resultType.apply(pResultPayload));
    } //TODO ensure this is stored
    setState(pTransaction, TaskState.Complete);// This triggers a database store. So do it after setting the results
  }

  @Override
  public void cancelTask(Transaction pTransaction) throws SQLException {
    setState(pTransaction, TaskState.Cancelled);
  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName() + " (" + aState + ")";
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  @Override
  public void failTask(Transaction pTransaction, final Throwable pCause) throws SQLException {
    aFailureCause = pCause;
    setState(pTransaction, TaskState.Failed);
  }

  @Override
  public void failTaskCreation(Transaction pTransaction, final Throwable pCause) throws SQLException {
    aFailureCause = pCause;
    setState(pTransaction, TaskState.FailRetry);
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param pResults the new results.
   */
  void setResult(List<ProcessData> pResults) {
    aResults.clear();
    aResults.addAll(pResults);
  }

  public void instantiateXmlPlaceholders(Transaction pTransaction, Source source, final Result result) throws XMLStreamException, SQLException {
    instantiateXmlPlaceholders(pTransaction, source, true);
  }

  public CompactFragment instantiateXmlPlaceholders(final Transaction pTransaction, final Source source, final boolean pRemoveWhitespace) throws
          SQLException, XMLStreamException {
    CharArrayWriter caw = new CharArrayWriter();
    List<ProcessData> defines = getDefines(pTransaction);
    PETransformer transformer = PETransformer.create(new ProcessNodeInstanceContext(this, defines, aState== TaskState.Complete), pRemoveWhitespace);
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

  public XmlProcessNodeInstance toXmlNode(Transaction pTransaction) throws SQLException {
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
        instantiateXmlPlaceholders(pTransaction, source, transformResult);
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
