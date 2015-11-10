package nl.adaptivity.process.tasks.items;

import android.text.Editable;
import android.text.TextWatcher;



public abstract class TextLabeledItem extends LabeledItem implements TextWatcher {

  public TextLabeledItem(String name, String label, String value) {
    super(name, label, value);
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    setValue(s.toString());
  }

  @Override
  public void afterTextChanged(Editable s) { /*do nothing*/ }

}
