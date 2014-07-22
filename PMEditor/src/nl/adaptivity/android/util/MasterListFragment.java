package nl.adaptivity.android.util;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;


public class MasterListFragment extends ListFragment {

  public interface Callbacks {
    public void onItemSelected(int pRow, long pId);
  }

  /**
   * A dummy implementation of the {@link Callbacks} interface that does
   * nothing. Used only when this fragment is not attached to an activity.
   */
  public static Callbacks sDummyCallbacks = new Callbacks() {
    @Override
    public void onItemSelected(int pRow, long pId) {/*dummy*/}
  };

  /**
   * The fragment's current callback object, which is notified of list item
   * clicks.
   */
  private Callbacks mCallbacks = sDummyCallbacks;

  @Override
  public void onActivityCreated(Bundle pSavedInstanceState) {
    super.onActivityCreated(pSavedInstanceState);
    getCallbacks();
  }

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (mCallbacks==sDummyCallbacks) {
      if (getActivity() instanceof Callbacks) {
        mCallbacks = (Callbacks) getActivity();
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // Reset the active callbacks interface to the dummy implementation.
    mCallbacks = sDummyCallbacks;
  }

  private Callbacks getCallbacks() {
    if (mCallbacks==sDummyCallbacks) {
      Fragment parent = getParentFragment();
      while (parent!=null) {
        if (parent instanceof Callbacks) {
          mCallbacks = (Callbacks) parent;
          return mCallbacks;
        }
        parent = getParentFragment();
      }
    }
    return mCallbacks;
  }

  protected void doOnItemSelected(int position, long modelid) {
    getCallbacks().onItemSelected(position, modelid);
  }

}
