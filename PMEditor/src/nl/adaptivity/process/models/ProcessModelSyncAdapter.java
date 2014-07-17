package nl.adaptivity.process.models;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.MultipartEntity;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

@SuppressWarnings("boxing")
public class ProcessModelSyncAdapter extends RemoteXmlSyncAdapter {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSMODELS = "processModels";
  private static final String TAG_PROCESSMODEL = "processModel";
  private static final String TAG = ProcessModelSyncAdapter.class.getSimpleName();
  private AuthenticatedWebClient mHttpClient;
  private String mBase;
  private XmlPullParserFactory mXpf;

  public ProcessModelSyncAdapter(Context pContext) {
    super(pContext, true, false, ProcessModels.CONTENT_ID_URI_BASE);
  }

  @Override
  protected String getSyncSource() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    return prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/ProcessEngine/");
  }

  private void onPerformSyncOld(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
    try {
      mXpf = XmlPullParserFactory.newInstance();
    } catch (XmlPullParserException e1) {
      pSyncResult.stats.numParseExceptions++;
      return;
    }
    mXpf.setNamespaceAware(true);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    mBase = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/ProcessEngine/");
    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }
    {
      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
      mHttpClient = new AuthenticatedWebClient(getContext(), authbase);
    }
    HttpGet getProcessesRequest = new HttpGet(mBase+"processModels");
    HttpResponse result;
    try {
      result = mHttpClient.execute(getProcessesRequest);
    } catch (IOException e) {
      pSyncResult.stats.numIoExceptions++;
      return;
    }
    if (result!=null) {
      final int statusCode = result.getStatusLine().getStatusCode();
      if (statusCode>=200 && statusCode<400) {
        try {
          updateItemListFromServer(pProvider, pSyncResult, result.getEntity().getContent());
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
      } else {
        pSyncResult.stats.numIoExceptions++;
      }
    } else {
      pSyncResult.stats.numAuthExceptions++;
    }
  }

  private void updateItemListFromServer(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, IOException, RemoteException {
    XmlPullParser parser = mXpf.newPullParser();
    parser.setInput(pContent, "UTF8");
    List<Long> handles = updateProcessModelList(pProvider, pSyncResult, parser);

    Cursor pendingPosts = pProvider.query(ProcessModels.CONTENT_ID_URI_BASE, new String[]{ BaseColumns._ID, ProcessModels.COLUMN_MODEL }, ProcessModels.COLUMN_SYNCSTATE+" = ?", new String[] {Integer.toString(RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER)}, null);
    while (pendingPosts.moveToNext()) {
      postProcessModel(pProvider, pSyncResult, pendingPosts.getLong(0), pendingPosts.getString(1));
    }
  }

  private void postProcessModel(ContentProviderClient pProvider, SyncResult pSyncResult, long pId, String pModel) throws IOException, XmlPullParserException {
    HttpPost post = new HttpPost(mBase+"processModels");
    try {
      final MultipartEntity entity = new MultipartEntity();
      entity.add("processUpload", new StringEntity(pModel, "UTF8"));
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    HttpResponse response = mHttpClient.execute(post);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      XmlPullParser parser = mXpf.newPullParser();
      parser.setInput(response.getEntity().getContent(), "UTF8");
      try {
        parser.nextTag(); // Skip document start etc.
        parseProcessModelRef(pProvider, pSyncResult, parser, pId);
        ContentValues values = new ContentValues();
        values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_UPTODATE));
        try {
          pProvider.update(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), values, null, null);
        } catch (RemoteException e1) {
          throw new RuntimeException(e1);
        }
      } catch (RemoteException e) {

        ContentValues values = new ContentValues();
        values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DETAILSPENDING));
        try {
          pProvider.update(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), values, null, null);
        } catch (RemoteException e1) {
          throw new RuntimeException(e1);
        }
        throw new RuntimeException(e);
      }
    } else {
      pSyncResult.stats.numIoExceptions++;
    }
  }

  private List<Long> updateProcessModelList(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser) throws XmlPullParserException, IOException, RemoteException {
    ContentValues values = new ContentValues(1);
    values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PENDING));
    pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+RemoteXmlSyncAdapter.SYNC_UPTODATE, null);
    try {
      List<Long> result = new ArrayList<>();
      parser.next();
      parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODELS);
      int type;
      while ((type = parser.next()) != END_TAG) {
        switch (type) {
          case START_TAG:
            result.add(parseProcessModelRef(pProvider, pSyncResult, parser, -1)); // unknown id
            break;
          default:
            throw new XmlPullParserException("Unexpected tag type: " + type);
        }
      }
      return result;
    } finally {
      values.clear();
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_UPTODATE));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+RemoteXmlSyncAdapter.SYNC_PENDING, null);
    }
  }


  private Long parseProcessModelRef(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser, long pId) throws XmlPullParserException, IOException, RemoteException {
    parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    String name = parser.getAttributeValue(null, "name");
    long handle;
    try {
      handle = Long.parseLong(parser.getAttributeValue(null, "handle"));
    } catch (NullPointerException|NumberFormatException e) {
      pSyncResult.stats.numSkippedEntries++;
      pSyncResult.stats.numParseExceptions++;
      return null;
    }
    parser.next();
    parser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    final long id;
    Cursor localModel;
    if (pId<0) {
      localModel = pProvider.query(ProcessModels.CONTENT_ID_URI_BASE, ProcessModels.BASE_PROJECTION, ProcessModels.SELECT_HANDLE, new String[] { Long.toString(handle)}, null);
    } else {
      localModel = pProvider.query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), ProcessModels.BASE_PROJECTION, null, null, null);
    }
    if (localModel.moveToFirst()) {
      int col_id = localModel.getColumnIndex(BaseColumns._ID);
      int col_name = localModel.getColumnIndex(ProcessModels.COLUMN_NAME);
      id = localModel.getLong(col_id);
      Uri uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
      ContentValues values = new ContentValues(2);
      boolean count = false;
      if (name!=null && (!name.equals(localModel.getString(col_name)))) {
        values.put(ProcessModels.COLUMN_NAME, name);
        count = true;
      }
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DETAILSPENDING));
      if (handle!=-1) {
        values.put(ProcessModels.COLUMN_HANDLE, handle);
      }
      int cnt = pProvider.update(uri, values, null, null);
      if (count && cnt>0) {
        pSyncResult.stats.numUpdates+=cnt;
      }
    } else if (pId>=0) {
      pSyncResult.databaseError=true;
      throw new IllegalStateException("The database does not contain the expected id");
    } else {
      ContentValues values = new ContentValues(3);
      if (name!=null) {
        values.put(ProcessModels.COLUMN_NAME, name);
      }
      values.put(ProcessModels.COLUMN_HANDLE, Long.valueOf(handle));
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DETAILSPENDING));
      Uri iduri = pProvider.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      id = ContentUris.parseId(iduri);
      pSyncResult.stats.numInserts++;
    }
    if (id>=0 && handle!=-1) {
      // TODO implement loading actual models from the server.
    }

    return Long.valueOf(handle);
  }

  @Override
  protected String getListUrl(String pBase) {
    return pBase+"processModels";
  }

  @Override
  protected ContentValuesProvider postItem(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    String model;
    {
      Cursor pendingPosts = pProvider.query(pItemuri, new String[]{ ProcessModels.COLUMN_MODEL }, ProcessModels.COLUMN_SYNCSTATE+" = ?", new String[] {Integer.toString(RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER)}, null);
      try {
        if (pendingPosts.moveToFirst()) {
          model = pendingPosts.getString(0);
        } else {
          return null;
        }

      } finally {
        pendingPosts.close();
      }
    }

    HttpPost post = new HttpPost(mBase+"processModels");
    try {
      final MultipartEntity entity = new MultipartEntity();
      entity.add("processUpload", new StringEntity(model, "UTF8"));
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    HttpResponse response = mHttpClient.execute(post);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      XmlPullParser parser = mXpf.newPullParser();
      parser.setInput(response.getEntity().getContent(), "UTF8");

      parser.nextTag(); // Skip document start etc.
      ContentValuesProvider values = parseItem(parser);
      return values;

    } else {
      pSyncResult.stats.numIoExceptions++;
    }
    return null;
  }

  @Override
  protected boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException {
    final ContentValues itemCv = pItem.getContentValues();
    Cursor localItem = pProvider.query(pUri, null, null, null, null);
    if (localItem.moveToFirst()) {
      for(String key: itemCv.keySet()){
        if (! (getSyncStateColumn().equals(key) ||
               BaseColumns._ID.equals(key) ||
               getKeyColumn().equals(key))) {
          int cursorIdx = localItem.getColumnIndex(key);
          if (cursorIdx>=0) {
            int colType=localItem.getType(cursorIdx);
            switch (colType) {
            case Cursor.FIELD_TYPE_BLOB:
              itemCv.put(key, localItem.getBlob(cursorIdx)); break;
            case Cursor.FIELD_TYPE_FLOAT:
              itemCv.put(key, Float.valueOf(localItem.getFloat(cursorIdx))); break;
            case Cursor.FIELD_TYPE_INTEGER:
              itemCv.put(key, Integer.valueOf(localItem.getInt(cursorIdx))); break;
            case Cursor.FIELD_TYPE_NULL:
              itemCv.putNull(key); break;
            case Cursor.FIELD_TYPE_STRING:
              itemCv.put(key, localItem.getString(cursorIdx)); break;
            }
          }
        }
      }
    }
    return true;
  }

  @Override
  protected ContentValuesProvider parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException {
    pParser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    String name = pParser.getAttributeValue(null, "name");
    long handle;
    try {
      handle = Long.parseLong(pParser.getAttributeValue(null, "handle"));
    } catch (NullPointerException|NumberFormatException e) {
      throw new XmlPullParserException(e.getMessage(), pParser, e);
    }
    UUID uuid = UUID.fromString(pParser.getAttributeValue(null, "uuid"));
    pParser.next();
    pParser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    ContentValues result = new ContentValues(4);
    result.put(ProcessModels.COLUMN_HANDLE, handle);
    result.put(ProcessModels.COLUMN_NAME, name);
    result.put(ProcessModels.COLUMN_UUID, uuid.toString());
    result.put(ProcessModels.COLUMN_SYNCSTATE, SYNC_DETAILSPENDING);
    return new SimpleContentValuesProvider(result);
  }

  @Override
  protected boolean doUpdateItemDetails(AuthenticatedWebClient pHttpClient, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, IOException {
    long handle;
    if (pPair!=null && pPair.mCV.getContentValues().containsKey(ProcessModels.COLUMN_HANDLE)) {
      handle = pPair.mCV.getContentValues().getAsLong(ProcessModels.COLUMN_HANDLE);
    } else {
      Cursor cursor = pProvider.query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), new String[] {ProcessModels.COLUMN_HANDLE}, null, null, null);
      try {
        if (cursor.moveToFirst()) {
          handle = cursor.getLong(0);
        } else {
          throw new IllegalStateException("There is no handle for the given item");
        }
      } finally {
        cursor.close();
      }
    }
    String uri = getListUrl(getSyncSource())+"/"+handle;
    HttpGet request = new HttpGet(uri);
    HttpResponse response = pHttpClient.execute(request);
    // TODO Auto-generated method stub
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      CharArrayWriter out = new CharArrayWriter();
      Reader in = new InputStreamReader(response.getEntity().getContent(),Charset.forName("UTF-8"));
      try {
        char[] buffer = new char[2048];
        int cnt;
        while ((cnt=in.read(buffer))>=0) {
          out.write(buffer, 0, cnt);
        }
      } finally {
        in.close();
      }
      ContentValues cv = new ContentValues(2);
      cv.put(ProcessModels.COLUMN_SYNCSTATE, SYNC_UPTODATE);
      cv.put(ProcessModels.COLUMN_MODEL, out.toString());
      return pProvider.update(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, pId), cv, null, null)>0;
    }
    return false;
  }

  @Override
  protected String getKeyColumn() {
    return ProcessModels.COLUMN_UUID;
  }

  @Override
  protected String getSyncStateColumn() {
    return ProcessModels.COLUMN_SYNCSTATE;
  }

  @Override
  protected String getItemNamespace() {
    return NS_PROCESSMODELS;
  }

  @Override
  protected String getItemsTag() {
    return TAG_PROCESSMODELS;
  }


}