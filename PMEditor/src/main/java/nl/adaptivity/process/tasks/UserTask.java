package nl.adaptivity.process.tasks;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.Log;
import nl.adaptivity.process.editor.android.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class UserTask {

  private static final String TAG = "UserTask";
  private static final int STATE_EDITABLE=1;
  private static final int STATE_AVAILABLE=2;

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

    private TaskState(String attrValue, @StringRes int labelId, @DrawableRes int decoratorId, final int state) {
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
      return (mState& STATE_EDITABLE)!=0;
    }

    public boolean isAvailable() {
      return (mState& STATE_AVAILABLE)!=0;
    }

    public static TaskState fromString(String state) {
      if (state==null) { return null; }
      for(TaskState candidate: values()) {
        if (state.equalsIgnoreCase(candidate.mAttrValue)) {
          return candidate;
        }
      }
      return null;
    }
  }

  public static final String NS_TASKS = "http://adaptivity.nl/userMessageHandler";
  public static final String TAG_TASKS = "tasks";
  public static final String TAG_TASK = "task";
  public static final String TAG_ITEM = "item";
  public static final String TAG_OPTION = "option";
  private String mSummary;
  private long mHandle;
  private String mOwner;
  private TaskState mState;
  private List<TaskItem> mItems;

  public UserTask(final String summary, final long handle, final String owner, final TaskState state, final List<TaskItem> items) {
    mSummary = summary;
    mHandle = handle;
    mOwner = owner;
    mState = state;
    mItems = items;
  }

  public String getSummary() {
    return mSummary;
  }


  public void setSummary(String summary) {
    mSummary = summary;
  }


  public long getHandle() {
    return mHandle;
  }


  public void setHandle(long handle) {
    mHandle = handle;
  }


  public String getOwner() {
    return mOwner;
  }


  public void setOwner(String owner) {
    mOwner = owner;
  }


  public TaskState getState() {
    return mState;
  }


  public void setState(TaskState state) {
    mState = state;
  }


  public List<TaskItem> getItems() {
    return mItems;
  }


  public void setItems(List<TaskItem> items) {
    mItems = items;
  }

  public boolean canComplete() {
    if (! mState.isEditable()) { return false; }
    for (TaskItem item:mItems) {
      if (! item.canComplete()) { return false; }
    }
    return true;
  }

  public static List<UserTask> parseTasks(InputStream in) throws XmlPullParserException, IOException {
    XmlPullParser parser;
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      parser = factory.newPullParser();
      parser.setInput(in, "utf-8");
    } catch (Exception e){
      Log.e(UserTask.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseTasks(in);
  }

  public static List<UserTask> parseTasks(XmlPullParser in) throws XmlPullParserException, IOException {
    if(in.getEventType()==XmlPullParser.START_DOCUMENT) {
      in.nextTag();
    }
    in.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASKS);
    ArrayList<UserTask> result = new ArrayList<>();
    while ((in.nextTag())==XmlPullParser.START_TAG) {
      result.add(parseTask(in));
    }
    in.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASKS);
    return result;
  }

  public static UserTask parseTask(XmlPullParser in) throws XmlPullParserException, IOException {
    in.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASK);
    String summary = in.getAttributeValue(null, "summary");
    long handle = Long.parseLong(in.getAttributeValue(null, "handle"));
    String owner = in.getAttributeValue(null, "owner");
    String state = in.getAttributeValue(null, "state");
    List<TaskItem> items = new ArrayList<>();
    while ((in.nextTag())==XmlPullParser.START_TAG) {
      items.add(TaskItem.parseTaskItem(in));
    }
    in.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASK);
    return new UserTask(summary, handle, owner, TaskState.fromString(state), items);
  }

}
