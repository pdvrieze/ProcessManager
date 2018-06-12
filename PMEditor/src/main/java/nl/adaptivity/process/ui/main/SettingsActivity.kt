/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.main


import android.accounts.Account
import android.annotation.TargetApi
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceClickListener
import android.support.v4.app.NavUtils
import android.text.InputType
import android.text.TextUtils
import android.view.MenuItem
import kotlinx.coroutines.experimental.android.UI
import nl.adaptivity.android.coroutines.ActivityCoroutineScope
import nl.adaptivity.android.coroutines.aAsync
import nl.adaptivity.android.coroutines.aLaunch
import nl.adaptivity.android.coroutines.getAuthToken
import nl.adaptivity.android.darwin.AuthenticatedWebClient
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory.isAccountValid
import nl.adaptivity.android.darwin.isAccountValid
import nl.adaptivity.android.preference.AutoCompletePreference
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.tasks.data.TaskProvider
import nl.adaptivity.process.ui.UIConstants
import java.net.URI


/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
 * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
 * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity(), OnPreferenceClickListener {
    private var prefAccount: Preference? = null
    internal var needsVerification = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            needsVerification = savedInstanceState.getBoolean(KEY_NEEDS_VERIFICATION, false)
        }
        setupActionBar()
        if (!onIsMultiPane()) {
            fragmentManager.beginTransaction().replace(android.R.id.content, MergedPreferenceFragment()).commit()
        }

        updateAccount(this, needsVerification, getAuthBase(this)).observe(this, Observer { prefAccount?.run { summary = it?.name } })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_NEEDS_VERIFICATION, needsVerification)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            UIConstants.REQUEST_SELECT_ACCOUNT -> {
                val account = AuthenticatedWebClientFactory.handleSelectAcountActivityResult(this, resultCode, data)
                if (prefAccount != null) {
                    prefAccount!!.summary = account?.name
                    if (account != null) {
                        aLaunch<SettingsActivity,Unit> {
                            verifyUpdatedAccount(account)
                        }
                    } else {
                        needsVerification = true
                    }
                }
            }
            else                               -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }


    /** {@inheritDoc}  */
    override fun onIsMultiPane(): Boolean {
        return resources.configuration.screenWidthDp > 500
    }

    /** {@inheritDoc}  */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        if (onIsMultiPane()) {
            loadHeadersFromResource(R.xml.pref_headers, target)
        } // Only use headers in multi-pane mode, but not in single mode. In that case just put things sequentially.
    }


    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName ||
               GeneralPreferenceFragment::class.java.name == fragmentName ||
               DataSyncPreferenceFragment::class.java.name == fragmentName ||
               NotificationPreferenceFragment::class.java.name == fragmentName
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (AuthenticatedWebClient.KEY_ACCOUNT_NAME == preference.key) {
            startActivityForResult(AuthenticatedWebClientFactory.selectAccount(this, null, getAuthBase(this), true),
                                   UIConstants.REQUEST_SELECT_ACCOUNT)
            return true
        } else if (PREF_SYNC_FREQUENCY == preference.key) {
            val account = AuthenticatedWebClientFactory.getStoredAccount(this)
            if (account != null) {
                val pollFrequency = preference.sharedPreferences.getInt(PREF_SYNC_FREQUENCY, -1) * 60
                for (authority in arrayOf(ProcessModelProvider.AUTHORITY, TaskProvider.AUTHORITY)) {
                    ContentResolver.removePeriodicSync(account, authority, null)
                    if (pollFrequency > 0) {
                        ContentResolver.addPeriodicSync(account, authority, null, pollFrequency.toLong())
                    }
                }
            }
            return true
        }
        return false
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
            createGeneralPreferences(this, false)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                NavUtils.navigateUpTo(activity, NavUtils.getParentActivityIntent(activity)!!)
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    /* This fragment shows notification preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class NotificationPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
            createNotificationPreferences(this, false)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                NavUtils.navigateUpTo(activity, NavUtils.getParentActivityIntent(activity)!!)
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class DataSyncPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)

            createDataSyncPreferences(this, false)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                NavUtils.navigateUpTo(activity, NavUtils.getParentActivityIntent(activity)!!)
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    class MergedPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
            // Create a root, as we need that for categories
            preferenceScreen = preferenceManager.createPreferenceScreen(activity)
            createGeneralPreferences(this, true)
            createNotificationPreferences(this, true)
            createDataSyncPreferences(this, true)
        }
    }

    companion object {

        private const val PREF_SYNC_FREQUENCY = "sync_frequency"

        const val PREF_NOTIFICATIONS_NEW_TASK_RINGTONE = "notifications_new_task_ringtone"

        const val PREF_KITKATFILE = "pref_kitkatfile"

        const val PREF_SYNC_LOCAL = "sync_local"

        const val PREF_SYNC_SOURCE = "sync_source"
        private const val KEY_NEEDS_VERIFICATION = "NEEDS_VERIFICATION"

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) preference.entries[index] else null)

            } else if (preference is RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent)

                } else {
                    val ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue))

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null)
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        val name = ringtone.getTitle(preference.getContext())
                        preference.setSummary(name)
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        @JvmStatic
        fun getSyncSource(context: Context): URI {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.contains(PREF_SYNC_SOURCE)) {
                var sync_source = prefs.getString(PREF_SYNC_SOURCE, "")
                if (sync_source!![sync_source.length - 1] != '/') {
                    sync_source = "$sync_source/"
                    prefs.edit().putString(PREF_SYNC_SOURCE, sync_source).apply()
                }
                return URI.create(sync_source)
            }
            return URI.create(context.getString(R.string.default_sync_location))

        }

        @JvmStatic
        fun getAuthBase(context: Context): URI? {
            return AuthenticatedWebClientFactory.getAuthBase(getSyncSource(context))
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                                                                     PreferenceManager.getDefaultSharedPreferences(
                                                                             preference.context)
                                                                             .getString(preference.key, "")!!)
        }

        private fun createGeneralPreferences(fragment: PreferenceFragment, addCategory: Boolean) {
            setCategory(fragment, R.string.pref_header_general, addCategory)
            fragment.addPreferencesFromResource(R.xml.pref_general)
        }

        private fun createNotificationPreferences(fragment: PreferenceFragment, addCategory: Boolean) {
            setCategory(fragment, R.string.pref_header_notifications, addCategory)
            fragment.addPreferencesFromResource(R.xml.pref_notification)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(fragment.findPreference(PREF_NOTIFICATIONS_NEW_TASK_RINGTONE))

        }

        private fun createDataSyncPreferences(fragment: PreferenceFragment, addCategory: Boolean) {
            setCategory(fragment, R.string.pref_header_data_sync, addCategory)
            fragment.addPreferencesFromResource(R.xml.pref_data_sync)
            bindPreferenceSummaryToValue(fragment.findPreference(PREF_SYNC_FREQUENCY))
            val pref_sync_source = fragment.findPreference(PREF_SYNC_SOURCE) as AutoCompletePreference
            pref_sync_source.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

            bindPreferenceSummaryToValue(pref_sync_source)

            val prefAccount = fragment.findPreference(AuthenticatedWebClient.KEY_ACCOUNT_NAME)
            (fragment.activity as SettingsActivity).prefAccount = prefAccount
            prefAccount.onPreferenceClickListener = fragment.activity as SettingsActivity
            bindPreferenceSummaryToValue(prefAccount)
        }

        private fun setCategory(fragment: PreferenceFragment, headerLabelId: Int, addCategory: Boolean) {
            if (addCategory) {
                val header = PreferenceCategory(fragment.activity)
                header.setTitle(headerLabelId)
                fragment.preferenceScreen.addPreference(header)
            }
        }
    }

}

private fun updateAccount(applicationContext: SettingsActivity, needsVerification: Boolean, authBase:URI?): LiveData<Account?> {
    val result = MutableLiveData<Account?>()
    val account = AuthenticatedWebClientFactory.getStoredAccount(applicationContext) ?: return result.apply { value = null }

    applicationContext.aAsync {
        if(isAccountValid(account, authBase)) {
            if (needsVerification) {
                verifyUpdatedAccount(account)
            }
        } else {
            AuthenticatedWebClientFactory.setStoredAccount(getAndroidContext(), null)
        }
        result.postValue(if(isAccountValid(account, authBase)) account else null)
    }
    return result
}

private suspend fun ActivityCoroutineScope<SettingsActivity,*>.verifyUpdatedAccount(
    account: Account) {
    if (getAuthToken(account, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE) != null) {
        ContentResolver.setSyncAutomatically(account, ProcessModelProvider.AUTHORITY, true)
        ContentResolver.setSyncAutomatically(account, TaskProvider.AUTHORITY, true)
        async(UI) {
            (this as ActivityCoroutineScope<SettingsActivity,*>).activity.needsVerification = false
        }
    }
}