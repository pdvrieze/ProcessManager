package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class PasswordItem extends LabeledItem {

  private String mValue;

  public PasswordItem(String pName, String pLabel, String pValue) {
    super(pName, pLabel);
    mValue = pValue;
  }

  @Override
  public Type getType() {
    return Type.PASSWORD;
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_password, pParent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View pDetail) {
    TextView view = (TextView) pDetail;
    view.setText(mValue);
    // TODO use the options as suggestions
  }

}
