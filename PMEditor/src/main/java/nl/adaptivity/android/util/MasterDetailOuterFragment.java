package nl.adaptivity.android.util;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import nl.adaptivity.android.compat.TitleFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class MasterDetailOuterFragment extends TitleFragment implements MasterListFragment.Callbacks {

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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View result = inflater.inflate(mLayoutId, container, false);
    if (result.findViewById(mListContainerId) != null) {
      mTwoPane = true;
    }

    Fragment existingFragment = getChildFragmentManager().findFragmentById(mListContainerId);
    if (existingFragment==null) {
      mListFragment = createListFragment();
      getChildFragmentManager().beginTransaction()
                               .replace(mListContainerId, mListFragment)
                               .commit();
    } else {
      mListFragment = (MasterListFragment) existingFragment;
    }

    return result;
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

  protected abstract MasterListFragment createListFragment();



}
