package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  public XmlTask(final long handle) {
    aHandle = handle;
  }

  public XmlTask(UserTask<?> task) {
    this.aHandle = task.getHandle();
    this.aRemoteHandle = task.getRemoteHandle();
    this.aInstanceHandle = task.getInstanceHandle();
    this.aState = task.getState();
    this.aSummary = task.getSummary();
    this.aEndPoint = null;
    this.aOwner = task.getOwner();
    this.aItems = new ArrayList<>(XmlItem.get(task.getItems()));
  }

  @XmlAttribute
  @Override
  public TaskState getState() {
    return aState;
  }

  void setState(TaskState state) {
    aState = state;
  }

  @Override
  public void setState(final TaskState newState, final Principal user) {
    try {
      TaskState verifiedNewState;
      if (newState == TaskState.Complete) {
        verifiedNewState = finishRemoteTask(user).get();
        //          newState = TaskState.Complete; // Use server state instead.
      } else if (newState == TaskState.Acknowledged) {
        verifiedNewState = newState; // Just shortcircuit. This is just record keeping
      } else {
        verifiedNewState = updateRemoteTaskState(newState, user).get();
      }
      aState = verifiedNewState;
    } catch (final JAXBException | MessagingException e) {
      Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
    } catch (final InterruptedException e) {
      Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
    } catch (final ExecutionException e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
    }
  }

  private Future<TaskState> updateRemoteTaskState(final TaskState state, final Principal user) throws JAXBException, MessagingException {
    return ServletProcessEngineClient.updateTaskState(aRemoteHandle, state, user, null);
  }

  private Future<TaskState> finishRemoteTask(final Principal user) throws JAXBException, MessagingException {
    return ServletProcessEngineClient.finishTask(aRemoteHandle, createResult(), user, null); // Ignore completion???
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
  public void setHandle(final long handle) {
    aHandle = handle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @XmlAttribute(name = "remotehandle")
  public void setRemoteHandle(final long handle) {
    aRemoteHandle = handle;
  }

  @Override
  public long getRemoteHandle() {
    return aRemoteHandle;
  }

  @XmlAttribute(name = "instancehandle")
  public void setInstanceHandle(final long handle) {
    aInstanceHandle = handle;
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

  public void setSummary(final String summary) {
    aSummary = summary;
  }

  /** Set the endpoint that is used for updating the task state */
  @Override
  public void setEndpoint(final EndpointDescriptorImpl endPoint) {
    aEndPoint = endPoint;
  }

  @Override
  public Principal getOwner() {
    return aOwner;
  }

  public void setOwner(final Principal owner) {
    aOwner = owner;
  }

  @XmlAttribute(name = "owner")
  String getOwnerString() {
    return aOwner==null ? null : aOwner.getName();
  }

  void setOwnerString(final String owner) {
    aOwner = owner==null ? null : new SimplePrincipal(owner);
  }

  @XmlElement(name ="item", namespace=Constants.USER_MESSAGE_HANDLER_NS)
  @Override
  public List<XmlItem> getItems() {
    if (aItems==null) { aItems = new ArrayList<>(); }
    return aItems;
  }

  @Override
  public void setItems(List<? extends TaskItem> items) {
    aItems = new ArrayList<>(items.size());
    for(TaskItem item: items) {
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

  public static XmlTask get(UserTask<?> task) {
    if (task instanceof XmlTask) { return (XmlTask) task; }
    return new XmlTask(task);
  }

  public XmlItem getItem(String name) {
    for(XmlItem item: getItems()) {
      if (name.equals(item.getName())) {
        return item;
      }
    }
    return null;
  }


}