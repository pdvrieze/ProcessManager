package nl.adaptivity.sync;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.xmlpull.v1.XmlPullParser.*;

import net.devrieze.util.StringUtil;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import static nl.adaptivity.sync.RemoteXmlSyncAdapter.*;

@SuppressWarnings("boxing")
public class RemoteXmlSyncAdapterDelegate implements ISyncAdapterDelegate {

  public static interface DelegatingResources {
    AuthenticatedWebClient getWebClient();
    String getSyncSource();
    XmlPullParser newPullParser() throws XmlPullParserException;
  }

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
        result.getEntity().consumeContent();
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
    final List<CVPair> result = new ArrayList<>();
    final List<CVPair> pendingResults = new ArrayList<>();

    final String itemNamespace = mActualDelegate.getItemNamespace();
    final String itemsTag = mActualDelegate.getItemsTag();

    final String colSyncstate = mActualDelegate.getSyncStateColumn();
    final String colKey = mActualDelegate.getKeyColumn();

//    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    List<ContentValuesProvider> remoteItems = parseItems(pDelegator, pContent);

    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    Cursor localItems = pProvider.query(mListContentUri, null, mActualDelegate.getListSelection(), mActualDelegate.getListSelectionArgs(), null);
    try {
      int colIdxId = localItems.getColumnIndex(BaseColumns._ID);
      int colIdxKey = localItems.getColumnIndex(colKey);
      int colIdxSyncState = localItems.getColumnIndex(colSyncstate);
      while (localItems.moveToNext()) {
        final String localKey = localItems.getString(colIdxKey);
        final long localId = localItems.getLong(colIdxId);
        final int localSyncState = localItems.getInt(colIdxSyncState);
        Uri itemUri = ContentUris.withAppendedId(mListContentUri, localId);

        int remoteItemIdx = getItemWithKeyIdx(remoteItems, colKey, localKey);

        if (remoteItemIdx>=0) {
          ContentValuesProvider remoteItem = remoteItems.get(remoteItemIdx);
          if (localSyncState==SYNC_UPTODATE || localSyncState==SYNC_PUBLISH_TO_SERVER) { // the item magically appeared on the server
            if (isChanged(mActualDelegate, localItems, remoteItem) || hasDetails(remoteItem)) {
              operations.add(ContentProviderOperation
                  .newUpdate(itemUri)
                  .withValues(remoteItem.getContentValues())
                  .build());
              result.add(new CVPair(localId, remoteItem));
              ++pSyncResult.stats.numUpdates;
            }
          } else if (localSyncState==SYNC_UPDATE_SERVER) {
            ContentValues itemCv = remoteItem.getContentValues();
            itemCv.remove(colSyncstate);
            ContentValues itemCpy = new ContentValues(itemCv);
            itemCpy.remove(colSyncstate);
            ContentValuesProvider newValues = null;
            try { // Don't jinx the entire sync when only the single update fails
              newValues = mActualDelegate.updateItemOnServer(pDelegator, pProvider, itemUri, localSyncState, pSyncResult);
            } catch (IOException | RemoteException | XmlPullParserException e) {
              Log.w(TAG, "Error updating the server", e);
              if (mActualDelegate.resolvePotentialConflict(pProvider, itemUri, remoteItem)) {
                if (! remoteItem.getContentValues().containsKey(colSyncstate)) {
                  if (remoteItem.getContentValues().equals(itemCpy)) {
                    remoteItem.getContentValues().put(colSyncstate, SYNC_DETAILSPENDING);
                  } else {
                    remoteItem.getContentValues().put(colSyncstate, SYNC_UPDATE_SERVER);
                  }
                }
              } else {
                ++pSyncResult.stats.numConflictDetectedExceptions;
              }
              newValues = remoteItem;
            }
            if (newValues!=null) {
              if (! newValues.getContentValues().containsKey(colSyncstate)) {
                newValues.getContentValues().put(colSyncstate, SYNC_DETAILSPENDING);
              }
              operations.add(ContentProviderOperation
                  .newUpdate(itemUri)
                  .withValues(newValues.getContentValues())
                  .build());
              ++pSyncResult.stats.numUpdates;
              result.add(new CVPair(localId, newValues));
            }

          }
          remoteItems.remove(remoteItemIdx);
        } else { // no matching remote item
          if (localSyncState==SYNC_PUBLISH_TO_SERVER) {
            ContentValuesProvider newLocalValues = mActualDelegate.createItemOnServer(pDelegator, pProvider, itemUri, pSyncResult);
            operations.add(ContentProviderOperation
                .newUpdate(itemUri)
                .withValues(newLocalValues.getContentValues())
                .build());
            ++pSyncResult.stats.numInserts;
            result.add(new CVPair(localId, newLocalValues)); // These need to be resolved from the batch operation
          } else if (localSyncState==SYNC_UPTODATE) {
            operations.add(ContentProviderOperation.newDelete(itemUri).build());
            ++pSyncResult.stats.numDeletes;
          } else {
            ++pSyncResult.stats.numSkippedEntries;
          }
        }
        ++pSyncResult.stats.numEntries;
      }
    } finally {
      localItems.close();
    }

