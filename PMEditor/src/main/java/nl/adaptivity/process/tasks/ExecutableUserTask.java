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
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.Log;
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.Util;
import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlStreaming.EventType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * UserTask implementation for execution of tasks (no modifyable params etc)
 */
@XmlDeserializer(ExecutableUserTask.Factory.class)
public class ExecutableUserTask extends UserTaskBase implements XmlSerializable {

  public static class Factory implements XmlDeserializerFactory<ExecutableUserTask> {

    @Override
    public ExecutableUserTask deserialize(final XmlReader in) throws XmlException {
      return ExecutableUserTask.deserialize(in);
    }
  }

  public enum TaskState {
    Available("Acknowledged", R.string.taskstate_available, R.drawable.decorator_taskstate_available, STATE_AVAILABLE),
    /**
     * Some tasks allow for alternatives (different users). Taken signifies that
     * the task has been claimed and others can not claim it anymore (unless
     * released again).
     */
    Taken("Taken", R.string.taskstate_taken, R.drawable.decorator_taskstate_accepted, STATE_EDITABLE),
    /**
     * Signifies that work on the task has actually started.
     */
    Started("Started", R.string.taskstate_started, R.drawable.decorator_taskstate_started, STATE_EDITABLE),
    /**
     * Signifies that the task is complete. This generally is the end state of a
     * task.
     */
    Complete("Complete", R.string.taskstate_complete, R.drawable.decorator_taskstate_completed, 0),
    /**
     * Signifies that the task has failed for some reason.
     */
    Failed("Failed", R.string.taskstate_failed, R.drawable.decorator_taskstate_failed, 0),
    /**
     * Signifies that the task has been cancelled (but not through a failure).
     */
    Cancelled("Cancelled", R.string.taskstate_cancelled, R.drawable.decorator_taskstate_cancelled, 0)
    ;

    private final String mAttrValue;
    private final int mLabelId;
    private final int mDecoratorId;
    private final int mState;

    private TaskState(final String attrValue, @StringRes final int labelId, @DrawableRes final int decoratorId, final int state) {
      mAttrValue = attrValue;
      mLabelId = labelId;
      mDecoratorId = decoratorId;
      mState=state;
    }

    public String getAttrValue() {
      return mAttrValue;
    }

    @StringRes
    public int getLabelId() {
      return mLabelId;
    }

    @DrawableRes
    public int getDecoratorId() {
      return mDecoratorId;
    }

    public boolean isEditable() {
      return (mState & STATE_EDITABLE) != 0;
    }

    public boolean isAvailable() {
      return (mState & STATE_AVAILABLE) != 0;
    }

    public static TaskState fromString(final String state) {
      if (state==null) { return null; }
      for(final TaskState candidate: TaskState.values()) {
        if (state.equalsIgnoreCase(candidate.mAttrValue)) {
          return candidate;
        }
      }
      return null;
    }
  }

  private static final String TAG = "UserTask";

  private static final int STATE_EDITABLE=1;
  private static final int STATE_AVAILABLE=2;
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
  protected TaskState mState;

  private String mSummary;
  private long mHandle;
  private String mOwner;
  private ObservableList<TaskItem> mItems;
  private boolean mDirty = false;
  private long mRemoteHandle;
  private long mInstanceHandle;

  private ExecutableUserTask() {
    mItems = new ObservableArrayList<>();
    // Constructor for deserialization.
  }

  public ExecutableUserTask(final String summary, final long handle, final String owner, final TaskState state, final List<TaskItem> items) {
    Log.d(TAG, "UserTask() called with " + "summary = [" + summary + "], handle = [" + handle + "], owner = [" + owner + "], state = [" + state + "], items = [" + items + "] -> " +toString() );
    mSummary = summary;
    mHandle = handle;
    mOwner = owner;
    mState = state;
    mItems = new ObservableArrayList<>();
    mItems.addAll(items);
    mItems.addOnListChangedCallback(mChangeCallback);
  }

  @Bindable
  public boolean isEditable() {
    return mState==null ? false :mState.isEditable();
  }

