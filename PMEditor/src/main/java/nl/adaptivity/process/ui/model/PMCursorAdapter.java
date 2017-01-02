/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

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

  public PMCursorAdapter(final Context context, final Cursor c) {
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
