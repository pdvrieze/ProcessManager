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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.View;
import nl.adaptivity.android.recyclerview.SelectableAdapter;
import nl.adaptivity.sync.SyncManager;


public class MasterListFragment extends RecyclerFragment {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  public static final String STATE_ACTIVATED_ID = "activated_id";

  public MasterListFragment() {
    Log.i(MasterListFragment.class.getSimpleName(), "Creating a new instanceo of "+getClass().getSimpleName());
  }

  public interface ProcessModelListCallbacks {
    /** Initiate a click. When this returns true, further ignore the event (don't select) */
    boolean onItemClicked(final int row, final long id);
    void onItemSelected(int row, long id);
    boolean isTwoPane();
    SyncManager getSyncManager();
  }

  /**
   * A dummy implementation of the {@link ProcessModelListCallbacks} interface that does
   * nothing. Used only when this fragment is not attached to an activity.
   */
  public static ProcessModelListCallbacks sDummyCallbacks = new ProcessModelListCallbacks() {
    @Override public boolean onItemClicked(final int row, final long id) {/*dummy, not processed*/ return false; }
    @Override
    public void onItemSelected(final int row, final long id) {/*dummy*/}

    @Override
    public boolean isTwoPane() {
      return false;
    }

    @Override
    public SyncManager getSyncManager() {
      return new SyncManager(null);
    }
  };

  /**
   * The fragment's current callback object, which is notified of list item
   * clicks.
   */
  private ProcessModelListCallbacks mCallbacks = sDummyCallbacks;

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getCallbacks();
  }

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    if (mCallbacks==sDummyCallbacks) {
      if (getActivity() instanceof ProcessModelListCallbacks) {
        mCallbacks = (ProcessModelListCallbacks) getActivity();
      }
    }
  }

  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null
        && savedInstanceState.containsKey(STATE_ACTIVATED_ID)) {
      setActivatedId(savedInstanceState.getLong(STATE_ACTIVATED_ID));
    } else {
      final Bundle arguments = getArguments();
      if (arguments != null && arguments.containsKey(MasterDetailOuterFragment.ARG_ITEM_ID)) {
        setActivatedId(arguments.getLong(MasterDetailOuterFragment.ARG_ITEM_ID));
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // Reset the active callbacks interface to the dummy implementation.
    mCallbacks = sDummyCallbacks;
  }

  protected ProcessModelListCallbacks getCallbacks() {
    if (mCallbacks==sDummyCallbacks) {
      Fragment parent = getParentFragment();
      while (parent!=null) {
        if (parent instanceof ProcessModelListCallbacks) {
          mCallbacks = (ProcessModelListCallbacks) parent;
          return mCallbacks;
        }
        parent = getParentFragment();
      }
    }
    return mCallbacks;
  }

  protected boolean doOnItemClicked(final int position, final long nodeId) {
    return getCallbacks().onItemClicked(position, nodeId);
  }

  protected void doOnItemSelected(final int position, final long nodeId) {
    getCallbacks().onItemSelected(position, nodeId);
  }

  protected void setActivatedId(final long id) {
    final ViewHolder vh = getRecyclerView().findViewHolderForItemId(id);
    final Adapter<?> adapter = getListAdapter();
    if (adapter instanceof SelectableAdapter) {
      final SelectableAdapter selectableAdapter = (SelectableAdapter) adapter;
      if (vh != null) {
        selectableAdapter.setSelectedItem(vh.getAdapterPosition());
      } else {
        selectableAdapter.setSelectedItem(id);
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    final Adapter adapter = getListAdapter();
    if (adapter instanceof SelectableAdapter) {
      SelectableAdapter selectableAdapter = (SelectableAdapter) adapter;

      if (selectableAdapter.getSelectedId() != RecyclerView.NO_ID) {
        // Serialize and persist the activated item position.
        outState.putLong(STATE_ACTIVATED_ID, selectableAdapter.getSelectedId());
      }
    }
  }

}
