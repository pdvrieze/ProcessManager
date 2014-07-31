package nl.adaptivity.process.tasks;

import java.util.List;

import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.process.tasks.items.LabelItem;
import nl.adaptivity.process.tasks.items.ListItem;
import nl.adaptivity.process.tasks.items.PasswordItem;
import nl.adaptivity.process.tasks.items.TextItem;

public abstract class TaskItem {

  public enum Type {
    LABEL("label") {

      @Override
      public TaskItem create(String pName, String pValue, List<String> pOptions) {
        return new LabelItem(pName, pValue);
      }
    },

    GENERIC("generic") {

      @Override
      public TaskItem create(String pName, String pValue, List<String> pOptions) {
        return new GenericItem(pName, "generic", pValue, pOptions);
      }
    },
    TEXT("text") {

      @Override
      public TaskItem create(String pName, String pValue, List<String> pOptions) {
        return new TextItem(pName, pValue, pOptions);
      }

    },
    LIST("list") {

      @Override
      public TaskItem create(String pName, String pValue, List<String> pOptions) {
        return new ListItem(pName, pValue, pOptions);
      }

    },
    PASSWORD("password") {

      @Override
      public TaskItem create(String pName, String pValue, List<String> pOptions) {
        return new PasswordItem(pName, pValue);
      }

    }

    ;
    private String mStr;

    Type(String pStr) {
      mStr = pStr;
    }

    public abstract TaskItem create(String pName, String pValue, List<String> pOptions);

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
    T create(String pName, String pType, String pValue, List<String> pOptions);
  }

  private static enum Factories implements Factory<TaskItem>{
    DEFAULT_FACTORY {
      @Override
      public TaskItem create(String pName, String pType, String pValue, List<String> pOptions) {
        Type type = Type.from(pType);
        if (type==null) {
          return new GenericItem(pName, pType, pValue, pOptions);
        } else {
          return type.create(pName, pValue, pOptions);
        }

      }
    },
    GENERIC_FACTORY {
      @Override
      public GenericItem create(String pName, String pType, String pValue, List<String> pOptions) {
        return new GenericItem(pName, pType, pValue, pOptions);
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

  protected String getDBType() {
    return getType().toString();
  }

  public static TaskItem create(String pName, String pType, String pValue, List<String> pOptions) {
    return defaultFactory().create(pName, pType, pValue, pOptions);
  }

  public static Factory<TaskItem> defaultFactory() {
    return Factories.DEFAULT_FACTORY;
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  public static Factory<GenericItem> genericFactory() {
    return (Factory<GenericItem>) (Factory)Factories.GENERIC_FACTORY;
  }

}