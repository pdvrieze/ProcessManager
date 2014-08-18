package nl.adaptivity.sync;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.xmlpull.v1.XmlPullParser.*;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;

@SuppressWarnings("boxing")
public abstract class RemoteXmlSyncAdapterDelegate {

  public static interface DelegatingResources {
    AuthenticatedWebClient getWebClient();
    String getSyncSource();
    XmlPullParser newPullParser() throws XmlPullParserException;
  }

  public static final int SYNC_PUBLISH_TO_SERVER = 6;
  public static final int SYNC_DELETE_ON_SERVER = 7;
  public static final int SYNC_UPDATE_SERVER = 1;
  public static final int SYNC_UPTODATE = 0;
  public static final int SYNC_PENDING = 2;
  public static final int SYNC_DETAILSPENDING = 3;
  public static final int SYNC_UPDATE_SERVER_PENDING = 4;
  public static final int SYNC_UPDATE_SERVER_DETAILSPENDING = 5;

  private static final String TAG = RemoteXmlSyncAdapterDelegate.class.getSimpleName();

  private final Uri mListContentUri;
  private List<CVPair> mUpdateList;

  public RemoteXmlSyncAdapterDelegate(Uri pListContentUri) {
    mListContentUri = pListContentUri.buildUpon().encodedFragment("nonetnotify").build();
  }

  /**
   * The code responsible for coordinating the publication of items to the server. Normally this does not need to be overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself, but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void publishItemsToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToCreate = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_PUBLISH_TO_SERVER, null, null);
    while(itemsToCreate.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToCreate.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = createItemOnServer(pDelegator, pProvider, itemuri, pSyncResult);
        ContentValues newValues  = newValuesProvider==null ? null : newValuesProvider.getContentValues();
        if (newValues==null) {
          newValues = new ContentValues(1);
          newValues.put(colSyncstate, SYNC_UPTODATE);
        } else if (!newValues.containsKey(colSyncstate)) {
          newValues.put(colSyncstate, SYNC_UPTODATE);
        }
        pProvider.update(itemuri, newValues, null, null);
        pSyncResult.stats.numUpdates++;
      } catch (IOException e) {
        pSyncResult.stats.numIoExceptions++;
      } catch (XmlPullParserException e) {
        pSyncResult.stats.numParseExceptions++;
      }
    }
  }


  /**
   * The code responsible for coordinating the deletion of items on the server. Normally this does not need to be overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself, but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void deleteOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToDelete = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_DELETE_ON_SERVER, null, null);
    while(itemsToDelete.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToDelete.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = deleteItemOnServer(pDelegator, pProvider, itemuri, pSyncResult);
        ContentValues newValues  = newValuesProvider==null ? null : newValuesProvider.getContentValues();
        if (newValues==null) {
          newValues = new ContentValues(1);
          newValues.put(colSyncstate, SYNC_PENDING);
        } else if (!newValues.containsKey(colSyncstate)) {
          newValues.put(colSyncstate, SYNC_PENDING);
        }
        pProvider.update(itemuri, newValues, colSyncstate+" = "+Integer.toString(SYNC_DELETE_ON_SERVER), null);
        pSyncResult.stats.numDeletes++;
      } catch (IOException e) {
        pSyncResult.stats.numIoExceptions++;
      } catch (XmlPullParserException e) {
        pSyncResult.stats.numParseExceptions++;
      }
    }
  }


  /**
   * The code responsible for updating the list of items from the server. This
   * code does not yet update the details. It only discovers which items were
   * added on the server and which where deleted. Normally this does not need to
   * be overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  final void updateListFromServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
    HttpGet getList = new HttpGet(getListUrl(pDelegator.getSyncSource()));
    HttpResponse result;
    try {
      result = pDelegator.getWebClient().execute(getList);
    } catch (IOException e) {
      pSyncResult.stats.numIoExceptions++;
      return;
    }
    if (result!=null) {
      final int statusCode = result.getStatusLine().getStatusCode();
      if (statusCode>=200 && statusCode<400) {
        mUpdateList = updateItemListFromServer(pDelegator, pProvider, pSyncResult, result.getEntity().getContent());
      } else {
        result.getEntity().consumeContent();
        mUpdateList = null;
        pSyncResult.stats.numIoExceptions++;
      }
    } else {
      pSyncResult.stats.numAuthExceptions++;
    }
  }

  /**
   * The code responsible for deleting local items that no longer exist on the
   * server. Normally this does not need to be overridden. This code depends on
   * the sync state in previous phases being set to non-pending for items that
   * are still present.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void deleteItemsMissingOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    int cnt = pProvider.delete(mListContentUri, colSyncstate + " = "+SYNC_PENDING+ " OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_PENDING, null);
    pSyncResult.stats.numDeletes+=cnt;
  }

  /**
   * The code responsible for sending local changes to the server. It will take
   * the server response and use it to update the local database.Normally this
   * does not need to be overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void sendLocalChangesToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    Cursor updateableItems = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_UPDATE_SERVER +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING, null, null);
    while(updateableItems.moveToNext()) {
      int syncState = updateableItems.getInt(1);
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, updateableItems.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = updateItemOnServer(pDelegator, pProvider, itemuri, syncState, pSyncResult);
        ContentValues newValues  = newValuesProvider.getContentValues();
        if (newValues==null) {
          newValues = new ContentValues(1);
          newValues.put(colSyncstate, syncState==SYNC_UPDATE_SERVER_DETAILSPENDING ? SYNC_DETAILSPENDING : SYNC_UPTODATE);
        } else if (!newValues.containsKey(colSyncstate)) {
          newValues.put(colSyncstate, syncState==SYNC_UPDATE_SERVER_DETAILSPENDING ? SYNC_DETAILSPENDING : SYNC_UPTODATE);
        }
        pProvider.update(itemuri, newValues, colSyncstate+" = "+SYNC_UPDATE_SERVER +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING, null);
        pSyncResult.stats.numUpdates++;
      } catch (IOException e) {
        pSyncResult.stats.numIoExceptions++;
      } catch (XmlPullParserException e) {
        pSyncResult.stats.numParseExceptions++;
      }
    }

  }

  /**
   * The code responsible for updating the details of items. This is is where
   * the values of the items can be set that were not set initially. To prevent
   * repeated server access the details provided through the return value of
   * {@link #parseItem(XmlPullParser)}. This function calls
   * {@link #doUpdateItemDetails(DelegatingResources, ContentProviderClient, long, CVPair)}
   * doUpdateItemDetails} which has access to the {@link ContentProviderClient},
   * therefore can update detail tables. Normally this does not need to be
   * overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */

