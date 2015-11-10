package nl.adaptivity.sync;


import android.accounts.Account;
import android.content.*;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.util.List;

public abstract class DelegatingRemoteXmlSyncAdapter extends AbstractThreadedSyncAdapter implements RemoteXmlSyncAdapterDelegate.DelegatingResources {

  private enum Phases {
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(DelegatingResources delegator, ISyncAdapterDelegate delegate, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
        delegate.updateListFromServer(delegator, provider, syncResult);
      }},

    UPDATE_ITEM_DETAILS_FROM_SERVER {

      @Override
      public void execute(DelegatingResources delegator, ISyncAdapterDelegate delegate, ContentProviderClient provider, SyncResult syncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        delegate.updateItemDetails(delegator, provider, syncResult);
      }};

    public abstract void execute(DelegatingResources delegator, ISyncAdapterDelegate delegate, ContentProviderClient provider, SyncResult syncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException;
  }

  private static final String TAG = DelegatingRemoteXmlSyncAdapter.class.getSimpleName();

  private XmlPullParserFactory mXpf;
  private AuthenticatedWebClient mHttpClient;
  private List<? extends ISyncAdapterDelegate> mDelegates;

  public DelegatingRemoteXmlSyncAdapter(Context context, boolean autoInitialize, List<? extends ISyncAdapterDelegate> delegates) {
    super(context, autoInitialize);
    mDelegates = delegates;
  }

  public DelegatingRemoteXmlSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs, List<? extends ISyncAdapterDelegate> delegates) {
    super(context, autoInitialize, allowParallelSyncs);
    mDelegates = delegates;
  }

  protected void setDelegates(List<? extends ISyncAdapterDelegate> delegates) {
    mDelegates = delegates;
  }

  @Override
  public final void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    String mBase = getSyncSource();
    if (! mBase.endsWith("/")) {
      mBase = mBase +'/'; }

    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), account, authbase);
    }
    for(ISyncAdapterDelegate delegate: mDelegates) {
      for(Phases phase:Phases.values()) {
        try {
          phase.execute(this, delegate, provider, syncResult);
        } catch (IllegalStateException|XmlPullParserException e) {
          syncResult.stats.numParseExceptions++;
          Log.e(TAG, "Error parsing list", e);
        } catch (IOException e) {
          syncResult.stats.numIoExceptions++;
          Log.e(TAG, "Error contacting server", e);
        } catch (RemoteException|OperationApplicationException e) {
          syncResult.databaseError=true;
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