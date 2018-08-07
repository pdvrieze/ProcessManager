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

package nl.adaptivity.sync;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import nl.adaptivity.xmlutil.XmlException;
import nl.adaptivity.xmlutil.XmlReader;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;


public interface ISimpleSyncDelegate {



//  void deleteOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException;

  /**
   * Hook for that should be used by subclasses to update item details. If it
   * has no details it can just return <code>true</code>.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient}
   * @param id The id within the local table of the item to update the details
   *          of.
   * @param pair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlReader)}.
   *
   * @return <code>true</code> on success.
   * @throws RemoteException
   * @throws OperationApplicationException
   * @throws IOException
   * @category Hooks
   */
  Collection<ContentProviderOperation> doUpdateItemDetails(DelegatingResources delegator, ContentProviderClient provider, long id, CVPair pair) throws RemoteException, OperationApplicationException, IOException;

  /**
   * Hook for that should be used by subclasses to delete an item on the server.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient}
   * @param itemuri The local content uri of the item.
   * @param syncResult The sync status.
   *
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider deleteItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to create an item on the server.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient}
   * @param itemuri The local content uri of the item.
   * @param syncresult The sync status.
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider createItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, SyncResult syncresult) throws RemoteException, IOException, XmlException;

  /**
   * Hook for that should be used by subclasses to update an item on the server.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient}
   * @param itemuri The local content uri of the item.
   * @param syncState The state of the item in the local database.
   * @param syncresult The sync status.
   *
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider updateItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, int syncState, SyncResult syncresult) throws RemoteException, IOException, XmlException;

  /**
   * Hook for that should be used by subclasses to resolve conflicts between the
   * server and the local database. This method does not need to update the
   * primary row in the database.
   *
   * @param provider The {@link ContentProviderClient}
   * @param uri The local content uri of the item.
   * @param item The details (if available) of the item. These are initially
   *          the values from the server, but if they are changed that will
   *          result in a server update to be triggered. In any case the local
   *          database will be updated with these values.
   * @return If <code>true</code>, the conflict has been resolved. If
   *         <code>false</code> this is not the case.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  boolean resolvePotentialConflict(ContentProviderClient provider, Uri uri, ContentValuesProvider item) throws RemoteException;

  /**
   * Hook to parse an item from XML. The function must consume the endTag
   * corresponding to the startTag that is the current position of the parser.
   *
   * @param in The parser that has been used. The parser is positioned at
   *          the first tag of the element.
   * @return The new values to be stored in the database for the object.
   * @throws XmlPullParserException
   * @throws IOException
   * @category Hooks
   */
  ContentValuesProvider parseItem(XmlReader in) throws IOException, XmlException;

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
   * @param base The base url for the entire synchronization.
   * @return The result.
   * @category Configuration
   */
  URI getListUrl(URI base);

  /**
   * Get the SELECT conditions needed to provide the list of items to sync (can be null if the entire list should be synchronized).
   * @return The conditions.
   */
  String getListSelection();

  /**
   * Get the arguments with the selection from {@link #getListSelection()}
   * @return
   */
  String[] getListSelectionArgs();

}
