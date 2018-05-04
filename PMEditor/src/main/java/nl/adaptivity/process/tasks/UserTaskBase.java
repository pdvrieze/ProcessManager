/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.tasks;

import android.databinding.BaseObservable;
import android.support.annotation.CallSuper;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.process.util.ModifyHelper;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;
import android.support.annotation.NonNull;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pdvrieze on 15/02/16.
 */
public abstract class UserTaskBase extends BaseObservable implements XmlSerializable, SimpleXmlDeserializable {

  public static final String TAG_TASKS = "tasks";
  public static final String ELEMENTLOCALNAME = "task";
  public static final QName ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME,
                                                    Constants.USER_MESSAGE_HANDLER_NS_PREFIX);
  public static final String TAG_ITEM = "item";
  public static final String TAG_OPTION = "option";
  protected static final String TAG = "UserTask";



  @Override
  public void serialize(final XmlWriter out) {
    XmlWriterUtilCore.smartStartTag(out, ELEMENTNAME);
    final List<XmlSerializable> pending = new ArrayList<>();
    ModifyHelper.writeAttribute(pending, out, "summary", getSummary());
    ModifyHelper.writeAttribute(pending, out, "instancehandle", getInstanceHandle());
    ModifyHelper.writeAttribute(pending, out, "remotehandle", getRemoteHandle());
    ModifyHelper.writeAttribute(pending, out, "owner", getOwner());
    serializeAdditionalAttributes(pending, out);
    for (final XmlSerializable item : pending) {
      item.serialize(out);
    }
    for(final TaskItem item: getItems()) {
      item.serialize(out);
    }
    XmlWriterUtilCore.endTag(out, ELEMENTNAME);
  }

  @CallSuper
  @Override
  public boolean deserializeChild(@NonNull final XmlReader reader) {
    if (XmlReaderUtil.isElement(reader, EventType.START_ELEMENT, TaskItem.ELEMENTNAME.getNamespaceURI(), TaskItem.ELEMENTNAME
                                                                                                                 .getLocalPart(), TaskItem.ELEMENTNAME
                                                                                                                                          .getPrefix())) {
      parseTaskItem(reader);
      return true;
    }
    return false;
  }

  /**
   * Parse the current task item.
   * @param in
   */
  protected abstract void parseTaskItem(final XmlReader in);

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final String attributeNamespace, final String attributeLocalName, final String attributeValue) {
    switch(attributeLocalName.toString()) {
      case "summary": setSummary(attributeValue.toString()); return true;
      case "instancehandle": setInstanceHandle(attributeValue.toString()); return true;
      case "remotehandle": setRemoteHandle(attributeValue.toString()); return true;
      case "owner": setOwner(attributeValue.toString()); return true;
    }
    return false;
  }

  protected abstract void setRemoteHandle(final String remoteHandle);

  protected abstract void setInstanceHandle(final String instanceHandle);

  protected abstract void setOwner(final String owner);

  protected abstract void setSummary(final String summary);

  @Override
  public void onBeforeDeserializeChildren(final XmlReader reader) {
    // do nothing
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  /**
   * Serialize additional attributes that are not known by the baseclass. The default implementation does nothing.
   * @param pending Pending events (mainly modifyable values)
   * @param out The writer to write the attributes to.
   */
  protected void serializeAdditionalAttributes(final List<XmlSerializable> pending, final XmlWriter out) {
    // Just a hook
  }

  protected abstract Iterable<? extends TaskItem> getItems();

  protected abstract CharSequence getOwner();

  protected abstract CharSequence getSummary();

  protected abstract CharSequence getRemoteHandle();

  protected abstract CharSequence getInstanceHandle();

}
