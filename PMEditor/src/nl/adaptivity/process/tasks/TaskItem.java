package nl.adaptivity.process.tasks;

import java.util.ArrayList;
import java.util.List;

public class TaskItem {

  private String mName;
  private String mType;
  private String mValue;
  private List<String> mOptions;

  public TaskItem(String pName, String pType, String pValue, List<String> pOptions) {
    mName = pName;
    mType = pType;
    mValue = pValue;
    mOptions = pOptions==null ? null : new ArrayList<>(pOptions);
  }


  public String getName() {
    return mName;
  }


  public void setName(String pName) {
    mName = pName;
  }


  public String getType() {
    return mType;
  }


  public void setType(String pType) {
    mType = pType;
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