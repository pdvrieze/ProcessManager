package nl.adaptivity.process.editor.android;

import java.io.IOException;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.editor.android.ProcessModelListOuterFragment.ProcessModelListCallbacks;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.android.TaskListOuterFragment;
import nl.adaptivity.process.tasks.android.TaskListOuterFragment.TaskListCallbacks;
import nl.adaptivity.process.tasks.data.TaskProvider;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The main activity that contains the navigation drawer.
 */
public class MainActivity extends ActionBarActivity implements OnItemClickListener, TaskListCallbacks, ProcessModelListCallbacks {

  private static final String TAG = MainActivity.class.getSimpleName();

  static final class DrawerAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private final TitleFragment mItems[] = new TitleFragment[] {
       new TaskListOuterFragment(),
       new ProcessModelListOuterFragment(),
    };

    @Override
    public TitleFragment getItem(int pPosition) {
      return mItems[pPosition];
    }

    @Override
    public int getCount() {
      return mItems.length;
    }

    @Override
    public long getItemId(int pPosition) {
      return pPosition;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
      final TextView result;
      if (pConvertView==null) {
        if (mInflater == null) { mInflater = LayoutInflater.from(pParent.getContext()); }
        result = (TextView) mInflater.inflate(R.layout.drawer_list_item, pParent, false);
      } else {
        result = (TextView) pConvertView;
      }
      result.setText(getItem(pPosition).getTitle(pParent.getContext()));
      return result;
    }

  }

  private static final class SyncTask extends AsyncTask<String, Void, Account> {

    private boolean mExpedited;
    private Context mContext;
    private String mAuthority;

    public SyncTask(Context pContext, String pAuthority, boolean pExpedited) {
      mContext = pContext;
      mAuthority = pAuthority;
      mExpedited = pExpedited;
    }

    @Override
    protected Account doInBackground(String... pParams) {
      Account account = AuthenticatedWebClient.ensureAccount(mContext, pParams[0]);
      ContentResolver.setIsSyncable(account, ProcessModelProvider.AUTHORITY, 1);
      AccountManager accountManager = AccountManager.get(mContext);
      AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

        @Override
        public void run(AccountManagerFuture<Bundle> pFuture) {
          try {
            pFuture.getResult();
          } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            Log.e(TAG, "Failure to get auth token", e);
          }
        }};
      if (mContext instanceof Activity) {
        accountManager.getAuthToken(account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, (Activity) mContext, callback  , null);
      } else {
        Compat.getAuthToken(accountManager, account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, true, callback, null);
      }

      return account;
    }

    @Override
    protected void onPostExecute(Account account) {
      if (account!=null) {
        if (mContext instanceof MainActivity) {
          ((MainActivity)mContext).mAccount = account;
        }
        requestSync(account, mAuthority, mExpedited);
      }
    }
  }

  private Account mAccount;

  private DrawerAdapter mDrawerAdapter;

  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;

  private ListView mDrawerList;

  private CharSequence mTitle;

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
        getActionBar().setTitle(title);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

      /** Called when a drawer has settled in a completely open state. */
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        getActionBar().setTitle(mTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }
    };

    // Set the drawer toggle as the DrawerListener
    mDrawerLayout.setDrawerListener(mDrawerToggle);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    showDrawerItem(getInitialDrawerItem());
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
  public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
//    int oldPos = mDrawerList.getCheckedItemPosition();
//    if (oldPos!=ListView.INVALID_POSITION) {
//      mDrawerList.setItemChecked(oldPos, false);
//    }

    // TODO, ignore staying on same item
    showDrawerItem(pPosition);
    mDrawerLayout.closeDrawer(mDrawerList);
  }

  protected void showDrawerItem(int pPosition) {
    TitleFragment newFragment = mDrawerAdapter.getItem(pPosition);
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_main_content, newFragment)
        .commit();
    mDrawerList.setItemChecked(pPosition, true);
  }

  @Override
  public void requestSyncTaskList(boolean pExpedited) {
    requestSync(TaskProvider.AUTHORITY, pExpedited);
  }

  @Override
  public void requestSyncProcessModelList(boolean pExpedited) {
    requestSync(ProcessModelProvider.AUTHORITY, pExpedited);
  }

  public void requestSync(final String authority, boolean pExpedited) {
    requestSync(mAccount, authority, pExpedited);
  }

  public static void requestSyncProcessModelList(Account account, boolean pExpedited) {
    requestSync(account, ProcessModelProvider.AUTHORITY, pExpedited);
  }

  public static void requestSyncTaskList(Account account, boolean pExpedited) {
    requestSync(account, TaskProvider.AUTHORITY, pExpedited);
  }

  private static void requestSync(Account account, final String authority, boolean pExpedited) {
    Bundle extras = new Bundle(1);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, pExpedited);
    ContentResolver.requestSync(account, authority, extras );
  }

  public static void requestSyncProcessModelList(Context pContext, boolean pExpedited) {
    requestSync(pContext, ProcessModelProvider.AUTHORITY, pExpedited);
  }

  public static void requestSyncTaskList(Context pContext, boolean pExpedited) {
    requestSync(pContext, TaskProvider.AUTHORITY, pExpedited);
  }

  private static void requestSync(Context pContext, final String pAuthority, boolean pExpedited) {
    String authbase = getAuthbase(pContext);
    if (authbase!=null) {
      (new SyncTask(pContext, pAuthority, pExpedited)).execute(authbase);
    }
  }

  private static String getSyncSource(Context pContext) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pContext);
    String source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, null);
    return source;
  }

  private static String getAuthbase(Context pContext) {
    final String source = getSyncSource(pContext);
    return source == null ? null : AuthenticatedWebClient.getAuthBase(source);
  }
}
