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

  public TextItem(String name, String label, String value, List<String> suggestions) {
    super(name, label, value);
    mSuggestions = suggestions;
  }

  @Override
  protected View createDetailView(LayoutInflater inflater, FrameLayout parent) {
    View view = inflater.inflate(R.layout.taskitem_detail_text, parent, false);
    updateDetailView(view);
    return view;
  }

  @Override
  protected void updateDetailView(View detail) {
    AutoCompleteTextView textview = (AutoCompleteTextView) detail;
    textview.setText(getValue());
    if (mSuggestions!=null && mSuggestions.size()>0) {
      if (mSuggestionAdapter == null) {
        mSuggestionAdapter = new ArrayAdapter<String>(detail.getContext(), android.R.layout.simple_dropdown_item_1line, mSuggestions);
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

  @Override
  public boolean canComplete() {
    return getValue()!=null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    TextItem textItem = (TextItem) o;

    if (mSuggestions != null ? !mSuggestions.equals(textItem.mSuggestions) : textItem.mSuggestions != null) {
      return false;
    }
    return mSuggestionAdapter != null ? mSuggestionAdapter.equals(textItem.mSuggestionAdapter) : textItem.mSuggestionAdapter == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mSuggestions != null ? mSuggestions.hashCode() : 0);
    result = 31 * result + (mSuggestionAdapter != null ? mSuggestionAdapter.hashCode() : 0);
    return result;
  }
}
