package nl.adaptivity.process.engine.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
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
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Handles;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.PermissionDeniedException;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.Header;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.messaging.ISendableMessage;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.MessagingFormatException;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.EndpointServlet;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.XmlUtil;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ServletProcessEngine extends EndpointServlet implements IMessageService<ServletProcessEngine.NewServletMessage, ProcessNodeInstance>, GenericEndpoint {


  private static final long serialVersionUID = -6277449163953383974L;

  public static final QName SERVICE_QNAME = new QName(Constants.PROCESS_ENGINE_NS, "ProcessEngine");

  private class MessagingCompletionListener implements CompletionListener {

    private final Handle<ProcessNodeInstance> aHandle;

    private final Principal aOwner;

    public MessagingCompletionListener(final Handle<ProcessNodeInstance> pHandle, final Principal pOwner) {
      aHandle = pHandle;
      aOwner = pOwner;
    }

    @Override
    public void onMessageCompletion(final Future<?> pFuture) {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      final Future<DataSource> future = ((Future) pFuture);
      ServletProcessEngine.this.onMessageCompletion(future, aHandle, aOwner);
    }

  }

  static class NewServletMessage implements ISendableMessage, DataSource {

    //    private Endpoint aDestination;
    //    private String aMethod;
    //    private String aContentType;
    private final EndpointDescriptor aLocalEndpoint;

    private ProcessNodeInstance aNodeInstance;

    private IXmlMessage aMessage;

    public NewServletMessage(final IXmlMessage pMessage, final EndpointDescriptor pLocalEndPoint) {
      aMessage = pMessage;
      aLocalEndpoint = pLocalEndPoint;
    }


    private Principal getOwner() {
      return aNodeInstance.getProcessInstance().getOwner();
    }


    private long getHandle() {
      return aNodeInstance.getHandle();
    }

    @Override
    public EndpointDescriptor getDestination() {
      return aMessage.getEndpointDescriptor();
    }

    @Override
    public String getMethod() {
      return aMessage.getMethod();
    }

    @Override
    public Collection<? extends IHeader> getHeaders() {
      final String contentType = aMessage.getContentType();
      if (contentType == null) {
        return Collections.emptyList();
      } else {
        return Collections.<IHeader> singletonList(new Header("Content-type", contentType));
      }
    }

    @Override
    public DataSource getBodySource() {
      return this;
    }

    @Override
    public String getContentType() {
      return aMessage.getContentType();
    }

    public Source getSource() {
      Source messageBody = aMessage.getBodySource();
      if (messageBody==null) {
        throw new NullPointerException();
      }
      if (messageBody instanceof DOMSource) {
        ((DOMSource) messageBody).getNode();
        messageBody = new StreamSource(Sources.toReader(messageBody));
      }
      return messageBody;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      Source messageBody;
      try {
        messageBody = getSource();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(baos);

        aNodeInstance.instantiateXmlPlaceholders(messageBody, result);
        return new ByteArrayInputStream(baos.toByteArray());

      } catch (final FactoryConfigurationError e) {
        throw new MessagingException(e);
      } catch (final XMLStreamException e) {
        throw new MessagingException(e);
      }


    }


    public static void fillInActivityMessage(Source messageBody, final Result result, ProcessNodeInstance pNodeInstance) throws FactoryConfigurationError, XMLStreamException {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLEventReader xer = xif.createXMLEventReader(messageBody);
      final XMLEventWriter xew = xof.createXMLEventWriter(result);

      while (xer.hasNext()) {
        final XMLEvent event = xer.nextEvent();
        if (event.isStartElement()) {
          final StartElement se = event.asStartElement();
          final QName eName = se.getName();
          if (Constants.MODIFY_NS.toString().equals(eName.getNamespaceURI())) {
            @SuppressWarnings("unchecked")
            final Iterator<Attribute> attributes = se.getAttributes();
            if (eName.getLocalPart().equals("attribute")) {
              writeAttribute(pNodeInstance, xer, attributes, xew);
            } else if (eName.getLocalPart().equals("element")) {
              writeElement(pNodeInstance, xer, attributes, xew);
            } else {
//                baos.reset(); baos.close();
              throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported activity modifier");
            }
          } else {
            xew.add(se);
          }
        } else {
          if (event.isCharacters()) {
            final Characters c = event.asCharacters();
            final String charData = c.getData();
            int i;
            for (i = 0; i < charData.length(); ++i) {
              if (!Character.isWhitespace(charData.charAt(i))) {
                break;
              }
            }
            if (i == charData.length()) {
              continue; // ignore it, and go to next event
            }
          }

          if (event instanceof Namespace) {

            final Namespace ns = (Namespace) event;
            if (!ns.getNamespaceURI().equals(Constants.MODIFY_NS)) {
              xew.add(event);
            }
          } else {
            try {
              xew.add(event);
            } catch (final IllegalStateException e) {
              final StringBuilder errorMessage = new StringBuilder("Error adding event: ");
              errorMessage.append(event.toString()).append(' ');
              if (event.isStartElement()) {
                errorMessage.append('<').append(event.asStartElement().getName()).append('>');
              } else if (event.isEndElement()) {
                errorMessage.append("</").append(event.asEndElement().getName()).append('>');
              }
              getLogger().log(Level.WARNING, errorMessage.toString(), e);
//                baos.reset(); baos.close();
              throw e;
            }
          }
        }
      }
    }

    private static void writeElement(ProcessNodeInstance pNodeInstance, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
      String valueName = null;
      {
        while (pAttributes.hasNext()) {
          final Attribute attr = pAttributes.next();
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          }
        }
      }
      {
        final XMLEvent ev = in.nextEvent();

        while (!ev.isEndElement()) {
          if (ev.isStartElement()) {
            throw new MessagingFormatException("Violation of schema");
          }
          if (ev.isAttribute()) {
            final Attribute attr = (Attribute) ev;
            final String attrName = attr.getName().getLocalPart();
            if ("value".equals(attrName)) {
              valueName = attr.getValue();
            }
          }
        }
      }
      if (valueName != null) {
        final XMLEventFactory xef = XMLEventFactory.newInstance();

        if ("handle".equals(valueName)) {
          out.add(xef.createCharacters(Long.toString(pNodeInstance.getHandle())));
        } else if ("endpoint".equals(valueName)) {
          final QName qname1 = new QName(Constants.MY_JBI_NS, "endpointDescriptor", "");
          final List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", Constants.MY_JBI_NS));
          out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

          {
            EndpointDescriptor localEndpoint = pNodeInstance.getProcessInstance().getEngine().getLocalEndpoint();
            out.add(xef.createAttribute("serviceNS", localEndpoint.getServiceName().getNamespaceURI()));
            out.add(xef.createAttribute("serviceLocalName", localEndpoint.getServiceName().getLocalPart()));
            out.add(xef.createAttribute("endpointName", localEndpoint.getEndpointName()));
            out.add(xef.createAttribute("endpointLocation", localEndpoint.getEndpointLocation().toString()));
          }

          out.add(xef.createEndElement(qname1, namespaces.iterator()));
        }
      } else {
        throw new MessagingFormatException("Missing parameter name");
      }

    }

    private static void writeAttribute(ProcessNodeInstance pNodeInstance, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
      String valueName = null;
      String paramName = null;
      {
        while (pAttributes.hasNext()) {
          final Attribute attr = pAttributes.next();
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          } else if ("name".equals(attrName)) {
            paramName = attr.getValue();
          }
        }
      }
      {
        final XMLEvent ev = in.nextEvent();

        while (!ev.isEndElement()) {
          if (ev.isStartElement()) {
            throw new MessagingFormatException("Violation of schema");
          }
          if (ev.isAttribute()) {
            final Attribute attr = (Attribute) ev;
            final String attrName = attr.getName().getLocalPart();
            if ("value".equals(attrName)) {
              valueName = attr.getValue();
            } else if ("name".equals(attrName)) {
              paramName = attr.getValue();
            }
          }
        }
      }
      if (valueName != null) {


        final XMLEventFactory xef = XMLEventFactory.newInstance();

        if ("handle".equals(valueName)) {
          Attribute attr;
          if (paramName != null) {
            attr = xef.createAttribute(paramName, Long.toString(pNodeInstance.getHandle()));
          } else {
            attr = xef.createAttribute("handle", Long.toString(pNodeInstance.getHandle()));
          }
          out.add(attr);
        } else if ("owner".equals(valueName)) {
          Attribute attr;
          if (paramName != null) {
            attr = xef.createAttribute(paramName, pNodeInstance.getProcessInstance().getOwner().getName());
          } else {
            attr = xef.createAttribute("owner", pNodeInstance.getProcessInstance().getOwner().getName());
          }
          out.add(attr);
        } else if ("instancehandle".equals(valueName)) {
          Attribute attr;
          if (paramName != null) {
            attr = xef.createAttribute(paramName, pNodeInstance.getProcessInstance().getOwner().getName());
          } else {
            attr = xef.createAttribute("instancehandle", pNodeInstance.getProcessInstance().getOwner().getName());
          }
          out.add(attr);
        }


      } else {
        throw new MessagingFormatException("Missing parameter name");
      }

    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException("Can not write to messages through a stream");
    }


    public void setHandle(final ProcessNodeInstance pNodeInstance) {
      aNodeInstance = pNodeInstance;
    }

    @Override
    public Map<String, DataSource> getAttachments() {
      return Collections.emptyMap();
    }


  }

  private Thread aThread;

  private ProcessEngine aProcessEngine;

  private EndpointDescriptorImpl aLocalEndPoint;

  /*
   * Servlet methods
   */

  @Override
  protected GenericEndpoint getEndpointProvider() {
    return this;
  }

  @Override
  public void destroy() {
    if (aThread != null) {
      aThread.interrupt();
    }
    MessagingRegistry.getMessenger().shutdown();
  }

  @Override
  public String getServletInfo() {
    return "ServletProcessEngine";
  }

  @Override
  public void init(final ServletConfig pConfig) throws ServletException {
    super.init(pConfig);
    aProcessEngine = new ProcessEngine(this);
    String hostname = pConfig.getInitParameter("hostname");
    String port = pConfig.getInitParameter("port");
    {
      URI localURL = null;

      if (hostname == null) {
        hostname = "localhost";
      }

      // TODO this should can be done better.
      if (port == null) {
        try {
          final Service[] services = ServerFactory.getServer().findServices();

          for (final Service service : services) {
            // Loop repeatedly, prefer
            final List<String> protocolList;
            if ("localhost".equals(hostname)) {
              protocolList = Arrays.asList("HTTP/1.1", "org.apache.coyote.http11.Http11NioProtocol");
            } else {
              protocolList = Arrays.asList("HTTP/1.1", "org.apache.coyote.http11.Http11NioProtocol", "AJP/1.3");
            }
            for (final String candidateProtocol : protocolList) {
              for (final Connector connector : service.findConnectors()) {
                final String protocol = connector.getProtocol();
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
        } catch (final Error e) { // We're not on tomcat, this trick won't work.
          localURL = URI.create("http://" + hostname + "/" + pConfig.getServletContext().getContextPath());
        }
      }
      if (port == null) {
        localURL = URI.create("http://" + hostname + pConfig.getServletContext().getContextPath());
      } else {
        localURL = URI.create("http://" + hostname + ":" + port + pConfig.getServletContext().getContextPath());
      }
      setLocalEndpoint(localURL);
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }



  /*
   * IMessageService methods
   */

  @Override
  public NewServletMessage createMessage(final IXmlMessage pMessage) {
    return new NewServletMessage(pMessage, aLocalEndPoint);
  }

  @Override
  public boolean sendMessage(final NewServletMessage pMessage, final ProcessNodeInstance pInstance) {
    assert pInstance.getHandle()>=0;
    pMessage.setHandle(pInstance);

    MessagingRegistry.sendMessage(pMessage, new MessagingCompletionListener(Handles.<ProcessNodeInstance>handle(pMessage.getHandle()), pMessage.getOwner()), DataSource.class, new Class<?>[0]);
    return true;
  }

  /*
   * Methods inherited from JBIProcessEngine
   */

  @Override
  public EndpointDescriptor getLocalEndpoint() {
    return aLocalEndPoint;
  }

  static Logger getLogger() {
    final Logger logger = Logger.getLogger(ServletProcessEngine.class.getName());
    logger.setLevel(Level.ALL);
    return logger;
  }


  /*
   * Web interface for this servlet
   */


  @RestMethod(method = HttpMethod.GET, path = "/processModels")
  public ProcessModelRefs<?> getProcesModelRefs() {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      final Iterable<ProcessModelImpl> processModels = aProcessEngine.getProcessModels(transaction);
      final ProcessModelRefs<?> list = new ProcessModelRefs<>();
      for (final ProcessModel<?> pm : processModels) {
        list.add(pm.getRef());
      }
      return transaction.commit(list);
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process model references", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @RestMethod(method = HttpMethod.GET, path = "/processModels/${handle}")
  public XmlProcessModel getProcessModel(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) throws FileNotFoundException {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      return transaction.commit(new XmlProcessModel(aProcessEngine.getProcessModel(transaction, Handles.<ProcessModelImpl>handle(pHandle), pUser)));
    } catch (final NullPointerException e) {
      throw (FileNotFoundException) new FileNotFoundException("Process handle invalid").initCause(e);
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process model", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}")
  public ProcessModelRef updateProcessModel(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(name = "processUpload", type = ParamType.ATTACHMENT) final DataHandler attachment, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) throws IOException {
    if (pUser==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    XmlProcessModel xmlpm;
    try {
      xmlpm = JAXB.unmarshal(attachment.getInputStream(), XmlProcessModel.class);
    } catch (final IOException e) {
      throw e;
    }
    if (xmlpm != null) {
      final ProcessModelImpl processModel = xmlpm.toProcessModel();
      if (xmlpm.getNodes().size()!=processModel.getModelNodes().size()) {
        throw new AssertionError("Process model sizes don't match");
      }
      try (DBTransaction transaction = aProcessEngine.startTransaction()){
        return transaction.commit(aProcessEngine.updateProcessModel(transaction, Handles.<ProcessModelImpl>handle(pHandle), processModel, pUser));
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error updating process model", e);
        throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
      }
    }
    throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "The posted process model is not valid");
  }

  @RestMethod(method = HttpMethod.POST, path = "/processModels")
  public ProcessModelRef postProcessModel(@RestParam(name = "processUpload", type = ParamType.ATTACHMENT) final DataHandler attachment, @RestParam(type = ParamType.PRINCIPAL) final Principal pOwner) throws IOException {
    if (pOwner==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    XmlProcessModel xmlpm;
    try {
      xmlpm = JAXB.unmarshal(attachment.getInputStream(), XmlProcessModel.class);
    } catch (final IOException e) {
      throw e;
    }
    if (xmlpm != null) {
      final ProcessModelImpl processModel = xmlpm.toProcessModel();
      if (xmlpm.getNodes().size()!=processModel.getModelNodes().size()) {
        throw new AssertionError("Process model sizes don't match");
      }
      try (DBTransaction transaction = aProcessEngine.startTransaction()){
        return transaction.commit(ProcessModelRef.get(aProcessEngine.addProcessModel(transaction, processModel, pOwner)));
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error adding process model", e);
        throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
      }
    }

    return null;
  }

  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}")
  public void renameProcess(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(name = "name", type = ParamType.QUERY) final String pName, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    aProcessEngine.renameProcessModel(pUser, Handles.<ProcessModelImpl>handle(pHandle), pName);
  }

  @RestMethod(method = HttpMethod.DELETE, path = "/processModels/${handle}")
  public void deleteProcess(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      if (! aProcessEngine.removeProcessModel(transaction, Handles.<ProcessModelImpl>handle(pHandle), pUser)) {
        throw new HttpResponseException(HttpServletResponse.SC_NOT_FOUND, "The given process does not exist");
      }
      transaction.commit();
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error renaming process model", e);
      throw new HttpResponseException(500, e);
    }
  }

  /**
   * Create a new process instance and start it.
   *
   * @param pHandle The handle of the process to start.
   * @param pName The name that will allow the user to remember the instance. If
   *          <code>null</code> a name will be assigned. This name has no
   *          semantic meaning.
   * @param pOwner The owner of the process instance. (Who started it).
   * @return
   */
  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}", query = { "op=newInstance" })
  public HProcessInstance startProcess(
         @RestParam(name = "handle", type = ParamType.VAR) final long pHandle,
         @RestParam(name = "name", type = ParamType.QUERY) final String pName,
         @RestParam(name = "uuid", type = ParamType.QUERY) final String pUUID,
         @RestParam(type = ParamType.PRINCIPAL) final Principal pOwner) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      UUID uuid = pUUID==null ? UUID.randomUUID() : UUID.fromString(pUUID);
      return transaction.commit(aProcessEngine.startProcess(transaction, pOwner, Handles.<ProcessModelImpl>handle(pHandle), pName, uuid, null));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error starting process", e);
      throw new HttpResponseException(500, e);
    }
  }

  @RestMethod(method = HttpMethod.GET, path = "/processInstances")
  @XmlElementWrapper(name = "processInstances", namespace = Constants.PROCESS_ENGINE_NS)
  public Collection<? extends ProcessInstanceRef> getProcesInstanceRefs(@RestParam(type = ParamType.PRINCIPAL) final Principal pOwner) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      final Iterable<ProcessInstance> processInstances = aProcessEngine.getOwnedProcessInstances(transaction, pOwner);
      final Collection<ProcessInstanceRef> list = new ArrayList<>();
      for (final ProcessInstance pi : processInstances) {
        list.add(pi.getRef());
      }
      transaction.commit();
      return list;
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process instances", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @RestMethod(method = HttpMethod.GET, path= "/processInstances/${handle}")
  public ProcessInstance getProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      return transaction.commit(aProcessEngine.getProcessInstance(transaction, pHandle, pUser));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process instance", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @RestMethod(method = HttpMethod.GET, path= "/processInstances/${handle}", query= {"op=tickle"} )
  public String tickleProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    aProcessEngine.tickleInstance(pHandle);
    return "success";
  }

  @RestMethod(method = HttpMethod.DELETE, path= "/processInstances/${handle}")
  public ProcessInstance cancelProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      return transaction.commit(aProcessEngine.cancelInstance(transaction, pHandle, pUser));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error cancelling process intance", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @WebMethod(operationName="getProcessNodeInstance")
  public XmlProcessNodeInstance getProcessNodeInstanceSoap(
          @WebParam(name="handle", mode=Mode.IN) final long pHandle,
          @WebParam(name="user", mode=Mode.IN) final Principal pUser) throws FileNotFoundException, SQLException
  {
    return getProcessNodeInstance(pHandle, pUser);
  }

  @RestMethod(method = HttpMethod.GET, path = "/tasks/${handle}")
  public XmlProcessNodeInstance getProcessNodeInstance(
          @RestParam(name = "handle", type = ParamType.VAR)
              final long pHandle,
          @RestParam(type = ParamType.PRINCIPAL)
              final Principal pUser) throws FileNotFoundException, SQLException
  {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      final ProcessNodeInstance result = aProcessEngine.getNodeInstance(transaction, pHandle, pUser);
      if (result==null) { throw new FileNotFoundException(); }
      return transaction.commit(result).toXmlNode();
    }
  }

  @WebMethod(operationName = "updateTaskState")
  public TaskState updateTaskStateSoap(@WebParam(name = "handle", mode = Mode.IN) final long pHandle, @WebParam(name = "state", mode = Mode.IN) final TaskState pNewState, @WebParam(name = "user", mode = Mode.IN) final Principal pUser) {
    return updateTaskState(pHandle, pNewState, pUser);
  }

  @RestMethod(method = HttpMethod.POST, path = "/tasks/${handle}", query = { "state" })
  public TaskState updateTaskState(@RestParam(name = "handle", type = ParamType.VAR) final long pHandle, @RestParam(name = "state", type = ParamType.QUERY) final TaskState pNewState, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      return transaction.commit(aProcessEngine.updateTaskState(transaction, Handles.<ProcessNodeInstance>handle(pHandle), pNewState, pUser));
    } catch (SQLException e) {
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  @WebMethod(operationName = "finishTask")
  public TaskState finishTaskSoap(@WebParam(name = "handle", mode = Mode.IN) final long pHandle, @WebParam(name = "payload", mode = Mode.IN) final Node pPayload, @WebParam(name = "principal", mode = Mode.IN, header = true) final String pUser) {
    return finishTask(pHandle, pPayload, new SimplePrincipal(pUser));
  }

  @WebMethod(operationName = "finishTask")
  @RestMethod(method = HttpMethod.POST, path = "/tasks/${handle}", query = { "state=Complete" })
  public TaskState finishTask(
        @WebParam(name = "handle", mode = Mode.IN)
        @RestParam(name = "handle", type = ParamType.VAR)
        final long pHandle,
        @WebParam(name = "payload", mode = Mode.IN)
        @RestParam(name = "payload", type = ParamType.QUERY)
        final Node pPayload,
        @RestParam(type = ParamType.PRINCIPAL)
        @WebParam(name = "principal", mode = Mode.IN, header = true)
        final Principal pUser) {
    try (DBTransaction transaction = aProcessEngine.startTransaction()){
      return transaction.commit(aProcessEngine.finishTask(transaction, Handles.<ProcessNodeInstance> handle(pHandle), pPayload, pUser));
    } catch (SQLException e) {
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }


  /**
   * Handle the completing of sending a message and receiving some sort of
   * reply. If the reply is an ActivityResponse message we handle that
   * specially.
   * @throws SQLException
   */
  public void onMessageCompletion(final Future<DataSource> pFuture, final Handle<ProcessNodeInstance> pHandle, final Principal pOwner) {
    // XXX do this better
    try {
      if (pFuture.isCancelled()) {
        try (DBTransaction transaction = aProcessEngine.startTransaction()) {
          aProcessEngine.cancelledTask(transaction, pHandle, pOwner);
          transaction.commit();
        }
      } else {
        try {
          final DataSource result = pFuture.get();
          try (DBTransaction transaction = aProcessEngine.startTransaction()) {
            ProcessNodeInstance inst = aProcessEngine.getNodeInstance(transaction, pHandle.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
            inst.setState(transaction, TaskState.Sent);
            transaction.commit();
          }
          try {
            final Document domResult = XmlUtil.tryParseXml(result.getInputStream());
            Element rootNode = domResult.getDocumentElement();
            // If we are seeing a Soap Envelope, get see if the body has a single value and set that as rootNode for further testing.
            if (Envelope.NAMESPACE.equals(rootNode.getNamespaceURI()) && Envelope.ELEMENTNAME.equals(rootNode.getLocalName())) {
              final Element header = XmlUtil.getFirstChild(rootNode, Envelope.NAMESPACE, org.w3.soapEnvelope.Header.ELEMENTNAME);
              if (header != null) {
                rootNode = XmlUtil.getFirstChild(header, Constants.PROCESS_ENGINE_NS, ActivityResponse.ELEMENTNAME);
              }
            }
            if (rootNode != null) {
              // If we receive an ActivityResponse, treat that specially.
              if (Constants.PROCESS_ENGINE_NS.equals(rootNode.getNamespaceURI()) && ActivityResponse.ELEMENTNAME.equals(rootNode.getLocalName())) {
                final String taskStateAttr = rootNode.getAttribute(ActivityResponse.ATTRTASKSTATE);
                try (DBTransaction transaction = aProcessEngine.startTransaction()) {
                  final TaskState taskState = TaskState.valueOf(taskStateAttr);
                  aProcessEngine.updateTaskState(transaction, pHandle, taskState, pOwner);
                  transaction.commit();
                  return;
                } catch (final NullPointerException e) {
                  // ignore
                } catch (final IllegalArgumentException e) {
                  try (DBTransaction transaction = aProcessEngine.startTransaction()) {
                    aProcessEngine.errorTask(transaction, pHandle, e, pOwner);
                    transaction.commit();
                  }
                }
              }
            } else {
              try (DBTransaction transaction = aProcessEngine.startTransaction()) {
                // XXX By default assume that we have finished the task
                aProcessEngine.finishedTask(transaction, pHandle, result, pOwner);
                transaction.commit();
              }
            }

          } catch (final NullPointerException e) {
            // ignore
          } catch (final IOException e) {
            // It's not xml or has more than one xml element ignore that and fall back to handling unknown services
          }

        } catch (final ExecutionException e) {
          getLogger().log(Level.INFO, "Task " + pHandle + ": Error in messaging", e.getCause());
          try (DBTransaction transaction = aProcessEngine.startTransaction()) {
            aProcessEngine.errorTask(transaction, pHandle, e.getCause(), pOwner);
            transaction.commit();
          }
        } catch (final InterruptedException e) {
          getLogger().log(Level.INFO, "Task " + pHandle + ": Interrupted", e);
          try (DBTransaction transaction = aProcessEngine.startTransaction()) {
            aProcessEngine.cancelledTask(transaction, pHandle, pOwner);
            transaction.commit();
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public QName getServiceName() {
    return new QName(Constants.PROCESS_ENGINE_NS, "ProcessEngine");
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
  public void initEndpoint(final ServletConfig pConfig) {
    // We know our config, don't do anything.
  }

  void setLocalEndpoint(URI localURL) {
    aLocalEndPoint = new EndpointDescriptorImpl(getServiceName(), getEndpointName(), localURL);
  }

}
