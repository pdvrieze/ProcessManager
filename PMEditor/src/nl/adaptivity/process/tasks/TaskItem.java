package nl.adaptivity.process.tasks;

import java.util.List;

import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.process.tasks.items.LabelItem;
import nl.adaptivity.process.tasks.items.ListItem;
import nl.adaptivity.process.tasks.items.PasswordItem;
import nl.adaptivity.process.tasks.items.TextItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class TaskItem {

  public enum Type {
    LABEL("label") {

      @Override
      public TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions) {
        return new LabelItem(pName,pValue==null ? pLabel : pValue);
      }
    },

    GENERIC("generic") {

      @Override
      public TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions) {
        return new GenericItem(pName, pLabel, "generic", pValue, pOptions);
      }
    },
    TEXT("text") {

      @Override
      public TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions) {
        return new TextItem(pName, pLabel, pValue, pOptions);
      }

    },
    LIST("list") {

      @Override
      public TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions) {
        return new ListItem(pName, pLabel, pValue, pOptions);
      }

    },
    PASSWORD("password") {

      @Override
      public TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions) {
        return new PasswordItem(pName, pLabel, pValue);
      }

    }

    ;
    private String mStr;

    Type(String pStr) {
      mStr = pStr;
    }

    public abstract TaskItem create(String pName, String pLabel, String pValue, List<String> pOptions);

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
    T create(String pName, String pLabel, String pType, String pValue, List<String> pOptions);
  }

  private static enum Factories implements Factory<TaskItem>{
    DEFAULT_FACTORY {
      @Override
      public TaskItem create(String pName, String pLabel, String pType, String pValue, List<String> pOptions) {
        Type type = Type.from(pType);
        if (type==null) {
          return new GenericItem(pName, pLabel, pType, pValue, pOptions);
        } else {
          return type.create(pName, pLabel, pValue, pOptions);
        }

      }
    },
    GENERIC_FACTORY {
      @Override
      public GenericItem create(String pName, String pLabel, String pType, String pValue, List<String> pOptions) {
        return new GenericItem(pName, pLabel, pType, pValue, pOptions);
      }
    },

  }

  private String mName;

  protected TaskItem(String pName) {
    mName = pName;
  }

  public String getName() {
    return mName;
  }

  public void setName(String pName) {
    mName = pName;
  }

  public abstract Type getType();

  public abstract boolean isDirty();

  public abstract String getValue();

  protected String getDBType() {
    return getType().toString();
  }

  public static TaskItem create(String pName, String pLabel, String pType, String pValue, List<String> pOptions) {
    return defaultFactory().create(pName, pLabel, pType, pValue, pOptions);
  }

  public static Factory<TaskItem> defaultFactory() {
    return Factories.DEFAULT_FACTORY;
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  public static Factory<GenericItem> genericFactory() {
    return (Factory<GenericItem>) (Factory)Factories.GENERIC_FACTORY;
  }

  public abstract View createView(LayoutInflater pInflater, ViewGroup pParent);

  public abstract void updateView(View pV);

}