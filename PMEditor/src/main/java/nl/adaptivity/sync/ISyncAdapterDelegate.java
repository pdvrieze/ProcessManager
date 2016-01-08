package nl.adaptivity.sync;

import java.io.IOException;

import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;


public interface ISyncAdapterDelegate {

  public void updateItemDetails(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, IOException,
      OperationApplicationException;

  public void updateListFromServer(DelegatingResources delegator, ContentProviderClient provider, SyncResult syncResult) throws RemoteException, XmlPullParserException,
      IOException, OperationApplicationException;

}
