package nl.adaptivity.process.tasks.items;

import android.databinding.ViewDataBinding;
import nl.adaptivity.process.editor.android.databinding.TaskitemLabelBinding;
import nl.adaptivity.process.tasks.TaskItem;


public class LabelItem extends TaskItem {

  private String mValue;

  public LabelItem(String name, String value) {
    super(name);
    mValue = value;
  }

  @Override
  public Type getType() {
    return Type.LABEL;
  }

  @Override
  public void updateView(ViewDataBinding binding) {
    ((TaskitemLabelBinding) binding).setTaskitem(this);
  }

  @Override
  public boolean isDirty() {
    return false; // This is not an editor, so never dirty
  }

  @Override
  public void setDirty(final boolean dirty) {
    if (dirty) { throw new IllegalArgumentException("Labels cannot be dirty"); }
  }

  @Override
  public String getValue() {
    return mValue;
  }

  @Override
  public String getLabel() {
    return null;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public boolean isCompleteable() {
    return true; // labels don't stop completion
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    LabelItem labelItem = (LabelItem) o;

    return mValue != null ? mValue.equals(labelItem.mValue) : labelItem.mValue == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
    return result;
  }
}
