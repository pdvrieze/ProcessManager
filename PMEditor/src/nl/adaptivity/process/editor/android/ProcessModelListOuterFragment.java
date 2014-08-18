package nl.adaptivity.process.editor.android;

import nl.adaptivity.android.util.MasterDetailOuterFragment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

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

    void requestSyncProcessModelList(boolean pImmediate);

    void onInstantiateModel(long pModelHandle);

  }


  public ProcessModelListOuterFragment() {
    super(R.layout.outer_processmodel_list, R.id.processmodel_list_container, R.id.processmodel_detail_container);
  }

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;

  private ProcessModelListCallbacks mCallbacks;

  @Override
  protected ProcessModelListFragment createListFragment() {
    return new ProcessModelListFragment();
  }

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof ProcessModelListCallbacks) {
      mCallbacks = (ProcessModelListCallbacks) pActivity;
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
  protected ProcessModelDetailFragment createDetailFragment(int pRow, long pItemId) {
    ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, pItemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(int pRow, long pItemId) {
    Intent detailIntent = new Intent(getActivity(), ProcessModelDetailActivity.class);
    detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, pItemId);
    return detailIntent;
  }

  @Override
  public CharSequence getTitle(Context pContext) {
    return pContext.getString(R.string.title_processmodel_list);
  }

  @Override
  public void onInstantiateModel(long pModelHandle) {
    mCallbacks.onInstantiateModel(pModelHandle);
  }
}
