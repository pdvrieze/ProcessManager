package nl.adaptivity.android.util;

import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.process.editor.android.ProcessModelDetailActivity;
import nl.adaptivity.process.editor.android.ProcessModelDetailFragment;
import nl.adaptivity.process.editor.android.ProcessModelListFragment;
import nl.adaptivity.process.editor.android.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;


public abstract class MasterDetailOuterFragment extends TitleFragment implements MasterListFragment.Callbacks {

  private int mLayoutId;
  private int mListContainerId;
  private int mDetailContainerId;
  private boolean mTwoPane;

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
      ListFragment newFragment = createListFragment();
      getChildFragmentManager().beginTransaction()
                               .replace(mListContainerId, newFragment)
                               .commit();
    }

    return result;
  }

  @Override
  public void onActivityCreated(Bundle pSavedInstanceState) {
    super.onActivityCreated(pSavedInstanceState);
    Fragment existingFragment = getChildFragmentManager().findFragmentById(mListContainerId);
    if (existingFragment!=null && mTwoPane) {
      ((ListFragment) existingFragment).getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }
  }


  /**
   * Callback method from {@link ProcessModelListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onItemSelected(int pRow, long pItemId) {
    if (mTwoPane) {
      if (pItemId>=0) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        ProcessModelDetailFragment fragment = createDetailFragment(pRow, pItemId);
        getChildFragmentManager().beginTransaction()
            .replace(R.id.processmodel_detail_container, fragment)
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

  protected abstract ProcessModelDetailFragment createDetailFragment(int pRow, long pItemId);

  protected abstract Intent getDetailIntent(int pRow, long pItemId);

  protected abstract MasterListFragment createListFragment();



}
