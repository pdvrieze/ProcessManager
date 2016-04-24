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

package nl.adaptivity.android.util;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.util.MasterListFragment.ListCallbacks;


public abstract class MasterDetailOuterFragment extends TitleFragment implements ListCallbacks {

  private static final String TAG = "MasterDetailOutFrag";
  public static final String ARG_ITEM_ID = "item_id";
  private @LayoutRes final int                mLayoutId;
  private @IdRes final     int                mListContainerId;
  private @IdRes final     int                mDetailContainerId;
  private                  boolean            mTwoPane;
  private                  MasterListFragment mListFragment;

  public MasterDetailOuterFragment(@LayoutRes final int layoutId, @IdRes final int listContainerId, @IdRes final int detailContainerId) {
    mLayoutId = layoutId;
    mListContainerId = listContainerId;
    mDetailContainerId = detailContainerId;
  }

  @Override
  public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    final View result = inflater.inflate(mLayoutId, container, false);
    if (result.findViewById(mDetailContainerId) != null) {
      mTwoPane = true;
    }
    {
      final Fragment existingListFragment = getChildFragmentManager().findFragmentById(mListContainerId);
      if (existingListFragment == null) {
        mListFragment = createListFragment(getListArgumentsIfNeeded());
        getChildFragmentManager().beginTransaction().replace(mListContainerId, mListFragment).commit();
      } else {
        mListFragment = (MasterListFragment) existingListFragment;
      }
    }

    return result;
  }

  private Bundle getListArgumentsIfNeeded() {
    final Bundle args = getArguments();
    if (args!=null && args.containsKey(ARG_ITEM_ID)) {
      final Bundle b = new Bundle(1);
      b.putLong(ARG_ITEM_ID, args.getLong(ARG_ITEM_ID));
      return b;
    }
    return null;
  }

  @Override
  @CallSuper
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState==null) {
      final Bundle args = getArguments();
      if (args!=null && args.containsKey(ARG_ITEM_ID)) {
        final long itemId = args.getLong(ARG_ITEM_ID);
        Log.d(TAG, "onCreateView: processing itemId arg: " + itemId);
        if (mTwoPane) {
          final Fragment detailFragment = createDetailFragment(itemId);
          getChildFragmentManager().beginTransaction().replace(mDetailContainerId, detailFragment).commit();
        } else {
          startActivity(getDetailIntent(itemId));
          getActivity().finish(); // Remove the current activity from the backstack, it will be the list container
        }
      }
    }
  }

  @Override
  public boolean isTwoPane() {
    return mTwoPane;
  }

  /**
   * Callback method from {@link ListCallbacks} indicating
   * that the item with the given ID was selected.
   * @param row Ignored by this implementation, but the row that was selected in the list.
   * @param itemId The id of the task that was selected (this is the local database ID, not the server assigned handle).
   */
  @Override
  public void onItemSelected(final int row, final long itemId) {
    onItemSelected(itemId, false);
  }

  /** When we are in single pane mode, handle item clicks, don't allow selection to happen. */
  @Override
  public boolean onItemClicked(final int row, final long itemId) {
    if (isTwoPane()) { return false; }
    onItemSelected(itemId, false);
    return true;
  }

  public MasterListFragment getListFragment() {
    return mListFragment;
  }

  public void onItemSelected(final long taskId, final boolean addToBackstack) {
    if (mTwoPane) {
      final RecyclerView recyclerView = mListFragment.getRecyclerView();
      if (taskId >= 0) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        final Fragment fragment = createDetailFragment(taskId);
        @SuppressLint("CommitTransaction")
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction()
                                                                         .replace(mDetailContainerId, fragment);
        if (addToBackstack) {
          transaction.addToBackStack(null);
        }
        transaction.commit();
      } else {
        final Fragment frag = getChildFragmentManager().findFragmentById(mDetailContainerId);
        if (frag!=null) {
          getChildFragmentManager().beginTransaction()
              .remove(frag)
              .commit();
        }
      }

    } else {
      if (taskId >= 0) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        final Intent detailIntent = getDetailIntent(taskId);
        startActivity(detailIntent);
      }
    }
  }

  protected abstract Fragment createDetailFragment(long itemId);

  protected abstract Intent getDetailIntent(long itemId);

  protected abstract MasterListFragment createListFragment(@Nullable Bundle args);

  public static @NonNull Bundle addArgs(@Nullable Bundle args, final long itemId) {
    if (args==null) {
      args = new Bundle(1);
    }
    args.putLong(ARG_ITEM_ID, itemId);
    return args;
  }

}
