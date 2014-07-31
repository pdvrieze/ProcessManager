package nl.adaptivity.process.tasks.items;

import java.util.List;

import nl.adaptivity.process.editor.android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class TextItem extends LabeledItem {

  private List<String> mSuggestions;
  private String mValue;

  public TextItem(String pName, String pLabel, String pValue, List<String> pSuggestions) {
    super(pName, pLabel);
    mValue = pValue;
    mSuggestions = pSuggestions;
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_text, pParent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View pDetail) {
    TextView view = (TextView) pDetail;
    view.setText(mValue);
    // TODO use the options as suggestions
  }

  @Override
  public Type getType() {
    return Type.TEXT;
  }

}
