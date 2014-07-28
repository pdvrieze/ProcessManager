package nl.adaptivity.process.models;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.xmlpull.v1.XmlPullParser.*;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.MultipartEntity;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

@SuppressWarnings("boxing")
public class ProcessModelSyncAdapter extends RemoteXmlSyncAdapter {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSMODELS = "processModels";
  private static final String TAG_PROCESSMODEL = "processModel";
  private static final String TAG = ProcessModelSyncAdapter.class.getSimpleName();

  public ProcessModelSyncAdapter(Context pContext) {
    super(pContext, true, false, ProcessModels.CONTENT_ID_URI_BASE);
  }

  @Override
  protected String getSyncSource() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    return prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/ProcessEngine/");
  }

  @Override
  protected String getListUrl(String pBase) {
    return pBase+"/processModels";
  }

  @Override
  protected ContentValuesProvider updateItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, int pSyncState, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    String model;
    long handle;
    {
      Cursor pendingPosts = pProvider.query(pItemuri, new String[]{ ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE }, null, null, null);
      try {
        if (pendingPosts.moveToFirst()) {
          model = pendingPosts.getString(0);
          handle = pendingPosts.getLong(1);
        } else {
          return null;
        }

      } finally {
        pendingPosts.close();
      }
    }

    return postToServer(pHttpClient, getSyncSource()+"/processModels/"+handle, model, pSyncResult);
  }

  @Override
  protected ContentValuesProvider createItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    String model;
    {
      Cursor pendingPosts = pProvider.query(pItemuri, new String[]{ ProcessModels.COLUMN_MODEL }, null, null, null);
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

    return postToServer(pHttpClient, getSyncSource()+"/processModels", model, pSyncResult);
  }

  private ContentValuesProvider postToServer(AuthenticatedWebClient pHttpClient, final String url, String model, SyncResult pSyncResult) throws ClientProtocolException,
      IOException, XmlPullParserException {
    HttpPost post = new HttpPost(url);
    try {
      final MultipartEntity entity = new MultipartEntity();
      entity.add("processUpload", new StringEntity(model, "UTF8"));
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    HttpResponse response = pHttpClient.execute(post);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      XmlPullParser parser = newPullParser();
      parser.setInput(response.getEntity().getContent(), "UTF8");

      parser.nextTag(); // Skip document start etc.
      ContentValuesProvider values = parseItem(parser);
      return values;

    } else {
      response.getEntity().consumeContent();

      throw new IOException("The server could not be updated");
    }
  }

  @Override
  protected ContentValuesProvider deleteItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri,
                                                     SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    long handle;
    {
      Cursor itemCursor = pProvider.query(pItemuri, new String[]{ ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE }, null, null, null);
      try {
        if (itemCursor.moveToFirst()) {
          handle = itemCursor.getLong(1);
        } else {
          return null;
        }

      } finally {
        itemCursor.close();
      }
    }

    String uri = getSyncSource()+"/processModels/"+handle;
    HttpDelete request = new HttpDelete(uri);
    HttpResponse response = pHttpClient.execute(request);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      CharArrayWriter out = new CharArrayWriter();
      InputStream ins = response.getEntity().getContent();
      Reader in = new InputStreamReader(ins, Charset.forName("UTF-8"));
      char[] buffer = new char[2048];
      int cnt;
      while ((cnt=in.read(buffer))>=0) {
        out.write(buffer, 0, cnt);
      }
      Log.i(TAG, "Response on deleting item: \""+out.toString()+"\"");

      ContentValues cv = new ContentValues(1);
      cv.put(ProcessModels.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
      return new SimpleContentValuesProvider(cv);
    } else {
      response.getEntity().consumeContent();
    }
    return null;
  }

  @Override
  protected boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException {
    final ContentValues itemCv = pItem.getContentValues();
    itemCv.clear();
    itemCv.put(getSyncStateColumn(), Integer.valueOf(SYNC_UPDATE_SERVER));
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
    UUID uuid = toUUID(pParser.getAttributeValue(null, "uuid"));
    pParser.next();
    pParser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    ContentValues result = new ContentValues(4);
    result.put(ProcessModels.COLUMN_HANDLE, handle);
    result.put(ProcessModels.COLUMN_NAME, name);
    result.put(ProcessModels.COLUMN_UUID, uuid.toString());
    result.put(ProcessModels.COLUMN_SYNCSTATE, SYNC_DETAILSPENDING);
    return new SimpleContentValuesProvider(result);
  }

  private static UUID toUUID(final String val) {
    return val==null ? null : UUID.fromString(val);
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
    } else {
      response.getEntity().consumeContent();
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