package nl.adaptivity.android.util;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import nl.adaptivity.android.util.ClickableCursorAdapter.ClickableViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public abstract class ClickableCursorAdapter<VH extends ClickableViewHolder> extends CursorRecyclerViewAdapter<VH> {

  public interface OnItemClickListener {

    /**
     * Callback to handle item clicks. This method can stop processing of the click by returning true.
     * @param adapter The adapter that caused the event
     * @param viewHolder The underlying viewHolder for the event.
     * @return true if the click has been absorbed, false if not.
     */
    boolean onClickItem(ClickableCursorAdapter<?> adapter, ViewHolder viewHolder);
  }

  public abstract class ClickableViewHolder extends ViewHolder implements OnClickListener {

    public ClickableViewHolder(final View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
    }

    public void onClick(final View v) {
      doClickView(this);
    }
  }

  private OnItemClickListener mItemClickListener;

  public ClickableCursorAdapter(final Context context, final Cursor cursor) {super(context, cursor);}

  private final void doClickView(final ViewHolder viewHolder) {
    if (mItemClickListener==null || (! mItemClickListener.onClickItem(this, viewHolder))) {
      onClickView(viewHolder);
    }
  }

  public void onClickView(ViewHolder viewHolder) {};

  public OnItemClickListener getOnItemClickListener() {
    return mItemClickListener;
  }

  public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
    mItemClickListener = itemClickListener;
  }
}
