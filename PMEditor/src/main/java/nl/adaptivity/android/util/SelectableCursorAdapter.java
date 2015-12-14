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

  private int mSelection = RecyclerView.NO_POSITION;

  public SelectableCursorAdapter(final Context context, final Cursor cursor) {super(context, cursor);}

  public void onClickView(final View v, final int adapterPosition) {
    setSelection(adapterPosition);
  }

  @Override
  @CallSuper
  public void onBindViewHolder(final VH viewHolder, final Cursor cursor) {
    viewHolder.itemView.setSelected(viewHolder.getAdapterPosition()==mSelection);
  }

  public int getSelection() {
    return mSelection;
  }

  public void setSelection(final int position) {
    if (mSelection != RecyclerView.NO_POSITION) {
      notifyItemChanged(mSelection);
    }
    if (mSelection == position) {
      // unselect
      mSelection = RecyclerView.NO_POSITION;
    } else {
      mSelection = position;
      if (position!=RecyclerView.NO_POSITION) {
        notifyItemChanged(position);
      }
    }
  }
}
