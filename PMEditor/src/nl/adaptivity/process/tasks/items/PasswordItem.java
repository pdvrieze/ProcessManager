package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class PasswordItem extends TextLabeledItem {

  private String mValue;

  public PasswordItem(String pName, String pLabel, String pValue) {
    super(pName, pLabel, pValue);
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
    TextView textview = (TextView) pDetail;
    textview.setText(mValue);
    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

}
