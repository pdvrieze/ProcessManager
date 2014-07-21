package nl.adaptivity.process.editor.android;

import java.io.IOException;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.models.ProcessModelProvider;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * The main activity that contains the navigation drawer.
 */
public class MainActivity extends FragmentActivity implements OnItemClickListener, TaskListCallbacks {

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
            Log.e(ProcessModelListActivity.class.getSimpleName(), "Failure to get auth token", e);
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

  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;

  private ListView mDrawerList;

  private CharSequence mTitle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    String[] sliderElems = getResources().getStringArray(R.array.array_slider_main_items);

    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
    mDrawerList = (ListView) findViewById(R.id.left_drawer);

    // Set the adapter for the list view
    mDrawerList.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, sliderElems));
    mDrawerList.setOnItemClickListener(this);
    mTitle = getTitle();

    mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
        R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

      /** Called when a drawer has settled in a completely closed state. */
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        getActionBar().setTitle(mTitle);
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
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);

    requestSyncProcessList(this, true);
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

  @Override
  public void requestSyncTaskList(boolean pExpedited) {
    requestSync(TaskProvider.AUTHORITY, pExpedited);
  }

  public void requestSyncProcessList(boolean pExpedited) {
    requestSync(ProcessModelProvider.AUTHORITY, pExpedited);
  }

  public void requestSync(final String authority, boolean pExpedited) {
    requestSync(mAccount, authority, pExpedited);
  }

  private static void requestSync(Account account, final String authority, boolean pExpedited) {
    Bundle extras = new Bundle(1);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, pExpedited);
    ContentResolver.requestSync(account, authority, extras );
  }

  public static void requestSyncProcessList(Context pContext, boolean pExpedited) {
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

  @Override
  public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pId) {
    // TODO Auto-generated method stub

  }
}
