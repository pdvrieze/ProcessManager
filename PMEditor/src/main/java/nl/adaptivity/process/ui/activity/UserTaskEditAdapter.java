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

package nl.adaptivity.process.ui.activity;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by pdvrieze on 18/01/16.
 */
public class UserTaskEditAdapter extends Adapter<UserTaskEditAdapter.ItemViewHolder> {

  class ItemViewHolder extends ViewHolder implements OnTouchListener {

    private final ViewDataBinding mBinding;

// Object Initialization
    public ItemViewHolder(final ViewDataBinding binding) {
      super(binding.getRoot());
      mBinding = binding;
      View dragHandle = mBinding.getRoot().findViewById(R.id.drag_handle);
      dragHandle.setOnTouchListener(this);
    }
// Object Initialization end

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
      if (MotionEventCompat.getActionMasked(event)==MotionEvent.ACTION_DOWN) {
        mItemTouchHelper.startDrag(this);
      }
      return false;
    }
  }

  private static final int VIEWTYPE_LABEL = 0;
  private static final int VIEWTYPE_LIST = 1;
  private static final int VIEWTYPE_PASSWORD = 2;
  private static final int VIEWTYPE_TEXT = 3;
  private static final int VIEWTYPE_GENERIC = 4;

  private final List<TaskItem> mItems;
  private final ItemTouchHelper mItemTouchHelper;
  private LayoutInflater mInflater;

// Object Initialization
  public UserTaskEditAdapter() {
    this(null);
  }

  public UserTaskEditAdapter(final List<? extends TaskItem> items) {
    mItems = (items==null || items.isEmpty()) ? new ArrayList<TaskItem>() : new ArrayList<TaskItem>(items);
    mItemTouchHelper = new ItemTouchHelper(new Callback() {

      @Override
      public int getMovementFlags(final RecyclerView recyclerView, final ViewHolder viewHolder) {
        int swipeFlags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        int dragFlags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.UP | ItemTouchHelper.DOWN);
        return makeMovementFlags(dragFlags, swipeFlags);
      }

      @Override
      public boolean isItemViewSwipeEnabled() {
        return true;
      }

      @Override
      public boolean onMove(final RecyclerView recyclerView, final ViewHolder viewHolder, final ViewHolder target) {
        return UserTaskEditAdapter.this.onMove(recyclerView, viewHolder, target);
      }

      @Override
      public void onSwiped(final ViewHolder viewHolder, final int direction) {
        UserTaskEditAdapter.this.onSwiped(viewHolder, direction);
      }
    });
  }
// Object Initialization end

  @Override
  public ItemViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    if (mInflater == null) {
      mInflater = LayoutInflater.from(parent.getContext());
    }
    switch (viewType) {
      case VIEWTYPE_LABEL:
        return new ItemViewHolder(DataBindingUtil.inflate(mInflater, R.layout.edit_item_label, parent, false));
      case VIEWTYPE_LIST:
        return new ItemViewHolder(DataBindingUtil.inflate(mInflater, R.layout.edit_item_list, parent, false));
      case VIEWTYPE_PASSWORD:
        return new ItemViewHolder(DataBindingUtil.inflate(mInflater, R.layout.edit_item_password, parent, false));
      case VIEWTYPE_TEXT:
        return new ItemViewHolder(DataBindingUtil.inflate(mInflater, R.layout.edit_item_text, parent, false));
      case VIEWTYPE_GENERIC:
        return new ItemViewHolder(DataBindingUtil.inflate(mInflater, R.layout.edit_item_generic, parent, false));
      default:
        throw new IllegalArgumentException("unsupported viewtype");
    }
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder holder, final int position) {

    holder.mBinding.setVariable(BR.taskitem, mItems.get(position));
  }

  @Override
  public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
    mItemTouchHelper.attachToRecyclerView(recyclerView);
  }

  @Override
  public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
    mItemTouchHelper.attachToRecyclerView(null);
  }

  @Override
  public int getItemViewType(final int position) {
    TaskItem item = mItems.get(position);
    switch (item.getType()) {
      case LABEL:
        return VIEWTYPE_LABEL;
      case LIST:
        return VIEWTYPE_LIST;
      case PASSWORD:
        return VIEWTYPE_PASSWORD;
      case TEXT:
        return VIEWTYPE_TEXT;
      case GENERIC:
      default:
        return VIEWTYPE_GENERIC;
    }
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }

  public boolean addItem(TaskItem item) {
    if (mItems.add(item)) {
      notifyItemInserted(mItems.size() - 1);
      return true;
    }
    return false;
  }

  boolean onMove(final RecyclerView recyclerView, final ViewHolder viewHolder, final ViewHolder target) {
    int fromPosition = viewHolder.getAdapterPosition();
    int toPosition = target.getAdapterPosition();
    if (fromPosition<toPosition) {
      for (int i = fromPosition; i < toPosition; i++) {
        Collections.swap(mItems, i, i+1);
      }
    } else {
      for (int i = fromPosition; i > toPosition; i--) {
        Collections.swap(mItems, i, i-1);
      }
    }
    notifyItemMoved(fromPosition, toPosition);
    return true;
  }

  void onSwiped(final ViewHolder viewHolder, final int direction) {
    if(mItems.remove(viewHolder.getAdapterPosition())!=null) {
      notifyItemRemoved(viewHolder.getAdapterPosition());
    }
  }

  public void setItems(final List<? extends TaskItem> items) {
    mItems.clear();
    mItems.addAll(items);
    // The entire dataset changed
    notifyDataSetChanged();
  }
}
