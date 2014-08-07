package nl.adaptivity.process.userMessageHandler.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.userMessageHandler.server.InternalEndpoint.XmlTask;
import nl.adaptivity.process.userMessageHandler.server.UserTask.TaskItem;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.ws.soap.SoapSeeAlso;


@XmlSeeAlso(InternalEndpoint.XmlTask.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InternalEndpoint implements GenericEndpoint {

  public class TaskUpdateCompletionListener implements CompletionListener {

    XmlTask aTask;

    public TaskUpdateCompletionListener(final XmlTask pTask) {
      aTask = pTask;
    }

    @Override
    public void onMessageCompletion(final Future<?> pFuture) {
      if (!pFuture.isCancelled()) {
        try {
          final TaskState result = (TaskState) pFuture.get();
          aTask.aState = result;
        } catch (final InterruptedException e) {
          Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
        } catch (final ExecutionException e) {
          Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
        }
      }
    }

  }

  @XmlRootElement(name="item")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class XmlItem implements TaskItem{
    private String aName;
    private String aLabel;
    private String aType;
    private String aValue;
    private String aParams;
    private List<String> aOptions;

    @Override
    @XmlAttribute(name="name")
    public String getName() {
      return aName;
    }

    public void setName(String pName) {
      aName = pName;
    }

    @Override
    @XmlAttribute(name="label")
    public String getLabel() {
      return aLabel;
    }

    public void setLabel(String pLabel) {
      aLabel = pLabel;
    }

    @Override
    @XmlAttribute(name="params")
    public String getParams() {
      return aParams;
    }

    public void setParams(String pParams) {
      aParams = pParams;
    }

    @Override
    @XmlAttribute(name="type")
    public String getType() {
      return aType;
    }

    public void setType(String pType) {
      aType = pType;
    }

    @Override
    @XmlAttribute(name="value")
    public String getValue() {
      return aValue;
    }

    public void setValue(String pValue) {
      aValue = pValue;
    }

    @Override
    public List<String> getOptions() {
      if (aOptions==null) { aOptions = new ArrayList<String>(); }
      return aOptions;
    }

    @XmlElement(name="option", namespace=Constants.USER_MESSAGE_HANDLER_NS)
    public void setOptions(List<String> pOptions) {
      aOptions = pOptions;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((aName == null) ? 0 : aName.hashCode());
      result = prime * result + ((aOptions == null || aOptions.isEmpty()) ? 0 : aOptions.hashCode());
      result = prime * result + ((aType == null) ? 0 : aType.hashCode());
      result = prime * result + ((aValue == null) ? 0 : aValue.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      XmlItem other = (XmlItem) obj;
      if (aName == null) {
        if (other.aName != null)
          return false;
      } else if (!aName.equals(other.aName))
        return false;
      if (aOptions==null || aOptions.isEmpty()) {
        if (other.aOptions!=null && ! aOptions.isEmpty())
          return false;
      } else if (!aOptions.equals(other.aOptions))
        return false;
      if (aType == null) {
        if (other.aType != null)
          return false;
      } else if (!aType.equals(other.aType))
        return false;
      if (aValue == null) {
        if (other.aValue != null)
          return false;
      } else if (!aValue.equals(other.aValue))
        return false;
      return true;
    }
  }

  @XmlRootElement(name = "task")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class XmlTask implements UserTask<XmlTask> {

    private long aHandle;

    private long aRemoteHandle;

    private long aInstanceHandle;

    private TaskState aState = TaskState.Sent;

    private String aSummary;

    private EndpointDescriptorImpl aEndPoint = null;

    private Principal aOwner;

    private List<XmlItem> aItems;

    public XmlTask() {
      aHandle = -1;
      aRemoteHandle = -1;
    }

    public XmlTask(final long pHandle) {
      aHandle = pHandle;
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public void setState(final TaskState pNewState, final Principal pUser) {
      try {
        TaskState newState;
        if (pNewState == TaskState.Complete) {
          newState = finishRemoteTask(pUser).get();
          //          newState = TaskState.Complete; // Use server state instead.
        } else if (pNewState == TaskState.Acknowledged) {
          newState = pNewState; // Just shortcircuit. This is just record keeping
        } else {
          newState = updateRemoteTaskState(pNewState, pUser).get();
          //          newState = pNewState; // TODO make this just reflect engine state, as returned by web methods.
        }
        aState = newState;
      } catch (final JAXBException e) {
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      } catch (final MessagingException e) {
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      } catch (final InterruptedException e) {
        Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
      } catch (final ExecutionException e) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
      }
    }

    private Future<TaskState> updateRemoteTaskState(final TaskState pState, final Principal pUser) throws JAXBException, MessagingException {
      return ServletProcessEngineClient.updateTaskState(aHandle, pState, pUser, null);
    }

    private Future<TaskState> finishRemoteTask(final Principal pUser) throws JAXBException, MessagingException {
      return ServletProcessEngineClient.finishTask(aHandle, null, pUser, null); // Ignore completion???
      // TODO Do something with reply!
    }

    @XmlAttribute(name = "handle")
    @Override
    public void setHandle(final long pHandle) {
      aHandle = pHandle;
    }

    @Override
    public long getHandle() {
      return aHandle;
    }

    @XmlAttribute(name = "remotehandle")
    public void setRemoteHandle(final long pHandle) {
      aRemoteHandle = pHandle;
    }

    public long getRemoteHandle() {
      return aRemoteHandle;
    }

    @XmlAttribute(name = "instancehandle")
    public void setInstanceHandle(final long pHandle) {
      aInstanceHandle = pHandle;
    }

    public long getInstanceHandle() {
      return aInstanceHandle;
    }

    @XmlAttribute(name = "summary")
    public String getSummary() {
      return aSummary;
    }

    public void setSummary(final String pSummary) {
      aSummary = pSummary;
    }

    /** Set the endpoint that is used for updating the task state */
    @Override
    public void setEndpoint(final EndpointDescriptorImpl pEndPoint) {
      aEndPoint = pEndPoint;
    }

    @Override
    public Principal getOwner() {
      return aOwner;
    }

    public void setOwner(final Principal pOwner) {
      aOwner = pOwner;
    }

    @XmlAttribute(name = "owner")
    String getOwnerString() {
      return aOwner==null ? null : aOwner.getName();
    }

    void setOwnerString(final String pOwner) {
      aOwner = pOwner==null ? null : new SimplePrincipal(pOwner);
    }

    @XmlElement(name ="item", namespace=Constants.USER_MESSAGE_HANDLER_NS)
    @Override
    public List<XmlItem> getItems() {
      if (aItems==null) { aItems = new ArrayList<XmlItem>(); }
      return aItems;
    }

    @Override
    public void setItems(List<? extends TaskItem> pItems) {
      aItems = new ArrayList<XmlItem>(pItems.size());
      for(TaskItem item: pItems) {
        aItems.add((XmlItem) item);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((aEndPoint == null) ? 0 : aEndPoint.hashCode());
      result = prime * result + (int) (aHandle ^ (aHandle >>> 32));
      result = prime * result + ((aItems == null||aItems.isEmpty()) ? 0 : aItems.hashCode());
      result = prime * result + ((aOwner == null) ? 0 : aOwner.hashCode());
      result = prime * result + (int) (aRemoteHandle ^ (aRemoteHandle >>> 32));
      result = prime * result + ((aState == null) ? 0 : aState.hashCode());
      result = prime * result + ((aSummary == null) ? 0 : aSummary.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      XmlTask other = (XmlTask) obj;
      if (aEndPoint == null) {
        if (other.aEndPoint != null)
          return false;
      } else if (!aEndPoint.equals(other.aEndPoint))
        return false;
      if (aHandle != other.aHandle)
        return false;
      if (aItems == null || aItems.isEmpty()) {
        if (other.aItems != null && ! aItems.isEmpty())
          return false;
      } else if (!aItems.equals(other.aItems))
        return false;
      if (aOwner == null) {
        if (other.aOwner != null)
          return false;
      } else if (!aOwner.equals(other.aOwner))
        return false;
      if (aRemoteHandle != other.aRemoteHandle)
        return false;
      if (aState != other.aState)
        return false;
      if (aSummary == null) {
        if (other.aSummary != null)
          return false;
      } else if (!aSummary.equals(other.aSummary))
        return false;
      return true;
    }


  }

  private static final String ENDPOINT = "internal";

  public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler");

  private final UserMessageService aService;

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
  public void initEndpoint(final ServletConfig pConfig) {
    final StringBuilder path = new StringBuilder(pConfig.getServletContext().getContextPath());
    path.append("/internal");
    try {
      aURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl pEndPoint, @WebParam(name = "taskParam", mode = Mode.IN) @SoapSeeAlso(XmlTask.class) final UserTask<?> pTask) {
    pTask.setEndpoint(pEndPoint);
    final boolean result = aService.postTask(pTask);
    pTask.setState(TaskState.Acknowledged, pTask.getOwner()); // Only now mark as acknowledged
    return ActivityResponse.create(TaskState.Acknowledged, Boolean.class, Boolean.valueOf(result));
  }

  @Override
  public void destroy() {
    aService.destroy();
  }
}
