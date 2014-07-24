package nl.adaptivity.sync;


import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;

@SuppressWarnings("boxing")
public abstract class RemoteXmlSyncAdapter extends AbstractThreadedSyncAdapter {


  public interface ContentValuesProvider {
    public ContentValues getContentValues();
  }

  public static class SimpleContentValuesProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;

    public SimpleContentValuesProvider(ContentValues pContentValues) {
      mContentValues = pContentValues;
    }

    @Override
    public ContentValues getContentValues() {
      return mContentValues;
    }

  }

  public static class CVPair implements Comparable<CVPair> {
    public final ContentValuesProvider mCV;
    public final long mId;

    public CVPair(long pId, ContentValuesProvider pCV) {
      mCV = pCV;
      mId = pId;
    }

    @Override
    public int compareTo(CVPair pAnother) {
      long rhs = pAnother.mId;
      return mId < rhs ? -1 : (mId == rhs ? 0 : 1);
    }

  }

  public static final int SYNC_PUBLISH_TO_SERVER = 6;
  public static final int SYNC_DELETE_ON_SERVER = 7;
  public static final int SYNC_UPDATE_SERVER = 1;
  public static final int SYNC_UPTODATE = 0;
  public static final int SYNC_PENDING = 2;
  public static final int SYNC_DETAILSPENDING = 3;
  public static final int SYNC_UPDATE_SERVER_PENDING = 4;
  public static final int SYNC_UPDATE_SERVER_DETAILSPENDING = 5;

  private enum Phases {
    DELETE_ON_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.deleteOnServer(pProvider, pSyncResult);
      }

    },
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
        pAdapter.updateListFromServer(pProvider, pSyncResult);
      }},

    PUBLISH_ITEMS_TO_SERVER{

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.publishItemsToServer(pProvider, pSyncResult);
      }

    }, DELETE_ITEMS_MISSING_ON_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.deleteItemsMissingOnServer(pProvider, pSyncResult);

      }

    },

    SEND_LOCAL_CHANGES_TO_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.sendLocalChangesToServer(pProvider, pSyncResult);
      }},

    UPDATE_ITEMS_FROM_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException, OperationApplicationException {
        pAdapter.updateItemDetails(pProvider, pSyncResult);
      }};

    public abstract void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException, OperationApplicationException;
  }

  private static final String TAG = RemoteXmlSyncAdapter.class.getSimpleName();

  private XmlPullParserFactory mXpf;
  private String mBase;
  private AuthenticatedWebClient mHttpClient;
  private final Uri mListContentUri;
  private List<CVPair> mUpdateList;

  public RemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, Uri pListContentUri) {
    super(pContext, pAutoInitialize);
    mListContentUri = pListContentUri;
  }

  public RemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, boolean pAllowParallelSyncs, Uri pListContentUri) {
    super(pContext, pAutoInitialize, pAllowParallelSyncs);
    mListContentUri = pListContentUri;
  }

  @Override
  public final void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    mBase = getSyncSource();
    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }

    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), pAccount, authbase);
    }

    for(Phases phase:Phases.values()) {
      try {
        phase.execute(this, pProvider, pSyncResult);
      } catch (IllegalStateException|XmlPullParserException e) {
        pSyncResult.stats.numParseExceptions++;
        Log.e(TAG, "Error parsing process model list", e);
      } catch (IOException e) {
        pSyncResult.stats.numIoExceptions++;
        Log.e(TAG, "Error contacting process model server", e);
      } catch (RemoteException|OperationApplicationException e) {
        pSyncResult.databaseError=true;
        Log.e(TAG, "Error updating process model database", e);
      }
    }
  }

  /**
   * Create a new pull parser with the member factory.
   * @return A new parser
   * @throws XmlPullParserException
   * @category Utils
   */
  protected XmlPullParser newPullParser() throws XmlPullParserException {
    return getParserFactory().newPullParser();
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
   * The code responsible for coordinating the publication of items to the server. Normally this does not need to be overridden.
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself, but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void publishItemsToServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToCreate = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_PUBLISH_TO_SERVER, null, null);
    while(itemsToCreate.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToCreate.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = createItemOnServer(pProvider, mHttpClient, itemuri, pSyncResult);
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
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself, but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void deleteOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID };

    Cursor itemsToDelete = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_DELETE_ON_SERVER, null, null);
    while(itemsToDelete.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, itemsToDelete.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = deleteItemOnServer(pProvider, mHttpClient, itemuri, pSyncResult);
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
   *
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  private final void updateListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException, OperationApplicationException {
    HttpGet getList = new HttpGet(getListUrl(mBase));
    HttpResponse result;
    try {
      result = mHttpClient.execute(getList);
    } catch (IOException e) {
      pSyncResult.stats.numIoExceptions++;
      return;
    }
    if (result!=null) {
      final int statusCode = result.getStatusLine().getStatusCode();
      if (statusCode>=200 && statusCode<400) {
        mUpdateList = updateItemListFromServer(pProvider, pSyncResult, result.getEntity().getContent());
      } else {
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
   *
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void deleteItemsMissingOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    int cnt = pProvider.delete(mListContentUri, colSyncstate + " = "+SYNC_PENDING+ " OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_PENDING, null);
    pSyncResult.stats.numDeletes+=cnt;
  }

  /**
   * The code responsible for sending local changes to the server. It will take
   * the server response and use it to update the local database.Normally this
   * does not need to be overridden.
   *
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected void sendLocalChangesToServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, OperationApplicationException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    Cursor updateableItems = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_UPDATE_SERVER +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING, null, null);
    while(updateableItems.moveToNext()) {
      int syncState = updateableItems.getInt(1);
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, updateableItems.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = updateItemOnServer(pProvider, mHttpClient, itemuri, syncState, pSyncResult);
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
   * {@link #doUpdateItemDetails(AuthenticatedWebClient, ContentProviderClient, long, CVPair)}
   * doUpdateItemDetails} which has access to the {@link ContentProviderClient},
   * therefore can update detail tables. Normally this does not need to be
   * overridden.
   *
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */

  protected void updateItemDetails(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, IOException, OperationApplicationException {
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
        if(doUpdateItemDetails(mHttpClient, pProvider, id, pair)) {
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
   *
   * @param pProvider The {@link ContentProviderClient provider} used.
   * @param pSyncResult The {@link SyncResult} used for sync statistics.
   * @param pContent The result of the server request.
   * @return The list of results. These results will be provided in the
   *         {@link #doUpdateItemDetails(AuthenticatedWebClient, ContentProviderClient, long, CVPair)
   *         doUpdateItemDetails} function.
   * @throws RemoteException If a direct operation failed.
   * @throws OperationApplicationException Not thrown by the operation itself,
   *           but children may, when using batch content provider operations.
   * @category Phase
   */
  protected List<CVPair> updateItemListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, RemoteException, IOException, OperationApplicationException {
    final String itemNamespace = getItemNamespace();
    final String itemsTag = getItemsTag();

    final String colSyncstate = getSyncStateColumn();
    final String colKey = getKeyColumn();

    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    XmlPullParser parser = newPullParser();
    parser.setInput(pContent, "UTF8");

    ContentValues values = new ContentValues(1);
    if (colSyncstate!=null && colSyncstate.length()>0) {
      ArrayList<ContentProviderOperation> operations = new ArrayList<>(2);
      values.put(colSyncstate, Integer.valueOf(SYNC_PENDING));

      operations.add(ContentProviderOperation.newUpdate(ProcessModels.CONTENT_ID_URI_BASE)
          .withSelection(colSyncstate + " = "+SYNC_UPTODATE, null)
          .withValue(colSyncstate,  Integer.valueOf(SYNC_PENDING))
          .build());

      operations.add(ContentProviderOperation.newUpdate(ProcessModels.CONTENT_ID_URI_BASE)
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

              Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
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
   *
   * @param pHttpClient The {@link AuthenticatedWebClient} to use.
   * @param pProvider The {@link ContentProviderClient}
   * @param pId The id within the local table of the item to update the details
   *          of.
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   * @return <code>true</code> on success.
   * @throws RemoteException
   * @throws OperationApplicationException
   * @throws IOException
   * @category Hooks
   */
  protected abstract boolean doUpdateItemDetails(AuthenticatedWebClient pHttpClient, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, OperationApplicationException, IOException;

  /**
   * Hook for that should be used by subclasses to delete an item on the server.
   *
   * @param pHttpClient The {@link AuthenticatedWebClient} to use.
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pSyncResult The sync status.
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  protected abstract ContentValuesProvider deleteItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to create an item on the server.
   * @param pProvider The {@link ContentProviderClient}
   * @param pHttpClient The {@link AuthenticatedWebClient} to use.
   * @param pItemuri The local content uri of the item.
   * @param pSyncState TODO
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   * @param pSyncResult The sync status.
   *
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  protected abstract ContentValuesProvider createItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  /**
   * Hook for that should be used by subclasses to update an item on the server.
   *
   * @param pHttpClient The {@link AuthenticatedWebClient} to use.
   * @param pProvider The {@link ContentProviderClient}
   * @param pItemuri The local content uri of the item.
   * @param pPair The details (if available) of the item, based upon the return
   *          of {@link #parseItem(XmlPullParser)}.
   * @param pSyncResult The sync status.
   * @return The new values to be stored in the database for the object.
   * @throws RemoteException
   * @throws IOException
   * @category Hooks
   */
  protected abstract ContentValuesProvider updateItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, int pSyncState, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

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

  /**
   * Get the url that is the basis for this synchronization.
   * @return The base url.
   * @category Configuration
   */
  protected abstract String getSyncSource();

}