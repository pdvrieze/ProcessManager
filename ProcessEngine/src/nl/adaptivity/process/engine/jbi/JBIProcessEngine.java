package nl.adaptivity.process.engine.jbi;

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.WebParam.Mode;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.devrieze.util.HandleMap;

import nl.adaptivity.jbi.rest.RestMessageHandler;
import nl.adaptivity.jbi.soap.SoapMessageHandler;
import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam.ParamType;

@WebService(targetNamespace=JBIProcessEngine.PROCESS_ENGINE_NS)
public class JBIProcessEngine implements Component, Runnable, IMessageService<JBIProcessEngine.JBIMessage, ProcessNodeInstance> {

  class JBIMessage {

    private final QName aRemoteService;
    private final String aRemoteEndpoint;
    private final QName aOperation;
    private Source aBody;

    public JBIMessage(QName pService, String pEndpoint, QName pOperation, Source pBody) {
      aRemoteService = pService;
      aRemoteEndpoint = pEndpoint;
      aOperation = pOperation;
      aBody = pBody;
    }


    public QName getService() {
      return aRemoteService;
    }


    public String getEndpoint() {
      return aRemoteEndpoint;
    }


    public QName getOperation() {
      return aOperation;
    }


    public Source getContent() {
      return aBody;
    }


    public void setHandle(long pHandle) throws MessagingException {
      try {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        if (aBody ==null) { throw new NullPointerException(); }
        XMLEventReader xer = xif.createXMLEventReader(aBody);
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db;
        try {
          db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
          throw new MessagingException(e);
        }
        DOMResult result = new DOMResult(db.newDocument());
        XMLEventWriter xew = xof.createXMLEventWriter(result);

        while (xer.hasNext()) {
          XMLEvent event = xer.nextEvent();
          if (event.isStartElement()) {
            StartElement se = event.asStartElement();
            final QName eName = se.getName();
            if (MODIFY_NS.toString().equals(eName.getNamespaceURI())) {
              @SuppressWarnings("unchecked") Iterator<Attribute> attributes = se.getAttributes();
              if (eName.getLocalPart().equals("attribute")) {
                writeAttribute(xer, attributes, xew, pHandle);
              } else if (eName.getLocalPart().equals("element")) {
                writeElement(xer, attributes, xew, pHandle);
              } else {
                throw new MessagingException("Unsupported activity modifier");
              }
            } else {
              xew.add(se);
            }
          } else {
            if (event.isCharacters()) {
              Characters c = event.asCharacters();
              String charData = c.getData();
              int i;
              for(i =0; i<charData.length(); ++i) {
                if (! Character.isWhitespace(charData.charAt(i))) {
                  break;
                }
              }
              if (i==charData.length()) {
                continue; // ignore it, and go to next event
              }
            }

            if (event instanceof Namespace) {

              Namespace ns = (Namespace) event;
              if (! ns.getNamespaceURI().equals(MODIFY_NS)) {
                xew.add(event);
              }
            } else {
              xew.add(event);
            }
          }
        }
        aBody = new DOMSource(result.getNode());

      } catch (FactoryConfigurationError e) {
        throw new MessagingException(e);
      } catch (XMLStreamException e) {
        throw new MessagingException(e);
      }
    }

    private void writeElement(XMLEventReader in, Iterator<Attribute> pAttributes, XMLEventWriter out, long pHandle) throws MessagingException, XMLStreamException {
      String valueName = null;
      {
        while(pAttributes.hasNext()) {
          Attribute attr = pAttributes.next();
          String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          }
        }
      }
      {
        XMLEvent ev = in.nextEvent();

        while (! ev.isEndElement()) {
          if (ev.isStartElement()) { throw new MessagingException("Violation of schema"); }
          if (ev.isAttribute()) {
            Attribute attr = (Attribute) ev;
            String attrName = attr.getName().getLocalPart();
            if ("value".equals(attrName)) {
              valueName = attr.getValue();
            }
          }
        }
      }
      if (valueName!=null) {
        XMLEventFactory xef = XMLEventFactory.newInstance();

        if ("handle".equals(valueName)) {
          out.add(xef.createCharacters(Long.toString(pHandle)));
        } else if ("endpoint".equals(valueName)) {

          QName qname1 = new QName(EndPointDescriptor.MY_JBI_NS, "endpointDescriptor", "");
          List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", EndPointDescriptor.MY_JBI_NS));
          out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

          {
            out.add(xef.createAttribute("serviceNS", aEndPoint.getServiceName().getNamespaceURI()));
            out.add(xef.createAttribute("serviceLocalName", aEndPoint.getServiceName().getLocalPart()));
            out.add(xef.createAttribute("endpointName", aEndPoint.getEndpointName()));
          }

          out.add(xef.createEndElement(qname1, namespaces.iterator()));
        }
      } else {
        throw new MessagingException("Missing parameter name");
      }

    }

