package nl.adaptivity.process.tasks.items;

import java.util.List;

import nl.adaptivity.process.tasks.TaskItem;


public class ListItem extends TaskItem {

  private String mValue;
  private List<String> mOptions;

  public ListItem(String pName, String pValue, List<String> pOptions) {
    super(pName);
    mValue = pValue;
    mOptions = pOptions;
  }

  @Override
  public Type getType() {
    return Type.LIST;
  }

}
