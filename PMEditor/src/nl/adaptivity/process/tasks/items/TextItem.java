package nl.adaptivity.process.tasks.items;

import java.util.List;

import nl.adaptivity.process.tasks.TaskItem;


public class TextItem extends TaskItem {

  private List<String> mSuggestions;
  private String mValue;

  public TextItem(String pName, String pValue, List<String> pSuggestions) {
    super(pName);
    mValue = pValue;
    mSuggestions = pSuggestions;
  }

  @Override
  public Type getType() {
    return Type.TEXT;
  }

}
