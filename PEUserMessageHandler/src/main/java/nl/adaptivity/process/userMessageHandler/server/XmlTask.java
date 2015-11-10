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

  private long mHandle = -1L;

  private long mRemoteHandle = -1L;

  private long mInstanceHandle = -1L;

  TaskState mState = TaskState.Sent;

  private String mSummary;

  private EndpointDescriptorImpl mEndPoint = null;

  private Principal mOwner;

  private List<XmlItem> mItems;

  public XmlTask() {
    // no special operations here
  }

  public XmlTask(final long handle) {
    mHandle = handle;
  }

  public XmlTask(UserTask<?> task) {
    this.mHandle = task.getHandle();
    this.mRemoteHandle = task.getRemoteHandle();
    this.mInstanceHandle = task.getInstanceHandle();
    this.mState = task.getState();
    this.mSummary = task.getSummary();
    this.mEndPoint = null;
    this.mOwner = task.getOwner();
    this.mItems = new ArrayList<>(XmlItem.get(task.getItems()));
  }

  @XmlAttribute
  @Override
  public TaskState getState() {
    return mState;
  }

  void setState(TaskState state) {
    mState = state;
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
      mState = verifiedNewState;
    } catch (final JAXBException | MessagingException e) {
      Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
    } catch (final InterruptedException e) {
      Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
    } catch (final ExecutionException e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
    }
  }

  private Future<TaskState> updateRemoteTaskState(final TaskState state, final Principal user) throws JAXBException, MessagingException {
    return ServletProcessEngineClient.updateTaskState(mRemoteHandle, state, user, null);
  }

  private Future<TaskState> finishRemoteTask(final Principal user) throws JAXBException, MessagingException {
    return ServletProcessEngineClient.finishTask(mRemoteHandle, createResult(), user, null); // Ignore completion???
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
    mHandle = handle;
  }

  @Override
  public long getHandle() {
    return mHandle;
  }

  @XmlAttribute(name = "remotehandle")
  public void setRemoteHandle(final long handle) {
    mRemoteHandle = handle;
  }

  @Override
  public long getRemoteHandle() {
    return mRemoteHandle;
  }

  @XmlAttribute(name = "instancehandle")
  public void setInstanceHandle(final long handle) {
    mInstanceHandle = handle;
  }

  @Override
  public long getInstanceHandle() {
    return mInstanceHandle;
  }

  @Override
  @XmlAttribute(name = "summary")
  public String getSummary() {
    return mSummary;
  }

  public void setSummary(final String summary) {
    mSummary = summary;
  }

  /** Set the endpoint that is used for updating the task state */
  @Override
  public void setEndpoint(final EndpointDescriptorImpl endPoint) {
    mEndPoint = endPoint;
  }

  @Override
  public Principal getOwner() {
    return mOwner;
  }

  public void setOwner(final Principal owner) {
    mOwner = owner;
  }

  @XmlAttribute(name = "owner")
  String getOwnerString() {
    return mOwner==null ? null : mOwner.getName();
  }

  void setOwnerString(final String owner) {
    mOwner = owner==null ? null : new SimplePrincipal(owner);
  }

  @XmlElement(name ="item", namespace=Constants.USER_MESSAGE_HANDLER_NS)
  @Override
  public List<XmlItem> getItems() {
    if (mItems==null) { mItems = new ArrayList<>(); }
    return mItems;
  }

  @Override
  public void setItems(List<? extends TaskItem> items) {
    mItems = new ArrayList<>(items.size());
    for(TaskItem item: items) {
      mItems.add((XmlItem) item);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mEndPoint == null) ? 0 : mEndPoint.hashCode());
    result = prime * result + (int) (mHandle ^ (mHandle >>> 32));
    result = prime * result + ((mItems == null||mItems.isEmpty()) ? 0 : mItems.hashCode());
    result = prime * result + ((mOwner == null) ? 0 : mOwner.hashCode());
    result = prime * result + (int) (mRemoteHandle ^ (mRemoteHandle >>> 32));
    result = prime * result + ((mState == null) ? 0 : mState.hashCode());
    result = prime * result + ((mSummary == null) ? 0 : mSummary.hashCode());
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
    if (mEndPoint == null) {
      if (other.mEndPoint != null)
        return false;
    } else if (!mEndPoint.equals(other.mEndPoint))
      return false;
    if (mHandle != other.mHandle)
      return false;
    if (mItems == null || mItems.isEmpty()) {
      if (other.mItems != null && ! mItems.isEmpty())
        return false;
    } else if (!mItems.equals(other.mItems))
      return false;
    if (mOwner == null) {
      if (other.mOwner != null)
        return false;
    } else if (!mOwner.equals(other.mOwner))
      return false;
    if (mRemoteHandle != other.mRemoteHandle)
      return false;
    if (mState != other.mState)
      return false;
    if (mSummary == null) {
      if (other.mSummary != null)
        return false;
    } else if (!mSummary.equals(other.mSummary))
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