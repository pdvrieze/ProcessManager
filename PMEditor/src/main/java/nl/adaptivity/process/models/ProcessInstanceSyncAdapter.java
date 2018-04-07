/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.models;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
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
import nl.adaptivity.xml.*;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;

import static nl.adaptivity.sync.RemoteXmlSyncAdapter.SYNC_UPTODATE;

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
  public URI getListUrl(final URI base) {
    return base.resolve("processInstances");
  }

  @Override
  public ContentValuesProvider updateItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri, final int syncState, final SyncResult syncResult) throws RemoteException, IOException, XmlException {
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
  public ContentValuesProvider createItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri, final SyncResult syncResult) throws RemoteException, IOException, XmlException {
    String name;
    long pmHandle;
    String uuid;
    {
      final Cursor pendingPosts = provider.query(itemuri, new String[]{ ProcessInstances.COLUMN_PMHANDLE, ProcessInstances.COLUMN_NAME, ProcessInstances.COLUMN_UUID }, null, null, null);
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
    final StringBuilder url = new StringBuilder();
    url.append(delegator.getSyncSource())
       .append("/processModels/")
       .append(Long.toString(pmHandle))
       .append("?op=newInstance&name=")
       .append(URLEncoder.encode(name, "UTF-8"));
    if (uuid!=null) { url.append("&uuid=").append(uuid); }

    return postInstanceToServer(delegator, URI.create(url.toString()), syncResult);
  }

  private static ContentValuesProvider postInstanceToServer(final DelegatingResources delegator, final URI url, final SyncResult syncResult) throws
          IOException, XmlException {
    final PostRequest post = new PostRequest(url, "");
    final HttpURLConnection urlConnection = delegator.getWebClient().execute(delegator.getContext(), post);
    try {
      final int status = urlConnection.getResponseCode();
      if (status >= 200 && status < 400) {
        final InputStream input = urlConnection.getInputStream();
        try {
          final XmlReader parser = XmlStreaming.newReader(input, "UTF-8");

          parser.nextTag(); // Skip document start etc.
          parser.require(EventType.START_ELEMENT, NS_PROCESSMODELS, TAG_HPROCESSINSTANCE);
          final long handle = Long.parseLong(XmlReaderUtil.allText(parser).toString());
          parser.nextTag();
          parser.require(EventType.END_ELEMENT, NS_PROCESSMODELS, TAG_HPROCESSINSTANCE);

          final ContentValues cv = new ContentValues(2);
          cv.put(ProcessInstances.COLUMN_HANDLE, handle);
          cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_UPTODATE);
          final ContentValuesProvider values = new SimpleContentValuesProvider(cv, false);
          ++syncResult.stats.numUpdates;
          return values;
        } finally {
          input.close();
        }
      } else if (status==404) {
        ContentValues cv = new ContentValues(1);
        cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          LogUtil.logResponse(TAG, Log.DEBUG, "Nonexisting process model: "+url.toString(), urlConnection.getResponseMessage(), urlConnection.getErrorStream());
        }
        ++syncResult.stats.numSkippedEntries;
        return new SimpleContentValuesProvider(cv, false);
      } else {
        final String statusline = Integer.toString(urlConnection.getResponseCode()) + urlConnection.getResponseMessage();
        //noinspection WrongConstant
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
  public ContentValuesProvider deleteItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri,
                                                  final SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    long handle;
    {
      final Cursor itemCursor = provider.query(itemuri, new String[]{ ProcessModels.COLUMN_HANDLE }, null, null, null);
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

    final URI uri =delegator.getSyncSource().resolve("processInstances/" + handle);
    final DeleteRequest request = new DeleteRequest(uri);
    final HttpURLConnection urlConnection = delegator.getWebClient().execute(delegator.getContext(), request);
    try {
      final int status = urlConnection.getResponseCode();
      if (status >= 200 && status < 400) {
        final CharArrayWriter out = new CharArrayWriter();
        final InputStream ins = urlConnection.getInputStream();
        final Reader in = new InputStreamReader(ins, Charset.forName("UTF-8"));
        final char[] buffer = new char[2048];
        int cnt;
        while ((cnt = in.read(buffer)) >= 0) {
          out.write(buffer, 0, cnt);
        }
        Log.i(TAG, "Response on deleting item: \"" + out.toString() + "\"");

        final ContentValues cv = new ContentValues(1);
        cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
        return new SimpleContentValuesProvider(cv, false);
      }
    } finally {
      urlConnection.disconnect();
    }
    return null;
  }

  @Override
  public boolean resolvePotentialConflict(final ContentProviderClient provider, final Uri uri, final ContentValuesProvider item) throws RemoteException {
    // Server always wins
    return true;
  }

  @Override
  public ContentValuesProvider parseItem(final XmlReader in) throws XmlException, IOException {
    in.require(EventType.START_ELEMENT, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    final CharSequence name = in.getAttributeValue(null, "name");
    final long handle;
    final long pmhandle;
    final String uuid;
    try {
      handle = Long.parseLong(in.getAttributeValue(null, "handle").toString());
      pmhandle = Long.parseLong(in.getAttributeValue(null, "processModel").toString());
      uuid = in.getAttributeValue(null, "uuid").toString();
    } catch (NullPointerException|NumberFormatException e) {
      throw new XmlException(e.getMessage(), in, e);
    }
    in.next();
    in.require(EventType.END_ELEMENT, NS_PROCESSMODELS, TAG_PROCESSINSTANCE);
    final ContentValues result = new ContentValues(4);
    result.put(ProcessInstances.COLUMN_HANDLE, handle);
    result.put(ProcessInstances.COLUMN_PMHANDLE, pmhandle);
    result.put(ProcessInstances.COLUMN_NAME, name.toString());
    result.put(ProcessInstances.COLUMN_UUID, uuid);
    result.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE); // No details at this stage
    return new SimpleContentValuesProvider(result, false);
  }

  @Override
  public Collection<ContentProviderOperation> doUpdateItemDetails(final DelegatingResources delegator, final ContentProviderClient provider, final long id, final CVPair pair) throws RemoteException, IOException {
    return Collections.emptyList();
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