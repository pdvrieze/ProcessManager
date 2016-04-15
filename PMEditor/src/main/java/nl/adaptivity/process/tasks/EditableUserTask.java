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

package nl.adaptivity.process.tasks;

import android.databinding.Bindable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.databinding.ObservableList.OnListChangedCallback;
import android.util.Log;
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.process.util.ModifyHelper;
import nl.adaptivity.process.util.ModifySequence;
import nl.adaptivity.process.util.ModifySequence.AttributeSequence;
import nl.adaptivity.util.Util;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.*;
import org.w3.soapEnvelope.Envelope;

import javax.xml.namespace.QName;

import java.io.StringWriter;
import java.util.List;


/**
 * UserTask implementation for editing of tasks.
 */
@XmlDeserializer(EditableUserTask.Factory.class)
public class EditableUserTask extends UserTaskBase {

  public static class Factory implements XmlDeserializerFactory<EditableUserTask> {

    @Override
    public EditableUserTask deserialize(final XmlReader in) throws XmlException {
      return EditableUserTask.deserialize(in);
    }
  }

  private final OnListChangedCallback mChangeCallback = new OnListChangedCallback() {

    @Override
    public void onChanged(final ObservableList sender) {
      if (sender==mItems) {
        setDirty(true, BR.items);
      }
    }

    @Override
    public void onItemRangeChanged(final ObservableList sender, final int positionStart, final int itemCount) {
      if (sender==mItems) {
        setDirty(true, BR.items);
        notifyPropertyChanged(BR.completeable);
      }
    }

    @Override
    public void onItemRangeInserted(final ObservableList sender, final int positionStart, final int itemCount) {
      onItemRangeChanged(sender, positionStart, itemCount);
    }

    @Override
    public void onItemRangeMoved(final ObservableList sender, final int fromPosition, final int toPosition, final int itemCount) {
      if (sender==mItems) {
        setDirty(true, BR.items);
      }
    }

    @Override
    public void onItemRangeRemoved(final ObservableList sender, final int positionStart, final int itemCount) {
      if (sender==mItems) {
        setDirty(true, BR.items);
        notifyPropertyChanged(BR.completeable);
      }
    }
  };

  private CharSequence mSummary;
  private CharSequence mHandle;
  private CharSequence mOwner;
  private ObservableList<TaskItem> mItems;
  private CharSequence mInstanceHandle;
  private CharSequence mRemoteHandle;

  private EditableUserTask() {
    mItems = new ObservableArrayList<>();
    // Constructor for deserialization.
  }

  public EditableUserTask(final CharSequence summary, final CharSequence handle, final CharSequence owner, final List<TaskItem> items) {
    Log.d(TAG, "UserTask() called with " + "summary = [" + summary + "], handle = [" + handle + "], owner = [" + owner + "], items = [" + items + "] -> " +toString() );
    mSummary = summary;
    mHandle = handle;
    mOwner = owner;
    mItems = new ObservableArrayList<>();
    mItems.addAll(items);
    mItems.addOnListChangedCallback(mChangeCallback);
  }

  public XmlMessage asMessage() {
    QName service  = UserTaskServiceDescriptor.SERVICENAME;
    String endpoint = UserTaskServiceDescriptor.ENDPOINT;
    String operation = PostTask.ELEMENTLOCALNAME;
    StringWriter bodyWriter = new StringWriter();
    try {
      XmlWriter writer = XmlStreaming.newWriter(bodyWriter, true);
      Envelope<PostTask> envelope = new Envelope<>(new PostTask(this));

      envelope.serialize(writer);
      writer.close();
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }

    XmlMessage result = new XmlMessage(service, endpoint, operation, null, null, null, new CompactFragment(bodyWriter.toString()));
    return result;
  }

  @Bindable
  public CharSequence getSummary() {
    return mSummary;
  }

  private void setDirty(final boolean dirty, final int fieldId) {
    if (dirty) {
      notifyPropertyChanged(fieldId);
    }
  }

  public void setSummary(final String summary) {
    setDirty(! Util.equals(mSummary, summary), BR.summary);
    mSummary = summary;
  }

  @Override
  protected void setRemoteHandle(final String remoteHandle) {
    mRemoteHandle = remoteHandle;
  }

  @Override
  protected CharSequence getRemoteHandle() {
    return mRemoteHandle!=null ? mRemoteHandle : ModifySequence.newAttributeSequence("remotehandle", "handle", null);
  }

  @Override
  protected void setInstanceHandle(final String instanceHandle) {
    mInstanceHandle = instanceHandle;
  }

  @Override
  protected CharSequence getInstanceHandle() {
    return mInstanceHandle!=null ? mInstanceHandle : ModifySequence.newAttributeSequence("instancehandle", "instancehandle", null);
  }

  @Bindable
  public CharSequence getOwner() {
    return mOwner != null ? mOwner : ModifySequence.newAttributeSequence("owner", "owner", null);
  }


  public void setOwner(final String owner) {
    setDirty(! Util.equals(mOwner, owner), BR.owner);
    mOwner = owner;
  }

  @Bindable
  public ObservableList<TaskItem> getItems() {
    return mItems;
  }


  public void setItems(final List<TaskItem> items) {
    // No need to check for dirtyness, the list will do that itself.
    if (items==null || items.isEmpty()) {
      mItems.clear();
    } else {
      CollectionUtil.mergeLists(mItems, items);
    }
  }

  public static EditableUserTask deserialize(XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new EditableUserTask(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    if (StringUtil.isEqual(Constants.MODIFY_NS_STR, in.getNamespaceUri())) {
      AttributeSequence attrVar = ModifyHelper.parseAttribute(in);
      switch (attrVar.getParamName().toString()) {
        case "summary": mSummary = attrVar; return true;
        case "handle": mHandle = attrVar; return true;
        case "instancehandle": mInstanceHandle = attrVar; return true;
        case "remotehandle": mRemoteHandle = attrVar; return true;
        case "owner": mOwner = attrVar; return true;
      }
    }
    return super.deserializeChild(in);
  }

  @Override
  protected void parseTaskItem(final XmlReader in) throws XmlException {
    mItems.add(TaskItem.parseTaskItem(in));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"#"+System.identityHashCode(this);
  }
}
