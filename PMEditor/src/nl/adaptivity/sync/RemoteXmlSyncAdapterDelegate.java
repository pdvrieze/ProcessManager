package nl.adaptivity.sync;


import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
public class RemoteXmlSyncAdapterDelegate implements ISyncAdapterDelegate {

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
  private ISimpleSyncDelegate mActualDelegate;

  public RemoteXmlSyncAdapterDelegate(Uri pListContentUri) {
    this(pListContentUri, null);
//    if (!(this instanceof ISimpleSyncDelegate)) { throw new IllegalArgumentException("You must implement ISimpleSyncDelegate"); }
  }

  public RemoteXmlSyncAdapterDelegate(Uri pListContentUri, ISimpleSyncDelegate pDelegate) {
    if (pDelegate ==null) {
      if (!(this instanceof ISimpleSyncDelegate)) { throw new IllegalArgumentException("You must implement ISimpleSyncDelegate"); }
      mActualDelegate = (ISimpleSyncDelegate) this;
    } else {
      mActualDelegate = pDelegate;
    }
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
  @Override
  public final void publishItemsToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToCreate = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_PUBLISH_TO_SERVER, null, null);
    while(itemsToCreate.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToCreate.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = mActualDelegate.createItemOnServer(pDelegator, pProvider, itemuri, pSyncResult);
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
  @Override
  public void deleteOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToDelete = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_DELETE_ON_SERVER, null, null);
    while(itemsToDelete.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToDelete.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = mActualDelegate.deleteItemOnServer(pDelegator, pProvider, itemuri, pSyncResult);
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
  @Override
  public
  final void updateListFromServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
    HttpGet getList = new HttpGet(mActualDelegate.getListUrl(pDelegator.getSyncSource()));
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
  @Override
  public final void deleteItemsMissingOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = mActualDelegate.getSyncStateColumn();
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
  @Override
  public final void sendLocalChangesToServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    Cursor updateableItems = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_UPDATE_SERVER +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING, null, null);
    while(updateableItems.moveToNext()) {
      int syncState = updateableItems.getInt(1);
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, updateableItems.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = mActualDelegate.updateItemOnServer(pDelegator, pProvider, itemuri, syncState, pSyncResult);
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

  @Override
  public final void updateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, IOException, OperationApplicationException {
    if (mUpdateList==null) {
      mUpdateList = Collections.emptyList();
    } else {
      Collections.sort(mUpdateList);
    }
    final String colSyncstate = mActualDelegate.getSyncStateColumn();
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
          } else if (candidate.mId>id) {
            listIterator.previous(); // back up the position
            break;
          }
        }
        if(mActualDelegate.doUpdateItemDetails(pDelegator, pProvider, id, pair)) {
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
  public final List<CVPair> updateItemListFromServer(DelegatingResources pDelegator, ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, RemoteException, IOException, OperationApplicationException {
    final String itemNamespace = mActualDelegate.getItemNamespace();
    final String itemsTag = mActualDelegate.getItemsTag();

    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String colKey = mActualDelegate.getKeyColumn();

    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    XmlPullParser parser = pDelegator.newPullParser();
    parser.setInput(pContent, "UTF8");

//    ContentValues values2 = new ContentValues(1);
    if (colSyncstate!=null && colSyncstate.length()>0) {
      ArrayList<ContentProviderOperation> operations = new ArrayList<>(2);
//      values2.put(colSyncstate, Integer.valueOf(SYNC_PENDING));

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
          ContentValuesProvider item = mActualDelegate.parseItem(parser);
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
                if (mActualDelegate.resolvePotentialConflict(pProvider, uri, item)) {
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

}