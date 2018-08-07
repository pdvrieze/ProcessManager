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

import android.databinding.*;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.items.*;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.process.util.ModifyHelper;
import nl.adaptivity.process.util.ModifySequence.AttributeSequence;
import nl.adaptivity.xmlutil.*;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

@XmlDeserializer(TaskItem.DeserializerFactory.class)
public abstract class TaskItem extends BaseObservable implements XmlSerializable {

  public enum Type {
    LABEL("label", R.layout.taskitem_label) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<? extends String> options) {
        return new LabelItem(name,value==null ? label : value);
      }
    },

    GENERIC("generic", R.layout.taskitem_generic) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<? extends String> options) {
        return new GenericItem(name, label, "generic", value, options);
      }
    },
    TEXT("text", R.layout.taskitem_text) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<? extends String> options) {
        return new TextItem(name, label, value, options);
      }

    },
    LIST("list", R.layout.taskitem_list) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<? extends String> options) {
        return new ListItem(name, label, value, options);
      }

    },
    PASSWORD("password", R.layout.taskitem_password) {

      @Override
      public TaskItem create(final String name, final String label, final String value, final List<? extends String> options) {
        return new PasswordItem(name, label, value);
      }

    }

    ;
    private final           String mStr;
    @LayoutRes public final int    layoutId;

    Type(final String str, @LayoutRes final int layoutId) {
      mStr = str;
      this.layoutId = layoutId;
    }

    public abstract TaskItem create(String name, String label, String value, List<? extends String> options);

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
    T create(String name, String label, String type, String value, List<? extends String> options);
  }

  private enum Factories implements Factory<TaskItem>{
    DEFAULT_FACTORY {
      @Override
      public TaskItem create(final String name, final String label, final String typeName, final String value, final List<? extends String> options) {
        final Type type = typeName instanceof String ? Type.from((String)typeName) : null;
        if (type==null) {
          return new GenericItem(name, label, typeName, value, options);
        } else {
          return type.create(name, label, value, options);
        }

      }
    },
    GENERIC_FACTORY {
      @Override
      public GenericItem create(final String name, final String label, final String type, final String value, final List<? extends String> options) {
        return new GenericItem(name, label, type, value, options);
      }
    },

  }

  public static class DeserializerFactory implements XmlDeserializerFactory<TaskItem> {

    @Override
    public TaskItem deserialize(final XmlReader reader) {
      return TaskItem.deserialize(reader);
    }
  }

  public static final String ELEMENTLOCALNAME = "item";
  public static final QName ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME,
                                                    Constants.USER_MESSAGE_HANDLER_NS_PREFIX);
  public static final String OPTION_ELEMENTLOCALNAME = "option";
  public static final QName OPTION_ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, OPTION_ELEMENTLOCALNAME,
                                                           Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  private CharSequence mName;

  protected TaskItem(final CharSequence name) {
    mName = name;
  }

  @Bindable
  public abstract boolean isCompleteable();

  @Bindable
  public CharSequence getName() {
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
  public abstract CharSequence getValue();

  public void setValue(final CharSequence value) {
    throw new UnsupportedOperationException("Not supported by this task item");
  }

  public abstract boolean hasValueProperty();

  @Bindable
  public abstract CharSequence getLabel();

  public void setLabel(final CharSequence value) {
    throw new UnsupportedOperationException("Not supported by this task item: ");
  }

  public abstract boolean hasLabelProperty();

  protected CharSequence getDBType() {
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

  public static TaskItem deserialize(final XmlReader in) {
    return parseTaskItem(in);
  }

  @Override
  public void serialize(final XmlWriter out) {
    serialize(out, true);
  }

  public void serialize(final XmlWriter out, final boolean serializeOptions){
    XmlWriterUtilCore.smartStartTag(out, ELEMENTNAME);
    if ((! (getName() instanceof XmlSerializable))) {
      XmlWriterUtilCore.writeAttribute(out, "name", getName());
    }
    if ((! (getLabel() instanceof XmlSerializable))) {
      XmlWriterUtilCore.writeAttribute(out, "label", getLabel());
    }
    if ((! (getType() instanceof XmlSerializable))) {
      XmlWriterUtilCore.writeAttribute(out, "type", getDBType());
    }
    if ((! (getValue() instanceof XmlSerializable))) {
      XmlWriterUtilCore.writeAttribute(out, "value", getValue());
    }
    
    if (getName() instanceof XmlSerializable) { ((XmlSerializable) getName()).serialize(out); }
    if (getLabel() instanceof XmlSerializable) { ((XmlSerializable) getLabel()).serialize(out); }
    if (getType() instanceof XmlSerializable) { ((XmlSerializable) getType()).serialize(out); }
    if (getValue() instanceof XmlSerializable) { ((XmlSerializable) getValue()).serialize(out); }

    if (serializeOptions) {
      for(final String option: getOptions()) {
        XmlWriterUtilCore.writeSimpleElement(out, OPTION_ELEMENTNAME, option);
      }
    }
    XmlWriterUtilCore.endTag(out, ELEMENTNAME);
  }

  public static GenericItem parseTaskGenericItem(final XmlReader in) {
    return parseTaskItemHelper(in, genericFactory());
  }

  private static <T extends TaskItem> T parseTaskItemHelper(@NonNull final XmlReader in, final Factory<T> factory) {
    XmlReaderUtil.skipPreamble(in);
    in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTaskBase.TAG_ITEM);
    String name = StringUtil.toString(in.getAttributeValue(null, "name"));
    String label = StringUtil.toString(in.getAttributeValue(null, "label"));
    String type = StringUtil.toString(in.getAttributeValue(null, "type"));
    String value = StringUtil.toString(in.getAttributeValue(null, "value"));
    final List<String> options = new ArrayList<>();
    while ((in.nextTag())==EventType.START_ELEMENT) {
      if (StringUtil.isEqual(Constants.MODIFY_NS_STR, in.getNamespaceURI())) {
        if (StringUtil.isEqual("attribute", in.getLocalName())) {
          final AttributeSequence attr = ModifyHelper.parseAttribute(in);
          switch (attr.getParamName().toString()) {
            case "name":
              name = attr.toString(); break;
            case "label":
              label = attr.toString(); break;
            case "type":
              type = attr.toString(); break;
            case "value":
              value = attr.toString(); break;
            default:
                new XmlException("Unexpected attribute in process model").doThrow();
          }
        } else {
          throw new UnsupportedOperationException("Non-attribute replacements are not supported yet by the editor");
        }
      } else {
        in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTaskBase.TAG_OPTION);
        XmlReaderUtil.skipPreamble(in);
        if (in.getEventType()==EventType.START_ELEMENT) {
          if (StringUtil.isEqual(Constants.MODIFY_NS_STR, in.getNamespaceURI())) {
            options.add(ModifyHelper.parseAny(in).toString());
          } else {
            in.require(EventType.TEXT, null, null);
          }
        }
        options.add(XmlReaderUtil.allText(in).toString());
        in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTaskBase.TAG_OPTION);
      }
    }
    in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, UserTaskBase.TAG_ITEM);
    return factory.create(name, label, type, value, options);
  }

  public static TaskItem parseTaskItem(final XmlReader in) {
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