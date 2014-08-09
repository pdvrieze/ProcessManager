package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;


public abstract class LabeledItem extends TaskItem {

  private String aLabel;
  private String aValue;
  private boolean aDirty = false;

  public LabeledItem(String pName, String pLabel, String pValue) {
    super(pName);
    aValue = pValue;
    setLabel(pLabel);
  }

  public String getLabel() {
    return aLabel;
  }

  public void setLabel(String pLabel) {
    aLabel = pLabel;
  }

  public void setValue(String pValue) {
    if (aValue==null) {
      if (pValue!=null) { aDirty = true; }
    } else if (! aValue.equals(pValue)) {
      aDirty = true;
    }
    aValue = pValue;
  }

  public final String getValue() {
    return aValue;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public boolean isDirty() {
    return aDirty;
  }

  @Override
  public View createView(LayoutInflater pInflater, ViewGroup pParent) {
    View view = pInflater.inflate(R.layout.taskitem_labeled, pParent, false);
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(aLabel);
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
    labelView.setText(aLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = detailContainer.getChildAt(0);
    updateDetailView(detail);
  }

  protected abstract void updateDetailView(View pDetail);

}
