package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class PasswordItem extends TextLabeledItem {

  public PasswordItem(String name, String label, String value) {
    super(name, label, value);
  }

  @Override
  public Type getType() {
    return Type.PASSWORD;
  }

  @Override
  protected View createDetailView(LayoutInflater inflater, FrameLayout parent) {
    View view = inflater.inflate(R.layout.taskitem_detail_password, parent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View detail) {
    TextView textview = (TextView) detail;
    textview.setText(getValue());
    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

  @Override
  public boolean canComplete() {
    return getValue()!=null;
  }
}
