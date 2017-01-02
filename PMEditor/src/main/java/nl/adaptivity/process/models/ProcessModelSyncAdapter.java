/*
 * Copyright (c) 2016.
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

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.DeleteRequest;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.GetRequest;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.PostRequest;
import nl.adaptivity.android.util.LogUtil;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.ISimpleSyncDelegate;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.CVPair;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.ContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.SimpleContentValuesProvider;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static nl.adaptivity.sync.RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER;
import static nl.adaptivity.sync.RemoteXmlSyncAdapter.SYNC_UPTODATE;


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
  public Collection<ContentProviderOperation> doUpdateItemDetails(final DelegatingResources delegator, final ContentProviderClient provider, final long id, final CVPair pair) throws
          RemoteException, IOException {
    long handle;
    if (pair != null && pair.mCV.getContentValues().containsKey(ProcessModels.COLUMN_HANDLE)) {
      handle = pair.mCV.getContentValues().getAsLong(ProcessModels.COLUMN_HANDLE);
    } else {
      final Cursor cursor = provider.query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id), new String[]{ProcessModels.COLUMN_HANDLE}, null, null, null);
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
    final URI uri = getListUrl(delegator.getSyncSource()).resolve(Long.toString(handle));
    final GetRequest request = new GetRequest(uri);
    final HttpURLConnection response = delegator.getWebClient().execute(request);
    if (response==null) {
      throw new IOException("Connection failed");
    }

    final int status = response.getResponseCode();
    try {
      if (status >= 200 && status < 400) {
        final CharArrayWriter out = new CharArrayWriter();
        final Reader in = new InputStreamReader(response.getInputStream(), Charset.forName("UTF-8"));
        try {
          final char[] buffer = new char[2048];
          int cnt;
          while ((cnt = in.read(buffer)) >= 0) {
            out.write(buffer, 0, cnt);
          }
        } finally {
          in.close();
        }
        final ContentValues cv = new ContentValues(2);
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
  public ContentValuesProvider deleteItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri,
                                                  final SyncResult syncResult) throws RemoteException, IOException,
                                                                                      XmlPullParserException {
    long handle;
    {
      final Cursor itemCursor = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE}, null, null, null);
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

    final URI uri = delegator.getSyncSource().resolve("processModels/" + handle);
    final DeleteRequest request = new DeleteRequest(uri);
    final HttpURLConnection response = delegator.getWebClient().execute(request);
    try {
      final int status = response.getResponseCode();
      if (status >= 200 && status < 400) {
        final CharArrayWriter out = new CharArrayWriter();
        final InputStream ins = response.getInputStream();
        try {
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
        } finally {
          ins.close();
        }
      } else if (status==404) {
        // Somehow already deleted on the server
        syncResult.stats.numConflictDetectedExceptions++;
        final ContentValues cv = new ContentValues(1);
        cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_PENDING);
        return new SimpleContentValuesProvider(cv, false);
      }
    } finally {
      response.disconnect();
    }
    return null;
  }

  @Override
  public ContentValuesProvider createItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri, final SyncResult syncResult) throws RemoteException, IOException, XmlException {
    String model;
    {
      final Cursor pendingPosts = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL}, null, null, null);
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
  public ContentValuesProvider updateItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri, final int syncState, final SyncResult syncResult) throws RemoteException, IOException, XmlException {
    String model;
    long handle;
    {
      final Cursor pendingPosts = provider.query(itemuri, new String[]{ProcessModels.COLUMN_MODEL, ProcessModels.COLUMN_HANDLE}, null, null, null);
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

  private ContentValuesProvider postToServer(final DelegatingResources delegator, final URI url, final String model, final SyncResult syncResult) throws
          IOException, XmlException {
    final PostRequest post = new PostRequest(url, model);
    post.setContentType("text/xml; charset=utf-8");
    final HttpURLConnection response = delegator.getWebClient().execute(post);
    try {
      final int status = response.getResponseCode();
      if (status >= 200 && status < 400) {
        final InputStream content = response.getInputStream();
        try {
          final XmlReader parser = XmlStreaming.newReader(content, "UTF-8");

          parser.nextTag(); // Skip document start etc.
          final ContentValuesProvider values = parseItem(parser); // We already have a local model if posting
          return values;
        } finally {
          content.close();
        }

      } else {
        //noinspection WrongConstant
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
  public boolean resolvePotentialConflict(final ContentProviderClient provider, final Uri uri, final ContentValuesProvider item) throws
          RemoteException {
    final ContentValues itemCv = item.getContentValues();
    itemCv.clear();
    itemCv.put(getSyncStateColumn(), Integer.valueOf(SYNC_UPDATE_SERVER));
    return true;
  }

  @Override
  public ContentValuesProvider parseItem(final XmlReader in) throws XmlException, IOException {
    in.require(EventType.START_ELEMENT, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    final CharSequence name = in.getAttributeValue(null, "name");
    final long handle;
    try {
      handle = Long.parseLong(StringUtil.toString(in.getAttributeValue(null, "handle")));
    } catch (NullPointerException | NumberFormatException e) {
      throw new XmlException(e.getMessage(), in, e);
    }
    final UUID uuid = toUUID(in.getAttributeValue(null, "uuid"));
    in.next();
    in.require(EventType.END_ELEMENT, NS_PROCESSMODELS, TAG_PROCESSMODEL);
    final ContentValues result = new ContentValues(4);
    result.put(ProcessModels.COLUMN_HANDLE, handle);
    result.put(ProcessModels.COLUMN_NAME, StringUtil.toString(name));
    result.put(ProcessModels.COLUMN_UUID, uuid.toString());
    result.put(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE); // should be overridden by caller
    return new SimpleContentValuesProvider(result, true);
  }

  private static UUID toUUID(final CharSequence val) {
    return val == null ? null : UUID.fromString(val.toString());
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
  public URI getListUrl(final URI base) {
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