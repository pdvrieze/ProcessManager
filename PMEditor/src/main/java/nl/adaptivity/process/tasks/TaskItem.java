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

import android.databinding.*;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.items.*;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;
import nl.adaptivity.xml.XmlWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.namespace.QName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@XmlDeserializer(TaskItem.DeserializerFactory.class)
public abstract class TaskItem extends BaseObservable implements XmlSerializable {

  public enum Type {
    LABEL("label", R.layout.taskitem_label) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<String> options) {
        return new LabelItem(name,value==null ? label : value);
      }
    },

    GENERIC("generic", R.layout.taskitem_generic) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<String> options) {
        return new GenericItem(name, label, "generic", value, options);
      }
    },
    TEXT("text", R.layout.taskitem_text) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<String> options) {
        return new TextItem(name, label, value, options);
      }

    },
    LIST("list", R.layout.taskitem_list) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<String> options) {
        return new ListItem(name, label, value, options);
      }

    },
    PASSWORD("password", R.layout.taskitem_password) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<String> options) {
        return new PasswordItem(name, label, value);
      }

    }

    ;
    private String mStr;
    @LayoutRes public final int layoutId;

    Type(final String str, @LayoutRes final int layoutId) {
      mStr = str;
      this.layoutId = layoutId;
    }

    public abstract TaskItem create(String name, String label, String value, List<String> options);

    @Override
    public String toString() {
      return mStr;
    }

    static Type from(final String s) {
      for(final Type candidate:values()) {
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
      public TaskItem create(final String name, final String label, final String typeName, final String value, final List<String> options) {
        final Type type = Type.from(typeName);
        if (type==null) {
          return new GenericItem(name, label, typeName, value, options);
        } else {
          return type.create(name, label, value, options);
        }

      }
    },
    GENERIC_FACTORY {
      @Override
      public GenericItem create(final String name, final String label, final String type, final String value, final List<String> options) {
        return new GenericItem(name, label, type, value, options);
      }
    },

  }

  public static class DeserializerFactory implements XmlDeserializerFactory<TaskItem> {

    @Override
    public TaskItem deserialize(final XmlReader in) throws XmlException {
      return TaskItem.deserialize(in);
    }
  }

  public static final String ELEMENTLOCALNAME = "item";
  private static final QName ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, Constants.USER_MESSAGE_HANDLER_NS_PREFIX);
  public static final String OPTION_ELEMENTLOCALNAME = "option";
  private static final QName OPTION_ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, OPTION_ELEMENTLOCALNAME, Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  private String mName;

  protected TaskItem(final String name) {
    mName = name;
  }

  @Bindable
  public abstract boolean isCompleteable();

  @Bindable
  public String getName() {
    return mName;
  }

  public void setName(final String name) {
    mName = name;
  }

  public abstract Type getType();

  @Bindable
  public abstract boolean isDirty();

  public abstract void setDirty(boolean dirty);

  @Bindable
  public abstract String getValue();

  public void setValue(String value) {
    throw new UnsupportedOperationException("Not supported by this task item");
  }

  public abstract boolean hasValueProperty();

  @Bindable
  public abstract String getLabel();

  public void setLabel(String value) {
    throw new UnsupportedOperationException("Not supported by this task item: ");
  }

  public abstract boolean hasLabelProperty();

  protected String getDBType() {
    return getType().toString();
  }

  public static TaskItem create(final String name, final String label, final String type, final String value, final List<String> options) {
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

  public static TaskItem deserialize(final XmlReader in) throws XmlException {
    return parseTaskItem(in);
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    serialize(out, true);
  }

  public void serialize(final XmlWriter out, final boolean serializeOptions) throws XmlException{
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    XmlUtil.writeAttribute(out, "name", getName());
    XmlUtil.writeAttribute(out, "label", getLabel());
    XmlUtil.writeAttribute(out, "type", getDBType());
    XmlUtil.writeAttribute(out, "value", getValue());
    if (serializeOptions) {
      for(final String option: getOptions()) {
        XmlUtil.writeSimpleElement(out, OPTION_ELEMENTNAME, option);
      }
    }
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  @Deprecated
  public void serialize(final XmlSerializer serializer, final boolean serializeOptions) throws IllegalArgumentException, IllegalStateException, IOException {
    serializer.startTag(Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_ITEM);
    if (getName()!=null) { serializer.attribute(null, "name", getName()); }
    if (getLabel()!=null) { serializer.attribute(null, "label", getLabel()); }
    if (getDBType()!=null) { serializer.attribute(null, "type", getDBType()); }
    if (getValue()!=null) { serializer.attribute(null, "value", getValue()); }
    if (serializeOptions) {
      for(final String option: getOptions()) {
        serializer.startTag(Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_OPTION);
        serializer.text(option);
        serializer.endTag(Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_OPTION);

      }
    }
    serializer.endTag(Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_ITEM);
  }

  public static GenericItem parseTaskGenericItem(final XmlReader in) throws XmlException {
    return parseTaskItemHelper(in, genericFactory());
  }

  private static <T extends TaskItem> T parseTaskItemHelper(@NonNull final XmlReader in, final Factory<T> factory) throws XmlException {
    XmlUtil.skipPreamble(in);
    in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_ITEM);
    final String name = StringUtil.toString(in.getAttributeValue(null, "name"));
    final String label = StringUtil.toString(in.getAttributeValue(null, "label"));
    final String type = StringUtil.toString(in.getAttributeValue(null, "type"));
    final String value = StringUtil.toString(in.getAttributeValue(null, "value"));
    final List<String> options = new ArrayList<>();
    while ((in.nextTag())==EventType.START_ELEMENT) {
      in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_OPTION);
      options.add(XmlUtil.nextText(in).toString());
      in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_OPTION);
    }
    in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTask.TAG_ITEM);
    return factory.create(name, label, type, value, options);
  }

  public static TaskItem parseTaskItem(final XmlReader in) throws XmlException {
    return TaskItem.parseTaskItemHelper(in, defaultFactory());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    final TaskItem taskItem = (TaskItem) o;

    return mName != null ? mName.equals(taskItem.mName) : taskItem.mName == null;

  }

  @Override
  public int hashCode() {
    return mName != null ? mName.hashCode() : 0;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"#"+System.identityHashCode(this);
  }

}