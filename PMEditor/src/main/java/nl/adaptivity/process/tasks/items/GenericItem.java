package nl.adaptivity.process.tasks.items;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.editor.android.R;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.FrameLayout;
import nl.adaptivity.util.Util;


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
  private List<String> mOptions;

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
    mOptions = options==null ? null : new ArrayList<>(options);
  }

  @Override
  public Type getType() {
    return Type.GENERIC;
  }


  @Override
  public String getDBType() {
    return mType;
  }


  public List<String> getOptions() {
    return mOptions;
  }


  public void setOptions(List<String> options) {
    mOptions = options==null ? null : new ArrayList<>(options);
  }

  @Override
  protected View createDetailView(LayoutInflater inflater, FrameLayout parent) {
    View view = inflater.inflate(R.layout.taskitem_detail_generic, parent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View detail) {
    AutoCompleteTextView textview = (AutoCompleteTextView) detail.findViewById(R.id.taskitem_detail_text_text);
    textview.setText(getValue());
    textview.setThreshold(1);
    textview.setAdapter(new ComboAdapter(detail.getContext(), mOptions));
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
  public boolean canComplete() {
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