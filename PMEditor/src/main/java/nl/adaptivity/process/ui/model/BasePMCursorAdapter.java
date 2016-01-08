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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.recyclerview.ClickableCursorAdapter;
import nl.adaptivity.android.recyclerview.SelectableCursorAdapter;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.BasePMCursorAdapter.BasePMViewHolder;


/**
 * Created by pdvrieze on 02/01/16.
 */
public abstract class BasePMCursorAdapter<VH extends BasePMViewHolder> extends SelectableCursorAdapter<VH> {

  public abstract class BasePMViewHolder<B extends ViewDataBinding> extends ClickableCursorAdapter<BasePMViewHolder<B>>.ClickableViewHolder {

    protected final B binding;

    public BasePMViewHolder(@LayoutRes final int itemLayout, final LayoutInflater inflater, final ViewGroup parent) {
      this((B) DataBindingUtil.inflate(inflater, itemLayout, parent, false));
    }

    private BasePMViewHolder(final B binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    public B getBinding() {
      return binding;
    }
  }

  private static final String TAG = "BasePMCursorAdapter";
  protected LayoutInflater mInflater;
  protected int mNameColumn;

  public BasePMCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {
    super(context, cursor, allowUnselection);
    mInflater = LayoutInflater.from(context);
    updateColumnIndices(cursor);
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
  public Cursor swapCursor(Cursor newCursor) {
    Log.d(TAG, "Swapping processmodel cursor");
    final Cursor result = super.swapCursor(newCursor);
    updateColumnIndices(newCursor);
    return result;
  }

  @Override
  public final void onBindViewHolder(final VH viewHolder, final Cursor cursor) {
    super.onBindViewHolder(viewHolder, cursor);
    addPendingBindings(viewHolder, cursor);
    viewHolder.binding.executePendingBindings();
  }

  protected void addPendingBindings(final VH viewHolder, final Cursor cursor) {
  }
}
