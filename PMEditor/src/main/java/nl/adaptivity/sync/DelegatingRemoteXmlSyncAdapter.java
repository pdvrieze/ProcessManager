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

package nl.adaptivity.sync;


import android.accounts.Account;
import android.content.*;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory;
import nl.adaptivity.android.darwinlib.BuildConfig;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public abstract class DelegatingRemoteXmlSyncAdapter extends AbstractThreadedSyncAdapter implements RemoteXmlSyncAdapterDelegate.DelegatingResources, LocalSyncAdapter {

  private enum Phases {
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(final DelegatingResources delegator, final ISyncAdapterDelegate delegate, final ContentProviderClient provider, final SyncResult syncResult) throws RemoteException, XmlException, IOException, OperationApplicationException {
        delegate.updateListFromServer(delegator, provider, syncResult);
      }},

    UPDATE_ITEM_DETAILS_FROM_SERVER {

      @Override
      public void execute(final DelegatingResources delegator, final ISyncAdapterDelegate delegate, final ContentProviderClient provider, final SyncResult syncResult)
          throws XmlException, IOException, RemoteException, OperationApplicationException {
        delegate.updateItemDetails(delegator, provider, syncResult);
      }};

    public abstract void execute(DelegatingResources delegator, ISyncAdapterDelegate delegate, ContentProviderClient provider, SyncResult syncResult) throws XmlException, IOException, RemoteException, OperationApplicationException;
  }

  private static final String TAG = DelegatingRemoteXmlSyncAdapter.class.getSimpleName();

  private XmlPullParserFactory mXpf;
  private AuthenticatedWebClient mHttpClient;
  private List<? extends ISyncAdapterDelegate> mDelegates;

  public DelegatingRemoteXmlSyncAdapter(final Context context, final boolean autoInitialize, final List<? extends ISyncAdapterDelegate> delegates) {
    super(context, autoInitialize);
    mDelegates = delegates;
  }

  public DelegatingRemoteXmlSyncAdapter(final Context context, final boolean autoInitialize, final boolean allowParallelSyncs, final List<? extends ISyncAdapterDelegate> delegates) {
    super(context, autoInitialize, allowParallelSyncs);
    mDelegates = delegates;
  }

  protected void setDelegates(final List<? extends ISyncAdapterDelegate> delegates) {
    mDelegates = delegates;
  }

  @Override
  public final void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider, final SyncResult syncResult) {
    if (!ProcessSyncManager.LOCALSYNC) {
      onPerformLocalSync(account, extras, authority, provider, syncResult);
    }
  }

  @Override
  public void onPerformLocalSync(final Account account, final Bundle bundle, final String authority, final ContentProviderClient provider, final SyncResult syncResult) {
    Trace.beginSection("SYNC-"+authority);
    try {
      URI mBase = getSyncSource();
      if (!mBase.toString().endsWith("/")) {
        if (BuildConfig.DEBUG) throw new AssertionError("Sync sources should be forced to end with / in all cases.");
        mBase = URI.create(mBase.toString() + '/');
      }

      if (account!=null){
        final URI authbase = AuthenticatedWebClientFactory.getAuthBase(mBase);
        mHttpClient = AuthenticatedWebClientFactory.newClient(getContext(), account, authbase);
      }
      for (final ISyncAdapterDelegate delegate : mDelegates) {
        for (final Phases phase : Phases.values()) {
          try {
          /*if (BuildConfig.DEBUG) { */
            Log.e(TAG, getClass().getSimpleName() + " STARTING phase " + phase); //}
            phase.execute(this, delegate, provider, syncResult);
          /*if (BuildConfig.DEBUG) { */
            Log.e(TAG, getClass().getSimpleName() + " FINISHED phase " + phase); //}
          } catch (IllegalStateException | XmlException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "Error parsing list", e);
          } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "Error contacting server", e);
          } catch (RemoteException | OperationApplicationException e) {
            syncResult.databaseError = true;
            Log.e(TAG, "Error updating database", e);
          } catch (Exception e) {
            syncResult.stats.numIoExceptions++; // Record as an IO exception
            Log.e(TAG, "An unknown error occurred synchronizing", e);
          }
        }
      }
    }  finally {
      Trace.endSection();
    }
  }

  /**
   * Get the parser factory member. It will create the factory if it didn't exist yet.
   * @return The factory.
   * @throws XmlPullParserException
   * @category Utils
   */
  protected XmlPullParserFactory getParserFactory() throws XmlPullParserException {
    if (mXpf==null) {
      mXpf = XmlPullParserFactory.newInstance();
      mXpf.setNamespaceAware(true);
    }
    return mXpf;
  }

  /**
   * Get the url that is the basis for this synchronization.
   * @return The base url.
   * @category Configuration
   */
  public abstract URI getSyncSource();

  @Override
  public AuthenticatedWebClient getWebClient() {
    return mHttpClient;
  }

  @Override
  public XmlPullParser newPullParser() throws XmlPullParserException {
    return getParserFactory().newPullParser();
 }

}