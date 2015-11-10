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

  public LabeledItem(String name, String label, String value) {
    super(name);
    aValue = value;
    setLabel(label);
  }

  public String getLabel() {
    return aLabel;
  }

  public void setLabel(String label) {
    aLabel = label;
  }

  public void setValue(String value) {
    if (aValue==null) {
      if (value!=null) { aDirty = true; }
    } else if (! aValue.equals(value)) {
      aDirty = true;
    }
    aValue = value;
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
  public View createView(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.taskitem_labeled, parent, false);
    TextView labelView = (TextView) view.findViewById(R.id.taskitem_labeled_label);
    labelView.setText(aLabel);
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
    labelView.setText(aLabel);
    FrameLayout detailContainer = (FrameLayout) view.findViewById(R.id.taskitem_labeled_detail);
    View detail = detailContainer.getChildAt(0);
    updateDetailView(detail);
  }

  protected abstract void updateDetailView(View detail);

}
