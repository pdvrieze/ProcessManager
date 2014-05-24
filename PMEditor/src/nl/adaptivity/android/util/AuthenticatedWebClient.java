package nl.adaptivity.android.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import static net.devrieze.util.CollectionUtil.*;
import nl.adaptivity.process.editor.android.SettingsActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;



public class AuthenticatedWebClient {

  private static final String TAG = AuthenticatedWebClient.class.getName();

  static final String ACCOUNT_TYPE = "uk.ac.bournemouth.darwin.account";

  public static final String ACCOUNT_TOKEN_TYPE="uk.ac.bournemouth.darwin.auth";

  private static final String KEY_ASKED_FOR_NEW = "askedForNewAccount";
  public static final String KEY_AUTH_BASE = "authbase";

  private static final String DARWIN_AUTH_COOKIE = "DWNID";

  private static class DarwinCookie implements Cookie {

    private static final int[] PORTS = new int[] {443};
    private final String aAuthToken;

    public DarwinCookie(String pAuthtoken) {
      aAuthToken = pAuthtoken;
    }

    @Override
    public String getComment() {
      return null;
    }

    @Override
    public String getCommentURL() {
      return null;
    }

    @Override
    public String getDomain() {
      return "darwin.bournemouth.ac.uk";
    }

    @Override
    public Date getExpiryDate() {
      return null;
    }

    @Override
    public String getName() {
      return DARWIN_AUTH_COOKIE;
    }

    @Override
    public String getPath() {
      return "/";
    }

    @Override
    public int[] getPorts() {
      return PORTS;
    }

    @Override
    public String getValue() {
      return aAuthToken;
    }

    @Override
    public int getVersion() {
      return 1;
    }

    @Override
    public boolean isExpired(Date pDate) {
      return false;
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public boolean isSecure() {
      return false;
    }

  }

  private Context mContext;
  private boolean mAskedForNewAccount = false;

  private String mToken = null;

  private DefaultHttpClient mHttpClient;

  public AuthenticatedWebClient(Context context) {
    mContext = context;
  }
  public HttpResponse execute(HttpUriRequest pRequest) throws ClientProtocolException, IOException {
    return execute(pRequest, false);
  }

  private HttpResponse execute(HttpUriRequest pRequest, boolean retry) throws ClientProtocolException, IOException {
    final AccountManager accountManager =AccountManager.get(mContext);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    String authbase;
    try {
      Uri host = Uri.parse(prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, null));
      authbase= host==null ? null : ( host.getScheme()+"//"+host.getHost()+"/accounts/");
    } catch (NullPointerException|IllegalArgumentException e) {
      authbase = null;
    }
    mToken = getAuthToken(accountManager, authbase);
    if (mToken==null) { return null; }

    if (mHttpClient==null) { mHttpClient = new DefaultHttpClient(); }

    mHttpClient.getCookieStore().addCookie(new DarwinCookie(mToken));

    final HttpResponse result = mHttpClient.execute(pRequest);
    if (result.getStatusLine().getStatusCode()==HttpURLConnection.HTTP_UNAUTHORIZED) {
      if (! retry) { // Do not repeat retry
        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mToken);
        return execute(pRequest, true);
      }
    }
    return result;
  }

  private String getAuthToken(AccountManager accountManager, String authbase) {
    if (mToken != null) return mToken;

    Account account = ensureAccount(mContext, authbase);
    if (account==null) { mAskedForNewAccount = true; }



    AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

      @Override
      public void run(AccountManagerFuture<Bundle> pFuture) {
        try {
          Bundle b = pFuture.getResult();
          if (b.containsKey(AccountManager.KEY_INTENT)) {
            Intent i = (Intent) b.get(AccountManager.KEY_INTENT);
            mContext.startActivity(i);
          }
        } catch (OperationCanceledException e) {
          e.printStackTrace();
        } catch (AuthenticatorException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    AccountManagerFuture<Bundle> result;
    if (mContext instanceof Activity) {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, null, (Activity) mContext, callback , null);
    } else {
      result = accountManager.getAuthToken(account, ACCOUNT_TOKEN_TYPE, null, true, callback, null);
    }
    Bundle b;
    try {
//      return accountManager.blockingGetAuthToken(account, ACCOUNT_TOKEN_TYPE, false);
      b = result.getResult();
    } catch (OperationCanceledException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    } catch (AuthenticatorException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "Error logging in: ", e);
      return null;
    }
    return b.getString(AccountManager.KEY_AUTHTOKEN);

  }
  private static Account[] filterAuthBase(AccountManager am, Account[] pSrcAccounts, String pAuthbase) {
    Account[] tmp = new Account[pSrcAccounts.length];
    int i=0;
    for(Account candidate:pSrcAccounts) {
      String srcBase = am.getUserData(candidate, KEY_AUTH_BASE);
      if ((pAuthbase==null && srcBase==null) || (pAuthbase!=null && pAuthbase.equals(srcBase))) {
        tmp[i++]=candidate;
      }
    }

    Account[] dst;
    if (i==tmp.length) {
      dst = tmp;
    } else {
      dst = new Account[i];
      System.arraycopy(tmp, 0, dst, 0, i);
    }
    return dst;
  }
  void writeToBundle(Bundle pDest) {
    pDest.putBoolean(KEY_ASKED_FOR_NEW, mAskedForNewAccount);
  }

  void updateFromBundle(Bundle pSource) {
    if (pSource==null) return;
    mAskedForNewAccount = pSource.getBoolean(KEY_ASKED_FOR_NEW, false);
  }
  public static Account ensureAccount(Context pContext, String pSource) {
    AccountManager accountManager = AccountManager.get(pContext);
    Account[] accounts = filterAuthBase(accountManager, accountManager.getAccountsByType(ACCOUNT_TYPE), pSource);
    if (accounts.length==0) {
      final Bundle options;
      if (pSource==null) {
        options = null;
      } else {
        options = new Bundle(1);
        options.putString(KEY_AUTH_BASE, pSource);
      }
      Bundle result;
      try {
        result = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_TOKEN_TYPE, null, options, pContext instanceof Activity ? ((Activity) pContext) : null, null, null).getResult();
      } catch (OperationCanceledException | AuthenticatorException | IOException e) {
        return null;
      }
      if (result.containsKey(AccountManager.KEY_INTENT)) {
        pContext.startActivity(result.<Intent>getParcelable(AccountManager.KEY_INTENT));
      } else if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
        String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
        for(Account candidate: accountManager.getAccountsByType(ACCOUNT_TYPE)) {
          if (name.equals(candidate.name)) {
            String candBase = accountManager.getUserData(candidate, KEY_AUTH_BASE);
            if (pSource==null) {
              if (candBase==null) {
                return candidate;
              }
            } else if (pSource.equals(candBase)) {
              return candidate;
            }
          }
        }
      }
      return null;
    }
    Account account = accounts[0];
    return account;
  }

}
