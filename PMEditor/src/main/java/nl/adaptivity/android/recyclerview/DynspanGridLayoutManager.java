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

package nl.adaptivity.android.recyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import nl.adaptivity.process.editor.android.R;


/**
 * Created by pdvrieze on 03/01/16.
 */
public class DynspanGridLayoutManager extends GridLayoutManager {

  private int mMinSpanWidth = -1;

  public DynspanGridLayoutManager(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DynspanGridLayoutManager, defStyleAttr, defStyleRes);
    mMinSpanWidth = a.getDimensionPixelSize(R.styleable.DynspanGridLayoutManager_minSpanWidth, -1);
    a.recycle();
  }

  public DynspanGridLayoutManager(final Context context, final int minSpanWidth) {
    super(context, 1);
    mMinSpanWidth = minSpanWidth;
  }

  public DynspanGridLayoutManager(final Context context, final int spanCount, final int orientation, final boolean reverseLayout) {
    super(context, spanCount, orientation, reverseLayout);
  }

  @Override
  public void onMeasure(final Recycler recycler, final State state, final int widthSpec, final int heightSpec) {
    if (mMinSpanWidth>0) {
      int widthSize = MeasureSpec.getSize(widthSpec);
      int widthMode = MeasureSpec.getMode(widthSpec);

      int heightSize = MeasureSpec.getSize(heightSpec);
      int heightMode = MeasureSpec.getMode(heightSpec);

      int width = 0;
      int height = 0;
      int spanCount = -1;
      switch (widthMode) {
        case MeasureSpec.AT_MOST:
          spanCount = widthSize/mMinSpanWidth;
          width= spanCount*mMinSpanWidth;
          break;
        case MeasureSpec.EXACTLY:
          spanCount = widthSize/mMinSpanWidth;
          width = widthSize;
          break;
        case MeasureSpec.UNSPECIFIED:
          width = Math.max(mMinSpanWidth, getMinimumWidth());
      }
      switch (heightMode) {
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
          height = heightSize;
          break;
        case MeasureSpec.UNSPECIFIED:
          height = getMinimumHeight();
      }
      setMeasuredDimension(width, height);
      if (spanCount>0) { setSpanCount(spanCount); }
    } else {
      super.onMeasure(recycler, state, widthSpec, heightSpec);

    }

  }
}
