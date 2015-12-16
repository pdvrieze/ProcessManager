package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class LabelItem extends TaskItem {

  private String mValue;

  public LabelItem(String name, String value) {
    super(name);
    mValue = value;
  }

  @Override
  public Type getType() {
    return Type.LABEL;
  }

  @Override
  public View createView(LayoutInflater inflater, ViewGroup parent) {
    TextView view = (TextView) inflater.inflate(R.layout.taskitem_label, parent, false);
    view.setText(mValue);
    return view;
  }

  @Override
  public void updateView(View v) {
    ((TextView) v).setText(mValue);
  }

  @Override
  public boolean isDirty() {
    return false; // This is not an editor, so never dirty
  }

  @Override
  public String getValue() {
    return mValue;
  }

  @Override
  public String getLabel() {
    return null;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

}