    for(ContentValuesProvider remoteItem: remoteItems) {
      // These are new items.
      final ContentValues itemCv = remoteItem.getContentValues();
      if (!itemCv.containsKey(colSyncstate)) {
        itemCv.put(colSyncstate, SYNC_DETAILSPENDING);
      }
      operations.add(ContentProviderOperation
          .newInsert(mListContentUri)
          .withValues(itemCv)
          .build());
      if (itemCv.getAsInteger(colSyncstate)!=SYNC_UPTODATE) {
        pendingResults.add(new CVPair(operations.size()-1, remoteItem));
      }
      ++pSyncResult.stats.numInserts;
    }
    ContentProviderResult[] batchResult = pProvider.applyBatch(operations);
    for(CVPair pendingResult:pendingResults) {
      ContentProviderResult cvpr = batchResult[(int)pendingResult.mId];
      long newId = ContentUris.parseId(cvpr.uri);
      result.add(new CVPair(newId, pendingResult.mCV));
    }
    return result;
  }

  private boolean hasDetails(ContentValuesProvider pRemoteItem) {
    Integer syncstate = pRemoteItem.getContentValues().getAsInteger(mActualDelegate.getSyncStateColumn());
    if (syncstate==null) {
      return true; // we don't know, assume details
    }
    int s = syncstate.intValue();
    return (s==SYNC_DETAILSPENDING|| s==SYNC_UPDATE_SERVER_DETAILSPENDING);
  }

  private boolean isChanged(ISimpleSyncDelegate pActualDelegate, Cursor pLocalItems, ContentValuesProvider pRemoteItem) {
    for(Entry<String, Object> remotePair: pRemoteItem.getContentValues().valueSet()) {
      String remoteKey = remotePair.getKey();
      if (!pActualDelegate.getSyncStateColumn().equals(remoteKey)) {
        int localColIdx = pLocalItems.getColumnIndex(remoteKey);
        if (localColIdx>=0) {
          Object remoteValue = remotePair.getValue();
          if (remoteValue==null) {
            if (! pLocalItems.isNull(localColIdx)) { return true; }
          } else {
            Object localValue = getLocalValue(pLocalItems, localColIdx, remoteValue.getClass());
            if (!remoteValue.equals(localValue)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static Object getLocalValue(Cursor pLocalItems, int pLocalColIdx, Class<? extends Object> pClass) {
    if (pClass==Integer.class) {
      return pLocalItems.getInt(pLocalColIdx);
    } else if (pClass==Long.class) {
      return pLocalItems.getLong(pLocalColIdx);
    } else if (pClass==String.class) {
      return pLocalItems.getString(pLocalColIdx);
    } else if (pClass==Double.class) {
      return pLocalItems.getDouble(pLocalColIdx);
    } else if (pClass==Float.class) {
      return pLocalItems.getFloat(pLocalColIdx);
    } else if (pClass==Short.class) {
      return pLocalItems.getShort(pLocalColIdx);
    }
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private int getItemWithKeyIdx(List<ContentValuesProvider> pItems, String pKeyColumn, String pKey) {
    final int l = pItems.size();
    for(int i=0; i<l;++i) {
      if (StringUtil.isEqual(pItems.get(i).getContentValues().getAsString(pKeyColumn), pKey)){
        return i;
      }
    }
    return -1;
  }

  protected List<ContentValuesProvider> parseItems(DelegatingResources pDelegator, InputStream pContent) throws XmlPullParserException, IOException {
    XmlPullParser parser = pDelegator.newPullParser();
    parser.setInput(pContent, "UTF8");
    List<ContentValuesProvider> items = new ArrayList<>();
    int type;
    parser.nextTag();
    parser.require(XmlPullParser.START_TAG, mActualDelegate.getItemNamespace(), mActualDelegate.getItemsTag());
    while ((type = parser.next()) != END_TAG) {
      switch (type) {
        case START_TAG:
          items.add(mActualDelegate.parseItem(parser));
          break;
        default:
          throw new XmlPullParserException("Unexpected tag type: " + type);
      }
    }
    parser.require(XmlPullParser.END_TAG, mActualDelegate.getItemNamespace(), mActualDelegate.getItemsTag());
    return items;
  }

}