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

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Handles;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.xml.*;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlUtil;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlDeserializer(XmlTask.Factory.class)
public class XmlTask implements UserTask<XmlTask>, XmlSerializable, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory<XmlTask> {

    @Override
    public XmlTask deserialize(final XmlReader in) throws XmlException {
      return XmlTask.deserialize(in);
    }
  }

  public static final String ELEMENTLOCALNAME = "task";
  public static final QName ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh");

  private long mHandle = -1L;

  private long mRemoteHandle = -1L;

  private long mInstanceHandle = -1L;

  NodeInstanceState mState = NodeInstanceState.Sent;

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
    this.mHandle = task.getHandleValue();
    this.mRemoteHandle = task.getRemoteHandle();
    this.mInstanceHandle = task.getInstanceHandle();
    this.mState = task.getState();
    this.mSummary = task.getSummary();
    this.mEndPoint = null;
    this.mOwner = task.getOwner();
    this.mItems = new ArrayList<>(XmlItem.get(task.getItems()));
  }

  public static XmlTask deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.<nl.adaptivity.process.userMessageHandler.server.XmlTask>deserializeHelper(new XmlTask(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    if (XmlReaderUtil.isElement(in, Constants.USER_MESSAGE_HANDLER_NS, "item")) {
      if (mItems==null) { mItems = new ArrayList<>(); }
      mItems.add(XmlItem.deserialize(in));
      return true;
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "state": mState = NodeInstanceState.valueOf(attributeValue.toString()); return true;
      case "handle": mHandle = Long.parseLong(attributeValue.toString()); return true;
      case "remotehandle": mRemoteHandle = Long.parseLong(attributeValue.toString()); return true;
      case "instancehandle": mInstanceHandle = Long.parseLong(attributeValue.toString()); return true;
      case "summary": mSummary = attributeValue.toString(); return true;
      case "owner": mOwner = new SimplePrincipal(attributeValue.toString()); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {

  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    if (mState!=null) {
      XmlWriterUtil.writeAttribute(out, "state", mState.name());
    }
    if (mHandle>=0) {
      XmlWriterUtil.writeAttribute(out, "handle", mHandle);
    }
    if (mRemoteHandle>=0) {
      XmlWriterUtil.writeAttribute(out, "remotehandle", mRemoteHandle);
    }
    if (mInstanceHandle>=0) {
      XmlWriterUtil.writeAttribute(out, "instancehandle", mInstanceHandle);
    }
    XmlWriterUtil.writeAttribute(out, "summary", mSummary);
    if (mOwner!=null) {
      XmlWriterUtil.writeAttribute(out, "owner", mOwner.getName());
    }
    XmlWriterUtil.writeChildren(out, mItems);
    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  @Override
  public NodeInstanceState getState() {
    return mState;
  }

  void setState(NodeInstanceState state) {
    mState = state;
  }

  @Override
  public void setState(final NodeInstanceState newState, final Principal user) { // TODO handle transactions
    try {
      NodeInstanceState verifiedNewState;
      if (newState == NodeInstanceState.Complete) {
        verifiedNewState = finishRemoteTask(user).get();
        //          newState = TaskState.Complete; // Use server state instead.
      } else if (newState == NodeInstanceState.Acknowledged) {
        verifiedNewState = newState; // Just shortcircuit. This is just record keeping
      } else {
        verifiedNewState = updateRemoteTaskState(newState, user).get();
      }
      mState = verifiedNewState;
    } catch (final XmlException | JAXBException | MessagingException e) {
      Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
    } catch (final InterruptedException e) {
      Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
    } catch (final ExecutionException e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
    }
  }

  private Future<NodeInstanceState> updateRemoteTaskState(final NodeInstanceState state, final Principal user) throws JAXBException, MessagingException, XmlException {
    return ServletProcessEngineClient.updateTaskState(mRemoteHandle, state, user, null);
  }

  private Future<NodeInstanceState> finishRemoteTask(final Principal user) throws JAXBException, MessagingException, XmlException {
    return ServletProcessEngineClient.finishTask(mRemoteHandle, createResult(), user, null); // Ignore completion???
  }

  private DocumentFragment createResult() {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document document;
    try {
      document = dbf.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    DocumentFragment fragment = document.createDocumentFragment();

    Element outer = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "result");
    outer.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", Constants.USER_MESSAGE_HANDLER_NS);
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

    fragment.appendChild(outer);
    return fragment;
  }

  @Override
  public void setHandleValue(final long handle) {
    mHandle = handle;
  }

  @Override
  public long getHandleValue() {
    return mHandle;
  }

  @Override
  public Handle<? extends XmlTask> getHandle() {
    return Handles.handle(mHandle);
  }

  public void setRemoteHandle(final long handle) {
    mRemoteHandle = handle;
  }

  @Override
  public long getRemoteHandle() {
    return mRemoteHandle;
  }

  @XmlName("instancehandle")
  public void setInstanceHandle(final long handle) {
    mInstanceHandle = handle;
  }

  @Override
  public long getInstanceHandle() {
    return mInstanceHandle;
  }

  @Override
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

  @XmlName("owner")
  String getOwnerString() {
    return mOwner==null ? null : mOwner.getName();
  }

  void setOwnerString(final String owner) {
    mOwner = owner==null ? null : new SimplePrincipal(owner);
  }

  @XmlName(value ="item"/*, namespace=Constants.USER_MESSAGE_HANDLER_NS*/)
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
      if (other.mItems != null && ! other.mItems.isEmpty())
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