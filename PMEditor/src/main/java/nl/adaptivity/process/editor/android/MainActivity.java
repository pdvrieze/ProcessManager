package nl.adaptivity.process.editor.android;

import android.accounts.*;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.process.android.ProviderHelper;
import nl.adaptivity.process.editor.android.ProcessModelListOuterFragment.ProcessModelListCallbacks;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.ui.task.TaskDetailFragment.TaskDetailCallbacks;
import nl.adaptivity.process.ui.task.TaskListOuterFragment;
import nl.adaptivity.process.ui.task.TaskListOuterFragment.TaskListCallbacks;
import nl.adaptivity.process.tasks.data.TaskProvider;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

/**
 * The main activity that contains the navigation drawer.
 */
public class MainActivity extends AppCompatActivity implements OnItemClickListener, TaskListCallbacks, ProcessModelListCallbacks, GetNameDialogFragment.Callbacks, ProcessModelDetailFragment.Callbacks, TaskDetailCallbacks {

  private static final String TAG = MainActivity.class.getSimpleName();

  private static final int DLG_MODEL_INSTANCE_NAME = 1;

  static final class DrawerAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private final TitleFragment mItems[] = new TitleFragment[] {
       new TaskListOuterFragment(),
       new ProcessModelListOuterFragment(),
    };

    @Override
    public TitleFragment getItem(int position) {
      return mItems[position];
    }

