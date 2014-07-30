package nl.adaptivity.android.util;

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

  public MasterDetailOuterFragment(int pLayoutId, int pListContainerId, int pDetailContainerId) {
    mLayoutId = pLayoutId;
    mListContainerId = pListContainerId;
    mDetailContainerId = pDetailContainerId;
  }

  @Override
  public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
    View result = pInflater.inflate(mLayoutId, pContainer, false);
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
   * Callback method from {@link ProcessModelListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onItemSelected(int pRow, long pItemId) {
    if (mTwoPane) {
      final ListView listView = mListFragment.getListView();
      {
        int oldCheckedItem = listView.getCheckedItemPosition();
        if (oldCheckedItem!=AdapterView.INVALID_POSITION) {
          listView.setItemChecked(oldCheckedItem, false);
        }
      }
      if (pItemId>=0) {
        if (pRow!=AdapterView.INVALID_POSITION) {
          listView.setItemChecked(pRow, true);
        }
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        Fragment fragment = createDetailFragment(pRow, pItemId);
        getChildFragmentManager().beginTransaction()
            .replace(mDetailContainerId, fragment)
            .commit();
      } else {
        Fragment frag = getChildFragmentManager().findFragmentById(mDetailContainerId);
        if (frag!=null) {
          getFragmentManager().beginTransaction()
              .remove(frag)
              .commit();
        }
      }

    } else {
      if (pItemId>=0) {
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent detailIntent = getDetailIntent(pRow, pItemId);
        startActivity(detailIntent);
      }
    }
  }

  protected abstract Fragment createDetailFragment(int pRow, long pItemId);

  protected abstract Intent getDetailIntent(int pRow, long pItemId);

  protected abstract MasterListFragment createListFragment();



}
