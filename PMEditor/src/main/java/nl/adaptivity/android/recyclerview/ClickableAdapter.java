package nl.adaptivity.android.recyclerview;

import android.support.v7.widget.RecyclerView.ViewHolder;


/**
 * Created by pdvrieze on 04/01/16.
 */
public interface ClickableAdapter {

  public interface OnItemClickListener {

    /**
     * Callback to handle item clicks. This method can stop processing of the click by returning true.
     * @param adapter The adapter that caused the event
     * @param viewHolder The underlying viewHolder for the event.
     * @return true if the click has been absorbed, false if not.
     */
    boolean onClickItem(ClickableAdapter adapter, ViewHolder viewHolder);
  }

  void onClickView(ViewHolder viewHolder);

  OnItemClickListener getOnItemClickListener();

  void setOnItemClickListener(OnItemClickListener itemClickListener);
}
