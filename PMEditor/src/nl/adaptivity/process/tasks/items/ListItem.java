package nl.adaptivity.process.tasks.items;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;


public class ListItem extends LabeledItem {

  private String mValue;
  private List<String> mOptions;

  public ListItem(String pName, String pLabel, String pValue, List<String> pOptions) {
    super(pName, pLabel);
    mValue = pValue;
    mOptions = pOptions;
  }

  @Override
  public Type getType() {
    return Type.LIST;
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_list, pParent, false);
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
