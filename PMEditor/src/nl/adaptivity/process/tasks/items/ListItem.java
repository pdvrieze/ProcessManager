package nl.adaptivity.process.tasks.items;

import java.util.List;

import nl.adaptivity.process.editor.android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;


public class ListItem extends LabeledItem {

  private String mValue;
  private List<String> mOptions;

  public ListItem(String pName, String pLabel, String pValue, List<String> pOptions) {
    super(pName, pLabel);
    mValue = pValue;
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

    view.setAdapter(new ArrayAdapter<>(pDetail.getContext(), android.R.layout.simple_dropdown_item_1line, mOptions));
//    view.setText(mValue);
    // TODO use the options as suggestions
  }

}
