package nl.adaptivity.process.userMessageHandler.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import net.devrieze.util.Tripple;
import net.devrieze.util.Tupple;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndPointDescriptor;
import nl.adaptivity.messaging.ISendableMessage;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.ws.soap.SoapHelper;

import org.w3c.dom.Node;

@XmlSeeAlso(InternalEndpoint.XmlTask.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InternalEndpoint implements GenericEndpoint {

  @XmlRootElement(name="task")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class XmlTask implements UserTask<XmlTask>{
    private static final QName UPDATE_OPERATION_NAME = new QName("http://adaptivity.nl/ProcessEngine/", "updateTaskState");
    private static final QName FINISH_OPERATION_NAME = new QName("http://adaptivity.nl/ProcessEngine/", "finishTask");
    private long aHandle;
    private long aRemoteHandle;
    private TaskState aState=TaskState.Sent;
    private String aSummary;
    private EndPointDescriptor aEndPoint = null;
    private AsyncMessenger aContext;
    private Principal aOwner;

    public XmlTask() {
      aHandle = -1;
      aRemoteHandle = -1;
    }

    public XmlTask(long pHandle) {
      aHandle = pHandle;
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public void setState(TaskState pNewState, Principal pUser) {
      try {
        TaskState newState;
        if (pNewState==TaskState.Complete) {
          finishRemoteTask(pUser);
          newState = TaskState.Complete; // Use server state instead.
        } else if (pNewState==TaskState.Acknowledged) {
          newState = pNewState; // Just shortcircuit. This is just record keeping
        } else {
          updateRemoteTaskState(pNewState, pUser);
          newState = pNewState; // TODO make this just reflect engine state, as returned by web methods.
        }
        aState = newState;
      } catch (JAXBException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      } catch (MyMessagingException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      }
    }

    private void updateRemoteTaskState(TaskState pState, Principal pUser) throws JAXBException, MyMessagingException {
      @SuppressWarnings("unchecked")
      Source messageContent = SoapHelper.createMessage(UPDATE_OPERATION_NAME, Tripple.<String, Class<?>, Object>tripple("handle", long.class, aRemoteHandle),
                                                                              Tripple.<String, Class<?>, Object>tripple("state", TaskState.class, pState),
                                                                                                             Tripple.<String, Class<?>, Object>tripple("user", String.class, pUser.getName()));
      aContext.sendMessage(createMessage(aRemoteHandle, messageContent), aHandle, null, pUser);
    }

    private void finishRemoteTask(Principal pUser) throws JAXBException, MyMessagingException {
      @SuppressWarnings("unchecked")
      Source messageContent = SoapHelper.createMessage(FINISH_OPERATION_NAME, Tripple.<String, Class<?>, Object>tripple("handle", long.class, aRemoteHandle),
                                                                              Tripple.<String, Class<?>, Object>tripple("payload", Node.class, null),
                                                                              Tripple.<String, Class<?>, Object>tripple("user", String.class, pUser.getName()));
      aContext.sendMessage(createMessage(aRemoteHandle, messageContent), aHandle, null, pUser);
    }

    private ISendableMessage createMessage(final long pRemoteHandle, final Source pMessageContent) {
      return new ISendableMessage() {

        @Override
        public void writeBody(OutputStream pOutputStream) throws IOException {
          try {
            Sources.writeToStream(pMessageContent, pOutputStream);
          } catch (TransformerException e) {
            throw new IOException(e);
          }
        }

        @Override
        public boolean hasBody() {
          return true;
        }

        @Override
        public String getMethod() {
          return "POST";
        }

        @Override
        public Collection<Tupple<String, String>> getHeaders() {
          return Collections.singletonList(Tupple.tupple("Content-Type", "application/soap+xml"));
        }

        @Override
        public String getDestination() {
          return aEndPoint.getEndpointLocationString();
        }
      };
    }

    @XmlAttribute(name="handle")
    @Override
    public void setHandle(long pHandle) {
      aHandle = pHandle;
    }

    @Override
    public long getHandle() {
      return aHandle;
    }

    @XmlAttribute(name="remotehandle")
    public void setRemoteHandle(long pHandle) {
      aRemoteHandle = pHandle;
    }

    public long getRemoteHandle() {
      return aRemoteHandle;
    }

    @XmlAttribute(name="summary")
    public String getSummary() {
      return aSummary;
    }

    public void setSummary(String pSummary) {
      aSummary = pSummary;
    }

    /** Set the endpoint that is used for updating the task state */
    @Override
    public void setEndpoint(EndPointDescriptor pEndPoint) {
      aEndPoint = pEndPoint;
    }

    @Override
    public void setContext(AsyncMessenger context) {
      aContext = context;
    }

    @Override
    public AsyncMessenger getContext() {
      return aContext;
    }

    @Override
    public Principal getOwner() {
      return aOwner;
    }

    public void setOwner(Principal pOwner) {
      aOwner=pOwner;
    }

    @XmlAttribute(name="owner")
    String getOwnerString() {
      return aOwner.getName();
    }

    void setOwnerString(String pOwner) {
      aOwner = new SimplePrincipal(pOwner);
    }
  }

  private static final String ENDPOINT = "internal";
  public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler");
  private UserMessageService aService;
  private URI aURI;

  public InternalEndpoint() {
    aService = UserMessageService.getInstance();
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }

  @Override
  public String getEndpointName() {
    return ENDPOINT;
  }



  @Override
  public URI getEndpointLocation() {
    // TODO Do this better
    return aURI;
  }

  @Override
  public void initEndpoint(ServletConfig pConfig) {
    StringBuilder path = new StringBuilder(pConfig.getServletContext().getContextPath());
    path.append("/internal");
    try {
      aURI = new URI(null, null, path.toString(), null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
  }

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name="replies", mode=Mode.IN) EndPointDescriptor pEndPoint, @WebParam(name="task", mode=Mode.IN) UserTask<?> pTask) {
    pTask.setEndpoint(pEndPoint);
    boolean result = aService.postTask(pTask);
    pTask.setState(TaskState.Acknowledged, pTask.getOwner()); // Only now mark as acknowledged
    return ActivityResponse.create(TaskState.Acknowledged, Boolean.class, Boolean.valueOf(result));
  }

  @Override
  public void destroy() {
    aService.destroy();
  }
}
