package nl.adaptivity.sync;

import java.io.IOException;

import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;


public interface ISyncAdapterDelegate {

  public void updateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, IOException,
      OperationApplicationException;

  public void sendLocalChangesToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException,
      OperationApplicationException;

  public void deleteItemsMissingOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException,
      OperationApplicationException;

  public void deleteOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException;

  public void publishItemsToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException;

  public void updateListFromServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException,
      IOException, OperationApplicationException;

}
