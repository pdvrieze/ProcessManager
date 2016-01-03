package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.OverviewModelListitemBinding;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.OverviewPMCursorAdapter.OverviewPMViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class OverviewPMCursorAdapter extends BasePMCursorAdapter<OverviewPMViewHolder> {

  public class OverviewPMViewHolder extends BasePMCursorAdapter<OverviewPMViewHolder>.BasePMViewHolder<OverviewModelListitemBinding> {

    public OverviewPMViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(R.layout.overview_model_listitem, inflater, parent);
    }
  }

  private int mCountColumn;

  public OverviewPMCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
  }

  @Override
  public OverviewPMViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new OverviewPMViewHolder(mInflater, parent);
  }

  @Override
  protected void updateColumnIndices(final Cursor c) {
    super.updateColumnIndices(c);
    mCountColumn = c==null ? -1 : c.getColumnIndex(ProcessModels.COLUMN_INSTANCECOUNT);
  }

  protected void addPendingBindings(final OverviewPMViewHolder viewHolder, final Cursor cursor) {
    viewHolder.binding.setName(mNameColumn >= 0 ? cursor.getString(mNameColumn) : null);
    viewHolder.binding.setInstanceCount(mCountColumn>=0 ? cursor.getInt(mCountColumn): 0);
  }

}
