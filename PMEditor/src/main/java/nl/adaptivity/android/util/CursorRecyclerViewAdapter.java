package nl.adaptivity.android.util;/*
 * Copyright (C) 2014 skyfish.jy@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;


/**
 * https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * Created by skyfishjy on 10/31/14.
 */

public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

  private class NotifyingDataSetObserver extends DataSetObserver {

    @Override
    public void onChanged() {
      super.onChanged();
      mDataValid = true;
      notifyDataSetChanged();
    }

    @Override
    public void onInvalidated() {
      super.onInvalidated();
      mDataValid = false;
      notifyDataSetChanged();
      //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
    }
  }
  private Context mContext;
  private Cursor mCursor;
  private boolean mDataValid;
  private int mRowIdColumn;
  private DataSetObserver mDataSetObserver;

// Object Initialization
  public CursorRecyclerViewAdapter(Context context, Cursor cursor) {
    mContext = context;
    mCursor = cursor;
    mDataValid = cursor != null;
    mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
    mDataSetObserver = new NotifyingDataSetObserver();
    if (mCursor != null) {
      mCursor.registerDataSetObserver(mDataSetObserver);
    }
  }
// Object Initialization end

  @Override
  public final void onBindViewHolder(VH viewHolder, int position) {
    if (!mDataValid) {
      throw new IllegalStateException("this should only be called when the cursor is valid");
    }
    if (!mCursor.moveToPosition(position)) {
      throw new IllegalStateException("couldn't move cursor to position " + position);
    }
    onBindViewHolder(viewHolder, mCursor);
  }

  @Override
  public void setHasStableIds(boolean hasStableIds) {
    super.setHasStableIds(true);
  }

  @Override
  public long getItemId(int position) {
    if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
      return mCursor.getLong(mRowIdColumn);
    }
    return 0;
  }

  /**
   * Get the current position of the item with the given itemId. Unlike {@link RecyclerView#findViewHolderForItemId(long)}
   * this method will search the entire cursor. In cases of long lists, it may be advantageous to use
   * that method if there is a good chance the item is actually currently visible. If the adapter
   * does not have stable ids, using this function may be surprising.
   * @param itemId The id of the item to find.
   * @return The position of the item with the given id.
   */
  public int getItemPos(final long itemId) {
    if (mDataValid && mCursor != null) {
      for(boolean valid = mCursor.moveToFirst(); valid; mCursor.moveToNext()) {
        if (mCursor.getLong(mRowIdColumn)==itemId) {
          return mCursor.getPosition();
        }
      }
    }
    return RecyclerView.NO_POSITION;
  }


  @Override
  public int getItemCount() {
    if (mDataValid && mCursor != null) {
      return mCursor.getCount();
    }
    return 0;
  }

  public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

  /**
   * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
   * closed.
   */
  public void changeCursor(Cursor cursor) {
    Cursor old = swapCursor(cursor);
    if (old != null) {
      old.close();
    }
  }

  /**
   * Swap in a new Cursor, returning the old Cursor.  Unlike
   * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
   * closed.
   */
  public Cursor swapCursor(Cursor newCursor) {
    if (newCursor == mCursor) {
      return null;
    }
    final Cursor oldCursor = mCursor;
    if (oldCursor != null && mDataSetObserver != null) {
      oldCursor.unregisterDataSetObserver(mDataSetObserver);
    }
    mCursor = newCursor;
    if (mCursor != null) {
      if (mDataSetObserver != null) {
        mCursor.registerDataSetObserver(mDataSetObserver);
      }
      mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
      mDataValid = true;
      notifyDataSetChanged();
    } else {
      mRowIdColumn = -1;
      mDataValid = false;
      notifyDataSetChanged();
      //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
    }
    return oldCursor;
  }

// Property accessors start
  public Cursor getCursor() {
    return mCursor;
  }
// Property acccessors end
}