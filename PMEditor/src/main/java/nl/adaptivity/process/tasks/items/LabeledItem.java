package nl.adaptivity.process.tasks.items;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;


public abstract class LabeledItem extends TaskItem {

  private String mLabel;
  private String mValue;
  private boolean mDirty = false;

  public LabeledItem(String name, String label, String value) {
    super(name);
    mValue = value;
    setLabel(label);
  }

  public String getLabel() {
    return mLabel;
  }

  public void setLabel(String label) {
    mLabel = label;
  }

  public void setValue(String value) {
    if (mValue==null) {
      if (value!=null) { mDirty = true; }
    } else if (! mValue.equals(value)) {
      mDirty = true;
    }
    mValue = value;
  }

  public final String getValue() {
    return mValue;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public boolean isDirty() {
    return mDirty;
  }

  @Override
  public View createView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.taskitem_labeled, parent, false);
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(mLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = createDetailView(inflater, detailContainer);
    detailContainer.removeAllViews();
    detailContainer.addView(detail);
    return view;
  }

  protected abstract View createDetailView(LayoutInflater inflater, FrameLayout parent);

  @Override
  public void updateView(View view) {
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(mLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = detailContainer.getChildAt(0);
    updateDetailView(detail);
  }

  protected abstract void updateDetailView(View detail);

}
