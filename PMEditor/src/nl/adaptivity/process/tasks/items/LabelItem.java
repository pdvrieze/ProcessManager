package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.tasks.TaskItem;


public class LabelItem extends TaskItem {

  private String mValue;

  public LabelItem(String pName, String pValue) {
    super(pName);
    mValue = pValue;
  }

  @Override
  public Type getType() {
    return Type.LABEL;
  }

}
