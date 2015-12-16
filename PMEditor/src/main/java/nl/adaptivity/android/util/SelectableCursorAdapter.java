package nl.adaptivity.android.util;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import nl.adaptivity.android.util.SelectableCursorAdapter.SelectableViewHolder;


/**
 * Class that allows selection state to be maintained in a recyclerview.
 */
public abstract class SelectableCursorAdapter<VH extends SelectableViewHolder> extends CursorRecyclerViewAdapter<VH> {

  public abstract class SelectableViewHolder extends ViewHolder implements OnClickListener {

    public SelectableViewHolder(final View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
    }

    public void onClick(final View v) {
      onClickView(v, getAdapterPosition());
    }
  }

  private int mSelectionPos = RecyclerView.NO_POSITION;
  private long mSelectionId = RecyclerView.NO_ID;
  private final boolean mAllowUnselection;

  public SelectableCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {super(context, cursor);
    mAllowUnselection = allowUnselection;
  }

  public void onClickView(final View v, final int adapterPosition) {
    setSelection(adapterPosition);
  }

  @Override
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
      }
    } else {
      mSelectionPos = position;
      mSelectionId = itemId;
      if (position!=RecyclerView.NO_POSITION) {
        notifyItemChanged(position);
      }
    }
  }

  public boolean isAllowUnselection() {
    return mAllowUnselection;
  }
}
