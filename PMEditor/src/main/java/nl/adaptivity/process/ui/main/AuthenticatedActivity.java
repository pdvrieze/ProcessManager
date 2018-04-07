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

package nl.adaptivity.process.ui.main;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory.AuthenticatedWebClientCallbacks;
import nl.adaptivity.process.ui.UIConstants;

import java.net.URI;


/**
 * Created by pdvrieze on 11/01/16.
 */
public abstract class AuthenticatedActivity extends AppCompatActivity {

  private class RequestAccountTask extends AsyncTask<URI, Void, Account> {

    @Override
    protected Account doInBackground(final URI... params) {
      Account account = AuthenticatedWebClientFactory.getStoredAccount(AuthenticatedActivity.this);
      if (account!=null) {
        if (!AuthenticatedWebClientFactory.isAccountValid(AuthenticatedActivity.this, account, params[0])) {
          return null;
        }
      }
      return account;
    }

    @Override
    protected void onPostExecute(final Account account) {
      mAccount  = account;
      doAccountDetermined(account);
    }
  }

  private Account mAccount;

  public Account getAccount() {
    return mAccount;
  }

    protected void doAccountDetermined(Account account) {
    // Hook for subclasses
  }

  public final void requestAccount(URI authBase) {
    new RequestAccountTask().execute(authBase);
  }
}
