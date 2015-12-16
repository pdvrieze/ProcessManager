package nl.adaptivity.process.ui.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.AsyncCallableTask;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.process.android.ProviderHelper;
import nl.adaptivity.process.android.ProviderHelper.SyncCallable;
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment;
import nl.adaptivity.process.ui.model.ProcessModelListOuterFragment;
import nl.adaptivity.process.ui.model.ProcessModelListOuterFragment.ProcessModelListCallbacks;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.ui.task.TaskDetailFragment.TaskDetailCallbacks;
import nl.adaptivity.process.ui.task.TaskListOuterFragment;
import nl.adaptivity.process.ui.task.TaskListOuterFragment.TaskListCallbacks;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class OverviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
                                                                   TaskListCallbacks,
                                                                   ProcessModelListCallbacks,
                                                                   GetNameDialogFragment.Callbacks,
                                                                   ProcessModelDetailFragment.Callbacks,
                                                                   TaskDetailCallbacks {

  private final class SyncTask extends AsyncCallableTask<Account, SyncCallable> {

    @Override
    protected void onPostExecute(Future<Account> accountf) {
      try {
        mAccount = accountf.get();
      } catch (InterruptedException e) {
        // ignore
      } catch (ExecutionException e) {
        Log.w(TAG, "Failure to link to the account", e.getCause());
      }
    }
  }

  private static final String TAG = "OverviewActivity";
  private static final int DLG_MODEL_INSTANCE_NAME = 1;

  private Account mAccount;

  private nl.adaptivity.process.ui.main.ActivityOverviewBinding mBinding;
  private CharSequence mTitle;
  private TitleFragment mActiveFragment;
  private ActionBarDrawerToggle mDrawerToggle;
  private long mModelIdToInstantiate = -1L;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTitle = getTitle();
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_overview);
    setSupportActionBar(mBinding.overviewAppBar.toolbar);

    DrawerLayout drawer = mBinding.overviewDrawer;
    mDrawerToggle = new ActionBarDrawerToggle(this, drawer, mBinding.overviewAppBar.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        CharSequence title = getActiveFragment()==null ? getTitle() : getActiveFragment().getTitle(OverviewActivity.this);
        ActionBar ab = getSupportActionBar();
        if(ab!=null) { ab.setTitle(title); }
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

      /** Called when a drawer has settled in a completely open state. */
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        ActionBar ab = getSupportActionBar();
        if(ab!=null) { ab.setTitle(mTitle); }
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

    };
    drawer.setDrawerListener(mDrawerToggle);

    NavigationView navigationView = mBinding.navView;
    navigationView.setNavigationItemSelectedListener(this);

    AsyncTask<URI, Void, Account> task = new AsyncTask<URI, Void, Account> () {

      @Override
      protected Account doInBackground(URI... params) {
        return AuthenticatedWebClient.ensureAccount(OverviewActivity.this, params[0], ProviderHelper.ENSURE_ACCOUNT_REQUEST_CODE);
      }

      @Override
      protected void onPostExecute(Account result) {
        mAccount = result;
        if (mAccount!=null) {
          ProviderHelper.requestSync(mAccount, ProcessModelProvider.AUTHORITY, true);
          ProviderHelper.requestSync(mAccount, TaskProvider.AUTHORITY, true);
        }
      }

    };
    task.execute(ProviderHelper.getAuthBase(this));

  }

  private TitleFragment getActiveFragment() {
    return mActiveFragment;
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode== ProviderHelper.ENSURE_ACCOUNT_REQUEST_CODE && resultCode==RESULT_OK) {
      String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      AuthenticatedWebClient.storeUsedAccount(this, accountName);
      mAccount = new Account(accountName, AuthenticatedWebClient.ACCOUNT_TYPE);
      ProviderHelper.requestSync(this, ProcessModelProvider.AUTHORITY, true);
      ProviderHelper.requestSync(this, TaskProvider.AUTHORITY, true);
    }
    // XXX Make this actually do something on account selection.
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.overview_drawer);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    return onNavigationItemSelected(id);
  }

  private boolean onNavigationItemSelected(@IdRes final int id) {
    switch (id) {
      case R.id.nav_home:
        // Handle the camera action
        break;
      case R.id.nav_tasks: {
        mActiveFragment = new TaskListOuterFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.overview_container, mActiveFragment).commit();

        break;
      }
      case R.id.nav_models: {
        mActiveFragment = new ProcessModelListOuterFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.overview_container, mActiveFragment).commit();
      }

        break;
      case R.id.nav_share:

        break;
      case R.id.nav_settings: {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        break;
      }
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.overview_drawer);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }


  @Override
  public void onNameDialogCompletePositive(GetNameDialogFragment dialog, int id, String name) {
    try {
      ProcessModelProvider.instantiate(this, mModelIdToInstantiate, name);
    } catch (RemoteException e) {
      Toast.makeText(this, "Unfortunately the process could not be instantiated: " + e.getMessage(), Toast.LENGTH_SHORT).show();;
    }
  }

  @Override
  public void onNameDialogCompleteNegative(GetNameDialogFragment dialog, int id) {
    mModelIdToInstantiate=-1L;
  }

  @Override
  public void requestSyncProcessModelList(final boolean immediate) {
    ProcessModelProvider.requestSyncProcessModelList(this, immediate);
  }

  @Override
  public void requestSyncTaskList(final boolean immediate) {
    TaskProvider.requestSyncTaskList(this, immediate);
  }

  @Override
  public void onInstantiateModel(final long modelId, final String suggestedName) {
    mModelIdToInstantiate = modelId;
    GetNameDialogFragment.show(getSupportFragmentManager(), DLG_MODEL_INSTANCE_NAME, "Instance name", "Provide a name for the process instance", this, suggestedName);

  }

  @Override
  public void onProcessModelSelected(final long processModelId) {
    mBinding.navView.setCheckedItem(R.id.nav_models);
    onNavigationItemSelected(R.id.nav_models);
    if (mActiveFragment instanceof ProcessModelListOuterFragment) {
        ProcessModelListOuterFragment fragment = (ProcessModelListOuterFragment) mActiveFragment;
        fragment.onProcessModelSelected(processModelId);
    }

  }

  @Override
  public void dismissTaskDetails() {
    TitleFragment af = getActiveFragment();
    if (af instanceof TaskListOuterFragment) {
      ((TaskListOuterFragment)af).onItemSelected(-1, -1);
    }
  }
}
