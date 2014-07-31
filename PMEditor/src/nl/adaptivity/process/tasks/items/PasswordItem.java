package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.tasks.TaskItem;


public class PasswordItem extends TaskItem {

  private String mValue;

  public PasswordItem(String pName, String pValue) {
    super(pName);
    mValue = pValue;
  }

  @Override
  public Type getType() {
    return Type.PASSWORD;
  }

}
