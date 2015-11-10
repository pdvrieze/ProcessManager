package nl.adaptivity.process.tasks;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class UserTask {

  public static final String NS_TASKS = "http://adaptivity.nl/userMessageHandler";
  public static final String TAG_TASKS = "tasks";
  public static final String TAG_TASK = "task";
  public static final String TAG_ITEM = "item";
  public static final String TAG_OPTION = "option";
  private String mSummary;
  private long mHandle;
  private String mOwner;
  private String mState;
  private List<TaskItem> mItems;

  public UserTask(String summary, long handle, String owner, String state, List<TaskItem> items) {
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


  public String getState() {
    return mState;
  }


  public void setState(String state) {
    mState = state;
  }


  public List<TaskItem> getItems() {
    return mItems;
  }


  public void setItems(List<TaskItem> items) {
    mItems = items;
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
    return new UserTask(summary, handle, owner, state, items);
  }

}
