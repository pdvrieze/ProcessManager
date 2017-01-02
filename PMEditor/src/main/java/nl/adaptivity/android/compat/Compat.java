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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.compat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.*;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.IOException;


public final class Compat {

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static final class Compat14 {

    public static AccountManagerFuture<Bundle> getAuthToken(final AccountManager accountManager, final Account account, final String accountTokenType, final Bundle options,
                                                            final boolean notifyAuthFailure, final AccountManagerCallback<Bundle> callback, final Handler handler) {
      return accountManager.getAuthToken(account, accountTokenType, options, notifyAuthFailure, callback, handler);
    }

  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private static final class Compat17 {

    public static Fragment getParentFragment(final Fragment fragment) {
      return fragment.getParentFragment();
    }

  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static final class Compat19 {

    public static void closeWithError(final ParcelFileDescriptor pfd, final String error) {
      try {
        pfd.closeWithError(error);
      } catch (IOException e) {
        Log.e(Compat.class.getSimpleName(), error, e);
      }
    }

    public static File getDocsDirectory() {
      return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    }

  }

  public static void postInvalidateOnAnimation(final View view) {
    ViewCompat.postInvalidateOnAnimation(view);
  }

  public static boolean isZoomIn(final MotionEvent event) {
    if (Build.VERSION.SDK_INT>=12) {
      return Compat12.isZoomIn(event);
    }
    return false;
  }

  public static void closeWithError(final ParcelFileDescriptor pfd, final String error) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      Compat19.closeWithError(pfd, error);
    } else {
      try {
        pfd.close();
        Log.e(Compat.class.getSimpleName(), error, new IOException(error));
      } catch (IOException e) {
        Log.e(Compat.class.getSimpleName(), error, e);
      }
    }
  }

  public static Fragment getParentFragment(final Fragment fragment) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return Compat17.getParentFragment(fragment);
    } else {
      return null;
    }
  }

  public static File getDocsDirectory() {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      return Compat19.getDocsDirectory();
    } else {
      return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }
  }

  @SuppressWarnings("deprecation")
  public static AccountManagerFuture<Bundle> getAuthToken(final AccountManager accountManager, final Account account, final String accountTokenType, final Bundle options, final boolean notifyAuthFailure, final AccountManagerCallback<Bundle> callback, final Handler handler) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return Compat14.getAuthToken(accountManager, account, accountTokenType, options, notifyAuthFailure, callback, handler);
    } else {
      return accountManager.getAuthToken(account, accountTokenType, notifyAuthFailure, callback, handler);
    }
  }

}
