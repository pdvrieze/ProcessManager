package nl.adaptivity.process.tasks.items;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;

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
    TextView view = (TextView) pDetail;
    view.setText(mValue);
    // TODO use the options as suggestions
  }

}