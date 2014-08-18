package nl.adaptivity.sync;


import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public abstract class DelegatingRemoteXmlSyncAdapter extends AbstractThreadedSyncAdapter implements RemoteXmlSyncAdapterDelegate.DelegatingResources {

  private enum Phases {
    DELETE_ON_SERVER {

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.deleteOnServer(pDelegator, pProvider, pSyncResult);
      }

    },
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
        pAdapter.updateListFromServer(pDelegator, pProvider, pSyncResult);
      }},

    PUBLISH_ITEMS_TO_SERVER{

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.publishItemsToServer(pDelegator, pProvider, pSyncResult);
      }

    }, DELETE_ITEMS_MISSING_ON_SERVER {

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.deleteItemsMissingOnServer(pDelegator, pProvider, pSyncResult);

      }

    },

    SEND_LOCAL_CHANGES_TO_SERVER {

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.sendLocalChangesToServer(pDelegator, pProvider, pSyncResult);
      }},

    UPDATE_ITEMS_FROM_SERVER {

      @Override
      public void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.updateItemDetails(pDelegator, pProvider, pSyncResult);
      }};

    public abstract void execute(DelegatingResources pDelegator, RemoteXmlSyncAdapterDelegate pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException;
  }

  private static final String TAG = DelegatingRemoteXmlSyncAdapter.class.getSimpleName();

  private XmlPullParserFactory mXpf;
  private String mBase;
  private AuthenticatedWebClient mHttpClient;
  private List<RemoteXmlSyncAdapterDelegate> mDelegates;

  public DelegatingRemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, List<RemoteXmlSyncAdapterDelegate> pDelegates) {
    super(pContext, pAutoInitialize);
    mDelegates = pDelegates;
  }

  public DelegatingRemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, boolean pAllowParallelSyncs, List<RemoteXmlSyncAdapterDelegate> pDelegates) {
    super(pContext, pAutoInitialize, pAllowParallelSyncs);
    mDelegates = pDelegates;
  }

  @Override
  public final void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    mBase = getSyncSource();
    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }

    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), pAccount, authbase);
    }
    for(RemoteXmlSyncAdapterDelegate delegate: mDelegates) {
      for(Phases phase:Phases.values()) {
        try {
          phase.execute(this, delegate, pProvider, pSyncResult);
        } catch (IllegalStateException|XmlPullParserException e) {
          pSyncResult.stats.numParseExceptions++;
          Log.e(TAG, "Error parsing list", e);
        } catch (IOException e) {
          pSyncResult.stats.numIoExceptions++;
          Log.e(TAG, "Error contacting server", e);
        } catch (RemoteException|OperationApplicationException e) {
          pSyncResult.databaseError=true;
          Log.e(TAG, "Error updating database", e);
        }
      }
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
  public abstract String getSyncSource();

  @Override
  public AuthenticatedWebClient getWebClient() {
    return mHttpClient;
  }

  @Override
  public XmlPullParser newPullParser() throws XmlPullParserException {
    return getParserFactory().newPullParser();
 }

}