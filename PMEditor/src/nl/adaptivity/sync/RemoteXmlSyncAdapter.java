package nl.adaptivity.sync;


import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

  public static final int SYNC_UPDATE_SERVER = 1;
  public static final int SYNC_UPTODATE = 0;
  public static final int SYNC_PENDING = 2;
  public static final int SYNC_DETAILSPENDING = 3;
  public static final int SYNC_UPDATE_SERVER_PENDING = 4;
  public static final int SYNC_UPDATE_SERVER_DETAILSPENDING = 5;

  private enum Phases {
    UPDATE_LIST_FROM_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException, XmlPullParserException, IOException {
        pAdapter.updateListFromServer(pProvider, pSyncResult);
      }},

    DELETE_ITEMS_MISSING_ON_SERVER {

      @Override
      public void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult)
          throws XmlPullParserException, IOException, RemoteException {
        pAdapter.delete_items_missing_on_server(pProvider, pSyncResult);

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
        pAdapter.updateItemContentFromServer(pProvider, pSyncResult);
      }};

    public abstract void execute(RemoteXmlSyncAdapter pAdapter, ContentProviderClient pProvider, SyncResult pSyncResult) throws XmlPullParserException, IOException, RemoteException;
  }

  private static final String TAG = RemoteXmlSyncAdapter.class.getSimpleName();

  private XmlPullParserFactory mXpf;
  private String mBase;
  private AuthenticatedWebClient mHttpClient;
  private final Uri mListContentUri;

  public RemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, Uri pListContentUri) {
    super(pContext, pAutoInitialize);
    mListContentUri = pListContentUri;
  }

  public RemoteXmlSyncAdapter(Context pContext, boolean pAutoInitialize, boolean pAllowParallelSyncs, Uri pListContentUri) {
    super(pContext, pAutoInitialize, pAllowParallelSyncs);
    mListContentUri = pListContentUri;
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
        updateItemListFromServer(pProvider, pSyncResult, result.getEntity().getContent());
      } else {
        pSyncResult.stats.numIoExceptions++;
      }
    } else {
      pSyncResult.stats.numAuthExceptions++;
    }
  }

  protected void delete_items_missing_on_server(ContentProviderClient pProvider, SyncResult pSyncResult) throws RemoteException {
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
        ContentValues newValues = postItem(pProvider, mHttpClient, itemuri, pSyncResult);
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

  protected void updateItemContentFromServer(ContentProviderClient pProvider, SyncResult pSyncResult) {
    // TODO Auto-generated method stub

  }

  @Override
  public final void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    if (mXpf==null) {
      try {
        mXpf = XmlPullParserFactory.newInstance();
      } catch (XmlPullParserException e1) {
        pSyncResult.stats.numParseExceptions++;
        return;
      }
      mXpf.setNamespaceAware(true);
    }

    mBase = getSyncSource();
    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }

    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), authbase);
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
//    HttpGet getList = new HttpGet(getListUrl(mBase));
//    HttpResponse result;
//    try {
//      result = mHttpClient.execute(getList);
//    } catch (IOException e) {
//      pSyncResult.stats.numIoExceptions++;
//      return;
//    }
//    if (result!=null) {
//      final int statusCode = result.getStatusLine().getStatusCode();
//      if (statusCode>=200 && statusCode<400) {
//        try {
//          updateItemListFromServer(pProvider, pSyncResult, result.getEntity().getContent());
//        } catch (IllegalStateException|XmlPullParserException e) {
//          pSyncResult.stats.numParseExceptions++;
//          Log.e(TAG, "Error parsing process model list", e);
//        } catch (IOException e) {
//          pSyncResult.stats.numIoExceptions++;
//          Log.e(TAG, "Error parsing process model list", e);
//        } catch (RemoteException e) {
//          pSyncResult.databaseError=true;
//          Log.e(TAG, "Error parsing process model list", e);
//        }
//      } else {
//        pSyncResult.stats.numIoExceptions++;
//      }
//    } else {
//      pSyncResult.stats.numAuthExceptions++;
//    }
  }

  private List<Long> updateItemListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, RemoteException, IOException {
    final String itemNamespace = getItemNamespace();
    final String itemsTag = getItemsTag();

    final String colSyncstate = getSyncStateColumn();
    final String colKey = getKeyColumn();

    final String[] projectionId = new String[] { BaseColumns._ID, colSyncstate };

    XmlPullParser parser = mXpf.newPullParser();
    parser.setInput(pContent, "UTF8");

    ContentValues values = new ContentValues(1);
    if (colSyncstate!=null && colSyncstate.length()>0) {
      values.put(colSyncstate, Integer.valueOf(SYNC_PENDING));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, colSyncstate + " = "+SYNC_UPTODATE, null);
      values.put(colSyncstate, Integer.valueOf(SYNC_UPDATE_SERVER_PENDING));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, colSyncstate + " = "+SYNC_UPDATE_SERVER, null);
    }

//    try {
      List<Long> result = new ArrayList<>();
      parser.next();
      parser.require(START_TAG, itemNamespace, itemsTag); // ensure the outer element
      int type;
      while ((type = parser.next()) != END_TAG) {
        switch (type) {
          case START_TAG:
            ContentValues item = parseItem(parser);
            Object key = item.get(colKey);
            if (key!=null) {
              if (!item.containsKey(colSyncstate)) {
                item.put(colSyncstate, SYNC_DETAILSPENDING);
              }
              Cursor localItem = pProvider.query(mListContentUri, projectionId, colKey+"= ?", new String[] {key.toString()}, null);
              if (localItem.moveToFirst()) {
                long id = localItem.getLong(0); // we know from the projection that _id is always the first column
                int syncState = localItem.getInt(1); // sync state is the second column
                localItem.close();
                Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
                if (syncState==SYNC_UPDATE_SERVER_PENDING) {
                  item.remove(colSyncstate);
                  ContentValues itemCpy = new ContentValues(item);
                  itemCpy.remove(colSyncstate);
                  if (resolvePotentialConflict(pProvider, uri, item)) {
                    if (item.equals(itemCpy)) {
                      // no server update needed
                      item.put(colSyncstate, SYNC_DETAILSPENDING);
                    } else {
                      item.put(colSyncstate, SYNC_UPDATE_SERVER_DETAILSPENDING);
                    }
                  } else {
                    pSyncResult.stats.numConflictDetectedExceptions++;
                  }
                }
                pProvider.update(uri, item, null, null);
                pSyncResult.stats.numUpdates++;
                result.add(Long.valueOf(id));
              } else {
                localItem.close();
                result.add(Long.valueOf(ContentUris.parseId(pProvider.insert(mListContentUri, item))));
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
//    } finally {
//      values.clear();
//      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(SYNC_UPTODATE));
//      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+SYNC_PENDING, null);
//    }


  }

  protected abstract ContentValues postItem(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException;

  protected abstract boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValues pItem) throws RemoteException;

  protected abstract ContentValues parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException;

  protected abstract String getKeyColumn();

  protected abstract String getSyncStateColumn();

  protected abstract String getItemNamespace();

  protected abstract String getItemsTag();

  protected abstract String getListUrl(String pBase);

  protected abstract String getSyncSource();

}