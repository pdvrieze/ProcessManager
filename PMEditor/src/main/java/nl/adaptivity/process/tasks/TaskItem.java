package nl.adaptivity.process.tasks;

import android.databinding.*;
import android.support.annotation.LayoutRes;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.items.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class TaskItem extends BaseObservable {

  public enum Type {
    LABEL("label", R.layout.taskitem_label) {

      @Override
      public TaskItem create(String name, String label, String value, List<String> options) {
        return new LabelItem(name,value==null ? label : value);
      }
    },

    GENERIC("generic", R.layout.taskitem_generic) {

      @Override
      public TaskItem create(String name, String label, String value, List<String> options) {
        return new GenericItem(name, label, "generic", value, options);
      }
    },
    TEXT("text", R.layout.taskitem_text) {

      @Override
      public TaskItem create(String name, String label, String value, List<String> options) {
        return new TextItem(name, label, value, options);
      }

    },
    LIST("list", R.layout.taskitem_list) {

      @Override
      public TaskItem create(String name, String label, String value, List<String> options) {
        return new ListItem(name, label, value, options);
      }

    },
    PASSWORD("password", R.layout.taskitem_password) {

      @Override
      public TaskItem create(String name, String label, String value, List<String> options) {
        return new PasswordItem(name, label, value);
      }

    }

    ;
    private String mStr;
    @LayoutRes public final int layoutId;

    Type(String str, @LayoutRes int layoutId) {
      mStr = str;
      this.layoutId = layoutId;
    }

    public abstract TaskItem create(String name, String label, String value, List<String> options);

    @Override
    public String toString() {
      return mStr;
    }

    static Type from(String s) {
      for(Type candidate:values()) {
        if (candidate.mStr.equals(s)) {
          return candidate;
        }
      }
      return null;
    }
  }

  public interface Factory<T extends TaskItem> {
    T create(String name, String label, String type, String value, List<String> options);
  }

  private enum Factories implements Factory<TaskItem>{
    DEFAULT_FACTORY {
      @Override
      public TaskItem create(String name, String label, String typeName, String value, List<String> options) {
        Type type = Type.from(typeName);
        if (type==null) {
          return new GenericItem(name, label, typeName, value, options);
        } else {
          return type.create(name, label, value, options);
        }

      }
    },
    GENERIC_FACTORY {
      @Override
      public GenericItem create(String name, String label, String type, String value, List<String> options) {
        return new GenericItem(name, label, type, value, options);
      }
    },

  }

  private String mName;

  protected TaskItem(String name) {
    mName = name;
  }

  @Bindable
  public abstract boolean isCompleteable();

  @Bindable
  public String getName() {
    return mName;
  }

  public void setName(String name) {
    mName = name;
  }

  public abstract Type getType();

  @Bindable
  public abstract boolean isDirty();

  public abstract void setDirty(boolean dirty);

  @Bindable
  public abstract String getValue();

  @Bindable
  public abstract String getLabel();

  protected String getDBType() {
    return getType().toString();
  }

  public static TaskItem create(String name, String label, String type, String value, List<String> options) {
    return defaultFactory().create(name, label, type, value, options);
  }

  public static Factory<TaskItem> defaultFactory() {
    return Factories.DEFAULT_FACTORY;
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  public static Factory<GenericItem> genericFactory() {
    return (Factory<GenericItem>) (Factory)Factories.GENERIC_FACTORY;
  }

  public abstract void updateView(ViewDataBinding v);

  public abstract boolean isReadOnly();

  /** Default implementation of getOptions() that returns the empty list. */
  protected ObservableList<String> getOptions() {
    return new ObservableArrayList<>();
  }

  public void serialize(XmlSerializer serializer, boolean serializeOptions) throws IllegalArgumentException, IllegalStateException, IOException {
    serializer.startTag(UserTask.NS_TASKS, UserTask.TAG_ITEM);
    if (getName()!=null) { serializer.attribute(null, "name", getName()); }
    if (getLabel()!=null) { serializer.attribute(null, "label", getLabel()); }
    if (getDBType()!=null) { serializer.attribute(null, "type", getDBType()); }
    if (getValue()!=null) { serializer.attribute(null, "value", getValue()); }
    if (serializeOptions) {
      for(String option: getOptions()) {
        serializer.startTag(UserTask.NS_TASKS, UserTask.TAG_OPTION);
        serializer.text(option);
        serializer.endTag(UserTask.NS_TASKS, UserTask.TAG_OPTION);

      }
    }
    serializer.endTag(UserTask.NS_TASKS, UserTask.TAG_ITEM);
  }

  public static GenericItem parseTaskGenericItem(XmlPullParser in) throws XmlPullParserException, IOException {
    return parseTaskItemHelper(in, genericFactory());
  }

  private static <T extends TaskItem> T parseTaskItemHelper(XmlPullParser in, TaskItem.Factory<T> factory) throws XmlPullParserException, IOException {
    in.require(XmlPullParser.START_TAG, UserTask.NS_TASKS, UserTask.TAG_ITEM);
    String name = in.getAttributeValue(null, "name");
    String label = in.getAttributeValue(null, "label");
    String type = in.getAttributeValue(null, "type");
    String value = in.getAttributeValue(null, "value");
    List<String> options = new ArrayList<>();
    while ((in.nextTag())==XmlPullParser.START_TAG) {
      in.require(XmlPullParser.START_TAG, UserTask.NS_TASKS, UserTask.TAG_OPTION);
      options.add(in.nextText());
      in.nextTag();
      in.require(XmlPullParser.END_TAG, UserTask.NS_TASKS, UserTask.TAG_OPTION);
    }
    in.require(XmlPullParser.END_TAG, UserTask.NS_TASKS, UserTask.TAG_ITEM);
    return factory.create(name, label, type, value, options);
  }

  public static TaskItem parseTaskItem(XmlPullParser in) throws XmlPullParserException, IOException {
    return TaskItem.parseTaskItemHelper(in, defaultFactory());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    TaskItem taskItem = (TaskItem) o;

    return mName != null ? mName.equals(taskItem.mName) : taskItem.mName == null;

  }

  @Override
  public int hashCode() {
    return mName != null ? mName.hashCode() : 0;
  }
}