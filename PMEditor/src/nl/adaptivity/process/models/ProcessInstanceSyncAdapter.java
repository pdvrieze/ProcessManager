package nl.adaptivity.process.models;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.xmlpull.v1.XmlPullParser.*;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessInstances;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.SimpleContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

@SuppressWarnings("boxing")
public class ProcessInstanceSyncAdapter extends RemoteXmlSyncAdapterDelegate {

  private static final String NS_PROCESSMODELS = "http://adaptivity.nl/ProcessEngine/";
  private static final String TAG_PROCESSINSTANCES = "processInstances";
  private static final String TAG_PROCESSINSTANCE = "processInstance";
  private static final String TAG = ProcessInstanceSyncAdapter.class.getSimpleName();

  public ProcessInstanceSyncAdapter() {
    super(ProcessInstances.CONTENT_ID_URI_BASE);
  }

  @Override
  protected String getListUrl(String pBase) {
    return pBase+"/processInstances";
  }

  @Override
  protected ContentValuesProvider updateItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, int pSyncState, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
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
  protected ContentValuesProvider createItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri, SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    String name;
    long pmHandle;
    {
      Cursor pendingPosts = pProvider.query(pItemuri, new String[]{ ProcessInstances.COLUMN_PMHANDLE, ProcessInstances.COLUMN_NAME }, null, null, null);
      try {
        if (pendingPosts.moveToFirst()) {
          pmHandle = pendingPosts.getLong(0);
          name= pendingPosts.getString(1);
        } else {
          return null;
        }

      } finally {
        pendingPosts.close();
      }
    }

    return postToServer2(pDelegator, pDelegator.getSyncSource()+"/processModels/"+Long.toString(pmHandle)+"?op=newInstance&name="+URLEncoder.encode(name, "UTF-8"), pSyncResult);
  }

  private ContentValuesProvider postToServer2(DelegatingResources pDelegator, final String url, SyncResult pSyncResult) throws ClientProtocolException,
      IOException, XmlPullParserException {
    HttpPost post = new HttpPost(url);
    HttpResponse response = pDelegator.getWebClient().execute(post);
    int status = response.getStatusLine().getStatusCode();
    if (status>=200 && status<400) {
      XmlPullParser parser = pDelegator.newPullParser();
      parser.setInput(response.getEntity().getContent(), "UTF8");

      parser.nextTag(); // Skip document start etc.
      ContentValuesProvider values = parseItem(parser);
      ++pSyncResult.stats.numUpdates;
      return values;

    } else {
      response.getEntity().consumeContent();
      // Don't throw an exception.
      ++pSyncResult.stats.numSkippedEntries;
//      ++pSyncResult.stats.numIoExceptions;
//      return null;

      throw new IOException("The server could not be updated");
    }
  }

  @Override
  protected ContentValuesProvider deleteItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri,
                                                     SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    long handle;
    {
      Cursor itemCursor = pProvider.query(pItemuri, new String[]{ ProcessModels.COLUMN_HANDLE }, null, null, null);
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

    String uri =pDelegator.getSyncSource()+"/processInstances/"+handle;
    HttpDelete request = new HttpDelete(uri);
    HttpResponse response = pDelegator.getWebClient().execute(request);
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
      cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
      return new SimpleContentValuesProvider(cv);
    } else {
      response.getEntity().consumeContent();
    }
    return null;
  }

  @Override
  protected boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException {
    // Server always wins
    return true;
  }

  @Override
  protected ContentValuesProvider parseItem(XmlPullParser pParser) throws XmlPullParserException, IOException {
    pParser.require(START_TAG, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    String name = pParser.getAttributeValue(null, "name");
    long handle;
    long pmhandle;
    try {
      handle = Long.parseLong(pParser.getAttributeValue(null, "handle"));
      pmhandle = Long.parseLong(pParser.getAttributeValue(null, "processModel"));
    } catch (NullPointerException|NumberFormatException e) {
      throw new XmlPullParserException(e.getMessage(), pParser, e);
    }
    pParser.next();
    pParser.require(END_TAG, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    ContentValues result = new ContentValues(4);
    result.put(ProcessInstances.COLUMN_HANDLE, handle);
    result.put(ProcessInstances.COLUMN_PMHANDLE, pmhandle);
    result.put(ProcessInstances.COLUMN_NAME, name);
    result.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE); // No details at this stage
    return new SimpleContentValuesProvider(result);
  }

  @Override
  protected boolean doUpdateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, long pId, CVPair pPair) throws RemoteException, IOException {
    return true; // We don't do details yet.
  }

  @Override
  protected String getKeyColumn() {
    return ProcessInstances.COLUMN_HANDLE;
  }

  @Override
  protected String getSyncStateColumn() {
    return XmlBaseColumns.COLUMN_SYNCSTATE;
  }

  @Override
  protected String getItemNamespace() {
    return NS_PROCESSMODELS;
  }

  @Override
  protected String getItemsTag() {
    return TAG_PROCESSINSTANCES;
  }


}