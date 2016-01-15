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

package nl.adaptivity.process.editor.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;

import java.io.File;


public class PMProcessesFragment extends Fragment {

  private static final String TAG=PMProcessesFragment.class.getSimpleName();
  private static final int TYPE_FILE = 0;
  private static final int TYPE_SVG = 1;
  private static final String KEY_PROCESSMODEL = "pm";
  private static final String KEY_FILE = "tmpfile";
  public static final String ARG_MENU = "menu";

  public interface ProcessesCallback {

    void requestShareFile(ClientProcessModel<?, ?> processModel);

    void requestSaveFile(ClientProcessModel<?, ?> processModel);

    void requestShareSVG(ClientProcessModel<?, ?> processModel);

    void requestExportSVG(ClientProcessModel<?, ?> processModel);
  }

  public interface PMProvider {
    ClientProcessModel<?, ?> getProcessModel();
  }

  private ClientProcessModel<?, ?> mProcessModel;
  protected File mTmpFile;
  private boolean mMenu;
  private PMProvider mProvider;
  private ProcessesCallback mCallback;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null) {
      final PMParcelable parcelable = savedInstanceState.<PMParcelable>getParcelable(KEY_PROCESSMODEL);
      mProcessModel = parcelable==null ? null : parcelable.getProcessModel();
      String s = savedInstanceState.getString(KEY_FILE);
      mTmpFile = s==null ? null : new File(s);
      mMenu = savedInstanceState.getBoolean(ARG_MENU, true);
    } else if (getArguments()!=null){
      mMenu = getArguments().getBoolean(ARG_MENU, true);
    } else {
      mMenu = true;
    }
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }



  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    Fragment parent = getParentFragment();
    if (this instanceof PMProvider) {
      mProvider = (PMProvider) this;
    } else if (parent!=null && (parent instanceof PMProvider)) {
      mProvider = (PMProvider) parent;
    } else if (activity instanceof PMProvider){
      mProvider = (PMProvider) activity;
    }
    if (activity instanceof ProcessesCallback) {
      mCallback = (ProcessesCallback) activity;
    }
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }



  @Override
  public void onDetach() {
    if (mTmpFile!=null) {
      mTmpFile.delete();
      mTmpFile=null;
    }
    super.onDetach();
  }



  public void doShareFile(ClientProcessModel<?, ?> processModel) {
    mCallback.requestShareFile(processModel);
  }

  public void doSaveFile(ClientProcessModel<?, ?> processModel) {
    mCallback.requestSaveFile(processModel);
  }

  public void doShareSVG(ClientProcessModel<?, ?> processModel) {
    mCallback.requestShareSVG(processModel);
  }

  public void doExportSVG(ClientProcessModel<?, ?> processModel) {
    mCallback.requestExportSVG(processModel);
  }

  public void setPMProvider(PMProvider pmProvider) {
    mProvider = pmProvider;
    if (mMenu) {
      setHasOptionsMenu(mProvider!=null);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.pm_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ClientProcessModel<?, ?> pm = null;
    if ((item.getItemId()==R.id.ac_export||item.getItemId()==R.id.ac_export_svg||item.getItemId()==R.id.ac_share_pm||item.getItemId()==R.id.ac_share_pm_svg)&&
        (mProvider==null|| (pm = mProvider.getProcessModel())==null)) {
      Toast.makeText(getActivity(), "No process model available", Toast.LENGTH_LONG).show();
      return true;
    }
    switch (item.getItemId()) {
      case R.id.ac_export:
        doSaveFile(pm);
        return true;
      case R.id.ac_export_svg:
        doExportSVG(pm);
        return true;
      case R.id.ac_share_pm:
        doShareFile(pm);;
        return true;
      case R.id.ac_share_pm_svg:
        doShareSVG(pm);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (mProcessModel!=null) outState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(mProcessModel));
    if (mTmpFile!=null)  outState.putString(KEY_FILE, mTmpFile.toString());
  }

}