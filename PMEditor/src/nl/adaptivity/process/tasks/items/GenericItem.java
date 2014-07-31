package nl.adaptivity.process.tasks.items;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.tasks.TaskItem;

public class GenericItem extends TaskItem {

  private String mType;
  private String mValue;
  private List<String> mOptions;

  public GenericItem(String pName, String pType, String pValue, List<String> pOptions) {
    super(pName);
    mType = pType;
    mValue = pValue;
    mOptions = pOptions==null ? null : new ArrayList<>(pOptions);
  }

  @Override
  public Type getType() {
    return Type.GENERIC;
  }


  @Override
  public String getDBType() {
    return mType;
  }

  public String getValue() {
    return mValue;
  }


  public void setValue(String pValue) {
    mValue = pValue;
  }


  public List<String> getOptions() {
    return mOptions;
  }


  public void setOptions(List<String> pOptions) {
    mOptions = pOptions==null ? null : new ArrayList<>(pOptions);
  }

}