package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ModelListitemBinding;
import nl.adaptivity.process.ui.model.PMCursorAdapter.PMViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class PMCursorAdapter extends BasePMCursorAdapter<PMViewHolder> {

  public class PMViewHolder extends BasePMCursorAdapter<PMViewHolder>.BasePMViewHolder<ModelListitemBinding> {

    public PMViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(R.layout.model_listitem, inflater, parent);
    }
  }

  public PMCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
  }

  @Override
  public PMViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new PMViewHolder(mInflater, parent);
  }

  protected void addPendingBindings(final PMViewHolder viewHolder, final Cursor cursor) {
    viewHolder.binding.setName(mNameColumn >= 0 ? cursor.getString(mNameColumn) : null);
  }

}
