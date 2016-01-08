package nl.adaptivity.android.recyclerview;


/**
 * Created by pdvrieze on 04/01/16.
 */
public interface SelectableAdapter extends ClickableAdapter {

  public interface OnSelectionListener {

    void onSelectionChanged(SelectableAdapter adapter);
  }

  long getSelectedId();

  int getSelectedPos();

  boolean isAllowUnselection();

  OnSelectionListener getOnSelectionListener();

  boolean isSelectionEnabled();

  void setSelection(int position);

  void setSelectedItem(long itemId);

  void setOnSelectionListener(OnSelectionListener onSelectionListener);

  void setSelectionEnabled(boolean enabled);
}
