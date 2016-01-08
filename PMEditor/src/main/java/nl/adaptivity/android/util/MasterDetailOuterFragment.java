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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import nl.adaptivity.android.compat.TitleFragment;


public abstract class MasterDetailOuterFragment extends TitleFragment implements MasterListFragment.Callbacks {

  private static final String TAG = "MasterDetailOutFrag";
  public static final String ARG_ITEM_ID = "item_id";
  private int mLayoutId;
  private int mListContainerId;
  private int mDetailContainerId;
  private boolean mTwoPane;
  private MasterListFragment mListFragment;

  public MasterDetailOuterFragment(int layoutId, int listContainerId, int detailContainerId) {
    mLayoutId = layoutId;
    mListContainerId = listContainerId;
    mDetailContainerId = detailContainerId;
  }

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View result = inflater.inflate(mLayoutId, container, false);
    if (result.findViewById(mListContainerId) != null) {
      mTwoPane = true;
    }
    {
      Fragment existingListFragment = getChildFragmentManager().findFragmentById(mListContainerId);
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
    Bundle args = getArguments();
    if (args!=null && args.containsKey(ARG_ITEM_ID)) {
      Bundle b = new Bundle(1);
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
      Bundle args = getArguments();
      if (args!=null && args.containsKey(ARG_ITEM_ID)) {
        long itemId = args.getLong(ARG_ITEM_ID);
        Log.d(TAG, "onCreateView: processing itemId arg: " + itemId);
        if (mTwoPane) {
          Fragment detailFragment = createDetailFragment(itemId);
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
   * Callback method from {@link MasterListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   * @param row Ignored by this implementation, but the row that was selected in the list.
   * @param itemId The id of the task that was selected (this is the local database ID, not the server assigned handle).
   */
  @Override
  public void onItemSelected(int row, long itemId) {
    onItemSelected(itemId, false);
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
        Fragment fragment = createDetailFragment(taskId);
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction()
                                                                         .replace(mDetailContainerId, fragment);
        if (addToBackstack) {
          transaction.addToBackStack(null);
        }
        transaction.commit();
      } else {
        Fragment frag = getChildFragmentManager().findFragmentById(mDetailContainerId);
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
        Intent detailIntent = getDetailIntent(taskId);
        startActivity(detailIntent);
      }
    }
  }

  protected abstract Fragment createDetailFragment(long itemId);

  protected abstract Intent getDetailIntent(long itemId);

  protected abstract MasterListFragment createListFragment(@Nullable Bundle args);

  public static @NonNull Bundle addArgs(@Nullable Bundle args, long itemId) {
    if (args==null) {
      args = new Bundle(1);
    }
    args.putLong(ARG_ITEM_ID, itemId);
    return args;
  }

}
