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

package nl.adaptivity.process.engine.servlet;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Handles;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.AuthenticationNeededException;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.io.Writable;
import nl.adaptivity.io.WritableReader;
import nl.adaptivity.messaging.*;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.*;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.EndpointServlet;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.xml.DomUtil;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.xml.SerializableList;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import org.jetbrains.annotations.TestOnly;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.security.Principal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The service representing a process engine.
 *
 * @param <T> The type of transaction used. Mainly used for testing with memory based storage
 */
//@ServiceInfo(targetNamespace = ServletProcessEngine.SERVICE_NS,
//             interfaceNS = ServletProcessEngine.SERVICE_NS,
//             interfaceLocalname = "soap",
//             interfacePrefix = "pe",
//             serviceLocalname = ServletProcessEngine.SERVICE_LOCALNAME)
public class ServletProcessEngine<T extends Transaction> extends EndpointServlet implements IMessageService<ServletProcessEngine.NewServletMessage, ProcessNodeInstance>, GenericEndpoint {

  private static final long serialVersionUID = -6277449163953383974L;

  public static final String SERVICE_NS = Constants.PROCESS_ENGINE_NS;
  public static final String SERVICE_LOCALNAME = "ProcessEngine";
  public static final QName SERVICE_QNAME = new QName(SERVICE_NS, SERVICE_LOCALNAME);

  private class MessagingCompletionListener implements CompletionListener<DataSource> {

    private final Handle<ProcessNodeInstance> mHandle;

    private final Principal mOwner;

    public MessagingCompletionListener(final Handle<ProcessNodeInstance> handle, final Principal owner) {
      mHandle = handle;
      mOwner = owner;
    }

    @Override
    public void onMessageCompletion(final Future<? extends DataSource> future) {
      ServletProcessEngine.this.onMessageCompletion(future, mHandle, mOwner);
    }

  }

  static class NewServletMessage implements ISendableMessage, Writable {

    //    private Endpoint mDestination;
    //    private String mMethod;
    //    private String mContentType;
    private final EndpointDescriptor mLocalEndpoint;

    private ProcessNodeInstance mNodeInstance;

    private IXmlMessage mMessage;

    private Writable mData;

    public NewServletMessage(final IXmlMessage message, final EndpointDescriptor localEndPoint) {
      mMessage = message;
      mLocalEndpoint = localEndPoint;
    }


    private Principal getOwner() {
      return mNodeInstance.getProcessInstance().getOwner();
    }


    private long getHandle() {
      return mNodeInstance.getHandle();
    }

    @Override
    public EndpointDescriptor getDestination() {
      return mMessage.getEndpointDescriptor();
    }

    @Override
    public String getMethod() {
      return mMessage.getMethod();
    }

    @Override
    public Collection<? extends IHeader> getHeaders() {
      final String contentType = mMessage.getContentType();
      if (contentType == null) {
        return Collections.emptyList();
      } else {
        return Collections.<IHeader> singletonList(new Header("Content-type", contentType));
      }
    }

    @Override
    public Writable getBodySource() {
      return mData;
    }

    @Override
    public Reader getBodyReader() {
      return new WritableReader(mData); // XXX see if there's a better way
    }

    public String getContentType() {
      return mMessage.getContentType();
    }

    public XmlReader getSource() throws XmlException {
      return XMLFragmentStreamReader.from(mMessage.getMessageBody());
    }

    @Override
    public void writeTo(final Writer destination) throws IOException {
      mData.writeTo(destination);
    }