  protected void updateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, IOException, OperationApplicationException {
    if (mUpdateList==null) {
      mUpdateList = Collections.emptyList();
    } else {
      Collections.sort(mUpdateList);
    }
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };
    Cursor updateableItems = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_DETAILSPENDING /* +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING*/, null, BaseColumns._ID);
    try {
      ListIterator<CVPair> listIterator = mUpdateList.listIterator();
      while(updateableItems.moveToNext()) {
        long id = updateableItems.getLong(0);
        CVPair pair=null;
        while (listIterator.hasNext()) {
          CVPair candidate = listIterator.next();
          if (candidate.mId==id) {
            pair = candidate;
            break;
          } else if (candidate.mId<id) {
            listIterator.previous(); // back up the position
            break;
          }
        }
        if(doUpdateItemDetails(pDelegator, pProvider, id, pair)) {
          pSyncResult.stats.numUpdates++;
        }
      }
    } finally {
      updateableItems.close();
    }
  }

  /**
   * The code responsible for actually parsing the server response, updating the
   * database and returning a list of states. Normally this does not need to be
   * overridden.
   * @param pDelegator TODO
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @param pContent The result of the server request.
   *
   * @return The list of results. These results will be provided in the
   *         {@link #doUpdateItemDetails(DelegatingResources, ContentProviderClient, long, CVPair)
   *         doUpdateItemDetails} function.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected List<CVPair> updateItemListFromServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, RemoteException, IOException, OperationApplicationException {
    final String itemNamespace = getItemNamespace();
    final String itemsTag = getItemsTag();

    final String colSyncstate = getSyncStateColumn();
    final String colKey = getKeyColumn();

    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    XmlPullParser parser = pDelegator.newPullParser();
    parser.setInput(pContent, "UTF8");

    ContentValues values = new ContentValues(1);
    if (colSyncstate!=null && colSyncstate.length()>0) {
      ArrayList<ContentProviderOperation> operations = new ArrayList<>(2);
      values.put(colSyncstate, Integer.valueOf(SYNC_PENDING));

      operations.add(ContentProviderOperation.newUpdate(mListContentUri)
          .withSelection(colSyncstate + " = "+SYNC_UPTODATE, null)
          .withValue(colSyncstate,  Integer.valueOf(SYNC_PENDING))
          .build());

      operations.add(ContentProviderOperation.newUpdate(mListContentUri)
          .withSelection(colSyncstate + " = "+SYNC_UPDATE_SERVER, null)
          .withValue(colSyncstate,  Integer.valueOf(SYNC_UPDATE_SERVER_PENDING))
          .build());

      pProvider.applyBatch(operations);
    }

    List<CVPair> result = new ArrayList<>();
    parser.next();
    parser.require(START_TAG, itemNamespace, itemsTag); // ensure the outer element
    int type;
    while ((type = parser.next()) != END_TAG) {
      switch (type) {
        case START_TAG:
          ContentValuesProvider item = parseItem(parser);
          final ContentValues itemCv = item.getContentValues();
          Object key = itemCv.get(colKey);
          if (key!=null) {
            if (!itemCv.containsKey(colSyncstate)) {
              itemCv.put(colSyncstate, SYNC_DETAILSPENDING);
            }
            Cursor localItem = pProvider.query(mListContentUri, projectionId, colKey+"= ?", new String[] {key.toString()}, null);
            if (localItem.moveToFirst()) {
              long id = localItem.getLong(0); // we know from the projection that _id is always the first column
              int syncState = localItem.getInt(1); // sync state is the second column
              localItem.close();

              final ArrayList<ContentProviderOperation> operations = new ArrayList<>();

              Uri uri = ContentUris.withAppendedId(mListContentUri, id);
              if (syncState==SYNC_UPDATE_SERVER_PENDING) {
                itemCv.remove(colSyncstate);
                ContentValues itemCpy = new ContentValues(itemCv);
                itemCpy.remove(colSyncstate);
                if (resolvePotentialConflict(pProvider, uri, item)) {
                  if (item.equals(itemCpy)) {
                    // no server update needed
                    itemCv.put(colSyncstate, SYNC_DETAILSPENDING);
                  } else {
                    itemCv.put(colSyncstate, SYNC_UPDATE_SERVER_DETAILSPENDING);
                  }
                } else {
                  pSyncResult.stats.numConflictDetectedExceptions++;
                  continue; // Handle next tag, don't continue with this item
                }
              }

              operations.add(ContentProviderOperation.newAssertQuery(uri)
                  .withValue(colSyncstate, Integer.valueOf(syncState))
                  .build());

              operations.add(ContentProviderOperation
                  .newUpdate(uri)
                  .withValues(itemCv)
                  .build());

              pProvider.applyBatch(operations);
              pSyncResult.stats.numUpdates++;
              result.add(new CVPair(id, item));
            } else {
              localItem.close();
              final long newId = ContentUris.parseId(pProvider.insert(mListContentUri, itemCv));
              result.add(new CVPair(newId, item));
              pSyncResult.stats.numInserts++;
            }
            pSyncResult.stats.numEntries++;
          } else {
            // skip items without key, we can't match them up between the client and server
            pSyncResult.stats.numSkippedEntries++;
          }
          break;
        default:
          throw new XmlPullParserException("Unexpected tag type: " + type);
      }
    }
    return result;
  }

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
  protected abstract boolean doUpdateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, OperationApplicationException, IOException;

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
  protected abstract ContentValuesProvider deleteItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException;

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
  protected abstract ContentValuesProvider createItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

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
  protected abstract ContentValuesProvider updateItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, int pSyncState, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

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
  protected abstract boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException;

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
  protected abstract ContentValuesProvider parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException;

  /**
   * Returns the column that is the key for items that is shared by both. This
   * allows better reconsiliation between items. A good use would be to store a
   * {@link UUID}.
   *
   * @return The column name, or null if there is not shared item key.
   * @category Configuration
   */
  protected abstract String getKeyColumn();

  /**
   * Returns the column that maintains the synchronization state.is the key for
   * items in the database. Normally this is {@link BaseColumns#_ID}.
   *
   * @return The column name.
   * @category Configuration
   */
  protected abstract String getSyncStateColumn();

  /**
   * Get the namespace for the parsing of the items.
   * @return
   * @category Configuration
   */
  protected abstract String getItemNamespace();

  /**
   * Get the outer tag that contains a list of items.
   * @return The tag name (without namespace)
   * @category Configuration
   */
  protected abstract String getItemsTag();

  /**
   * Get the server url from which to retrieve the item list.
   * @param pBase The base url for the entire synchronization.
   * @return The result.
   * @category Configuration
   */
  protected abstract String getListUrl(String pBase);

}