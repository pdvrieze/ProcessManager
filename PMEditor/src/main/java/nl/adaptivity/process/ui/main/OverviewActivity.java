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

package nl.adaptivity.process.ui.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.AsyncCallableTask;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks;
import nl.adaptivity.process.data.ProviderHelper;
import nl.adaptivity.process.data.ProviderHelper.SyncCallable;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ActivityOverviewBinding;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.ui.main.OverviewFragment.OverviewCallbacks;
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks;
import nl.adaptivity.process.ui.model.ProcessModelListOuterFragment;
import nl.adaptivity.process.ui.task.TaskDetailFragment.TaskDetailCallbacks;
import nl.adaptivity.process.ui.task.TaskListOuterFragment;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class OverviewActivity extends ProcessBaseActivity implements OnNavigationItemSelectedListener,
                                                                     OverviewCallbacks, GetNameDialogFragmentCallbacks, ProcessModelDetailFragmentCallbacks,
                                                                     TaskDetailCallbacks, OnBackStackChangedListener {

  private static final String TAG = "OverviewActivity";
  private static final int DLG_MODEL_INSTANCE_NAME = 1;
  public static final String SERVERPATH_MODELS = "/ProcessEngine/processModels";
  public static final String SERVERPATH_TASKS = "/PEUserMessageHandler/UserMessageService/pendingTasks";

  private ActivityOverviewBinding mBinding;
  private CharSequence mTitle;
  private TitleFragment mActiveFragment;
  private ActionBarDrawerToggle mDrawerToggle;
  private long mModelIdToInstantiate = -1L;


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getSupportFragmentManager().addOnBackStackChangedListener(this);
    mTitle = getTitle();
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_overview);
    setSupportActionBar(mBinding.overviewAppBar.toolbar);

    final DrawerLayout drawer = mBinding.overviewDrawer;
    mDrawerToggle = new ActionBarDrawerToggle(this, drawer, mBinding.overviewAppBar.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      @Override
      public void onDrawerClosed(final View drawerView) {
        super.onDrawerClosed(drawerView);
        finishSettingFragment();
      }

      /** Called when a drawer has settled in a completely open state. */
      @Override
      public void onDrawerOpened(final View drawerView) {
        super.onDrawerOpened(drawerView);
        final ActionBar ab = getSupportActionBar();
        if(ab!=null) { ab.setTitle(mTitle); }
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

    };
    drawer.setDrawerListener(mDrawerToggle);

    final NavigationView navigationView = mBinding.navView;
    navigationView.setNavigationItemSelectedListener(this);

    requestAccount(ProviderHelper.getAuthBase(this));

    if (savedInstanceState==null) {
      final Intent intent    = getIntent();
      final Uri    uri       = intent.getData();
      long         handle    = 0;
      @IdRes int   navTarget = R.id.nav_home;
      if (uri!=null) {
        final String path = uri.getPath();
        if (path.startsWith(SERVERPATH_MODELS)) {
          navTarget = R.id.nav_models;
          if (path.length()>SERVERPATH_MODELS.length() && path.charAt(SERVERPATH_MODELS.length())=='/') {
            handle = Long.parseLong(path.substring(SERVERPATH_MODELS.length() + 1));
          }
        } else if (path.startsWith(SERVERPATH_TASKS)) {
          navTarget = R.id.nav_tasks;
          if (path.length()>SERVERPATH_TASKS.length() && path.charAt(SERVERPATH_TASKS.length())=='/') {
            handle = Long.parseLong(path.substring(SERVERPATH_TASKS.length() + 1));
          }
        }
      }
      if (handle!=0) {
        final AsyncTask<Long, Void, Long> bgNavigation = new AsyncTask<Long, Void, Long>() {
          public int mNavTarget;

          @Override
          protected Long doInBackground(final Long... params) {
            mNavTarget = params[0].intValue();
            final long handle = params[1].longValue();
            final long id;
            switch (mNavTarget) {
              case R.id.nav_models:
                id = ProcessModelProvider.getIdForHandle(OverviewActivity.this, handle);
                break;
              case R.id.nav_tasks:
                id = TaskProvider.getIdForHandle(OverviewActivity.this, handle);
                break;
              default:
                return Long.valueOf(0);
            }

            return Long.valueOf(id);
          }

          @Override
          protected void onPostExecute(final Long itemId) {
            onNavigationItemSelected(mNavTarget, false, itemId.longValue());
            mBinding.navView.setCheckedItem(mNavTarget);
          }
        };
        bgNavigation.execute(Long.valueOf(navTarget), Long.valueOf(handle));
      } else {
        // Go by default to the home fragment. Don't add it to the back stack.
        onNavigationItemSelected(navTarget, false);
        mBinding.navView.setCheckedItem(navTarget);
      }
    }

  }

  @Override
  protected void onDestroy() {
    getSupportFragmentManager().removeOnBackStackChangedListener(this);
    super.onDestroy();

  }

  private void finishSettingFragment() {
    final CharSequence title = getActiveFragment() == null ? getTitle() : getActiveFragment().getTitle(OverviewActivity.this);
    final ActionBar ab = getSupportActionBar();
    if(ab!=null) { ab.setTitle(title); }
    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
  }

  @Override
  public void onBackStackChanged() {
    final FragmentManager fm = getSupportFragmentManager();
    final Fragment currentFragment = fm.findFragmentById(R.id.overview_container);
    int navId=-1;
    if (currentFragment instanceof OverviewFragment) {
      navId = R.id.nav_home;
    } else if (currentFragment instanceof ProcessModelListOuterFragment) {
      navId = R.id.nav_models;
    } else if (currentFragment instanceof TaskListOuterFragment) {
      navId = R.id.nav_tasks;
    }
    if (currentFragment instanceof TitleFragment) {
      mActiveFragment = (TitleFragment) currentFragment;
    }
    if (navId>=0) {
      mBinding.navView.setCheckedItem(navId);
    }
    finishSettingFragment();

  }



  private TitleFragment getActiveFragment() {
    return mActiveFragment;
  }

  @Override
  protected void doAccountDetermined(final Account account) {
    if (account!=null) {
      ProviderHelper.requestSync(this, ProcessModelProvider.AUTHORITY, true);
      ProviderHelper.requestSync(this, TaskProvider.AUTHORITY, true);
    }
  }

  @Override
  public void onBackPressed() {
    if (mBinding.overviewDrawer.isDrawerOpen(GravityCompat.START)) {
      mBinding.overviewDrawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPostCreate(final Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  public boolean onNavigationItemSelected(final MenuItem item) {
    // Handle navigation view item clicks here.
    final int id = item.getItemId();

    return onNavigationItemSelected(id, false);
  }

  @Override
  public void showTasksFragment() {
    mBinding.navView.setCheckedItem(R.id.nav_tasks);
    onNavigationItemSelected(R.id.nav_tasks, true);
    finishSettingFragment();
  }

  @Override
  public void showModelsFragment() {
    mBinding.navView.setCheckedItem(R.id.nav_models);
    onNavigationItemSelected(R.id.nav_models, true);
    finishSettingFragment();
  }

  @Override
  public void onShowTask(final long taskId) {
    if (mActiveFragment instanceof TaskListOuterFragment) {
      ((TaskListOuterFragment)mActiveFragment).showTask(taskId);
    } else {
      mActiveFragment = TaskListOuterFragment.newInstance(taskId);
      getSupportFragmentManager().beginTransaction().replace(R.id.overview_container, mActiveFragment).addToBackStack("task").commit();
    }
  }

  private boolean onNavigationItemSelected(@IdRes final int id, final boolean addToBackstack) {
    return onNavigationItemSelected(id, addToBackstack, 0);
  }

  private boolean onNavigationItemSelected(@IdRes final int id, final boolean addToBackstack, final long itemId) {
    switch (id) {
      case R.id.nav_home:
        if (! (mActiveFragment instanceof OverviewFragment)) {

          mActiveFragment = OverviewFragment.newInstance();
          final FragmentManager fragmentManager = getSupportFragmentManager();
          @SuppressLint("CommitTransaction")
          final FragmentTransaction transaction = fragmentManager.beginTransaction()
                      .replace(R.id.overview_container, mActiveFragment, "home");
          // don't add it to the backstack if there is no child visible yet.
          if (addToBackstack) { transaction.addToBackStack("home"); }
          transaction.commit();
        }
        break;
      case R.id.nav_tasks: {
        if (!(mActiveFragment instanceof TaskListOuterFragment)) {
          mActiveFragment = TaskListOuterFragment.newInstance(itemId);
          @SuppressLint("CommitTransaction")
          final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                                                                         .replace(R.id.overview_container, mActiveFragment, "tasks");
          if (addToBackstack) { transaction.addToBackStack("tasks"); }
          transaction.commit();
        }
        break;
      }
      case R.id.nav_models: {
        if (!(mActiveFragment instanceof ProcessModelListOuterFragment)) {
          mActiveFragment = ProcessModelListOuterFragment.newInstance(itemId);
          @SuppressLint("CommitTransaction")
          final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                                                                        .replace(R.id.overview_container, mActiveFragment, "models");
          if (addToBackstack) { transaction.addToBackStack("models"); }
          transaction.commit();
        }
        break;
      }
      case R.id.nav_share:

        break;
      case R.id.nav_settings: {
        final Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        break;
      }
    }

    final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.overview_drawer);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }


  @Override
  public void onNameDialogCompletePositive(final GetNameDialogFragment dialog, final int id, final String name) {
    try {
      ProcessModelProvider.instantiate(this, mModelIdToInstantiate, name);
    } catch (RemoteException e) {
      Toast.makeText(this, "Unfortunately the process could not be instantiated: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onNameDialogCompleteNegative(final GetNameDialogFragment dialog, final int id) {
    mModelIdToInstantiate=-1L;
  }

  @Override
  public void requestSyncProcessModelList(final boolean immediate) {
    if (getAccount()!=null) {
      getSyncManager().requestSyncProcessModelList(immediate);
    }
  }

  @Override
  public void requestSyncTaskList(final boolean immediate) {
    if (getAccount()!=null) {
      getSyncManager().requestSyncTaskList(immediate);
    }
  }

  @Override
  public void onInstantiateModel(final long modelId, final String suggestedName) {
    mModelIdToInstantiate = modelId;
    GetNameDialogFragment.show(getSupportFragmentManager(), DLG_MODEL_INSTANCE_NAME, "Instance name", "Provide a name for the process instance", this, suggestedName);

  }

  @Override
  public void onProcessModelSelected(final long processModelId) {
    mBinding.navView.setCheckedItem(R.id.nav_models);
    onNavigationItemSelected(R.id.nav_models, true);
    if (mActiveFragment instanceof ProcessModelListOuterFragment && mActiveFragment.getActivity()!=null) {
        final ProcessModelListOuterFragment fragment = (ProcessModelListOuterFragment) mActiveFragment;
        fragment.onProcessModelSelected(processModelId);
    }

  }

  @Override
  public void dismissTaskDetails() {
    final TitleFragment af = getActiveFragment();
    if (af instanceof TaskListOuterFragment) {
      ((TaskListOuterFragment)af).onItemSelected(-1, -1);
    }
  }
}
