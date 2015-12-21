package nl.adaptivity.process.tasks.items;

import android.databinding.Bindable;
import nl.adaptivity.process.editor.android.BR;
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

  @Bindable
  public String getLabel() {
    return mLabel;
  }

  public void setLabel(String label) {
    mLabel = label;
    notifyPropertyChanged(BR.label);
  }

  public void setValue(String value) {
    boolean dirty = false;
    if (mValue==null) {
      if (value!=null) { dirty = true; }
    } else if (! mValue.equals(value)) {
      dirty = true;
    }
    boolean oldCanComplete = isCompleteable();
    mValue = value;
    if (dirty) {
      notifyPropertyChanged(BR.value);
      setDirty(true);
      if (oldCanComplete != isCompleteable()) {
        notifyPropertyChanged(BR.completeable);
      }
    }
  }

  @Bindable
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

  protected void setDirty(boolean dirty) {
    if (mDirty!=dirty) {
      mDirty = dirty;
      notifyPropertyChanged(BR.dirty);
    }
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    LabeledItem that = (LabeledItem) o;

    if (mLabel != null ? !mLabel.equals(that.mLabel) : that.mLabel != null) { return false; }
    return mValue != null ? mValue.equals(that.mValue) : that.mValue == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mLabel != null ? mLabel.hashCode() : 0);
    result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
    return result;
  }
}
