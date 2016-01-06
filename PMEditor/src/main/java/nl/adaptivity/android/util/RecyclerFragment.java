/*
 * Copyright (C) 2011 The Android Open Source Project
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
 */

package nl.adaptivity.android.util;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import nl.adaptivity.android.recyclerview.SelectableAdapter;


/**
 * Static library support version of the framework's {@link android.app.ListFragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */
public class RecyclerFragment extends Fragment {
  @IdRes static final int INTERNAL_EMPTY_ID = 0x00ff0001;
  @IdRes static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
  @IdRes static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

  final private Handler mHandler = new Handler();

  final private Runnable mRequestFocus = new Runnable() {
    public void run() {
      mRecyclerView.focusableViewAvailable(mRecyclerView);
    }
  };

  RecyclerView.Adapter<? extends ViewHolder> mAdapter;
  RecyclerView mRecyclerView;
  boolean mListShown;

  public RecyclerFragment() {
  }

  /**
   * Provide default implementation to return a simple list view.  Subclasses
   * can override to replace with their own layout.  If doing so, the
   * returned view hierarchy <em>must</em> have a ListView whose id
   * is {@link android.R.id#list android.R.id.list} and can optionally
   * have a sibling view id {@link android.R.id#empty android.R.id.empty}
   * that is to be shown when the list is empty.
   *
   * <p>If you are overriding this method with your own custom content,
   * consider including the standard layout {@link android.R.layout#list_content}
   * in your layout file, so that you continue to retain all of the standard
   * behavior of ListFragment.  In particular, this is currently the only
   * way to have the built-in indeterminant progress state be shown.
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    final Context context = getActivity();

    RecyclerView rv = new RecyclerView(getActivity());
    rv.setId(android.R.id.list);
    rv.setLayoutManager(new LinearLayoutManager(getActivity()));

    rv.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

    return rv;
  }

  /**
   * Attach to list view once the view hierarchy has been created.
   */
  @CallSuper
  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ensureList();
  }

  /**
   * Detach from list view.
   */
  @Override
  public void onDestroyView() {
    mHandler.removeCallbacks(mRequestFocus);
    mRecyclerView = null;
    mListShown = false;
    super.onDestroyView();
  }

  /**
   * Provide the cursor for the list view.
   */
  public void setListAdapter(Adapter<?> adapter) {
    boolean hadAdapter = mAdapter != null;
    mAdapter = adapter;
    if (mRecyclerView != null) {
      mRecyclerView.setAdapter(adapter);
    }
  }

  /**
   * Get the activity's list view widget.
   */
  public RecyclerView getRecyclerView() {
    ensureList();
    return mRecyclerView;
  }

  /**
   * Get the ListAdapter associated with this activity's ListView.
   */
  public Adapter<? extends ViewHolder> getListAdapter() {
    return mAdapter;
  }

  private void ensureList() {
    if (mRecyclerView != null) {
      return;
    }
    View root = getView();
    if (root == null) {
      throw new IllegalStateException("Content view not yet created");
    }
    if (root instanceof RecyclerView) {
      mRecyclerView = (RecyclerView) root;
    } else {
      View rawRecyclerView = root.findViewById(android.R.id.list);
      if (!(rawRecyclerView instanceof RecyclerView)) {
        if (rawRecyclerView == null) {
          throw new RuntimeException(
                  "Your content must have a RecyclerView whose id attribute is " +
                  "'android.R.id.list'");
        }
        throw new RuntimeException(
                "Content has view with id attribute 'android.R.id.list' "
                + "that is not a RecyclerView class");
      }
      mRecyclerView = (RecyclerView) rawRecyclerView;
    }
    mListShown = true;
    if (mAdapter != null) {
      Adapter adapter = mAdapter;
      mAdapter = null;
      setListAdapter(adapter);
    }
    mHandler.post(mRequestFocus);
  }


  public void setSelectedId(final long itemId) {
    if (mAdapter instanceof SelectableAdapter) {
      ((SelectableAdapter) mAdapter).setSelectedItem(itemId);
    }
  }

}
