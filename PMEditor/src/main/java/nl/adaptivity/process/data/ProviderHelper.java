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

package nl.adaptivity.process.data;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import nl.adaptivity.android.coroutines.Maybe;
import nl.adaptivity.android.coroutines.SerializableHandler;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.util.AsyncCallableTask;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.ui.main.SettingsActivity;
import org.jetbrains.annotations.NotNull;

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

  @NotNull
  public static URI getSyncSource(final Context context) {
    final SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(context);
      final String          defaultSyncLocation = context.getString(R.string.default_sync_location);
      String                source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, defaultSyncLocation);
    if (source.length()==0) source = defaultSyncLocation;
    return URI.create(source);
  }

  @NotNull
  public static URI getAuthBase(final Context context) {
    final URI source = getSyncSource(context);
    return AuthenticatedWebClientFactory.getAuthBase(source);
  }

  public static void requestSync(final Account account, final String authority, final boolean expedited) {
    if (account!=null) {
      final Bundle extras = new Bundle(1);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, expedited);
      ContentResolver.requestSync(account, authority, extras );
    }
  }


  public static final class SyncCallable implements Callable<Void> {

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

    void handleAccountChosen(Activity a, Maybe<Account> maybeAccount) {
      if (maybeAccount.isOk()) {
        Account account = maybeAccount.flatMap();
        if (AuthenticatedWebClientFactory.isAccountValid(a, account, getAuthBase(a))) {
          if (mAuthority!=null) {
            ProviderHelper.requestSync(account, mAuthority, mExpedited);
          }
        }
      }
    }

    @WorkerThread
    @Override
    public Void call() throws Exception {
      final URI     source  = mAuthbase;

      final SerializableHandler<Activity, Maybe<Account>> callbacks;
      if (true) {
        callbacks = this::handleAccountChosen;

      }

      AuthenticatedWebClientFactory.tryEnsureAccount(mContext, source, callbacks);
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
      new AsyncCallableTask<Void, SyncCallable>().execute(new SyncCallable(context, authority, authbase, expedited));
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
