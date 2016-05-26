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
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory.AuthenticatedWebClientCallbacks;
import nl.adaptivity.android.preference.AutoCompletePreference;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.sync.SyncManager;

import java.net.URI;
import java.util.List;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener, AuthenticatedWebClientCallbacks {

  private static class VerifyUpdatedAccountTask extends AsyncTask<Object,Void, String> {

    private final Account mAccount;

    public VerifyUpdatedAccountTask(final Account account) {mAccount = account;}

    @Override
    protected String doInBackground(final Object... params) {
      Activity activity = (Activity) params[0];
      URI      authBase = (URI) params[1];
      return AuthenticatedWebClientFactory.getAuthToken(activity, authBase, mAccount);
    }

    @Override
    protected void onPostExecute(final String token) {

      if (token!=null) {
        ContentResolver.setSyncAutomatically(mAccount, ProcessModelProvider.AUTHORITY, true);
        ContentResolver.setSyncAutomatically(mAccount, TaskProvider.AUTHORITY, true);
      }
    }
  }

  private class AccountLoader extends AsyncTask<URI, Void, Account> {

    @Override
    protected Account doInBackground(final URI... params) {
      URI authBase = params[0];
      Account account = AuthenticatedWebClientFactory.getStoredAccount(SettingsActivity.this);
      if (account!=null && AuthenticatedWebClientFactory.isAccountValid(SettingsActivity.this, account, authBase)) {
        return account;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(final Account account) {
      if (account==null) {
        AuthenticatedWebClientFactory.setStoredAccount(SettingsActivity.this, null);
        if (mNeedsVerification) {
          new VerifyUpdatedAccountTask(account).execute(SettingsActivity.this, getAuthBase(SettingsActivity.this));
          mNeedsVerification = false;
        }
      }
      if (mPrefAccount!=null) {
        mPrefAccount.setSummary(account == null ? null : account.name);
      }
    }
  }

  private static final String PREF_SYNC_FREQUENCY = "sync_frequency";

  public static final String PREF_NOTIFICATIONS_NEW_TASK_RINGTONE = "notifications_new_task_ringtone";

  public static final String PREF_KITKATFILE="pref_kitkatfile";

  public static final String PREF_SYNC_SOURCE     = "sync_source";
  private static final java.lang.String KEY_NEEDS_VERIFICATION = "NEEDS_VERIFICATION";
  private Preference mPrefAccount;
  private boolean mNeedsVerification = false;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null) {
      mNeedsVerification = savedInstanceState.getBoolean(KEY_NEEDS_VERIFICATION, false);
    }
    setupActionBar();
    if (! onIsMultiPane()) {
      final MergedPreferenceFragment preferenceFragment;
      preferenceFragment = new MergedPreferenceFragment();
      getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();
    }

    new AccountLoader().execute(getAuthBase(this));
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_NEEDS_VERIFICATION, mNeedsVerification);
  }

  @Override
  public void showDownloadDialog() {
    AuthenticatedWebClientFactory.doShowDownloadDialog(this, UIConstants.REQUEST_DOWNLOAD_AUTHENTICATOR);
  }

  @Override
  public void startSelectAccountActivity(final Intent selectAccount) {
    startActivityForResult(selectAccount, UIConstants.REQUEST_SELECT_ACCOUNT);
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
      case UIConstants.REQUEST_SELECT_ACCOUNT: {
        final Account account = AuthenticatedWebClientFactory.handleSelectAcountActivityResult(this, resultCode, data);
        if (mPrefAccount!=null) {
          mPrefAccount.setSummary(account.name);
          if (account!=null) {
            new VerifyUpdatedAccountTask(account).execute(this, getAuthBase(this));
          } else {
            mNeedsVerification = true;
          }
        }
        break;
      }
      case UIConstants.REQUEST_DOWNLOAD_AUTHENTICATOR: {
        AuthenticatedWebClientFactory.handleInstallAuthenticatorActivityResult(this, resultCode, data);
        if (resultCode==RESULT_OK) {
          // Trigger an automatic new attempt to show the download dialog
          AuthenticatedWebClientFactory.showAccountSelection(this, this, getAuthBase(this));
        }
        break;
      }
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Set up the {@link android.app.ActionBar}, if the API is available.
   */
  private void setupActionBar() {
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      // Show the Up button in the action bar.
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }


  /** {@inheritDoc} */
  @Override
  public boolean onIsMultiPane() {
    return getResources().getConfiguration().screenWidthDp > 500;
  }

  /** {@inheritDoc} */
  @Override
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void onBuildHeaders(final List<Header> target) {
    if (onIsMultiPane()) {
      loadHeadersFromResource(R.xml.pref_headers, target);
    } // Only use headers in multi-pane mode, but not in single mode. In that case just put things sequentially.
  }

  /**
   * A preference value change listener that updates the preference's summary
   * to reflect its new value.
   */
  private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
    @Override
    public boolean onPreferenceChange(final Preference preference, final Object value) {
      final String stringValue = value.toString();

      if (preference instanceof ListPreference) {
        // For list preferences, look up the correct display value in
        // the preference's 'entries' list.
        final ListPreference listPreference = (ListPreference) preference;
        final int            index          = listPreference.findIndexOfValue(stringValue);

        // Set the summary to reflect the new value.
        preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

      } else if (preference instanceof RingtonePreference) {
        // For ringtone preferences, look up the correct display value
        // using RingtoneManager.
        if (TextUtils.isEmpty(stringValue)) {
          // Empty values correspond to 'silent' (no ringtone).
          preference.setSummary(R.string.pref_ringtone_silent);

        } else {
          final Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

          if (ringtone == null) {
            // Clear the summary if there was a lookup error.
            preference.setSummary(null);
          } else {
            // Set the summary to reflect the new ringtone display
            // name.
            final String name = ringtone.getTitle(preference.getContext());
            preference.setSummary(name);
          }
        }

      } else {
        // For all other preferences, set the summary to the value's
        // simple string representation.
        preference.setSummary(stringValue);
      }
      return true;
    }
  };



  /**
   * This method stops fragment injection in malicious applications.
   * Make sure to deny any unknown fragments here.
   */
  protected boolean isValidFragment(final String fragmentName) {
    return PreferenceFragment.class.getName().equals(fragmentName) ||
           GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
           DataSyncPreferenceFragment.class.getName().equals(fragmentName) ||
           NotificationPreferenceFragment.class.getName().equals(fragmentName);
  }

  @Override
  public boolean onPreferenceClick(final Preference preference) {
    if (AuthenticatedWebClient.KEY_ACCOUNT_NAME.equals(preference.getKey())) {
      AuthenticatedWebClientFactory.showAccountSelection(this, this, getAuthBase(this));
      return true;
    } else if (PREF_SYNC_FREQUENCY.equals(preference.getKey())) {
      Account account = AuthenticatedWebClientFactory.getStoredAccount(this);
      if (account!=null) {
        int pollFrequency = preference.getSharedPreferences().getInt(PREF_SYNC_FREQUENCY, -1) * 60;
        for (String authority : new String[]{ProcessModelProvider.AUTHORITY, TaskProvider.AUTHORITY}) {
          ContentResolver.removePeriodicSync(account, authority, null);
          if (pollFrequency > 0) {
            ContentResolver.addPeriodicSync(account, authority, null, pollFrequency);
          }
        }
      }
      return true;
    }
    return false;
  }


  public static URI getSyncSource(final Context context) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (prefs.contains(PREF_SYNC_SOURCE)) {
      String sync_source = prefs.getString(PREF_SYNC_SOURCE, "");
      if (! (sync_source.charAt(sync_source.length()-1)=='/')) {
        sync_source = sync_source+'/';
        prefs.edit().putString(PREF_SYNC_SOURCE, sync_source).apply();
      }
      return URI.create(sync_source);
    }
    return URI.create(context.getString(R.string.default_sync_location));

  }

  public static URI getAuthBase(final Context context) {
    return AuthenticatedWebClientFactory.getAuthBase(getSyncSource(context));
  }

  /**
   * This fragment shows general preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class GeneralPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      createGeneralPreferences(this, false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
      final int id = item.getItemId();
      if (id == android.R.id.home) {
        NavUtils.navigateUpTo(getActivity(), NavUtils.getParentActivityIntent(getActivity()));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

   /* This fragment shows notification preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class NotificationPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      createNotificationPreferences(this, false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
      final int id = item.getItemId();
      if (id == android.R.id.home) {
        NavUtils.navigateUpTo(getActivity(), NavUtils.getParentActivityIntent(getActivity()));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Binds a preference's summary to its value. More specifically, when the
   * preference's value is changed, its summary (line of text below the
   * preference title) is updated to reflect the value. The summary is also
   * immediately updated upon calling this method. The exact display format is
   * dependent on the type of preference.
   *
   * @see #sBindPreferenceSummaryToValueListener
   */
  private static void bindPreferenceSummaryToValue(final Preference preference) {
    // Set the listener to watch for value changes.
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

    // Trigger the listener immediately with the preference's
    // current value.
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference
                                                                                                                               .getContext())
                                                                                          .getString(preference.getKey(), ""));
  }  /**
   * This fragment shows data and sync preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class DataSyncPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);

      createDataSyncPreferences(this, false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
      final int id = item.getItemId();
      if (id == android.R.id.home) {
        NavUtils.navigateUpTo(getActivity(), NavUtils.getParentActivityIntent(getActivity()));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  public static class MergedPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      // Create a root, as we need that for categories
      setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
      createGeneralPreferences(this, true);
      createNotificationPreferences(this, true);
      createDataSyncPreferences(this, true);
    }
  }

  private static void createGeneralPreferences(final PreferenceFragment fragment, final boolean addCategory) {
    setCategory(fragment, R.string.pref_header_general, addCategory);
    fragment.addPreferencesFromResource(R.xml.pref_general);
  }

  private static void createNotificationPreferences(final PreferenceFragment fragment, final boolean addCategory) {
    setCategory(fragment, R.string.pref_header_notifications, addCategory);
    fragment.addPreferencesFromResource(R.xml.pref_notification);

    // Bind the summaries of EditText/List/Dialog/Ringtone preferences
    // to their values. When their values change, their summaries are
    // updated to reflect the new value, per the Android Design
    // guidelines.
    bindPreferenceSummaryToValue(fragment.findPreference(PREF_NOTIFICATIONS_NEW_TASK_RINGTONE));

  }

  private static void createDataSyncPreferences(final PreferenceFragment fragment, final boolean addCategory) {
    setCategory(fragment, R.string.pref_header_data_sync, addCategory);
    fragment.addPreferencesFromResource(R.xml.pref_data_sync);
    bindPreferenceSummaryToValue(fragment.findPreference(PREF_SYNC_FREQUENCY));
    final AutoCompletePreference pref_sync_source = (AutoCompletePreference) fragment.findPreference(PREF_SYNC_SOURCE);
    pref_sync_source.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

    bindPreferenceSummaryToValue(pref_sync_source);

    final Preference prefAccount = fragment.findPreference(AuthenticatedWebClient.KEY_ACCOUNT_NAME);
    ((SettingsActivity)fragment.getActivity()).mPrefAccount = prefAccount;
    prefAccount.setOnPreferenceClickListener((SettingsActivity)fragment.getActivity());
    bindPreferenceSummaryToValue(prefAccount);
  }

  private static void setCategory(final PreferenceFragment fragment, final int headerLabelId, final boolean addCategory) {
    if (addCategory) {
      final PreferenceCategory header = new PreferenceCategory(fragment.getActivity());
      header.setTitle(headerLabelId);
      fragment.getPreferenceScreen().addPreference(header);
    }
  }

}
