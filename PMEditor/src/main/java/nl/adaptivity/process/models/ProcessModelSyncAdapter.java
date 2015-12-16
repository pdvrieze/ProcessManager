package nl.adaptivity.process.models;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.DeleteRequest;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.GetRequest;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.PostRequest;
import nl.adaptivity.android.util.LogUtil;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.ISimpleSyncDelegate;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.*;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static nl.adaptivity.sync.RemoteXmlSyncAdapter.*;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;


@SuppressWarnings("boxing")
public class ProcessModelSyncAdapter extends RemoteXmlSyncAdapterDelegate implements ISimpleSyncDelegate {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSMODELS = "processModels";
  private static final String TAG_PROCESSMODEL = "processModel";
  private static final String TAG = ProcessModelSyncAdapter.class.getSimpleName();

// Object Initialization
  public ProcessModelSyncAdapter() {
    super(ProcessModels.CONTENT_ID_URI_BASE);
  }
// Object Initialization end

  @Override
  public Collection<ContentProviderOperation> doUpdateItemDetails(DelegatingResources delegator, ContentProviderClient provider, long id, CVPair pair) throws
          RemoteException, IOException {
    long handle;
    if (pair != null && pair.mCV.getContentValues().containsKey(ProcessModels.COLUMN_HANDLE)) {
      handle = pair.mCV.getContentValues().getAsLong(ProcessModels.COLUMN_HANDLE);
    } else {
      Cursor cursor = provider.query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id), new String[]{ProcessModels.COLUMN_HANDLE}, null, null, null);
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
    URI uri = getListUrl(delegator.getSyncSource()).resolve(Long.toString(handle));
    GetRequest request = new GetRequest(uri);
    HttpURLConnection response = delegator.getWebClient().execute(request);

    int status = response.getResponseCode();
    try {
      if (status >= 200 && status < 400) {
        CharArrayWriter out = new CharArrayWriter();
        Reader in = new InputStreamReader(response.getInputStream(), Charset.forName("UTF-8"));
        try {
          char[] buffer = new char[2048];
          int cnt;
          while ((cnt = in.read(buffer)) >= 0) {
            out.write(buffer, 0, cnt);
          }
        } finally {
          in.close();
        }
        ContentValues cv = new ContentValues(2);
        cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE);
        cv.put(ProcessModels.COLUMN_MODEL, out.toString());
        return Collections.singletonList(ContentProviderOperation.newUpdate(ProcessModels.CONTENT_ID_URI_BASE.buildUpon()
                                                                .appendPath(Long.toString(id))
                                                                .encodedFragment("nonetnotify")
                                                                .build()).withValues(cv).build());
      }
    } finally {
      response.disconnect();
    }
    return Collections.emptyList();
  }

  @Override
  public ContentValuesProvider deleteItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri,
                                                  SyncResult syncResult) throws RemoteException, IOException,
          XmlPullParserException {
    long handle;
    {
      Cursor itemCursor = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE}, null, null, null);
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

    URI uri = delegator.getSyncSource().resolve("processModels/" + handle);
    DeleteRequest request = new DeleteRequest(uri);
    HttpURLConnection response = delegator.getWebClient().execute(request);
    try {
      int status = response.getResponseCode();
      if (status >= 200 && status < 400) {
        CharArrayWriter out = new CharArrayWriter();
        InputStream ins = response.getInputStream();
        try {
          Reader in = new InputStreamReader(ins, Charset.forName("UTF-8"));
          char[] buffer = new char[2048];
          int cnt;
          while ((cnt = in.read(buffer)) >= 0) {
            out.write(buffer, 0, cnt);
          }
          Log.i(TAG, "Response on deleting item: \"" + out.toString() + "\"");

          ContentValues cv = new ContentValues(1);
          cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
          return new SimpleContentValuesProvider(cv, false);
        } finally {
          ins.close();
        }
      }
    } finally {
      response.disconnect();
    }
    return null;
  }

  @Override
  public ContentValuesProvider createItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, SyncResult syncResult) throws
          RemoteException, IOException, XmlPullParserException {
    String model;
    {
      Cursor pendingPosts = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL}, null, null, null);
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

    return postToServer(delegator, delegator.getSyncSource().resolve("processModels"), model, syncResult);
  }

  @Override
  public ContentValuesProvider updateItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, int syncState, SyncResult syncResult) throws
          RemoteException, IOException, XmlPullParserException {
    String model;
    long handle;
    {
      Cursor pendingPosts = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE}, null, null, null);
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

    return postToServer(delegator, delegator.getSyncSource().resolve("processModels/" + handle), model, syncResult);
  }

  private ContentValuesProvider postToServer(DelegatingResources delegator, final URI url, String model, SyncResult syncResult) throws
          IOException, XmlPullParserException {
    PostRequest post = new PostRequest(url, model);
    post.setContentType("text/xml; charset=utf-8");
    HttpURLConnection response = delegator.getWebClient().execute(post);
    try {
      int status = response.getResponseCode();
      if (status >= 200 && status < 400) {
        XmlPullParser parser = delegator.newPullParser();
        final InputStream content = response.getInputStream();
        try {
          parser.setInput(content, "UTF-8");

          parser.nextTag(); // Skip document start etc.
          ContentValuesProvider values = parseItem(parser); // We already have a local model if posting
          return values;
        } finally {
          content.close();
        }

      } else {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          LogUtil.logResponse(TAG, Log.DEBUG, url.toString(), Integer.toString(response.getResponseCode()) + " " + response
                  .getResponseMessage(), response.getErrorStream());
        }

        throw new IOException("The server could not be updated: " + response.getResponseCode() + " " + response.getResponseMessage());
      }
    } finally {
      response.disconnect();
    }
  }

  @Override
  public boolean resolvePotentialConflict(ContentProviderClient provider, Uri uri, ContentValuesProvider item) throws
          RemoteException {
    final ContentValues itemCv = item.getContentValues();
    itemCv.clear();
    itemCv.put(getSyncStateColumn(), Integer.valueOf(SYNC_UPDATE_SERVER));
    return true;
  }

  @Override
  public ContentValuesProvider parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
    parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    String name = parser.getAttributeValue(null, "name");
    long handle;
    try {
      handle = Long.parseLong(parser.getAttributeValue(null, "handle"));
    } catch (NullPointerException | NumberFormatException e) {
      throw new XmlPullParserException(e.getMessage(), parser, e);
    }
    UUID uuid = toUUID(parser.getAttributeValue(null, "uuid"));
    parser.next();
    parser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    ContentValues result = new ContentValues(4);
    result.put(ProcessModels.COLUMN_HANDLE, handle);
    result.put(ProcessModels.COLUMN_NAME, name);
    result.put(ProcessModels.COLUMN_UUID, uuid.toString());
    result.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE); // should be overridden by caller
    return new SimpleContentValuesProvider(result, true);
  }

  private static UUID toUUID(final String val) {
    return val == null ? null : UUID.fromString(val);
  }

  @Override
  public String getKeyColumn() {
    return ProcessModels.COLUMN_UUID;
  }

  @Override
  public String getSyncStateColumn() {
    return XmlBaseColumns.COLUMN_SYNCSTATE;
  }

  @Override
  public String getItemNamespace() {
    return NS_PROCESSMODELS;
  }

  @Override
  public String getItemsTag() {
    return TAG_PROCESSMODELS;
  }

  // XXX remove trailing slash if possible.
  @Override
  public URI getListUrl(URI base) {
    return base.resolve("processModels/");
  }

  @Override
  public String getListSelection() {
    return null;
  }

  @Override
  public String[] getListSelectionArgs() {
    return null;
  }


}