    @Override
    public int getCount() {
      return mItems.length;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final TextView result;
      if (convertView==null) {
        if (mInflater == null) { mInflater = LayoutInflater.from(parent.getContext()); }
        result = (TextView) mInflater.inflate(R.layout.drawer_list_item, parent, false);
      } else {
        result = (TextView) convertView;
      }
      result.setText(getItem(position).getTitle(parent.getContext()));
      return result;
    }

  }

  private static final class SyncTask extends AsyncTask<URI, Void, Account> {

    private boolean mExpedited;
    private Activity mContext;
    private String mAuthority;

    public SyncTask(Activity context, String authority, boolean expedited) {
      mContext = context;
      mAuthority = authority;
      mExpedited = expedited;
    }

    @Override
    protected Account doInBackground(URI... params) {
      Account account = AuthenticatedWebClient.ensureAccount(mContext, params[0], ENSURE_ACCOUNT_REQUEST_CODE);
      if (account!=null) {
        ContentResolver.setIsSyncable(account, ProcessModelProvider.AUTHORITY, 1);
        AccountManager accountManager = AccountManager.get(mContext);
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

          @Override
          public void run(AccountManagerFuture<Bundle> future) {
            try {
              future.getResult();
            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
              Log.e(TAG, "Failure to get auth token", e);
            }
          }
        };
        if (mContext instanceof Activity) {
          accountManager.getAuthToken(account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, (Activity) mContext, callback, null);
        } else {
          Compat.getAuthToken(accountManager, account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, true, callback, null);
        }

        return account;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Account account) {
      if (account!=null) {
        if (mContext instanceof MainActivity) {
          ((MainActivity)mContext).mAccount = account;
        }
        ProviderHelper.requestSync(account, mAuthority, mExpedited);
      }
    }
  }

  private static final int ENSURE_ACCOUNT_REQUEST_CODE = 1;

  private Account mAccount;

  private DrawerAdapter mDrawerAdapter;

  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;

  private ListView mDrawerList;

  private CharSequence mTitle;

  private long mModelIdToInstantiate;
  private Set<String> mPendingSyncs = new TreeSet<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
    mDrawerList = (ListView) findViewById(R.id.left_drawer);

    mDrawerAdapter = new DrawerAdapter();

    // Set the adapter for the list view
    mDrawerList.setAdapter(mDrawerAdapter);
    mDrawerList.setOnItemClickListener(this);
    mTitle = getTitle();

    mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
        R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        CharSequence title = getActiveFragment().getTitle(MainActivity.this);
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

    // Set the drawer toggle as the DrawerListener
    mDrawerLayout.setDrawerListener(mDrawerToggle);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    showDrawerItem(getInitialDrawerItem());
    AsyncTask<URI, Void, Account> task = new AsyncTask<URI, Void, Account> () {

      @Override
      protected Account doInBackground(URI... params) {
        return AuthenticatedWebClient.ensureAccount(MainActivity.this, params[0], ENSURE_ACCOUNT_REQUEST_CODE);
      }

      @Override
      protected void onPostExecute(Account result) {
        mAccount = result;
        if (mAccount!=null) {
          for (String authority : mPendingSyncs) {
            ProviderHelper.requestSync(mAccount, authority, true);
          }
        }
      }

    };
    task.execute(ProviderHelper.getAuthBase(this));
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private int getInitialDrawerItem() {
    return 1;
  }

  protected TitleFragment getActiveFragment() {
    return (TitleFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_content);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode== ENSURE_ACCOUNT_REQUEST_CODE && resultCode==RESULT_OK) {
      String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      AuthenticatedWebClient.storeUsedAccount(MainActivity.this, accountName);
      mAccount = AuthenticatedWebClient.getAccount(AccountManager.get(this), this, ProviderHelper.getAuthBase(this));
      ProviderHelper.requestSync(mAccount, ProcessModelProvider.AUTHORITY, true);
      ProviderHelper.requestSync(mAccount, TaskProvider.AUTHORITY, true);
    }
    // XXX Make this actually do something on account selection.
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    int id = item.getItemId();
    if (id == android.R.id.home) {
      // This ID represents the Home or Up button. In the case of this
      // activity, the Up button is shown. Use NavUtils to allow users
      // to navigate up one level in the application structure. For
      // more details, see the Navigation pattern on Android Design:
      //
      // http://developer.android.com/design/patterns/navigation.html#up-vs-back
      //
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  // Handles the drawer click
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//    int oldPos = mDrawerList.getCheckedItemPosition();
//    if (oldPos!=ListView.INVALID_POSITION) {
//      mDrawerList.setItemChecked(oldPos, false);
//    }

    // TODO, ignore staying on same item
    showDrawerItem(position);
    mDrawerLayout.closeDrawer(mDrawerList);
  }

  @Override
  public void onInstantiateModel(long modelId, String suggestedName) {
    mModelIdToInstantiate = modelId;
    GetNameDialogFragment.show(getSupportFragmentManager(), DLG_MODEL_INSTANCE_NAME, "Instance name", "Provide a name for the process instance", this, suggestedName);
  }

  @Override
  public void onProcessModelSelected(long processModelId) {
    for(int i=0; i<mDrawerAdapter.getCount();++i) {
      if (mDrawerAdapter.getItem(i) instanceof ProcessModelListOuterFragment) {
        showDrawerItem(i);
        ProcessModelListOuterFragment fragment = (ProcessModelListOuterFragment) mDrawerAdapter.getItem(i);
        fragment.onProcessModelSelected(processModelId);
        break;
      }
    }
  }

  @Override
  public void onNameDialogCompletePositive(GetNameDialogFragment dialog, int id, String name) {
    try {
      ProcessModelProvider.instantiate(this, mModelIdToInstantiate, name);
    } catch (RemoteException e) {
      Toast.makeText(this, "Unfortunately the process could not be instantiated: "+e.getMessage(), Toast.LENGTH_SHORT).show();;
    }
  }

  @Override
  public void onNameDialogCompleteNegative(GetNameDialogFragment dialog, int id) {
    mModelIdToInstantiate=-1L;
  }

  protected void showDrawerItem(int position) {
    TitleFragment newFragment = mDrawerAdapter.getItem(position);
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_main_content, newFragment)
        .commit();
    mDrawerList.setItemChecked(position, true);
  }

  @Override
  public void requestSyncTaskList(boolean expedited) {
    requestSync(TaskProvider.AUTHORITY, expedited);
  }

  @Override
  public void requestSyncProcessModelList(boolean expedited) {
    requestSync(ProcessModelProvider.AUTHORITY, expedited);
  }

  public void requestSync(final String authority, boolean expedited) {
    if (mAccount!=null) {
      ProviderHelper.requestSync(mAccount, authority, expedited);
    } else if (expedited){
      mPendingSyncs.add(authority);
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
