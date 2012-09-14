package nl.adaptivity.process.engine.servlet;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import net.devrieze.util.HandleMap;
import net.devrieze.util.Tupple;
import net.devrieze.util.Urls;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.AsyncMessenger.AsyncFuture;
import nl.adaptivity.process.messaging.AsyncMessenger.CompletionListener;
import nl.adaptivity.process.messaging.EndpointServlet;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.messaging.ISendableMessage;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.ws.rest.RestMessageHandler;
import nl.adaptivity.ws.soap.SoapMessageHandler;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;


public class ServletProcessEngine extends EndpointServlet implements IMessageService<ServletProcessEngine.ServletMessage, ProcessNodeInstance>, CompletionListener, GenericEndpoint {

  private static final long serialVersionUID = -6277449163953383974L;
  
  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";
  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";
  public static final URI MODIFY_NS = URI.create("http://adaptivity.nl/ProcessEngine/activity");
  public static final QName SERVICE_QNAME = new QName(PROCESS_ENGINE_NS,"ProcessEngine");

  private static final String OP_POST_MESSAGE = "postMessage";
  private static final String OP_START_PROCESS = "startProcess";

  static class ServletMessage implements ISendableMessage{

    private final QName aRemoteService;
    private final String aRemoteEndpoint;
    private final QName aOperation;
    private Source aBody;
    private final String aMethod;
    private final String aUrl;
    private final String aContentType;
    
    private ServletMessage(XmlMessage pSource) {
      this(pSource.getService(), pSource.getEndpoint(), pSource.getOperation(), pSource.getBodySource(), pSource.getMethod(), pSource.getUrl(), pSource.getType());
    }
    
    
    private ServletMessage(QName pService, String pEndpoint, QName pOperation, Source pBody, String pMethod, String pUrl, String pContentType) {
      aRemoteService = pService;
      aRemoteEndpoint = pEndpoint;
      aOperation = pOperation;
      aBody = pBody;
      aMethod = pMethod;
      aUrl = pUrl;
      aContentType = pContentType;
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


    public void setHandle(long pHandle) {
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
          throw new MyMessagingException(e);
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
                throw new MyMessagingException("Unsupported activity modifier");
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
        throw new MyMessagingException(e);
      } catch (XMLStreamException e) {
        throw new MyMessagingException(e);
      }
    }

