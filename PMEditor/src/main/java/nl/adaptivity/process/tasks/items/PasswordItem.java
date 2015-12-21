package nl.adaptivity.process.tasks.items;

import android.databinding.ViewDataBinding;
import android.text.TextWatcher;
import android.widget.TextView;
import nl.adaptivity.process.editor.android.databinding.TaskitemPasswordBinding;


public class PasswordItem extends TextLabeledItem {

  public PasswordItem(String name, String label, String value) {
    super(name, label, value);
  }

  @Override
  public Type getType() {
    return Type.PASSWORD;
  }

  @Override
  public void updateView(ViewDataBinding binding) {
    TaskitemPasswordBinding b = (TaskitemPasswordBinding) binding;
    b.setTaskitem(this);
    TextView textview = b.taskitemDetailTextText;
    textview.setText(getValue());
    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

  @Override
  public boolean isCompleteable() {
    return getValue()!=null;
  }
}
