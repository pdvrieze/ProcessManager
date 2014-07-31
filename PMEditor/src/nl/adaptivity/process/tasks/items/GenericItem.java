package nl.adaptivity.process.tasks.items;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.editor.android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

public class GenericItem extends LabeledItem {

  private String mType;
  private String mValue;
  private List<String> mOptions;

  public GenericItem(String pName, String pLabel, String pType, String pValue, List<String> pOptions) {
    super(pName, pLabel);
    mType = pType;
    mValue = pValue;
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

  public String getValue() {
    return mValue;
  }


  public void setValue(String pValue) {
    mValue = pValue;
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
    textview.setText(mValue);
    textview.setAdapter(new ArrayAdapter<>(pDetail.getContext(), android.R.layout.simple_dropdown_item_1line, mOptions));
  }

}