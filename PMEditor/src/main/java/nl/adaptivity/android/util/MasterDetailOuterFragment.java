package nl.adaptivity.android.util;

import android.support.v7.widget.RecyclerView;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.process.editor.android.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


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
   */
  @Override
  public void onItemSelected(int row, long itemId) {
    if (mTwoPane) {
      final RecyclerView recyclerView = mListFragment.getRecyclerView();
      if (itemId>=0) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        Fragment fragment = createDetailFragment(row, itemId);
        getChildFragmentManager().beginTransaction()
            .replace(mDetailContainerId, fragment)
            .commit();
      } else {
        Fragment frag = getChildFragmentManager().findFragmentById(mDetailContainerId);
        if (frag!=null) {
          getChildFragmentManager().beginTransaction()
              .remove(frag)
              .commit();
        }
      }

    } else {
      if (itemId>=0) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent detailIntent = getDetailIntent(row, itemId);
        startActivity(detailIntent);
      }
    }
  }

  protected abstract Fragment createDetailFragment(int row, long itemId);

  protected abstract Intent getDetailIntent(int row, long itemId);

  protected abstract MasterListFragment createListFragment();



}
