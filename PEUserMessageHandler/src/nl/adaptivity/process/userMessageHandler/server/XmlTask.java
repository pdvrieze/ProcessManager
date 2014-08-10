package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.util.Constants;

@XmlRootElement(name = "task")
@XmlAccessorType(XmlAccessType.NONE)
public class XmlTask implements UserTask<XmlTask> {

  private long aHandle = -1L;

  private long aRemoteHandle = -1L;

  private long aInstanceHandle = -1L;

  TaskState aState = TaskState.Sent;

  private String aSummary;

  private EndpointDescriptorImpl aEndPoint = null;

  private Principal aOwner;

  private List<XmlItem> aItems;

  public XmlTask() {
    // no special operations here
  }

  public XmlTask(final long pHandle) {
    aHandle = pHandle;
  }

  public XmlTask(UserTask<?> pTask) {
    this.aRemoteHandle = pTask.getRemoteHandle();
    this.aInstanceHandle = pTask.getInstanceHandle();
    this.aState = pTask.getState();
    this.aSummary = pTask.getSummary();
    this.aEndPoint = null;
    this.aOwner = pTask.getOwner();
    this.aItems = new ArrayList<>(XmlItem.get(pTask.getItems()));
  }

  @XmlAttribute
  @Override
  public TaskState getState() {
    return aState;
  }

  void setState(TaskState pState) {
    aState = pState;
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
    return ServletProcessEngineClient.updateTaskState(aRemoteHandle, pState, pUser, null);
  }

  private Future<TaskState> finishRemoteTask(final Principal pUser) throws JAXBException, MessagingException {
    return ServletProcessEngineClient.finishTask(aRemoteHandle, createResult(), pUser, null); // Ignore completion???
  }

  private Node createResult() {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);;
    Document document;
    try {
      document = dbf.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    Element outer = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "result");
    for(XmlItem item: getItems()) {
      if (! "label".equals(item.getType())) {
        Element inner = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "value");
        inner.setAttribute("name", item.getName());
        if (item.getValue()!=null) {
          inner.setTextContent(item.getValue());
        }
        outer.appendChild(inner);
      }
    }

    return outer;
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

  @Override
  public long getRemoteHandle() {
    return aRemoteHandle;
  }

  @XmlAttribute(name = "instancehandle")
  public void setInstanceHandle(final long pHandle) {
    aInstanceHandle = pHandle;
  }

  @Override
  public long getInstanceHandle() {
    return aInstanceHandle;
  }

  @Override
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
    if (aItems==null) { aItems = new ArrayList<>(); }
    return aItems;
  }

  @Override
  public void setItems(List<? extends TaskItem> pItems) {
    aItems = new ArrayList<>(pItems.size());
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

  public static XmlTask get(UserTask<?> pTask) {
    if (pTask instanceof XmlTask) { return (XmlTask) pTask; }
    return new XmlTask(pTask);
  }

  public XmlItem getItem(String pName) {
    for(XmlItem item: getItems()) {
      if (pName.equals(item.getName())) {
        return item;
      }
    }
    return null;
  }


}