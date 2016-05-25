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
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory.EnsureCallbacks;

import java.net.URI;


/**
 * Created by pdvrieze on 11/01/16.
 */
public abstract class AuthenticatedActivity extends AppCompatActivity implements EnsureCallbacks {

  private class RequestAccountTask extends AsyncTask<URI, Void, Account> {

    @Override
    protected Account doInBackground(final URI... params) {
      return AuthenticatedWebClientFactory.tryEnsureAccount(AuthenticatedActivity.this, params[0], AuthenticatedActivity.this);
    }

    @Override
    protected void onPostExecute(final Account account) {
      mAccount  = account;
      doAccountDetermined(account);
    }
  }

  public static final int REQUEST_DOWNLOAD_AUTHENTICATOR = 41;
  private static final int REQUEST_SELECT_ACCOUNT        = 42;

  private Account mAccount;

  public Account getAccount() {
    return mAccount;
  }

  @Override
  public void showDownloadDialog() {
    AuthenticatedWebClientFactory.doShowDownloadDialog(this, REQUEST_DOWNLOAD_AUTHENTICATOR);
  }

  @Override
  public void startSelectAccountActivity(final Intent selectAccount) {
    startActivityForResult(selectAccount, REQUEST_SELECT_ACCOUNT);
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
      case REQUEST_DOWNLOAD_AUTHENTICATOR: AuthenticatedWebClientFactory.handleInstallAuthenticatorActivityResult(this, resultCode, data); break;
      case REQUEST_SELECT_ACCOUNT: mAccount = AuthenticatedWebClientFactory.handleSelectAcountActivityResult(this, resultCode, data);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  protected void doAccountDetermined(Account account) {
    // Hook for subclasses
  }

  public final void requestAccount(URI authBase) {
    new RequestAccountTask().execute(authBase);
  }
}
