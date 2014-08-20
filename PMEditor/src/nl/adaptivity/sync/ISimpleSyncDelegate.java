package nl.adaptivity.sync;

import java.io.IOException;
import java.util.UUID;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;


public interface ISimpleSyncDelegate {



//  void deleteOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException;

  /**
   * Hook for that should be used by subclasses to update item details. If it
   * has no details it can just return <code>true</code>.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient}
   * @param pId The id within the local table of the item to update the details
   *          of.
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   *
   * @return <code>true</code> on success.
   * @throws RemoteException
   * @throws OperationApplicationException
   * @throws IOException
   * @category Hooks
   */
  boolean doUpdateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, OperationApplicationException, IOException;

  /**
   * Hook for that should be used by subclasses to delete an item on the server.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pSyncResult The sync status.
   *
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider deleteItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to create an item on the server.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pSyncState TODO
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   * @param pSyncResult The sync status.
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider createItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to update an item on the server.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pSyncState The state of the item in the local database.
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   * @param pSyncResult The sync status.
   *
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider updateItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, int pSyncState, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to resolve conflicts between the
   * server and the local database. This method does not need to update the
   * primary row in the database.
   *
   * @param pHttpClient The {@link AuthenticatedWebClient} to use.
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pItem The details (if available) of the item. These are initially
   *          the values from the server, but if they are changed that will
   *          result in a server update to be triggered. In any case the local
   *          database will be updated with these values.
   * @param pSyncResult The sync status.
   * @return If <code>true</code>, the conflict has been resolved. If
   *         <code>false</code> this is not the case.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException;

  /**
   * Hook to parse an item from XML. The function must consume the endTag
   * corresponding to the startTag that is the current position of the parser.
   *
   * @param pParser The parser that has been used. The parser is positioned at
   *          the first tag of the element.
   * @return The new values to be stored in the database for the object.
   * @throws XmlPullParserException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException;

  /**
   * Returns the column that is the key for items that is shared by both. This
   * allows better reconsiliation between items. A good use would be to store a
   * {@link UUID}.
   *
   * @return The column name, or null if there is not shared item key.
   * @category Configuration
   */
  String getKeyColumn();

  /**
   * Returns the column that maintains the synchronization state.is the key for
   * items in the database. Normally this is {@link BaseColumns#_ID}.
   *
   * @return The column name.
   * @category Configuration
   */
  String getSyncStateColumn();

  /**
   * Get the namespace for the parsing of the items.
   * @return
   * @category Configuration
   */
  String getItemNamespace();

  /**
   * Get the outer tag that contains a list of items.
   * @return The tag name (without namespace)
   * @category Configuration
   */
  String getItemsTag();

  /**
   * Get the server url from which to retrieve the item list.
   * @param pBase The base url for the entire synchronization.
   * @return The result.
   * @category Configuration
   */
  String getListUrl(String pBase);

}
