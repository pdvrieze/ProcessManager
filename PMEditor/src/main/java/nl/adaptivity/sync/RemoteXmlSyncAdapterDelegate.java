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


import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.GetRequest;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import static nl.adaptivity.sync.RemoteXmlSyncAdapter.*;


@SuppressWarnings("boxing")
public class RemoteXmlSyncAdapterDelegate implements ISyncAdapterDelegate {

  public interface DelegatingResources {
    Context getContext();
    AuthenticatedWebClient getWebClient();
    URI getSyncSource();
    XmlPullParser newPullParser() throws XmlPullParserException;
  }

  private static final String TAG = RemoteXmlSyncAdapterDelegate.class.getSimpleName();

  private final Uri mListContentUri;
  private List<CVPair> mUpdateList;
  private ISimpleSyncDelegate mActualDelegate;

  public RemoteXmlSyncAdapterDelegate(final Uri listContentUri) {
    this(listContentUri, null);
//    if (!(this instanceof ISimpleSyncDelegate)) { throw new IllegalArgumentException("You must implement ISimpleSyncDelegate"); }
  }

  public RemoteXmlSyncAdapterDelegate(final Uri listContentUri, final ISimpleSyncDelegate delegate) {
    if (delegate ==null) {
      if (!(this instanceof ISimpleSyncDelegate)) { throw new IllegalArgumentException("You must implement ISimpleSyncDelegate"); }
      mActualDelegate = (ISimpleSyncDelegate) this;
    } else {
      mActualDelegate = delegate;
    }
    mListContentUri = listContentUri.buildUpon().encodedFragment("nonetnotify").build();
  }

  /**
   * The code responsible for updating the list of items from the server. This
   * code does not yet update the details. It only discovers which items were
   * added on the server and which where deleted. Normally this does not need to
   * be overridden.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient provider} used.
   * @param syncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  @Override
  public
  final void updateListFromServer(final DelegatingResources delegator, final ContentProviderClient provider, final SyncResult syncResult) throws RemoteException, XmlException, IOException, OperationApplicationException {
    final GetRequest        getList = new GetRequest(mActualDelegate.getListUrl(delegator.getSyncSource()));
    final HttpURLConnection result;
    try {
      result = delegator.getWebClient().execute(getList);
    } catch (IOException e) {
      syncResult.stats.numIoExceptions++;
      return;
    }
    if (result != null) {
      try {
        final int statusCode = result.getResponseCode();
        if (statusCode >= 200 && statusCode < 400) {
          final InputStream content = result.getInputStream();
          try {
            mUpdateList = updateItemListFromServer(delegator, provider, syncResult, content);
          } finally {
            content.close();
          }
        } else {
          result.getErrorStream().skip(Integer.MAX_VALUE);
          Log.e(TAG, "failure to get list from server: "+statusCode+" "+result.getResponseMessage()+"\n  url:"+getList.getUri());
          mUpdateList = null;
          syncResult.stats.numIoExceptions++;
        }
      } finally {
        result.disconnect();
      }
    } else {
      syncResult.stats.numAuthExceptions++;
    }
  }

  /**
   * The code responsible for updating the details of items. This is is where
   * the values of the items can be set that were not set initially. To prevent
   * repeated server access the details provided through the return value of
   * {@link #parseItems(DelegatingResources, InputStream)}. This function calls
   * {@link ISimpleSyncDelegate#doUpdateItemDetails(DelegatingResources, ContentProviderClient, long, CVPair)}
   * doUpdateItemDetails} which has access to the {@link ContentProviderClient},
   * therefore can update detail tables. Normally this does not need to be
   * overridden.
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient provider} used.
   * @param syncResult The {@link SyncResult} used for sync statistics.
   *
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */

