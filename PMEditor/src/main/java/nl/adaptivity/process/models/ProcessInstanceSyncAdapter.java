package nl.adaptivity.process.models;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.DeleteRequest;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.PostRequest;
import nl.adaptivity.android.util.LogUtil;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessInstances;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.ISimpleSyncDelegate;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.SimpleContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import static nl.adaptivity.sync.RemoteXmlSyncAdapter.SYNC_UPTODATE;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

@SuppressWarnings("boxing")
public class ProcessInstanceSyncAdapter extends RemoteXmlSyncAdapterDelegate implements ISimpleSyncDelegate {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSINSTANCES = "processInstances";
  private static final String TAG_PROCESSINSTANCE = "processInstance";
  private static final String TAG_HPROCESSINSTANCE = "instanceHandle";
  private static final String TAG = "TaskSyncAdapter";

  public ProcessInstanceSyncAdapter() {
    super(ProcessInstances.CONTENT_ID_URI_BASE);
  }

  @Override
  public URI getListUrl(URI base) {
    return base.resolve("processInstances");
  }

  @Override
  public ContentValuesProvider updateItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, int syncState, SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException("The server can not be changed yet");
//    long pmHandle;
//    long handle;
//    {
//      Cursor pendingPosts = pProvider.query(pItemuri, new String[]{ ProcessInstances.COLUMN_PMHANDLE, ProcessInstances.COLUMN_HANDLE, ProcessInstances.COLUMN_NAME }, null, null, null);
//      try {
//        if (pendingPosts.moveToFirst()) {
//          pmHandle = pendingPosts.getLong(0);
//          handle = pendingPosts.getLong(1);
//        } else {
//          return null;
//        }
//
//      } finally {
//        pendingPosts.close();
//      }
//    }
//
//    return postToServer(pDelegator, pDelegator.getSyncSource()+"/processInstances/"+handle, pmHandle, name, pSyncResult);
  }

  @Override
  public ContentValuesProvider createItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri, SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    String name;
    long pmHandle;
    String uuid;
    {
      Cursor pendingPosts = provider.query(itemuri, new String[]{ ProcessInstances.COLUMN_PMHANDLE, ProcessInstances.COLUMN_NAME, ProcessInstances.COLUMN_UUID }, null, null, null);
      try {
        if (pendingPosts.moveToFirst()) {
          pmHandle = pendingPosts.getLong(0);
          name= pendingPosts.getString(1);
          uuid = pendingPosts.getString(2);
        } else {
          return null;
        }

      } finally {
        pendingPosts.close();
      }
    }
    StringBuilder url = new StringBuilder();
    url.append(delegator.getSyncSource())
       .append("/processModels/")
       .append(Long.toString(pmHandle))
       .append("?op=newInstance&name=")
       .append(URLEncoder.encode(name, "UTF-8"));
    if (uuid!=null) { url.append("&uuid=").append(uuid); }

    return postInstanceToServer(delegator, URI.create(url.toString()), syncResult);
  }

  private static ContentValuesProvider postInstanceToServer(DelegatingResources delegator, final URI url, SyncResult syncResult) throws
          IOException, XmlPullParserException {
    PostRequest post = new PostRequest(url, "");
    HttpURLConnection urlConnection = delegator.getWebClient().execute(post);
    try {
      int status = urlConnection.getResponseCode();
      if (status >= 200 && status < 400) {
        XmlPullParser parser = delegator.newPullParser();
        InputStream input = urlConnection.getInputStream();
        try {
          parser.setInput(input, "UTF-8");

          parser.nextTag(); // Skip document start etc.
          parser.require(XmlPullParser.START_TAG, NS_PROCESSMODELS, TAG_HPROCESSINSTANCE);
          long handle = Long.parseLong(parser.nextText());
          parser.nextTag();
          parser.require(XmlPullParser.END_TAG, NS_PROCESSMODELS, TAG_HPROCESSINSTANCE);

          ContentValues cv = new ContentValues(2);
          cv.put(ProcessInstances.COLUMN_HANDLE, handle);
          cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_UPTODATE);
          ContentValuesProvider values = new SimpleContentValuesProvider(cv);
          ++syncResult.stats.numUpdates;
          return values;
        } finally {
          input.close();
        }
      } else {
        String statusline = Integer.toString(urlConnection.getResponseCode())+urlConnection.getResponseMessage();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          LogUtil.logResponse(TAG, Log.DEBUG, url.toString(), statusline, urlConnection.getErrorStream());
        }
        // Don't throw an exception.
        ++syncResult.stats.numSkippedEntries;
        //      ++pSyncResult.stats.numIoExceptions;
        //      return null;

        throw new IOException("The server could not be updated: " + statusline);
      }
    } finally {
      urlConnection.disconnect();
    }
  }

  @Override
  public ContentValuesProvider deleteItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri,
                                                     SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    long handle;
    {
      Cursor itemCursor = provider.query(itemuri, new String[]{ ProcessModels.COLUMN_HANDLE }, null, null, null);
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

    URI uri =delegator.getSyncSource().resolve("processInstances/"+handle);
    DeleteRequest request = new DeleteRequest(uri);
    HttpURLConnection urlConnection = delegator.getWebClient().execute(request);
    try {
      int status = urlConnection.getResponseCode();
      if (status >= 200 && status < 400) {
        CharArrayWriter out = new CharArrayWriter();
        InputStream ins = urlConnection.getInputStream();
        Reader in = new InputStreamReader(ins, Charset.forName("UTF-8"));
        char[] buffer = new char[2048];
        int cnt;
        while ((cnt = in.read(buffer)) >= 0) {
          out.write(buffer, 0, cnt);
        }
        Log.i(TAG, "Response on deleting item: \"" + out.toString() + "\"");

        ContentValues cv = new ContentValues(1);
        cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
        return new SimpleContentValuesProvider(cv);
      }
    } finally {
      urlConnection.disconnect();
    }
    return null;
  }

  @Override
  public boolean resolvePotentialConflict(ContentProviderClient provider, Uri uri, ContentValuesProvider item) throws RemoteException {
    // Server always wins
    return true;
  }

  @Override
  public ContentValuesProvider parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
    parser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    String name = parser.getAttributeValue(null, "name");
    long handle;
    long pmhandle;
    String uuid;
    try {
      handle = Long.parseLong(parser.getAttributeValue(null, "handle"));
      pmhandle = Long.parseLong(parser.getAttributeValue(null, "processModel"));
      uuid = parser.getAttributeValue(null, "uuid");
    } catch (NullPointerException|NumberFormatException e) {
      throw new XmlPullParserException(e.getMessage(), parser, e);
    }
    parser.next();
    parser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    ContentValues result = new ContentValues(4);
    result.put(ProcessInstances.COLUMN_HANDLE, handle);
    result.put(ProcessInstances.COLUMN_PMHANDLE, pmhandle);
    result.put(ProcessInstances.COLUMN_NAME, name);
    result.put(ProcessInstances.COLUMN_UUID, uuid);
    result.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE); // No details at this stage
    return new SimpleContentValuesProvider(result);
  }

  @Override
  public boolean doUpdateItemDetails(DelegatingResources delegator, ContentProviderClient provider, long id, CVPair pair) throws RemoteException, IOException {
    return true; // We don't do details yet.
  }

  @Override
  public String getKeyColumn() {
    return ProcessInstances.COLUMN_UUID;
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
    return TAG_PROCESSINSTANCES;
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