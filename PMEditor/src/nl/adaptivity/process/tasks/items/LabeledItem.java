package nl.adaptivity.process.tasks.items;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;


public abstract class LabeledItem extends TaskItem {

  private String mLabel;

  public LabeledItem(String pName, String pLabel) {
    super(pName);
    setLabel(pLabel);
  }

  public String getLabel() {
    return mLabel;
  }

  public void setLabel(String pLabel) {
    mLabel = pLabel;
  }

  @Override
  public View createView(LayoutInflater pInflater, ViewGroup pParent) {
    View view = pInflater.inflate(R.layout.taskitem_labeled, pParent, false);
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(mLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = createDetailView(pInflater, detailContainer);
    detailContainer.removeAllViews();
    detailContainer.addView(detail);
    return view;
  }

  protected abstract View createDetailView(LayoutInflater pInflater, FrameLayout pParent);

  @Override
  public void updateView(View view) {
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(mLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = detailContainer.getChildAt(0);
    updateDetailView(detail);
  }

  protected abstract void updateDetailView(View pDetail);

}
