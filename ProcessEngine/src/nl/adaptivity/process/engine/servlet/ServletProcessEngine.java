package nl.adaptivity.process.engine.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
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
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndPointDescriptor;
import nl.adaptivity.messaging.ISendableMessage;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.AsyncMessenger.AsyncFuture;
import nl.adaptivity.process.messaging.AsyncMessenger.CompletionListener;
import nl.adaptivity.process.messaging.EndpointServlet;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.XmlUtil;
import nl.adaptivity.util.activation.Sources;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ServletProcessEngine extends EndpointServlet implements IMessageService<ServletProcessEngine.ServletMessage, ProcessNodeInstance>, CompletionListener, GenericEndpoint {

  private static final long serialVersionUID = -6277449163953383974L;

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";
  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";
  public static final URI MODIFY_NS = URI.create("http://adaptivity.nl/ProcessEngine/activity");
  public static final QName SERVICE_QNAME = new QName(PROCESS_ENGINE_NS,"ProcessEngine");

  static class ServletMessage implements ISendableMessage{

    private final QName aRemoteService;
    private final String aRemoteEndpoint;
    private final QName aOperation;
    private Source aBody;
    private final String aMethod;
    private final String aUrl;
    private final String aContentType;
    private final EndPointDescriptor aReplies;

    private ServletMessage(XmlMessage pSource, EndPointDescriptor pReplies) {
      this(pSource.getService(), pSource.getEndpoint(), pSource.getOperation(), pSource.getBodySource(), pSource.getMethod(), pSource.getUrl(), pSource.getType(), pReplies);
    }


    private ServletMessage(QName pService, String pEndpoint, QName pOperation, Source pBody, String pMethod, String pUrl, String pContentType, EndPointDescriptor pReplies) {
      aRemoteService = pService;
      aRemoteEndpoint = pEndpoint;
      aOperation = pOperation;
      aBody = pBody;
      aMethod = pMethod;
      aUrl = pUrl;
      aContentType = pContentType;
      aReplies = pReplies;
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


    public void setHandle(long pHandle, Principal pOwner) {
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
                writeAttribute(xer, attributes, xew, pHandle, pOwner);
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
              try {
                xew.add(event);
              } catch (IllegalStateException e) {
                StringBuilder errorMessage= new StringBuilder("Error adding event: ");
                errorMessage.append(event.toString()).append(' ');
                if (event.isStartElement()) {
                  errorMessage.append('<').append(event.asStartElement().getName()).append('>');
                } else if (event.isEndElement()) {
                  errorMessage.append("</").append(event.asEndElement().getName()).append('>');
                }
                getLogger().log(Level.WARNING, errorMessage.toString(), e);
                throw e;
              }
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
          // TODO Why can't we use STAX?
          QName qname1 = new QName(MY_JBI_NS, "endpointDescriptor", "");
          List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", MY_JBI_NS));
          out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

          {
            out.add(xef.createAttribute("serviceNS", aReplies.getServiceName().getNamespaceURI()));
            out.add(xef.createAttribute("serviceLocalName", aReplies.getServiceName().getLocalPart()));
            out.add(xef.createAttribute("endpointName", aReplies.getEndpointName()));
            out.add(xef.createAttribute("endpointLocation", aReplies.getEndpointLocationString()));
          }

          out.add(xef.createEndElement(qname1, namespaces.iterator()));


//          JAXBContext jaxbContext;
//          try {
//            jaxbContext = JAXBContext.newInstance(EndPointDescriptor.class);
//            Marshaller marshaller = jaxbContext.createMarshaller();
//            StringWriter outBuffer = new StringWriter();
//            marshaller.marshal(aReplies, outBuffer);
//            XMLInputFactory xif = XMLInputFactory.newFactory();
//            out.add(xif.createXMLEventReader(new StringReader(outBuffer.toString())));
//          } catch (JAXBException e) {
//            throw new MyMessagingException(e);
//          }
        }
      } else {
        throw new MyMessagingException("Missing parameter name");
      }

    }

    private void writeAttribute(XMLEventReader in, Iterator<Attribute> pAttributes, XMLEventWriter out, long pHandle, Principal pOwner) throws XMLStreamException {
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
        } else if ("owner".equals(valueName)) {
          Attribute attr;
          if (paramName !=null) {
            attr = xef.createAttribute(paramName, pOwner.getName());
          } else {
            attr = xef.createAttribute("owner", pOwner.getName());
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
  private ProcessEngine aProcessEngine;

  private AsyncMessenger aMessagingService;

  private EndPointDescriptor aLocalEndPoint;

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
    String hostname = pConfig.getInitParameter("hostname");
    String port = pConfig.getInitParameter("port");
    {
      URI localURL = null;

      if (hostname==null) { hostname="localhost"; }

      // TODO this should can be done better.
      if (port==null) {
        try {
          Service[] services = ServerFactory.getServer().findServices();

          for (Service service: services) {
            // Loop repeatedly, prefer
            final List<String> protocolList;
            if ("localhost".equals(hostname)) {
              protocolList = Arrays.asList("HTTP/1.1", "org.apache.coyote.http11.Http11NioProtocol");
            } else {
              protocolList = Arrays.asList("HTTP/1.1", "org.apache.coyote.http11.Http11NioProtocol", "AJP/1.3");
            }
            for (String candidateProtocol: protocolList) {
              for(Connector connector: service.findConnectors()) {
                String protocol = connector.getProtocol();
                if (candidateProtocol.equals(protocol)) {
                  if ("localhost".equals(hostname)) {
                    port = Integer.toString(connector.getPort());
                  } else {
                    port = Integer.toString(connector.getProxyPort());
                  }
                }
              }

            }
          }
        } catch (Error e) { // We're not on tomcat, this trick won't work.
          localURL = URI.create("http://"+hostname+"/"+pConfig.getServletContext().getContextPath());
        }
      }
      if (port==null) {
        localURL = URI.create("http://"+hostname+pConfig.getServletContext().getContextPath());
      } else {
        localURL = URI.create("http://"+hostname+":"+port+pConfig.getServletContext().getContextPath());
      }
      aLocalEndPoint=new EndPointDescriptor(getServiceName(), getEndpointName(), localURL);
    }
    aMessagingService = AsyncMessenger.getInstance();

    aMessagingService.addCompletionListener(this);
  }

  /*
   * IMessageService methods
   */

  @Override
  public ServletMessage createMessage(XmlMessage pMessage) {
    return new ServletMessage(pMessage, aLocalEndPoint);
  }

  @Override
  public boolean sendMessage(ServletMessage pMessage, ProcessNodeInstance pInstance) {
    long handle = aProcessEngine.registerMessage(pInstance);
    pMessage.setHandle(handle, pInstance.getProcessInstance().getOwner());

    aMessagingService.sendMessage(pMessage, handle, aLocalEndPoint, pInstance.getProcessInstance().getOwner());
    return true;
  }

  /*
   * Methods inherited from JBIProcessEngine
   */

  static Logger getLogger() {
    Logger logger =  Logger.getLogger(ServletProcessEngine.class.getName());
    logger.setLevel(Level.ALL);
    return logger;
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
  public Collection<? extends ProcessInstanceRef> getProcesInstanceRefs(@RestParam(type=ParamType.PRINCIPAL)Principal pOwner) {
    Iterable<ProcessInstance> processInstances = aProcessEngine.getOwnedProcessInstances(pOwner);
    Collection<ProcessInstanceRef> list = new ArrayList<ProcessInstanceRef>();
    for (ProcessInstance pi: processInstances) {
      list.add(pi.getRef());
    }
    return list;
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels")
  public ProcessModelRefs postProcessModel(@RestParam(name="processUpload", type=ParamType.ATTACHMENT) DataHandler attachment, @RestParam(type=ParamType.PRINCIPAL)Principal pOwner) throws IOException {
    if (pOwner==null) {
      throw new MyMessagingException("Login required");
    }
    XmlProcessModel xmlpm;
    try {
      xmlpm = JAXB.unmarshal(attachment.getInputStream(), XmlProcessModel.class);
    } catch (IOException e) {
      throw e;
    }
    if (xmlpm!=null) {
      final ProcessModel processModel = xmlpm.toProcessModel();
      aProcessEngine.addProcessModel(processModel, pOwner);
    }

    return getProcesModelRefs();
  }

  /**
   * Create a new process instance and start it.
   * @param pHandle The handle of the process to start.
   * @param pName The name that will allow the user to remember the instance. If <code>null</code> a name will be assigned. This name has no semantic meaning.
   * @param pOwner The owner of the process instance. (Who started it).
   * @return
   */
  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}", query={"op=newInstance"})
  public HProcessInstance startProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName, @RestParam(type=ParamType.PRINCIPAL)Principal pOwner) {
    return aProcessEngine.startProcess(pOwner, HandleMap.<ProcessModel>handle(pHandle), pName, null);
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}")
  public void renameProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName, @RestParam(type=ParamType.PRINCIPAL)Principal pUser) {
    aProcessEngine.renameProcessModel(pUser, HandleMap.<ProcessModel>handle(pHandle), pName);
  }

  @WebMethod(operationName="updateTaskState")
  public TaskState updateTaskStateSoap(
                   @WebParam(name = "handle", mode = Mode.IN) long pHandle,
                   @WebParam(name = "state", mode = Mode.IN) TaskState pNewState,
                   @WebParam(name = "user", mode = Mode.IN) String pUser) {
    return updateTaskState(pHandle, pNewState, new SimplePrincipal(pUser));
  }

  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state"})
  public TaskState updateTaskState(
                   @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                   @RestParam(name="state", type=ParamType.QUERY) TaskState pNewState,
                   @RestParam(type=ParamType.PRINCIPAL)Principal pUser) {
    return aProcessEngine.updateTaskState(HandleMap.<ProcessNodeInstance>handle(pHandle), pNewState, pUser);
  }

  @WebMethod(operationName="finishTask")
  public TaskState finishTaskSoap(
                   @WebParam(name="handle",mode=Mode.IN) long pHandle,
                   @WebParam(name="payload", mode=Mode.IN) Node pPayload,
                   @WebParam(name="user", mode=Mode.IN) String pUser) {
    return finishTask(pHandle, pPayload, new SimplePrincipal(pUser));
  }

  @WebMethod(operationName="finishTask")
  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state=Complete"})
  public TaskState finishTask(@WebParam(name="handle",mode=Mode.IN) @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                              @WebParam(name="payload", mode=Mode.IN) @RestParam(name="payload", type=ParamType.QUERY) Node pPayload, @RestParam(type=ParamType.PRINCIPAL)Principal pUser) {
    return aProcessEngine.finishTask(HandleMap.<ProcessNodeInstance>handle(pHandle), pPayload, pUser);
  }


  @RestMethod(method=HttpMethod.GET, path="/processModels/${handle}")
  public XmlProcessModel getProcessModel(@RestParam(name="handle",type=ParamType.VAR) long pHandle, @RestParam(type=ParamType.PRINCIPAL)Principal pUser) throws FileNotFoundException {
    try {
      return new XmlProcessModel(aProcessEngine.getProcessModel(HandleMap.<ProcessModel>handle(pHandle), pUser));
    } catch (NullPointerException e) {
      throw (FileNotFoundException) new FileNotFoundException("Process handle invalid").initCause(e);
    }
  }

  /**
   * Handle the completing of sending a message and receiving some sort of reply. If the
   * reply is an ActivityResponse message we handle that specially.
   */
  @Override
  public void onMessageCompletion(AsyncFuture pFuture) {
    if (pFuture.isCancelled()) {
      aProcessEngine.cancelledTask(pFuture.<ProcessNodeInstance>getHandle(), pFuture.getOwner());
    } else {
      try {
        DataSource result = pFuture.get();
        try {
          Document domResult = XmlUtil.tryParseXml(result.getInputStream());
          Element rootNode = domResult.getDocumentElement();
          // If we are seeing a Soap Envelope, get see if the body has a single value and set that as rootNode for further testing.
          if (Envelope.NAMESPACE.equals(rootNode.getNamespaceURI()) && Envelope.ELEMENTNAME.equals(rootNode.getLocalName())) {
            Element header = XmlUtil.getFirstChild(rootNode, Envelope.NAMESPACE, Header.ELEMENTNAME);
            rootNode = XmlUtil.getFirstChild(header, PROCESS_ENGINE_NS, ActivityResponse.ELEMENTNAME);
          }
          if (rootNode!=null) {
            // If we receive an ActivityResponse, treat that specially.
            if (PROCESS_ENGINE_NS.equals(rootNode.getNamespaceURI()) && ActivityResponse.ELEMENTNAME.equals(rootNode.getLocalName())) {
              String taskStateAttr = rootNode.getAttribute(ActivityResponse.ATTRTASKSTATE);
              try {
                TaskState taskState = TaskState.valueOf(taskStateAttr);
                aProcessEngine.updateTaskState(pFuture.<ProcessNodeInstance>getHandle(), taskState, pFuture.getOwner());
                return;
              } catch (NullPointerException e) {
                // ignore
              } catch (IllegalArgumentException e) {
                aProcessEngine.errorTask(pFuture.<ProcessNodeInstance>getHandle(), e, pFuture.getOwner());
              }
            }
          }

        } catch (NullPointerException e) {
          // ignore
        } catch (IOException e) {
          // It's not xml or has more than one xml element ignore that and fall back to handling unknown services
        }


        // By default assume that we have finished the task
        aProcessEngine.finishedTask(pFuture.<ProcessNodeInstance>getHandle(), result, pFuture.getOwner());
      } catch (ExecutionException e) {
        aProcessEngine.errorTask(pFuture.<ProcessNodeInstance>getHandle(), e.getCause(), pFuture.getOwner());
      } catch (InterruptedException e) {
        aProcessEngine.cancelledTask(pFuture.<ProcessNodeInstance>getHandle(), pFuture.getOwner());
      }
    }
  }

  @Override
  public QName getServiceName() {
    return new QName(PROCESS_ENGINE_NS, "ProcessEngine");
  }

  @Override
  public String getEndpointName() {
    return "soap";
  }

  @Override
  public URI getEndpointLocation() {
    return null;
  }

  @Override
  public void initEndpoint(ServletConfig pConfig) {
    // We know our config, don't do anything.
  }



}
