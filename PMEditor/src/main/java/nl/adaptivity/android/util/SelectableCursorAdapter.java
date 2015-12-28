package nl.adaptivity.android.util;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import nl.adaptivity.android.util.ClickableCursorAdapter.ClickableViewHolder;


/**
 * Class that allows selection state to be maintained in a recyclerview.
 */
public abstract class SelectableCursorAdapter<VH extends ClickableViewHolder> extends ClickableCursorAdapter<VH> {

  public interface OnSelectionListener {

    void onSelectionChanged(SelectableCursorAdapter<?> adapter);
  }

  private int mSelectionPos = RecyclerView.NO_POSITION;
  private long mSelectionId = RecyclerView.NO_ID;
  private final boolean mAllowUnselection;
  private boolean mSelectionEnabled = true;
  private OnSelectionListener mOnSelectionListener;

  public SelectableCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {super(context, cursor);
    mAllowUnselection = allowUnselection;
  }

  @Override
  public void onClickView(final ViewHolder viewHolder) {
    if (mSelectionEnabled) {
      setSelection(viewHolder.getAdapterPosition());
    }
  }

  @CallSuper
  public void onBindViewHolder(final VH viewHolder, final Cursor cursor) {
//    viewHolder.itemView.setSelected(viewHolder.getAdapterPosition()==mSelection);
    if (hasStableIds()) {
      viewHolder.itemView.setActivated(viewHolder.getItemId() == mSelectionId);
    } else {
      viewHolder.itemView.setActivated(viewHolder.getAdapterPosition() == mSelectionPos);
    }
  }

  public long getSelectedId() {
    return mSelectionId;
  }

  public int getSelectedPos() {
    return mSelectionPos;
  }

  public void setSelection(final int position) {
    final long itemId = getItemId(position);
    setSelection(position, itemId);
  }

  /**
   * Set the selection to the given ItemId. Note that this may loop through the entire cursor
   * to find the id.
   * @param itemId
   */
  public void setSelectedItem(final long itemId) {
    setSelection(getItemPos(itemId), itemId);
  }

  private void setSelection(final int position, final long itemId) {
    if (mSelectionPos != RecyclerView.NO_POSITION) {
      notifyItemChanged(mSelectionPos);
    }
    if (mSelectionPos == position) {
      if (isAllowUnselection()) {
        // unselect
        mSelectionPos = RecyclerView.NO_POSITION;
        mSelectionId = RecyclerView.NO_ID;
        if (mOnSelectionListener != null) { mOnSelectionListener.onSelectionChanged(this); }
      }
    } else {
      mSelectionPos = position;
      mSelectionId = itemId;
      if (mOnSelectionListener != null) { mOnSelectionListener.onSelectionChanged(this); }
      if (position!=RecyclerView.NO_POSITION) {
        notifyItemChanged(position);
      }
    }
  }

  public boolean isAllowUnselection() {
    return mAllowUnselection;
  }

  public OnSelectionListener getOnSelectionListener() {
    return mOnSelectionListener;
  }

  public void setOnSelectionListener(final OnSelectionListener onSelectionListener) {
    mOnSelectionListener = onSelectionListener;
  }

  public boolean isSelectionEnabled() {
    return mSelectionEnabled;
  }

  public void setSelectionEnabled(final boolean enabled) {
    mSelectionEnabled = enabled;
  }
}
