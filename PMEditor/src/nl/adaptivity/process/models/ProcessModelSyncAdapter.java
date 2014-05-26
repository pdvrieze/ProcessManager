package nl.adaptivity.process.models;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
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
import static org.xmlpull.v1.XmlPullParser.*;


public class ProcessModelSyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSMODELS = "processModels";
  private static final String TAG = ProcessModelSyncAdapter.class.getSimpleName();
  private AuthenticatedWebClient mHttpClient;
  private String mBase;
  private XmlPullParserFactory mXpf;

  public ProcessModelSyncAdapter(Context pContext) {
    super(pContext, true, false);
  }

  @Override
  public void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
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
    HttpGet getProcessesRequest = new HttpGet(mBase+"ProcessModels");
    HttpResponse result;
    try {
      result = mHttpClient.execute(getProcessesRequest);
    } catch (IOException e) {
      pSyncResult.stats.numIoExceptions++;
      return;
    }
    final int statusCode = result.getStatusLine().getStatusCode();
    if (statusCode>=200 && statusCode<400) {
      try {
        updateProcessModelList(pProvider, pSyncResult, result.getEntity().getContent());
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
  }

  private void updateProcessModelList(ContentProviderClient pProvider, SyncResult pSyncResult, InputStream pContent) throws XmlPullParserException, IOException, RemoteException {
    XmlPullParser parser = mXpf.newPullParser();
    parser.setInput(pContent, "UTF8");
    List<Long> handles = updateProcessModelList(pProvider, pSyncResult, parser);

    Cursor pendingPosts = pProvider.query(ProcessModels.CONTENT_ID_URI_BASE, new String[]{ BaseColumns._ID, ProcessModels.COLUMN_MODEL }, ProcessModels.COLUMN_SYNCSTATE+" = ?", new String[] {Integer.toString(ProcessModels.SYNC_UPDATE_SERVER)}, null);
    while (pendingPosts.moveToNext()) {
      postProcessModel(pProvider, pSyncResult, pendingPosts.getLong(0), pendingPosts.getString(1));
    }
  }

  private void postProcessModel(ContentProviderClient pProvider, SyncResult pSyncResult, long pLong, String pModel) throws IOException, XmlPullParserException {
    HttpPost post = new HttpPost(mBase+"ProcessModels");
    try {
      post.setEntity(new StringEntity(pModel, "UTF8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    HttpResponse response = mHttpClient.execute(post);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      XmlPullParser parser = mXpf.newPullParser();
    } else {
      pSyncResult.stats.numIoExceptions++;
    }
  }

  private List<Long> updateProcessModelList(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser) throws XmlPullParserException, IOException, RemoteException {
    ContentValues values = new ContentValues(1);
    values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_PENDING));
    pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+ProcessModels.SYNC_UPTODATE, null);
    try {
      List<Long> result = new ArrayList<>();
      parser.next();
      parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODELS);
      int type;
      while ((type = parser.next()) != END_TAG) {
        switch (type) {
          case START_TAG:
            result.add(parseProcessModelRef(pProvider, pSyncResult, parser));
            break;
          default:
            throw new XmlPullParserException("Unexpected tag type: " + type);
        }
      }
      return result;
    } finally {
      values.clear();
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_UPTODATE));
      pProvider.update(ProcessModels.CONTENT_ID_URI_BASE, values, ProcessModels.COLUMN_SYNCSTATE + " = "+ProcessModels.SYNC_PENDING, null);
    }
  }


  private Long parseProcessModelRef(ContentProviderClient pProvider, SyncResult pSyncResult, XmlPullParser parser) throws XmlPullParserException, IOException, RemoteException {
    parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODELS);
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
    parser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSMODELS);
    final long id;
    Cursor localModel = pProvider.query(ProcessModels.CONTENT_ID_URI_BASE, ProcessModels.BASE_PROJECTION, ProcessModels.SELECT_HANDLE, new String[] { Long.toString(handle)}, null);
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
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_MODELPENDING));
      int cnt = pProvider.update(uri, values, null, null);
      if (count && cnt>0) {
        pSyncResult.stats.numUpdates+=cnt;
      }
    } else {
      ContentValues values = new ContentValues(3);
      if (name!=null) {
        values.put(ProcessModels.COLUMN_NAME, name);
      }
      values.put(ProcessModels.COLUMN_HANDLE, Long.valueOf(handle));
      values.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(ProcessModels.SYNC_MODELPENDING));
      Uri iduri = pProvider.insert(ProcessModels.CONTENT_ID_URI_BASE, values);
      id = ContentUris.parseId(iduri);
      pSyncResult.stats.numInserts++;
    }
    if (id>=0) {
      // TODO implement loading actual models from the server.
    }

    return Long.valueOf(handle);
  }
}