    private void writeElement(XMLEventReader in, Iterator<Attribute> pAttributes, XMLEventWriter out, long pHandle) throws XMLStreamException {
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
          if (ev.isStartElement()) { throw new MyMessagingException("Violation of schema"); }
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
          // TODO Write our own location.
          QName qname1 = new QName(MY_JBI_NS, "endpointDescriptor", "");
          List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", MY_JBI_NS));
          out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

//          {
//            out.add(xef.createAttribute("serviceNS", aEndPoint.getServiceName().getNamespaceURI()));
//            out.add(xef.createAttribute("serviceLocalName", aEndPoint.getServiceName().getLocalPart()));
//            out.add(xef.createAttribute("endpointName", aEndPoint.getEndpointName()));
//          }

          out.add(xef.createEndElement(qname1, namespaces.iterator()));
        }
      } else {
        throw new MyMessagingException("Missing parameter name");
      }

    }

    private void writeAttribute(XMLEventReader in, Iterator<Attribute> pAttributes, XMLEventWriter out, long pHandle) throws XMLStreamException {
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
          if (ev.isStartElement()) { throw new MyMessagingException("Violation of schema"); }
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
        throw new MyMessagingException("Missing parameter name");
      }

    }


    @Override
    public String getDestination() {
      return aUrl;
    }


    @Override
    public String getMethod() {
      return aMethod;
    }

    @Override
    public boolean hasBody() {
      return aBody!=null;
    }


    @Override
    public Collection<Tupple<String, String>> getHeaders() {
      return Collections.singletonList(Tupple.tupple("Content-Type", aContentType));
    }


    @Override
    public void writeBody(OutputStream pOutputStream) throws IOException {
      try {
        Sources.writeToStream(aBody, pOutputStream);
      } catch (TransformerException e) {
        throw new IOException(e);
      }
    }
    
  }
  private Thread aThread;
  private boolean aKeepRunning = true;
  private Logger aLogger;
  private ProcessEngine aProcessEngine;
  private RestMessageHandler aRestMessageHandler;
  private SoapMessageHandler aSoapMessageHandler;

  private AsyncMessenger aMessagingService;
  
  /*
   * Servlet methods 
   */

  @Override
  protected
  GenericEndpoint getEndpointProvider() {
    return this;
  }
  
  @Override
  public void destroy() {
    aKeepRunning = false;
    if (aThread!=null) {
      aThread.interrupt();
    }
    aMessagingService.destroy();
  }

  @Override
  public String getServletInfo() {
    return "ServletProcessEngine";
  }
  
  @Override
  public void init(ServletConfig pConfig) throws ServletException {
    super.init(pConfig);
    aProcessEngine = new ProcessEngine(this);
    String port = pConfig.getInitParameter("port");
    if (port==null) { 
      aMessagingService = AsyncMessenger.getInstance(Urls.newURL("http://localhost/"+pConfig.getServletContext().getContextPath()));
    } else {
      aMessagingService = AsyncMessenger.getInstance(Urls.newURL("http://localhost:"+port+"/"+pConfig.getServletContext().getContextPath()));
    }
    
    aMessagingService.addCompletionListener(this);
  }

  /*
   * IMessageService methods 
   */

  @Override
  protected void doDelete(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    updatePort(pReq);
    super.doDelete(pReq, pResp);
  }

  private void updatePort(HttpServletRequest pReq) {
    if (aMessagingService.getOwnUrl().getPort()<0) {
      aMessagingService.setOwnPort(pReq.getLocalPort());
    }
  }

  @Override
  protected void doGet(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    updatePort(pReq);
    super.doGet(pReq, pResp);
  }

  @Override
  protected void doHead(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    updatePort(pReq);
    super.doHead(pReq, pResp);
  }

  @Override
  protected void doPost(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    updatePort(pReq);
    super.doPost(pReq, pResp);
  }

  @Override
  protected void doPut(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    updatePort(pReq);
    super.doPut(pReq, pResp);
  }

  @Override
  public ServletMessage createMessage(XmlMessage pMessage) {
    return new ServletMessage(pMessage);
  }

  @Override
  public boolean sendMessage(ServletMessage pMessage, ProcessNodeInstance pInstance) {
    long handle = aProcessEngine.registerMessage(pInstance);
    pMessage.setHandle(handle);
    
    aMessagingService.sendMessage(pMessage, handle);
    return true;
  }

  private RestMessageHandler getRestMessageHandler() {
    if (aRestMessageHandler == null) {
      aRestMessageHandler = RestMessageHandler.newInstance(this);
    }
    return aRestMessageHandler;
  }

  private SoapMessageHandler getSoapMessageHandler() {
    if (aSoapMessageHandler == null) {
      aSoapMessageHandler = SoapMessageHandler.newInstance(this);
    }
    return aSoapMessageHandler;
  }
  
  /*
   * Methods inherited from JBIProcessEngine
   */

  Logger getLogger() {
    if (aLogger != null) {
      return aLogger;
    }
    aLogger = Logger.getLogger(getClass().getName());
    aLogger.setLevel(Level.ALL);
    return aLogger;
  }

  

  /*
   * Web interface for this servlet 
   */


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

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}")
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

  @Override
  public void onMessageCompletion(AsyncFuture pFuture) {
    if (pFuture.isCancelled()) {
      aProcessEngine.cancelledTask(pFuture.getHandle());
    } else {
      try {
        byte[] result = pFuture.get();
        InputSource source = new InputSource(new ByteArrayInputStream(result));
        aProcessEngine.finishedTask(pFuture.getHandle(), source);
      } catch (ExecutionException e) {
        aProcessEngine.errorTask(pFuture.getHandle(), e.getCause()); 
      } catch (InterruptedException e) {
        aProcessEngine.cancelledTask(pFuture.getHandle());
      }
    }
  }

  @Override
  public QName getService() {
    return new QName("http://adaptivity.nl/ProcessEngine/", "ProcessEngine");
  }

  @Override
  public String getEndpoint() {
    return "soap";
  }
  
  

}
