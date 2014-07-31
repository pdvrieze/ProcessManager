package nl.adaptivity.process.tasks.items;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.editor.android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;


public class ListItem extends LabeledItem implements OnItemSelectedListener {

  private List<String> mOptions;

  public ListItem(String pName, String pLabel, String pValue, List<String> pOptions) {
    super(pName, pLabel, pValue);
    mOptions = pOptions;
  }

  @Override
  public Type getType() {
    return Type.LIST;
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_list, pParent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View pDetail) {
    Spinner view = (Spinner) pDetail;
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
    view.setAdapter(mOptions==null ? null : new ArrayAdapter<>(pDetail.getContext(), android.R.layout.simple_dropdown_item_1line, mOptions));
    view.setSelection(index, false);

    view.setOnItemSelectedListener(this);
  }

  @Override
  public void onItemSelected(AdapterView<?> pParent, View pView, int pPosition, long pId) {
    setValue(mOptions.get(pPosition));
  }

  @Override
  public void onNothingSelected(AdapterView<?> pParent) {
    setValue(null);
  }

}