  @Bindable
  public String getSummary() {
    return mSummary;
  }

  private void setDirty(final boolean dirty, final int fieldId) {
    final boolean oldDirty = mDirty;
    mDirty = mDirty || dirty;
    if (dirty) {
      notifyPropertyChanged(fieldId);
      if (! oldDirty) {
        notifyPropertyChanged(BR.dirty);
      }
    }
  }

  public void setSummary(final String summary) {
    setDirty(! Util.equals(mSummary, summary), BR.summary);
    mSummary = summary;
  }


  public long getHandle() {
    return mHandle;
  }

  public void setHandle(final long handle) {
    mHandle = handle;
  }

  @Override
  protected CharSequence getRemoteHandle() {
    return mRemoteHandle>=0 ? Long.toString(mRemoteHandle) : null;
  }

  @Override
  protected void setRemoteHandle(final String remoteHandle) {
    mRemoteHandle = remoteHandle == null ? -1L : Long.parseLong(remoteHandle);
  }

  @Override
  protected CharSequence getInstanceHandle() {
    return mInstanceHandle>=0 ? Long.toString(mInstanceHandle) : null;
  }

  @Override
  protected void setInstanceHandle(final String instanceHandle) {
    mInstanceHandle = instanceHandle == null ? -1L : Long.parseLong(instanceHandle);
  }

  @Bindable
  public String getOwner() {
    return mOwner;
  }


  public void setOwner(final String owner) {
    setDirty(! Util.equals(mOwner, owner), BR.owner);
    mOwner = owner;
  }

  @Bindable
  public TaskState getState() {
    return mState;
  }


  public void setState(final TaskState state) {
    final boolean oldCanComplete = isCompleteable();
    final boolean oldEditable = isEditable();
    setDirty(! Util.equals(mState, state), BR.state);
    mState = state;
    if (oldCanComplete != isCompleteable()) {
      notifyPropertyChanged(BR.completeable);
    }
    if (oldEditable!=isEditable()) {
      notifyPropertyChanged(BR.editable);
    }
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

  @Bindable
  public boolean isCompleteable() {
    if (mState ==null || (! mState.isEditable())) { return false; }
    for (final TaskItem item:mItems) {
      if (! item.isCompleteable()) { return false; }
    }
    return true;
  }

  /**
   * Method that can be used to trigger completeability checks that trigger change notifications.
   */
  public boolean checkCompleteable(final boolean oldValue) {
    final boolean newValue = isCompleteable();
    if (newValue != oldValue) {
      notifyPropertyChanged(BR.completeable);
    }
    return newValue;
  }

  @Bindable
  public boolean isDirty() {
    if (mDirty) return true;
    for(final TaskItem i: mItems) {
      if (i.isDirty()) return true;
    }
    return false;
  }

  @Override
  protected void serializeAdditionalAttributes(final List<XmlSerializable> pending, final XmlWriter out) throws XmlException {
    if (mState!=null) { out.attribute(null, "state", null, mState.name()); }
    if (mHandle>=0) { out.attribute(null, "handle", null, Long.toString(mHandle)); }
  }

  public static ExecutableUserTask deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new ExecutableUserTask(), in);
  }

  @Override
  protected void parseTaskItem(final XmlReader in) throws XmlException {
    mItems.add(TaskItem.parseTaskItem(in));
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (StringUtil.isEqual("handle", attributeLocalName)) {
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  public static List<ExecutableUserTask> parseTasks(final InputStream in) throws XmlException {
    final XmlReader parser;
    try {
      parser = XmlStreaming.newReader(in, "UTF-8");
    } catch (Exception e){
      Log.e(ExecutableUserTask.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseTasks(parser);
  }

  public static List<ExecutableUserTask> parseTasks(final XmlReader in) throws XmlException {
    XmlUtil.skipPreamble(in);
    in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, TAG_TASKS);
    final ArrayList<ExecutableUserTask> result = new ArrayList<>();
    while ((in.nextTag())==EventType.START_ELEMENT) {
      result.add(deserialize(in));
    }
    in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, TAG_TASKS);
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"#"+System.identityHashCode(this);
  }
}
