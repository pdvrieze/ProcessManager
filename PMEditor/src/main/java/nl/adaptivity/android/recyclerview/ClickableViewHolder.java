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

package nl.adaptivity.android.recyclerview;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;


/**
 * Created by pdvrieze on 04/02/16.
 */
public abstract class ClickableViewHolder extends ViewHolder implements OnClickListener {

  private final ClickableAdapter mClickableAdapter;

  public ClickableViewHolder(final ClickableAdapter clickableAdapter, final View itemView) {
    super(itemView);
    mClickableAdapter = clickableAdapter;
    itemView.setOnClickListener(this);
  }

  public void onClick(final View v) {
    mClickableAdapter.doClickView(this);
  }
}
