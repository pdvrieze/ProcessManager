package nl.adaptivity.process.editor.android;

import nl.adaptivity.android.compat.TitleFragment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
public class ProcessModelListOuterFragment extends TitleFragment implements ProcessModelListFragment.Callbacks, ProcessModelDetailFragment.Callbacks {

  public interface ProcessModelListCallbacks {

    void requestSyncProcessModelList(boolean pImmediate);

  }

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;

  private ProcessModelListCallbacks mCallbacks;



  @Override
  public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
    View result = pInflater.inflate(R.layout.activity_processmodel_list, pContainer, false);
    if (result.findViewById(R.id.processmodel_detail_container) != null) {
      // The detail container view will be present only in the
      // large-screen layouts (res/values-large and
      // res/values-sw600dp). If this view is present, then the
      // activity should be in two-pane mode.
      mTwoPane = true;
    }
    return result;
  }

  @Override
  public void onActivityCreated(Bundle pSavedInstanceState) {
    super.onActivityCreated(pSavedInstanceState);
    if (mTwoPane) {
      // In two-pane mode, list items should be given the
      // 'activated' state when touched.
      ((ProcessModelListFragment) getFragmentManager()
          .findFragmentById(R.id.processmodel_list))
          .setActivateOnItemClick(true);
    }
  }




  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof ProcessModelListCallbacks) {
      mCallbacks = (ProcessModelListCallbacks) pActivity;
      mCallbacks.requestSyncProcessModelList(true);
    }
  }

  /**
   * Callback method from {@link ProcessModelListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onItemSelected(long pProcessModelRowId) {
    if (mTwoPane) {
      if (pProcessModelRowId>=0) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        Bundle arguments = new Bundle();
        arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, pProcessModelRowId);
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
      if (pProcessModelRowId>=0) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent detailIntent = new Intent(getActivity(), ProcessModelDetailActivity.class);
        detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, pProcessModelRowId);
        startActivity(detailIntent);
      }
    }
  }

  @Override
  public CharSequence getTitle(Context pContext) {
    return pContext.getString(R.string.title_processmodel_list);
  }
}
