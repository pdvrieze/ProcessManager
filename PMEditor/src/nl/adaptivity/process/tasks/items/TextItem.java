package nl.adaptivity.process.tasks.items;

import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import nl.adaptivity.process.editor.android.R;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class TextItem extends TextLabeledItem {

  private List<String> mSuggestions;
  private ArrayAdapter<String> mSuggestionAdapter;

  public TextItem(String pName, String pLabel, String pValue, List<String> pSuggestions) {
    super(pName, pLabel, pValue);
    mSuggestions = pSuggestions;
  }

  @Override
  protected View createDetailView(LayoutInflater pInflater, FrameLayout pParent) {
    View view = pInflater.inflate(R.layout.taskitem_detail_text, pParent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View pDetail) {
    AutoCompleteTextView textview = (AutoCompleteTextView) pDetail;
    textview.setText(getValue());
    if (mSuggestions!=null && mSuggestions.size()>0) {
      if (mSuggestionAdapter == null) {
        mSuggestionAdapter = new ArrayAdapter<String>(pDetail.getContext(), android.R.layout.simple_dropdown_item_1line, mSuggestions);
      }
      textview.setAdapter(mSuggestionAdapter);
    }

    Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

  @Override
  public Type getType() {
    return Type.TEXT;
  }

}
