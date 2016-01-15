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

import nl.adaptivity.android.util.MasterDetailOuterFragment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import nl.adaptivity.process.editor.android.R;


/**
 * An activity representing a list of ProcessModels. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ProcessModelDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProcessModelListFragment} and the item details (if present) is a
 * {@link ProcessModelDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ProcessModelListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class ProcessModelListOuterFragment extends MasterDetailOuterFragment implements ProcessModelListFragment.Callbacks, ProcessModelDetailFragment.Callbacks {

  public interface ProcessModelListCallbacks {

    void requestSyncProcessModelList(boolean immediate);

    void onInstantiateModel(long id, String suggestedName);

  }


  public ProcessModelListOuterFragment() {
    super(R.layout.outer_processmodel_list, R.id.processmodel_list_container, R.id.processmodel_detail_container);
  }

  public static ProcessModelListOuterFragment newInstance(final long modelId) {
    ProcessModelListOuterFragment result = new ProcessModelListOuterFragment();
    if (modelId!=0) {
      result.setArguments(addArgs(null, modelId));
    }
    return result;
  }

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;

  private ProcessModelListCallbacks mCallbacks;

  @Override
  protected ProcessModelListFragment createListFragment(Bundle args) {
    ProcessModelListFragment listFragment = new ProcessModelListFragment();
    if (args!=null) { listFragment.setArguments(args); }
    return listFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof ProcessModelListCallbacks) {
      mCallbacks = (ProcessModelListCallbacks) activity;
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (mCallbacks!=null) {
      mCallbacks.requestSyncProcessModelList(true);
    }
  }

  /**
   * Callback method from {@link ProcessModelListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onProcessModelSelected(long processModelId) {
    if (mTwoPane) {
      if (processModelId>=0) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        Bundle arguments = new Bundle();
        arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, processModelId);
        ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
        fragment.setArguments(arguments);
        getChildFragmentManager().beginTransaction()
            .replace(R.id.processmodel_detail_container, fragment)
            .commit();
      } else {
        Fragment frag = getChildFragmentManager().findFragmentById(R.id.processmodel_detail_container);
        if (frag!=null) {
          getFragmentManager().beginTransaction()
              .remove(frag)
              .commit();
        }
      }

    } else {
      if (processModelId>=0) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent detailIntent = new Intent(getActivity(), ProcessModelDetailActivity.class);
        detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, processModelId);
        startActivity(detailIntent);
      }
    }
  }

  @Override
  protected ProcessModelDetailFragment createDetailFragment(long itemId) {
    ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, itemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(long itemId) {
    Intent detailIntent = new Intent(getActivity(), ProcessModelDetailActivity.class);
    detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, itemId);
    return detailIntent;
  }

  @Override
  public CharSequence getTitle(Context context) {
    return context.getString(R.string.title_processmodel_list);
  }

  @Override
  public void onInstantiateModel(long modelId, String suggestedName) {
    mCallbacks.onInstantiateModel(modelId, suggestedName);
  }
}
