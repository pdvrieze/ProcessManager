package nl.adaptivity.process.tasks.items;

import android.databinding.ViewDataBinding;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import nl.adaptivity.process.editor.android.databinding.TaskitemListBinding;

import java.util.ArrayList;
import java.util.List;


public class ListItem extends LabeledItem implements OnItemSelectedListener {

  private List<String> mOptions;

  public ListItem(String name, String label, String value, List<String> options) {
    super(name, label, value);
    mOptions = options;
  }

  @Override
  public Type getType() {
    return Type.LIST;
  }

  @Override
  public void updateView(ViewDataBinding binding) {
    TaskitemListBinding b = (TaskitemListBinding) binding;
    b.setTaskitem(this);
    Spinner view = b.taskitemDetailList;
    String value = getValue();
    int index = AdapterView.INVALID_POSITION;
    if (value!=null) {
      if (mOptions == null) {
        mOptions=new ArrayList<>(1);
        mOptions.add(value);
        index = 0;
      } else {
        index = mOptions.indexOf(value);
        if (index<0) {
          mOptions.add(value);
          index = mOptions.size()-1;
        }
      }
    }
    view.setAdapter(mOptions==null ? null : new ArrayAdapter<>(view.getContext(), android.R.layout.simple_dropdown_item_1line, mOptions));
    view.setSelection(index, false);

    view.setOnItemSelectedListener(this);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    setValue(mOptions.get(position));
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    setValue(null);
  }

  @Override
  public boolean isCompleteable() {
    return getValue()!=null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    ListItem listItem = (ListItem) o;

    return mOptions != null ? mOptions.equals(listItem.mOptions) : listItem.mOptions == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mOptions != null ? mOptions.hashCode() : 0);
    return result;
  }
}
