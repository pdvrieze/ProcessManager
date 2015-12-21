package nl.adaptivity.process.tasks.items;

import android.databinding.Bindable;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.tasks.TaskItem;


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

  public void setDirty(boolean dirty) {
    if (mDirty!=dirty) {
      mDirty = dirty;
      notifyPropertyChanged(BR.dirty);
    }
  }

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
