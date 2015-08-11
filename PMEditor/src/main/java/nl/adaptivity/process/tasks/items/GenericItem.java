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

public class GenericItem extends LabeledItem implements TextWatcher, OnClickListener {


  private static class ComboFilter extends Filter {

    private List<String> mOriginal;

    public ComboFilter(List<String> pOriginal) {
      mOriginal = pOriginal;
    }

    @Override
    protected FilterResults performFiltering(CharSequence pConstraint) {
      FilterResults result = new FilterResults();
      result.count=mOriginal.size();
      result.values=mOriginal;
      return result;
    }

    @Override
    protected void publishResults(CharSequence pConstraint, FilterResults pResults) {
      // We don't change the results.
    }

  }

  private String mType;
  private List<String> mOptions;

  private static class ComboAdapter extends ArrayAdapter<String> {
    private List<String> mOriginal;

    public ComboAdapter(Context pContext, List<String> pOriginal) {
      super(pContext, android.R.layout.simple_dropdown_item_1line, pOriginal);
      mOriginal = pOriginal;
    }

    @Override
    public Filter getFilter() {
      return new ComboFilter(mOriginal);
    }
  }


  public GenericItem(String pName, String pLabel, String pType, String pValue, List<String> pOptions) {
    super(pName, pLabel, pValue);
    mType = pType;
    mOptions = pOptions==null ? null : new ArrayList<>(pOptions);
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


  public void setOptions(List<String> pOptions) {
    mOptions = pOptions==null ? null : new ArrayList<>(pOptions);
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_generic, pParent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View pDetail) {
    AutoCompleteTextView textview = (AutoCompleteTextView) pDetail.findViewById(R.id.taskitem_detail_text_text);
    textview.setText(getValue());
    textview.setThreshold(1);
    textview.setAdapter(new ComboAdapter(pDetail.getContext(), mOptions));
    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.setTag(this);
    textview.addTextChangedListener(this);
    textview.setOnClickListener(this);
  }

  @Override
  public void beforeTextChanged(CharSequence pS, int pStart, int pCount, int pAfter) { /*do nothing*/ }

  @Override
  public void onTextChanged(CharSequence pS, int pStart, int pBefore, int pCount) {
    setValue(pS.toString());
  }

  @Override
  public void afterTextChanged(Editable pS) { /*do nothing*/ }

  @Override
  public void onClick(View pV) {
    final AutoCompleteTextView tv = (AutoCompleteTextView) pV;
    if (tv.getText().length()==0 &&  !tv.isPopupShowing()) {
      tv.showDropDown();
    }
  }

}