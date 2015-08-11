package nl.adaptivity.process.tasks.items;

import android.text.Editable;
import android.text.TextWatcher;



public abstract class TextLabeledItem extends LabeledItem implements TextWatcher {

  public TextLabeledItem(String pName, String pLabel, String pValue) {
    super(pName, pLabel, pValue);
  }

  @Override
  public void beforeTextChanged(CharSequence pS, int pStart, int pCount, int pAfter) { /*do nothing*/ }

  @Override
  public void onTextChanged(CharSequence pS, int pStart, int pBefore, int pCount) {
    setValue(pS.toString());
  }

  @Override
  public void afterTextChanged(Editable pS) { /*do nothing*/ }

}
