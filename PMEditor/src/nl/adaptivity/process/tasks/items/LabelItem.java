package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


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

  @Override
  public View createView(LayoutInflater pInflater, ViewGroup pParent) {
    TextView view = (TextView) pInflater.inflate(R.layout.taskitem_label, pParent, false);
    view.setText(mValue);
    return view;
  }

  @Override
  public void updateView(View pV) {
    ((TextView) pV).setText(mValue);
  }

  @Override
  public boolean isDirty() {
    return false; // This is not an editor, so never dirty
  }

  @Override
  public String getValue() {
    return mValue;
  }

}
