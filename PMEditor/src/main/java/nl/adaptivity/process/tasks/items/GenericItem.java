package nl.adaptivity.process.tasks.items;

import android.content.Context;
import android.databinding.Bindable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.editor.android.databinding.TaskitemGenericBinding;

import java.util.List;


public class GenericItem extends LabeledItem implements TextWatcher, OnClickListener {


  private static class ComboFilter extends Filter {

    private List<String> mOriginal;

    public ComboFilter(List<String> original) {
      mOriginal = original;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults result = new FilterResults();
      result.count=mOriginal.size();
      result.values=mOriginal;
      return result;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      // We don't change the results.
    }

  }

  private String mType;
  private ObservableList<String> mOptions;

  private static class ComboAdapter extends ArrayAdapter<String> {
    private List<String> mOriginal;

    public ComboAdapter(Context context, List<String> original) {
      super(context, android.R.layout.simple_dropdown_item_1line, original);
      mOriginal = original;
    }

    @Override
    public Filter getFilter() {
      return new ComboFilter(mOriginal);
    }
  }


  public GenericItem(String name, String label, String type, String value, List<String> options) {
    super(name, label, value);
    mType = type;
    mOptions = new ObservableArrayList<>();
    if (options!=null && options.size()>0) {
      mOptions.addAll(options);
    }
  }

  @Override
  public Type getType() {
    return Type.GENERIC;
  }


  @Override
  public String getDBType() {
    return mType;
  }

  @Bindable
  public ObservableList<String> getOptions() {
    return mOptions;
  }


  public void setOptions(List<String> options) {
    mOptions = new ObservableArrayList<>();
    if (options!=null && options.size()>0) {
      mOptions.addAll(options);
    }
    notifyPropertyChanged(BR.options);
  }

  @Override
  public void updateView(ViewDataBinding binding) {
    TaskitemGenericBinding b = (TaskitemGenericBinding) binding;
    b.setTaskitem(this);
    AutoCompleteTextView textview = b.taskitemDetailTextText;
    textview.setText(getValue());
    textview.setThreshold(1);
    textview.setAdapter(new ComboAdapter(textview.getContext(), mOptions));
    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.setTag(this);
    textview.addTextChangedListener(this);
    textview.setOnClickListener(this);
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    setValue(s.toString());
  }

  @Override
  public void afterTextChanged(Editable s) { /*do nothing*/ }

  @Override
  public void onClick(View v) {
    final AutoCompleteTextView tv = (AutoCompleteTextView) v;
    if (tv.getText().length()==0 &&  !tv.isPopupShowing()) {
      tv.showDropDown();
    }
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

    GenericItem that = (GenericItem) o;

    if (mType != null ? !mType.equals(that.mType) : that.mType != null) { return false; }
    return mOptions != null ? mOptions.equals(that.mOptions) : that.mOptions == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mType != null ? mType.hashCode() : 0);
    result = 31 * result + (mOptions != null ? mOptions.hashCode() : 0);
    return result;
  }
}