    public static void fillInActivityMessage(Source messageBody, final Result result, ProcessNodeInstance nodeInstance) throws FactoryConfigurationError, XMLStreamException {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLEventReader xer = xif.createXMLEventReader(messageBody);
      final XMLEventWriter xew = xof.createXMLEventWriter(result);

      while (xer.hasNext()) {
        final XMLEvent event = xer.nextEvent();
        if (event.isStartElement()) {
          final StartElement se = event.asStartElement();
          final QName eName = se.getName();
          if (Constants.MODIFY_NS_STR.equals(eName.getNamespaceURI())) {
            @SuppressWarnings("unchecked")
            final Iterator<Attribute> attributes = se.getAttributes();
            if (eName.getLocalPart().equals("attribute")) {
              writeAttribute(nodeInstance, xer, attributes, xew);
            } else if (eName.getLocalPart().equals("element")) {
              writeElement(nodeInstance, xer, attributes, xew);
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
            if (!ns.getNamespaceURI().equals(Constants.MODIFY_NS_STR)) {
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

    private static void writeElement(ProcessNodeInstance nodeInstance, final XMLEventReader in, final Iterator<Attribute> attributes, final XMLEventWriter out) throws XMLStreamException {
      String valueName = null;
      {
        while (attributes.hasNext()) {
          final Attribute attr = attributes.next();
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
          out.add(xef.createCharacters(Long.toString(nodeInstance.getHandle())));
        } else if ("endpoint".equals(valueName)) {
          final QName qname1 = new QName(Constants.MY_JBI_NS_STR, "endpointDescriptor", "");
          final List<Namespace> namespaces = Collections.singletonList(xef.createNamespace("", Constants.MY_JBI_NS_STR));
          out.add(xef.createStartElement(qname1, null, namespaces.iterator()));

          {
            EndpointDescriptor localEndpoint = nodeInstance.getProcessInstance().getEngine().getLocalEndpoint();
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

    private static void writeAttribute(ProcessNodeInstance nodeInstance, final XMLEventReader in, final Iterator<Attribute> attributes, final XMLEventWriter out) throws XMLStreamException {
      String valueName = null;
      String paramName = null;
      {
        while (attributes.hasNext()) {
          final Attribute attr = attributes.next();
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
            attr = xef.createAttribute(paramName, Long.toString(nodeInstance.getHandle()));
          } else {
            attr = xef.createAttribute("handle", Long.toString(nodeInstance.getHandle()));
          }
          out.add(attr);
        } else if ("owner".equals(valueName)) {
          Attribute attr;
          if (paramName != null) {
            attr = xef.createAttribute(paramName, nodeInstance.getProcessInstance().getOwner().getName());
          } else {
            attr = xef.createAttribute("owner", nodeInstance.getProcessInstance().getOwner().getName());
          }
          out.add(attr);
        } else if ("instancehandle".equals(valueName)) {
          Attribute attr;
          if (paramName != null) {
            attr = xef.createAttribute(paramName, nodeInstance.getProcessInstance().getOwner().getName());
          } else {
            attr = xef.createAttribute("instancehandle", nodeInstance.getProcessInstance().getOwner().getName());
          }
          out.add(attr);
        }


      } else {
        throw new MessagingFormatException("Missing parameter name");
      }

    }


    public void setHandle(Transaction transaction, final ProcessNodeInstance nodeInstance) throws SQLException {
      mNodeInstance = nodeInstance;

      try {

        mData = mNodeInstance.instantiateXmlPlaceholders(transaction, getSource(), false);

      } catch (final FactoryConfigurationError | XmlException e) {
        throw new MessagingException(e);
      }

    }

    @Override
    public Map<String, DataSource> getAttachments() {
      return Collections.emptyMap();
    }


  }

  private ProcessEngine mProcessEngine;

  private EndpointDescriptorImpl mLocalEndPoint;

  /*
   * Servlet methods
   */

  @Override
  protected GenericEndpoint getEndpointProvider() {
    return this;
  }

  @Override
  public void destroy() {
    MessagingRegistry.getMessenger().unregisterEndpoint(this);
  }

  @Override
  public String getServletInfo() {
    return "ServletProcessEngine";
  }

  @TestOnly
  protected void init(final ProcessEngine engine) {
    mProcessEngine = engine;
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    mProcessEngine = ProcessEngine.newInstance(this);
    String hostname = config.getInitParameter("hostname");
    String port = config.getInitParameter("port");
    {
      URI localURL = null;

      if (hostname == null) {
        hostname = "localhost";
      }

      // TODO this should can be done better.
      if (port == null) {
        localURL = URI.create("/" + config.getServletContext().getContextPath());
      }
      if (port == null) {
        localURL = URI.create("http://" + hostname + config.getServletContext().getContextPath());
      } else {
        localURL = URI.create("http://" + hostname + ":" + port + config.getServletContext().getContextPath());
      }
      setLocalEndpoint(localURL);
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }



  /*
   * IMessageService methods
   */

  @Override
  public NewServletMessage createMessage(final IXmlMessage message) {
    return new NewServletMessage(message, mLocalEndPoint);
  }

  @Override
  public boolean sendMessage(Transaction transaction, final NewServletMessage message, final ProcessNodeInstance instance) throws SQLException {
    assert instance.getHandle()>=0;
    message.setHandle(transaction, instance);

    Future<DataSource> result = MessagingRegistry.sendMessage(message, new MessagingCompletionListener(Handles.<ProcessNodeInstance>handle(message.getHandle()), message.getOwner()), DataSource.class, new Class<?>[0]);
    if (result.isCancelled()) { return false; }
    if (result.isDone()) {
      try {
        result.get();
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) { throw (RuntimeException) cause; }
        if (cause instanceof SQLException) { throw (SQLException) cause; }
        throw new RuntimeException(cause);
      } catch (InterruptedException e) {
        return false;
      }
    }
    return true;
  }

  /*
   * Methods inherited from JBIProcessEngine
   */

  @Override
  public EndpointDescriptor getLocalEndpoint() {
    return mLocalEndPoint;
  }

  static Logger getLogger() {
    final Logger logger = Logger.getLogger(ServletProcessEngine.class.getName());
    logger.setLevel(Level.ALL);
    return logger;
  }

  private static final QName REFS_TAG = new QName(SERVICE_NS, "processModels");
  /**
   * Get the list of all process models in the engine. This will be limited to user owned ones. The list will contain only
   * a summary of each model including name, id and uuid, not the content.
   */
  @RestMethod(method = HttpMethod.GET, path = "/processModels")
  public SerializableList<ProcessModelRef> getProcesModelRefs() {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      final Iterable<? extends ProcessModelImpl> processModels = mProcessEngine.getProcessModels(transaction);

      final ArrayList<ProcessModelRef<?,?>> list = new ArrayList<>();
      for (final ProcessModelImpl pm : processModels) {
        list.add(ProcessModelRef.get(pm.getRef()));
      }
      return transaction.commit(new SerializableList<>(REFS_TAG, list));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process model references", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Get the full process model content for the model with the given handle.
   * @param handle The handle of the process model
   * @param user The user whose permissions are verified
   * @return The process model
   * @throws FileNotFoundException When the model does not exist. This is translated to a 404 error in http.
   */
  @RestMethod(method = HttpMethod.GET, path = "/processModels/${handle}")
  public ProcessModelImpl getProcessModel(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) throws FileNotFoundException {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      Handle<ProcessModelImpl> handle1 = Handles.<ProcessModelImpl>handle(handle);
      mProcessEngine.invalidateModelCache(handle1);
      return transaction.commit(mProcessEngine.getProcessModel(transaction, handle1, user));
    } catch (final NullPointerException e) {
      throw (FileNotFoundException) new FileNotFoundException("Process handle invalid").initCause(e);
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process model", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Update the process model with the given handle.
   * @param handle The model handle
   * @param attachment The actual new model
   * @param user The user performing the update. This will be verified against ownership and permissions
   * @return A reference to the model. This may include a newly generated uuid if not was provided.
   * @throws IOException
   * @throws XmlException
   */
  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}")
  public ProcessModelRef updateProcessModel(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(name = "processUpload", type = ParamType.ATTACHMENT) final DataHandler attachment, @RestParam(type = ParamType.PRINCIPAL) final Principal user) throws IOException, XmlException {
    ProcessModelImpl processModel = XmlStreaming.deSerialize(attachment.getInputStream(), ProcessModelImpl.class);
    return updateProcessModel(handle, processModel, user);
  }

  /**
   * Update the process model with the given handle.
   * @param handle The model handle
   * @param processModel The actual new model
   * @param user The user performing the update. This will be verified against ownership and permissions
   * @return A reference to the model. This may include a newly generated uuid if not was provided.
   * @throws IOException
   * @throws XmlException
   */
  @WebMethod(operationName = "updateProcessModel")
  public ProcessModelRef updateProcessModel(final @WebParam(name="handle") long handle, @WebParam(name = "processModel", mode = Mode.IN) final ProcessModelBase processModel, final  @WebParam(name = "principal", mode = Mode.IN, header = true) @RestParam(type = ParamType.PRINCIPAL) Principal user) throws FileNotFoundException {
    if (user == null) { throw new AuthenticationNeededException("There is no user associated with this request"); }
    if (processModel != null) {
      processModel.setHandle(handle);

      try (Transaction transaction = mProcessEngine.startTransaction()){
        return transaction.commit(mProcessEngine.updateProcessModel(transaction, Handles.<ProcessModelImpl>handle(handle), processModel, user));
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error updating process model", e);
        throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
      }
    }
    throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "The posted process model is not valid");
  }

  /**
   * Add a process model to the system.
   * @param attachment The process model to add.
   * @param owner The creator/owner of the model.
   * @return A reference to the model with handle and a new uuid if none was provided.
   * @throws IOException
   * @throws XmlException
   */
  @RestMethod(method = HttpMethod.POST, path = "/processModels")
  public ProcessModelRef postProcessModel(@RestParam(name = "processUpload", type = ParamType.ATTACHMENT) final DataHandler attachment, @RestParam(type = ParamType.PRINCIPAL) final Principal owner) throws IOException, XmlException {
    ProcessModelImpl processModel = XmlStreaming.deSerialize(attachment.getInputStream(), ProcessModelImpl.class);
    return postProcessModel(processModel, owner);
  }

  /**
   * Add a process model to the system.
   * @param processModel The process model to add.
   * @param owner The creator/owner of the model.
   * @return A reference to the model with handle and a new uuid if none was provided.
   * @throws IOException
   * @throws XmlException
   */
  @WebMethod(operationName = "postProcessModel")
  public ProcessModelRef postProcessModel(@WebParam(name = "processModel", mode = Mode.IN) final ProcessModelBase processModel, final @RestParam(type = ParamType.PRINCIPAL) @WebParam(name = "principal", mode = Mode.IN, header = true) Principal owner) {
    if (owner==null) { throw new AuthenticationNeededException("There is no user associated with this request"); }
    if (processModel != null) {
      processModel.setHandle(-1); // The handle cannot be set
      try (Transaction transaction = mProcessEngine.startTransaction()){
        return transaction.commit(ProcessModelRef.get(mProcessEngine.addProcessModel(transaction, processModel, owner)));
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error adding process model", e);
        throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
      }
    }

    return null;
  }

  /**
   * Rename the given process model.
   * @param handle The handle to the model
   * @param name The new name
   * @param user The user performing the action.
   */
  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}")
  public void renameProcess(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(name = "name", type = ParamType.QUERY) final String name, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    mProcessEngine.renameProcessModel(user, Handles.<ProcessModelImpl>handle(handle), name);
  }

  /**
   * Delete the process model.
   * @param handle The handle of the process model to delete
   * @param user A user with permission to delete the model.
   */
  @RestMethod(method = HttpMethod.DELETE, path = "/processModels/${handle}")
  public void deleteProcess(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      if (! mProcessEngine.removeProcessModel(transaction, Handles.<ProcessModelImpl>handle(handle), user)) {
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
   * @param handle The handle of the process to start.
   * @param name The name that will allow the user to remember the instance. If
   *          <code>null</code> a name will be assigned. This name has no
   *          semantic meaning.
   * @param owner The owner of the process instance. (Who started it).
   * @return A handle to the process
   */
  @WebMethod
  @RestMethod(method = HttpMethod.POST, path = "/processModels/${handle}", query = { "op=newInstance" })
  public XmlHandle<?> startProcess(
         @WebParam(name="handle") @RestParam(name = "handle", type = ParamType.VAR) final long handle,
         @WebParam(name="name") @RestParam(name = "name", type = ParamType.QUERY) final String name,
         @WebParam(name="uuid") @RestParam(name = "uuid", type = ParamType.QUERY) final String uUID,
         @WebParam(name="owner", header = true) @RestParam(type = ParamType.PRINCIPAL) final Principal owner) throws FileNotFoundException {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      UUID uuid = uUID==null ? UUID.randomUUID() : UUID.fromString(uUID);
      return transaction.commit(mProcessEngine.startProcess(transaction, owner, Handles.<ProcessModelImpl>handle(handle), name, uuid, null));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error starting process", e);
      throw new HttpResponseException(500, e);
    }
  }


  private static final QName INSTANCEREFS_TAG = new QName(SERVICE_NS, "processInstances");

  /**
   * Get a list of all process instances owned by the current user. This will provide a summary list, providing a subset.
   * of information from the process instance.
   * @param owner The user.
   * @return A list of process instances.
   */
  @RestMethod(method = HttpMethod.GET, path = "/processInstances")
  @XmlElementWrapper(name = "processInstances", namespace = Constants.PROCESS_ENGINE_NS)
  public Collection<? extends ProcessInstanceRef> getProcesInstanceRefs(@RestParam(type = ParamType.PRINCIPAL) final Principal owner) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      final Iterable<ProcessInstance> processInstances = mProcessEngine.getOwnedProcessInstances(transaction, owner);
      final List<ProcessInstanceRef> list = new ArrayList<>();
      for (final ProcessInstance pi : processInstances) {
        list.add(pi.getRef());
      }
      return transaction.commit(new SerializableList<ProcessInstanceRef>(INSTANCEREFS_TAG, list));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process instances", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Get the given process instance.
   * @param handle The handle of the instance.
   * @param user A user with permission to see the model.
   * @return The full process instance.
   */
  @RestMethod(method = HttpMethod.GET, path= "/processInstances/${handle}")
  public ProcessInstance getProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      return transaction.commit(mProcessEngine.getProcessInstance(transaction, new HProcessInstance(handle), user));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error getting process instance", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Cause the process instance state to be re-evaluated. For failed service invocations, this will cause the server to
   * try again.
   * @param handle The handle of the instance
   * @param user A user with appropriate permissions.
   * @return A string indicating success.
   */
  @RestMethod(method = HttpMethod.GET, path= "/processInstances/${handle}", query= {"op=tickle"} )
  public String tickleProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    mProcessEngine.tickleInstance(handle);
    return "success";
  }

  /**
   * Cancel the process instance execution. This will cause all aspects of the instance to be cancelled. Note that this
   * cannot undo the past.
   * @param handle The instance to cancel
   * @param user A user with permission to cancel the instance.
   * @return The instance that was cancelled.
   */
  @RestMethod(method = HttpMethod.DELETE, path= "/processInstances/${handle}")
  public ProcessInstance cancelProcessInstance(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      return transaction.commit(mProcessEngine.cancelInstance(transaction, new HProcessInstance(handle), user));
    } catch (SQLException e) {
      getLogger().log(Level.WARNING, "Error cancelling process intance", e);
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Get the data for a specific task.
   * @param handle The handle
   * @param user A user with appropriate permissions
   * @return The node instance.
   * @throws FileNotFoundException
   * @throws SQLException
   * @throws XmlException
   */
  @WebMethod(operationName="getProcessNodeInstance")
  public XmlProcessNodeInstance getProcessNodeInstanceSoap(
          @WebParam(name="handle", mode=Mode.IN) final long handle,
          @WebParam(name="user", mode=Mode.IN) final Principal user) throws FileNotFoundException, SQLException, XmlException {
    return getProcessNodeInstance(handle, user);
  }

  /**
   * Get the information for a specific task.
   * @param handle The handle of the task
   * @param user A user with appropriate permissions
   * @return the task
   * @throws FileNotFoundException
   * @throws SQLException
   * @throws XmlException
   */
  @RestMethod(method = HttpMethod.GET, path = "/tasks/${handle}")
  public XmlProcessNodeInstance getProcessNodeInstance(
          @RestParam(name = "handle", type = ParamType.VAR)
              final long handle,
          @RestParam(type = ParamType.PRINCIPAL)
              final Principal user) throws FileNotFoundException, SQLException, XmlException {
    Handle<? extends ProcessNodeInstance> handle1 = new HProcessNodeInstance(handle);
    try (Transaction transaction = mProcessEngine.startTransaction()){
      final ProcessNodeInstance result = mProcessEngine.getNodeInstance(transaction, handle1, user);
      if (result==null) { throw new FileNotFoundException(); }
      return transaction.commit(result.toSerializable(transaction));
    }
  }

  /**
   * Update the state of a task.
   * @param handle Handle of the task to update
   * @param newState The new state
   * @param user A user with appropriate permissions
   * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
   */
  @WebMethod(operationName = "updateTaskState")
  public NodeInstanceState updateTaskStateSoap(@WebParam(name = "handle", mode = Mode.IN) final long handle, @WebParam(name = "state", mode = Mode.IN) final NodeInstanceState newState, @WebParam(name = "user", mode = Mode.IN) final Principal user) {
    return updateTaskState(handle, newState, user);
  }

  /**
   * Update the state of a task.
   * @param handle Handle of the task to update
   * @param newState The new state
   * @param user A user with appropriate permissions
   * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
   */
  @RestMethod(method = HttpMethod.POST, path = "/tasks/${handle}", query = { "state" })
  public NodeInstanceState updateTaskState(@RestParam(name = "handle", type = ParamType.VAR) final long handle, @RestParam(name = "state", type = ParamType.QUERY) final NodeInstanceState newState, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      return transaction.commit(mProcessEngine.updateTaskState(transaction, Handles.<ProcessNodeInstance>handle(handle), newState, user));
    } catch (SQLException e) {
      throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * finish a task. Process aware services will need to call this to signal completion.
   * @param handle Handle of the task to update
   * @param payload An XML document that is the "result" of the service.
   * @param user A user with appropriate permissions
   * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
   */
  @WebMethod(operationName = "finishTask")
  @RestMethod(method = HttpMethod.POST, path = "/tasks/${handle}", query = { "state=Complete" })
  public NodeInstanceState finishTask(
        @WebParam(name = "handle", mode = Mode.IN)
        @RestParam(name = "handle", type = ParamType.VAR)
        final long handle,
        @WebParam(name = "payload", mode = Mode.IN)
        @RestParam(name = "payload", type = ParamType.QUERY)
        final Node payload,
        @RestParam(type = ParamType.PRINCIPAL)
        @WebParam(name = "principal", mode = Mode.IN, header = true)
        final Principal user) {
    try (Transaction transaction = mProcessEngine.startTransaction()){
      return transaction.commit(mProcessEngine.finishTask(transaction, Handles.<ProcessNodeInstance> handle(handle), payload, user));
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
  public void onMessageCompletion(final Future<? extends DataSource> future, final Handle<ProcessNodeInstance> handle, final Principal owner) {
    // XXX do this better
    try {
      if (future.isCancelled()) {
        try (Transaction transaction = mProcessEngine.startTransaction()) {
          mProcessEngine.cancelledTask(transaction, handle, owner);
          transaction.commit();
        }
      } else {
        try {
          final DataSource result = future.get();
          try (Transaction transaction = mProcessEngine.startTransaction()) {
            ProcessNodeInstance inst = mProcessEngine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL);
            assert inst.getState() == NodeInstanceState.Pending;
            if (inst.getState() == NodeInstanceState.Pending) {
              inst.setState(transaction, NodeInstanceState.Sent);
            }
            transaction.commit();
          }
          try {
            final Document domResult = DomUtil.tryParseXml(result.getInputStream());
            Element rootNode = domResult.getDocumentElement();
            // If we are seeing a Soap Envelope, if there is an activity response in the header treat that as the root node.
            if (Envelope.NAMESPACE.equals(rootNode.getNamespaceURI()) && Envelope.ELEMENTLOCALNAME.equals(rootNode.getLocalName())) {
              final Element header = DomUtil.getFirstChild(rootNode, Envelope.NAMESPACE, org.w3.soapEnvelope.Header.ELEMENTLOCALNAME);
              if (header != null) {
                rootNode = DomUtil.getFirstChild(header, Constants.PROCESS_ENGINE_NS, ActivityResponse.ELEMENTLOCALNAME);
              }
            }
            if (rootNode != null) {
              // If we receive an ActivityResponse, treat that specially.
              if (Constants.PROCESS_ENGINE_NS.equals(rootNode.getNamespaceURI()) && ActivityResponse.ELEMENTLOCALNAME.equals(rootNode.getLocalName())) {
                final String taskStateAttr = rootNode.getAttribute(ActivityResponse.ATTRTASKSTATE);
                try (Transaction transaction = mProcessEngine.startTransaction()) {
                  final NodeInstanceState nodeInstanceState = NodeInstanceState.valueOf(taskStateAttr);
                  mProcessEngine.updateTaskState(transaction, handle, nodeInstanceState, owner);
                  transaction.commit();
                  return;
                } catch (final NullPointerException e) {
                  // ignore
                } catch (final IllegalArgumentException e) {
                  try (Transaction transaction = mProcessEngine.startTransaction()) {
                    mProcessEngine.errorTask(transaction, handle, e, owner);
                    transaction.commit();
                  }
                }
              }
            } else {
              try (Transaction transaction = mProcessEngine.startTransaction()) {
                // XXX By default assume that we have finished the task
                mProcessEngine.finishedTask(transaction, handle, result, owner);
                transaction.commit();
              }
            }

          } catch (final NullPointerException e) {
            // ignore
          } catch (final IOException e) {
            // It's not xml or has more than one xml element ignore that and fall back to handling unknown services
          }

        } catch (final ExecutionException e) {
          getLogger().log(Level.INFO, "Task " + handle + ": Error in messaging", e.getCause());
          try (Transaction transaction = mProcessEngine.startTransaction()) {
            mProcessEngine.errorTask(transaction, handle, e.getCause(), owner);
            transaction.commit();
          }
        } catch (final InterruptedException e) {
          getLogger().log(Level.INFO, "Task " + handle + ": Interrupted", e);
          try (Transaction transaction = mProcessEngine.startTransaction()) {
            mProcessEngine.cancelledTask(transaction, handle, owner);
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
    return new QName(Constants.PROCESS_ENGINE_NS, SERVICE_LOCALNAME);
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
  public boolean isSameService(final EndpointDescriptor other) {
    return Constants.PROCESS_ENGINE_NS.equals(other.getServiceName().getNamespaceURI()) &&
           SERVICE_LOCALNAME.equals(other.getServiceName().getLocalPart()) &&
           getEndpointName().equals(other.getEndpointName());
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    // We know our config, don't do anything.
  }

  protected void setLocalEndpoint(URI localURL) {
    mLocalEndPoint = new EndpointDescriptorImpl(getServiceName(), getEndpointName(), localURL);
  }

}
