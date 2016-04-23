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

package nl.adaptivity.process.data;

import android.accounts.*;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.AsyncCallableTask;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.ui.main.AuthenticatedActivity;
import nl.adaptivity.process.ui.main.SettingsActivity;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;


/**
 * A class to contain various bits that help with synchronization.
 * Created by pdvrieze on 15/12/15.
 */
public final class ProviderHelper {

  public static final int ENSURE_ACCOUNT_REQUEST_CODE = 1;
  private static final String TAG = "ProviderHelper";

  private ProviderHelper() {
  }

  public static String getSyncSource(final Context context) {
    final SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(context);
    final String            source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, null);
    return source;
  }

  public static URI getAuthBase(final Context context) {
    final String source = getSyncSource(context);
    return source == null ? null : AuthenticatedWebClient.getAuthBase(source);
  }

  public static void requestSync(final Account account, final String authority, final boolean expedited) {
    if (account!=null) {
      final Bundle extras = new Bundle(1);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, expedited);
      ContentResolver.requestSync(account, authority, extras );
    }
  }


  public static final class SyncCallable implements Callable<Account> {

    private final URI      mAuthbase;
    private final boolean  mExpedited;
    private final Activity mContext;
    private final String   mAuthority;

    /**
     * Create a new callable for synching and getting the required account.
     * @param context The context to use for the operation.
     * @param authority The authority to sync against. If this is <code>null</code>, only the account is retrieved,
     *                  but synchronization is not performed.
     * @param authbase The base for authentication to use.
     * @param expedited Whether expedited synchronization should be requested.
     */
    public SyncCallable(final Activity context, final String authority, final URI authbase, final boolean expedited) {
      mContext = context;
      mAuthority = authority;
      mExpedited = expedited;
      mAuthbase = authbase;
    }

    @Override
    public Account call() throws Exception {
      final URI     source  = mAuthbase;
      final Account account = AuthenticatedWebClient.ensureAccount(mContext, source, ENSURE_ACCOUNT_REQUEST_CODE, AuthenticatedActivity.REQUEST_DOWNLOAD_AUTHENTICATOR);
      if (account!=null) {
        ContentResolver.setIsSyncable(account, ProcessModelProvider.AUTHORITY, 1);
        final AccountManager accountManager = AccountManager.get(mContext);
        final AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

          @Override
          public void run(final AccountManagerFuture<Bundle> future) {
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

        if (mAuthority!=null) {
          ProviderHelper.requestSync(account, mAuthority, mExpedited);
        }
        return account;
      }
      return null;
    }

    public String getAuthority() {
      return mAuthority;
    }

    public boolean getExpedited() {
      return mExpedited;
    }
  }

  public static void requestSync(final Activity context, final String authority, final boolean expedited) {
    final URI authbase = getAuthBase(context);
    if (authbase!=null) {
      new AsyncCallableTask<Account, SyncCallable>().execute(new SyncCallable(context, authority, authbase, expedited));
    }
  }

  public static boolean isColumnInProjection(final String column, final String[] projection) {
    if (projection==null) { return false; }
    for (final String elem:projection) {
      if(column.equalsIgnoreCase(elem)) return true;
    }
    return false;
  }
}