    private void writeAttribute(XMLEventReader in, Iterator<Attribute> pAttributes, XMLEventWriter out, long pHandle) throws XMLStreamException, MessagingException {
      String valueName = null;
      String paramName = null;
      {
        while(pAttributes.hasNext()) {
          Attribute attr = pAttributes.next();
          String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          } else if ("name".equals(attrName)) {
            paramName = attr.getValue();
          }
        }
      }
      {
        XMLEvent ev = in.nextEvent();

        while (! ev.isEndElement()) {
          if (ev.isStartElement()) { throw new MessagingException("Violation of schema"); }
          if (ev.isAttribute()) {
            Attribute attr = (Attribute) ev;
            String attrName = attr.getName().getLocalPart();
            if ("value".equals(attrName)) {
              valueName = attr.getValue();
            } else if ("name".equals(attrName)) {
              paramName = attr.getValue();
            }
          }
        }
      }
      if (valueName!=null) {


        XMLEventFactory xef = XMLEventFactory.newInstance();

        if ("handle".equals(valueName)) {
          Attribute attr;
          if (paramName !=null) {
            attr = xef.createAttribute(paramName, Long.toString(pHandle));
          } else {
            attr = xef.createAttribute("handle", Long.toString(pHandle));
          }
          out.add(attr);
        }


      } else {
        throw new MessagingException("Missing parameter name");
      }

    }

  }

  public static final URI MODIFY_NS = URI.create("http://adaptivity.nl/ProcessEngine/activity");
  private static final String OP_POST_MESSAGE = "postMessage";
  private static final String OP_START_PROCESS = "startProcess";
  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";
  public static final QName SERVICE_QNAME = new QName(PROCESS_ENGINE_NS,"ProcessEngine");
  private static final String LOGSUFFIX = null;
  private ComponentContext aContext;
  private ServiceEndpoint aEndPoint;
  private Thread aThread;
  private boolean aKeepRunning = true;
  private Logger aLogger;
  private ProcessEngine aProcessEngine;
  private RestMessageHandler aRestMessageHandler;
  private SoapMessageHandler aSoapMessageHandler;

  @Override
  public ComponentLifeCycle getLifeCycle() {
    return new PELifeCycle(this);
  }

  @Override
  public Document getServiceDescription(ServiceEndpoint pEndpoint) {
    logEntry();
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document result = builder.parse(getClass().getClassLoader().getResourceAsStream("ProcessEngine-jbi.wsdl"));
      return result;
    } catch (ParserConfigurationException e) {
      logError(e);
    } catch (SAXException e) {
      logError(e);
    } catch (IOException e) {
      logError(e);
    }
    return null;
  }

  private void logError(Throwable pE) {
    Logger logger = getLogger();
    if (logger==null) {
      pE.printStackTrace();
      return;
    }


    StackTraceElement[] stackTrace = pE.getStackTrace();

    String className = null;
    String methodName = null;
    if (stackTrace.length>=1) {
      className=stackTrace[0].getClassName();
      methodName=stackTrace[0].getMethodName();
    }
    logger.throwing(className, methodName, pE);

    CharArrayWriter writer = new CharArrayWriter();
    pE.printStackTrace(new PrintWriter(writer));
    logger.warning(writer.toString());

  }

  private void logError(String pMessage) {
    Logger logger = getLogger();
    if (logger==null) {
      System.err.print(pMessage);
      return;
    }

    logger.log(Level.WARNING, pMessage);
  }

  private void logEntry() {
    Logger logger = getLogger();
    if (logger==null) {
      System.err.print("Failing to get logger");
      return;
    }

    if (logger.isLoggable(Level.FINER)) {
      StackTraceElement caller = new Exception().getStackTrace()[1];
      logger.entering(caller.getClassName(), caller.getMethodName());
    }
  }

  Logger getLogger() {
    if (aLogger != null) {
      return aLogger;
    }
    try {
      aLogger = aContext.getLogger(LOGSUFFIX, null);
      aLogger.setLevel(Level.ALL);
      return aLogger;
    } catch (MissingResourceException e1) {
      e1.printStackTrace();
    } catch (JBIException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ServiceUnitManager getServiceUnitManager() {
    // TODO accept process models as service units?
    return null;
  }

  @Override
  public boolean isExchangeWithConsumerOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    logEntry();
    if (pEndpoint.getEndpointName()=="endpoint" && pEndpoint.getServiceName().equals(SERVICE_QNAME)) {
      final String operationName = pExchange.getOperation().getLocalPart();
      return OP_START_PROCESS.equals(operationName) ||
             OP_POST_MESSAGE.equals(operationName) ||
             "GET".equals(operationName) ||
             "POST".equals(operationName) ||
             "PUT".equals(operationName) ||
             "DELETE".equals(operationName) ||
             pExchange.getOperation().getNamespaceURI().equals(PROCESS_ENGINE_NS);

    } else {
      return false;
    }
  }

  @Override
  public boolean isExchangeWithProviderOkay(ServiceEndpoint pEndpoint, MessageExchange pExchange) {
    logEntry();
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public ServiceEndpoint resolveEndpointReference(DocumentFragment pEpr) {
    logError("resolveEndpointReference");
    return null;
  }

  public void setContext(ComponentContext pContext) {
    aContext = pContext;
  }

  ComponentContext getContext() {
    return aContext;
  }

  void startEndPoint() throws JBIException {
    aThread = new Thread(this);
    aThread.start();
    aEndPoint = aContext.activateEndpoint(SERVICE_QNAME, "endpoint");
  }

  void activateEndPoint() {
    logEntry();
  }

  public void run() {
    Logger logger = getLogger();

    DeliveryChannel deliveryChannel;
    try {
      deliveryChannel = aContext.getDeliveryChannel();
    } catch (MessagingException e) {
      logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
      return;
    }

    while (aKeepRunning) {

      try {
        MessageExchange ex = deliveryChannel.accept(1000);
        if (ex!=null) {
          processMessage(deliveryChannel, ex);
        }
      } catch (MessagingException e) {
        if (logger != null) {
          logger.throwing(JBIProcessEngine.class.getCanonicalName(), "run", e);
        } else {
          e.printStackTrace();
        }
      }
    }
  }

  private void processMessage(DeliveryChannel pDeliveryChannel, MessageExchange ex) throws MessagingException {
    logEntry();
    try {
      if (ex.getStatus()==ExchangeStatus.ACTIVE) {
        processRestSoap(pDeliveryChannel, ex);
      }
    } catch (Exception e) {
      logError(e);
      ex.setError(e);
      pDeliveryChannel.send(ex);
    }
  }

  private RestMessageHandler getRestMessageHandler() {
    if (aRestMessageHandler == null) {
      aRestMessageHandler = RestMessageHandler.newInstance();
    }
    return aRestMessageHandler;
  }

  private SoapMessageHandler getSoapMessageHandler() {
    if (aSoapMessageHandler == null) {
      aSoapMessageHandler = SoapMessageHandler.newInstance();
    }
    return aSoapMessageHandler;
  }

  private void processRestSoap(DeliveryChannel pDeliveryChannel, MessageExchange pEx) throws MessagingException{
    NormalizedMessage inMessage = pEx.getMessage("in");
    NormalizedMessage reply = pEx.createMessage();
    if (pEx.getOperation().getNamespaceURI().equals(Constants.WEBMETHOD_NS.toString())) {
      HttpMethod operation = HttpMethod.valueOf(pEx.getOperation().getLocalPart());
      if (getRestMessageHandler().processRequest(operation, inMessage, reply, this)) {
        pEx.setMessage(reply, "out");
        pDeliveryChannel.send(pEx);
      } else {
        pEx.setError(new FileNotFoundException());
        pDeliveryChannel.send(pEx);
      }
    } else {
      if (getSoapMessageHandler().processRequest(pEx.getOperation(), inMessage, reply, this)) {
        if (pEx.getPattern().equals(Constants.WSDL_MEP_IN_ONLY)|| pEx.getPattern().equals(Constants.WSDL_MEP_ROBUST_IN_ONLY)) {
          pEx.setStatus(ExchangeStatus.DONE);
        } else {
          pEx.setMessage(reply, "out");
        }
      } else {
        pEx.setError(new FileNotFoundException());
      }
      pDeliveryChannel.send(pEx);
    }
  }

  @RestMethod(method=HttpMethod.GET, path="/processModels")
  public ProcessModelRefs getProcesModelRefs() {
    Iterable<ProcessModel> processModels = aProcessEngine.getProcessModels();
    ProcessModelRefs list = new ProcessModelRefs();
    for (ProcessModel pm: processModels) {
      list.add(pm.getRef());
    }
    return list;
  }

  @RestMethod(method=HttpMethod.GET, path="/processInstances")
  @XmlElementWrapper(name="processInstances", namespace=PROCESS_ENGINE_NS)
  public Collection<? extends ProcessInstanceRef> getProcesInstanceRefs() {
    Iterable<ProcessInstance> processInstances = aProcessEngine.getInstances();
    Collection<ProcessInstanceRef> list = new ArrayList<ProcessInstanceRef>();
    for (ProcessInstance pi: processInstances) {
      list.add(pi.getRef());
    }
    return list;
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels")
  public ProcessModelRefs postProcessModel(@RestParam(name="processUpload", type=ParamType.ATTACHMENT) DataHandler attachment) throws IOException {

    XmlProcessModel pm;
    try {
      pm = JAXB.unmarshal(attachment.getInputStream(), XmlProcessModel.class);
    } catch (IOException e) {
      throw e;
    }
    if (pm!=null) {
      aProcessEngine.addProcessModel(pm.toProcessModel());
    }

    return getProcesModelRefs();
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}", query={"op=newInstance"})
  public HProcessInstance startProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName) {
    return aProcessEngine.startProcess(HandleMap.<ProcessModel>handle(pHandle), pName, null);
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}", post={"name"})
  public void renameProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName) {
    aProcessEngine.renameProcess(HandleMap.<ProcessModel>handle(pHandle), pName);
  }

  @WebMethod(operationName="updateTaskState")
  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state"})
  public TaskState updateTaskState(@WebParam(name="handle",mode=Mode.IN) @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                              @WebParam(name="state", mode=Mode.IN) @RestParam(name="state", type=ParamType.QUERY) TaskState pNewState) {
    return aProcessEngine.updateTaskState(pHandle, pNewState);
  }

  @WebMethod(operationName="finishTask")
  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state=Complete"})
  public TaskState finishTask(@WebParam(name="handle",mode=Mode.IN) @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                              @WebParam(name="payload", mode=Mode.IN) @RestParam(name="payload", type=ParamType.QUERY) Node pPayload) {
    return aProcessEngine.finishTask(pHandle, pPayload);
  }


  @RestMethod(method=HttpMethod.GET, path="/processModels/${handle}")
  public XmlProcessModel getProcessModel(@RestParam(name="handle",type=ParamType.VAR) long pHandle) throws FileNotFoundException {
    try {
      return new XmlProcessModel(aProcessEngine.getProcessModel(pHandle));
    } catch (NullPointerException e) {
      throw (FileNotFoundException) new FileNotFoundException("Process handle invalid").initCause(e);
    }
  }


  public void stop() throws JBIException {
    aKeepRunning = false;
    aContext.deactivateEndpoint(aEndPoint);
    aThread.interrupt();
  }

  public void startEngine() {
    aProcessEngine = new ProcessEngine(this);
  }

  public ProcessEngine getProcessEngine() {
    return aProcessEngine;
  }

  @Override
  public JBIMessage createMessage(XmlMessage pMessage) {
    return new JBIMessage(pMessage.getService(), pMessage.getEndpoint(), pMessage.getOperation(), pMessage.getBodySource());
  }

  @Override
  public boolean sendMessage(JBIMessage pMessage, ProcessNodeInstance pInstance) {
    try {
      DeliveryChannel deliveryChannel = aContext.getDeliveryChannel();
      ServiceEndpoint se = aContext.getEndpoint(pMessage.getService(), pMessage.getEndpoint());
      MessageExchangeFactory exchangeFactory = deliveryChannel.createExchangeFactory(se);
      RobustInOnly ex = exchangeFactory.createRobustInOnlyExchange();
      ex.setOperation(pMessage.getOperation());
      NormalizedMessage msg = ex.createMessage();


      long handle = aProcessEngine.registerMessage(pInstance);
      pMessage.setHandle(handle);

      msg.setContent(pMessage.getContent());
      ex.setInMessage(msg);
      deliveryChannel.send(ex);
      return true;
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

}