  @Override
  public final void updateItemDetails(final DelegatingResources delegator, final ContentProviderClient provider, final SyncResult syncResult) throws RemoteException, IOException, OperationApplicationException {
    if (mUpdateList==null) {
      mUpdateList = Collections.emptyList();
    } else {
      Collections.sort(mUpdateList);
    }
    final String   colSyncstate    = mActualDelegate.getSyncStateColumn();
    final String[] projectionId    = new String[] { BaseColumns._ID, colSyncstate };
    final Cursor   updateableItems = provider.query(mListContentUri, projectionId, colSyncstate + " = " + SYNC_NEWDETAILSPENDING + " OR " + colSyncstate + " = " + SYNC_DETAILUPDATEPENDING/* +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING*/, null, BaseColumns._ID);
    try {
      final ListIterator<CVPair> listIterator = mUpdateList.listIterator();
      while(updateableItems.moveToNext()) {
        final long id   = updateableItems.getLong(0);
        CVPair     pair =null;
        while (listIterator.hasNext()) {
          final CVPair candidate = listIterator.next();
          if (candidate.mId==id) {
            pair = candidate;
            break;
          } else if (candidate.mId>id) {
            listIterator.previous(); // back up the position
            break;
          }
        }
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.addAll(mActualDelegate.doUpdateItemDetails(delegator, provider, id, pair));
        final ContentProviderResult[] applyResult = provider.applyBatch(operations);
        for(final ContentProviderResult result: applyResult) {
          syncResult.stats.numUpdates+=result.count==null ? 0 : result.count;
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
   * @param delegator TODO
   * @param provider The {@link ContentProviderClient provider} used.
   * @param syncResult The {@link SyncResult} used for sync statistics.
   * @param content The result of the server request.
   *
   * @return The list of results. These results will be provided in the
   *         {@link ISimpleSyncDelegate#doUpdateItemDetails(DelegatingResources, ContentProviderClient, long, CVPair)
   *         doUpdateItemDetails} function.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  public final List<CVPair> updateItemListFromServer(final DelegatingResources delegator, final ContentProviderClient provider, final SyncResult syncResult, final InputStream content) throws XmlException, RemoteException, IOException, OperationApplicationException {
    final List<CVPair> result = new ArrayList<>();
    final List<CVPair> pendingResults = new ArrayList<>();

    final String itemNamespace = mActualDelegate.getItemNamespace();
    final String itemsTag = mActualDelegate.getItemsTag();

    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String colKey = mActualDelegate.getKeyColumn();

//    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    final List<ContentValuesProvider> remoteItems = parseItems(delegator, content);

    // PROCESSING LOCAL ITEMS
    final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    final Cursor                              localItems = provider.query(mListContentUri, null, mActualDelegate.getListSelection(), mActualDelegate.getListSelectionArgs(), null);
    try {
      final int colIdxId        = localItems.getColumnIndex(BaseColumns._ID);
      final int colIdxKey       = localItems.getColumnIndex(colKey);
      final int colIdxSyncState = localItems.getColumnIndex(colSyncstate);
      while (localItems.moveToNext()) {
        final String localKey       = localItems.getString(colIdxKey);
        final long   localId        = localItems.getLong(colIdxId);
        final int    localSyncState = localItems.getInt(colIdxSyncState);
        final Uri    itemUri        = ContentUris.withAppendedId(mListContentUri, localId);

        final int remoteItemIdx = getItemWithKeyIdx(remoteItems, colKey, localKey);

        if (remoteItemIdx>=0) {
          final ContentValuesProvider remoteItem = remoteItems.get(remoteItemIdx);
          if (localSyncState==SYNC_UPTODATE || localSyncState==SYNC_PUBLISH_TO_SERVER) { // the item magically appeared on the server

            if (isChanged(mActualDelegate, localItems, remoteItem)) {
              if (remoteItem.syncDetails()) {
                remoteItem.getContentValues().put(colSyncstate, SYNC_DETAILUPDATEPENDING);
                result.add(new CVPair(localId, remoteItem));
              }
              operations.add(ContentProviderOperation
                                     .newUpdate(itemUri)
                                     .withValues(remoteItem.getContentValues())
                                     .build());
              ++syncResult.stats.numUpdates;
            } else if (remoteItem.syncDetails()) { // only details need update.
              operations.add(ContentProviderOperation
                                     .newUpdate(itemUri)
                                     .withValue(colSyncstate, SYNC_DETAILUPDATEPENDING)
                                     .build());
              result.add(new CVPair(localId, remoteItem));
            }
          } else if (localSyncState==SYNC_UPDATE_SERVER) {
            final ContentValues itemCv = remoteItem.getContentValues();
            itemCv.remove(colSyncstate);
            final ContentValues itemCpy = new ContentValues(itemCv);
            itemCpy.remove(colSyncstate);
            ContentValuesProvider newValues = null;
            try { // Don't jinx the entire sync when only the single update fails
              newValues = mActualDelegate.updateItemOnServer(delegator, provider, itemUri, localSyncState, syncResult);
            } catch (IOException | RemoteException e) {
              Log.w(TAG, "Error updating the server", e);
              if (mActualDelegate.resolvePotentialConflict(provider, itemUri, remoteItem)) {
                if (! remoteItem.getContentValues().containsKey(colSyncstate)) {
                  if (remoteItem.getContentValues().equals(itemCpy)) {
                    remoteItem.getContentValues().put(colSyncstate, SYNC_DETAILUPDATEPENDING);
                  } else {
                    remoteItem.getContentValues().put(colSyncstate, SYNC_UPDATE_SERVER);
                  }
                }
              } else {
                ++syncResult.stats.numConflictDetectedExceptions;
              }
              newValues = remoteItem;
            }
            if (newValues!=null) {
              if (! newValues.getContentValues().containsKey(colSyncstate)) {
                newValues.getContentValues().put(colSyncstate, SYNC_DETAILUPDATEPENDING);
              }
              operations.add(ContentProviderOperation
                  .newUpdate(itemUri)
                  .withValues(newValues.getContentValues())
                  .build());
              ++syncResult.stats.numUpdates;
              result.add(new CVPair(localId, newValues));
            }

          }
          remoteItems.remove(remoteItemIdx);
        } else { // no matching remote item
          if (localSyncState==SYNC_PUBLISH_TO_SERVER) {
            final ContentValuesProvider newLocalValues = mActualDelegate.createItemOnServer(delegator, provider, itemUri, syncResult);
            operations.add(ContentProviderOperation
                .newUpdate(itemUri)
                .withValues(newLocalValues.getContentValues())
                .build());
            ++syncResult.stats.numInserts;
            result.add(new CVPair(localId, newLocalValues)); // These need to be resolved from the batch operation
          } else if (localSyncState==SYNC_UPTODATE) {
            operations.add(ContentProviderOperation.newDelete(itemUri).build());
            ++syncResult.stats.numDeletes;
          } else {
            ++syncResult.stats.numSkippedEntries;
          }
        }
        ++syncResult.stats.numEntries;
      }
    } finally {
      localItems.close();
    }

    for(final ContentValuesProvider remoteItem: remoteItems) {
      // These are new items.
      final ContentValues itemCv = remoteItem.getContentValues();
      if (remoteItem.syncDetails()) {
        itemCv.put(colSyncstate, SYNC_NEWDETAILSPENDING);
      } else if (!itemCv.containsKey(colSyncstate)) {
        itemCv.put(colSyncstate, SYNC_UPTODATE);
      }
      operations.add(ContentProviderOperation
          .newInsert(mListContentUri)
          .withValues(itemCv)
          .build());
      if (itemCv.getAsInteger(colSyncstate)!=SYNC_UPTODATE) {
        pendingResults.add(new CVPair(operations.size()-1, remoteItem));
      }
      ++syncResult.stats.numInserts;
    }
    final ContentProviderResult[] batchResult = provider.applyBatch(operations);
    for(final CVPair pendingResult:pendingResults) {
      final ContentProviderResult cvpr  = batchResult[(int)pendingResult.mId];
      final long                  newId = ContentUris.parseId(cvpr.uri);
      result.add(new CVPair(newId, pendingResult.mCV));
    }
    return result;
  }

  private boolean isChanged(final ISimpleSyncDelegate actualDelegate, final Cursor localItems, final ContentValuesProvider remoteItem) {
    for(final Entry<String, Object> remotePair: remoteItem.getContentValues().valueSet()) {
      final String remoteKey = remotePair.getKey();
      if (!actualDelegate.getSyncStateColumn().equals(remoteKey)) {
        final int localColIdx = localItems.getColumnIndex(remoteKey);
        if (localColIdx>=0) {
          final Object remoteValue = remotePair.getValue();
          if (remoteValue==null) {
            if (! localItems.isNull(localColIdx)) { return true; }
          } else {
            final Object localValue = getLocalValue(localItems, localColIdx, remoteValue.getClass());
            if (!remoteValue.equals(localValue)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static Object getLocalValue(final Cursor localItems, final int localColIdx, final Class<?> clazz) {
    if (clazz==Integer.class) {
      return localItems.getInt(localColIdx);
    } else if (clazz==Long.class) {
      return localItems.getLong(localColIdx);
    } else if (clazz==String.class) {
      return localItems.getString(localColIdx);
    } else if (clazz==Double.class) {
      return localItems.getDouble(localColIdx);
    } else if (clazz==Float.class) {
      return localItems.getFloat(localColIdx);
    } else if (clazz==Short.class) {
      return localItems.getShort(localColIdx);
    }
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private int getItemWithKeyIdx(final List<ContentValuesProvider> items, final String keyColumn, final String key) {
    final int l = items.size();
    for(int i=0; i<l;++i) {
      if (StringUtil.isEqual(items.get(i).getContentValues().getAsString(keyColumn), key)){
        return i;
      }
    }
    return -1;
  }

  protected List<ContentValuesProvider> parseItems(final DelegatingResources delegator, final InputStream content) throws XmlException, IOException {
    final String                      colSyncstate = mActualDelegate.getSyncStateColumn();
    final XmlReader                   parser       = XmlStreaming.newReader(content, "UTF8");
    final List<ContentValuesProvider> items        = new ArrayList<>();
    EventType                         type;
    parser.nextTag();
    parser.require(EventType.START_ELEMENT, mActualDelegate.getItemNamespace(), mActualDelegate.getItemsTag());
    while ((type = parser.next()) != EventType.END_ELEMENT) {
      switch (type) {
        case START_ELEMENT:
          final ContentValuesProvider item = mActualDelegate.parseItem(parser);
          items.add(item);
          break;
        default:
          throw new XmlException("Unexpected tag type: " + type);
      }
    }
    parser.require(EventType.END_ELEMENT, mActualDelegate.getItemNamespace(), mActualDelegate.getItemsTag());
    return items;
  }

}