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
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException {
        pAdapter.deleteOnServer(pProvider, pSyncResult);
      }

    },
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException {
        pAdapter.updateListFromServer(pProvider, pSyncResult);
      }},

    PUBLISH_ITEMS_TO_SERVER{

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException {
        pAdapter.publishItemsToServer(pProvider, pSyncResult);
      }

    }, DELETE_ITEMS_MISSING_ON_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException {
        pAdapter.deleteItemsMissingOnServer(pProvider, pSyncResult);

      }

    },

    SEND_LOCAL_CHANGES_TO_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException {
        pAdapter.sendLocalChangesToServer(pProvider, pSyncResult);
      }},

    UPDATE_ITEMS_FROM_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException {
        pAdapter.updateItemDetails(pProvider, pSyncResult);
      }};

    public abstract void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException;
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

  protected XmlPullParser newPullParser() throws XmlPullParserException {
    return getParserFactory().newPullParser();
  }

  protected XmlPullParserFactory getParserFactory() throws XmlPullParserException {
    if (mXpf==null) {
      mXpf = XmlPullParserFactory.newInstance();
      mXpf.setNamespaceAware(true);
    }
    return mXpf;
  }

  protected void publishItemsToServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException {
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

  public RemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, boolean pAllowParallelSyncs, Uri pListContentUri) {
    super(pContext, pAutoInitialize, pAllowParallelSyncs);
    mListContentUri = pListContentUri;
  }

  protected void deleteOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException {
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
        pProvider.update(itemuri, newValues, null, null);
        pSyncResult.stats.numDeletes++;
      } catch (IOException e) {
        pSyncResult.stats.numIoExceptions++;
      } catch (XmlPullParserException e) {
        pSyncResult.stats.numParseExceptions++;
      }
    }
    // TODO Auto-generated method stub

  }

  protected void updateListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException {
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

  protected void deleteItemsMissingOnServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException {
    final String colSyncstate = getSyncStateColumn();
    int cnt = pProvider.delete(mListContentUri, colSyncstate + " = "+SYNC_PENDING+ " OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_PENDING, null);
    pSyncResult.stats.numDeletes+=cnt;
  }

  protected void sendLocalChangesToServer(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException {
    final String colSyncstate = getSyncStateColumn();
    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    Cursor updateableItems = pProvider.query(mListContentUri, projectionId, colSyncstate+" = "+SYNC_UPDATE_SERVER +" OR "+colSyncstate+" = "+SYNC_UPDATE_SERVER_DETAILSPENDING, null, null);
    while(updateableItems.moveToNext()) {
      Uri itemuri = ContentUris.withAppendedId(mListContentUri, updateableItems.getLong(0));
      try {
        ContentValuesProvider newValuesProvider = updateItemOnServer(pProvider, mHttpClient, itemuri, pSyncResult);
        ContentValues newValues  = newValuesProvider.getContentValues();
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

  protected void updateItemDetails(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, IOException {
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

  protected abstract boolean doUpdateItemDetails(AuthenticatedWebClient pHttpClient, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, IOException;

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
        Log.e(TAG, "Error parsing process model list", e);
      } catch (RemoteException e) {
        pSyncResult.databaseError=true;
        Log.e(TAG, "Error parsing process model list", e);
      }
    }
  }

  private List<CVPair> updateItemListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, RemoteException, IOException {
    final String itemNamespace = getItemNamespace();
    final String itemsTag = getItemsTag();

    final String colSyncstate = getSyncStateColumn();
    final String colKey = getKeyColumn();

    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    XmlPullParser parser = newPullParser();
    parser.setInput(pContent, "UTF8");

    ContentValues values = new ContentValues(1);
    if (colSyncstate!=null && colSyncstate.length()>0) {
      values.put(colSyncstate, Integer.valueOf(SYNC_PENDING));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, colSyncstate + " = "+SYNC_UPTODATE, null);
      values.put(colSyncstate, Integer.valueOf(SYNC_UPDATE_SERVER_PENDING));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, colSyncstate + " = "+SYNC_UPDATE_SERVER, null);
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
                }
              }
              pProvider.update(uri, itemCv, null, null);
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

  protected abstract ContentValuesProvider deleteItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException;

  protected abstract ContentValuesProvider createItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  protected abstract ContentValuesProvider updateItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  protected abstract boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException;

  protected abstract ContentValuesProvider parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException;

  protected abstract String getKeyColumn();

  protected abstract String getSyncStateColumn();

  protected abstract String getItemNamespace();

  protected abstract String getItemsTag();

  protected abstract String getListUrl(String pBase);

  protected abstract String getSyncSource();

}