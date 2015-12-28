package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.util.ClickableCursorAdapter;
import nl.adaptivity.android.util.SelectableCursorAdapter;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ModelListitemBinding;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.PMCursorAdapter.PMViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class PMCursorAdapter extends SelectableCursorAdapter<PMViewHolder> {

  private static final String TAG = "PMCursorAdapter";

  public class PMViewHolder extends ClickableCursorAdapter.ClickableViewHolder {

    final ModelListitemBinding binding;

    public PMViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(inflater.inflate(R.layout.model_listitem, parent, false));
      binding = DataBindingUtil.bind(itemView);
    }
  }

  private LayoutInflater mInflater;
  private int mNameColumn;

  public PMCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
    mInflater = LayoutInflater.from(context);
    updateColumnIndices(c);
    setHasStableIds(true);
  }

  protected void updateColumnIndices(Cursor c) {
    if (c == null) {
      mNameColumn = -1;
    } else {
      mNameColumn = c.getColumnIndex(ProcessModels.COLUMN_NAME);
    }
  }

  @Override
  public void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override
  public Cursor swapCursor(Cursor newCursor) {
    Log.d(TAG, "Swapping processmodel cursor");
    final Cursor result = super.swapCursor(newCursor);
    updateColumnIndices(newCursor);
    return result;
  }

  @Override
  public void onBindViewHolder(final PMViewHolder viewHolder, final Cursor cursor) {
    super.onBindViewHolder(viewHolder, cursor);
    viewHolder.binding.setName(mNameColumn >= 0 ? cursor.getString(mNameColumn) : null);
    viewHolder.binding.executePendingBindings();
  }

  @Override
  public PMViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new PMViewHolder(mInflater, parent);
  }

  @Override
  public void onClickView(final ViewHolder viewHolder) {
    super.onClickView(viewHolder);
  }
}
