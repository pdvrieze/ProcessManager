package nl.adaptivity.process.userMessageHandler.server;

import java.util.logging.Logger;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import net.devrieze.util.Tripple;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.jbi.soap.SoapHelper;
import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.exec.Task.TaskState;

@XmlSeeAlso(InternalEndpoint.XmlTask.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InternalEndpoint implements GenericEndpoint {

  @XmlRootElement(name="task")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class XmlTask implements UserTask{
    private static final QName UPDATE_OPERATION_NAME = new QName("http://adaptivity.nl/ProcessEngine/", "updateTaskState");
    private long aHandle;
    private long aRemoteHandle;
    private TaskState aState=TaskState.Available;
    private String aSummary;
    private EndPointDescriptor aEndPoint = null;
    private ComponentContext aContext;

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
    public void setState(TaskState pNewState) {
      try {
        updateRemoteTaskState(aState);
        aState = pNewState;
      } catch (JAXBException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      } catch (MessagingException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      }
    }

    private void updateRemoteTaskState(TaskState pState) throws JAXBException, MessagingException {
      @SuppressWarnings("unchecked") Source messageContent = SoapHelper.createMessage(UPDATE_OPERATION_NAME, Tripple.<String, Class<?>, Object>tripple("handle", long.class, aRemoteHandle), Tripple.<String, Class<?>, Object>tripple("state", TaskState.class, pState));
      DeliveryChannel channel = aContext.getDeliveryChannel();

      ServiceEndpoint se = null;
      {
        for(ServiceEndpoint candidate:aContext.getEndpointsForService(aEndPoint.getServiceName())) {
          if (candidate.getEndpointName().equals(aEndPoint.getEndpointName())) {
            se = candidate;
            break;
          }
        }
      }
      if (se==null) { throw new MessagingException("No endpoint found"); }

      MessageExchangeFactory exf = channel.createExchangeFactory(se);
      RobustInOnly ex = exf.createRobustInOnlyExchange();
      ex.setOperation(UPDATE_OPERATION_NAME);
      NormalizedMessage message = ex.createMessage();
      message.setContent(messageContent);
      ex.setInMessage(message);
      channel.send(ex);
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
    public void setContext(ComponentContext context) {
      aContext = context;
    }

    @Override
    public ComponentContext getContext() {
      return aContext;
    }
  }

  private static final String ENDPOINT = "internal";
  public static final QName SERVICENAME = new QName(UserMessageService.UMH_NS, "userMessageHandler");
  private UserMessageService aService;

  public InternalEndpoint(UserMessageService pService) {
    aService = pService;
  }

  @Override
  public QName getService() {
    return SERVICENAME;
  }

  @Override
  public String getEndpoint() {
    return ENDPOINT;
  }

  @WebMethod
  public boolean postTask(@WebParam(name="replies", mode=Mode.IN) EndPointDescriptor pEndPoint, @WebParam(name="task", mode=Mode.IN) UserTask pTask) {
    pTask.setEndpoint(pEndPoint);
    return aService.postTask(pTask);
  }
}
