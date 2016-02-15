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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.tasks.data;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.PostRequest;
import nl.adaptivity.process.tasks.ExecutableUserTask;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.XMLConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

import static nl.adaptivity.process.tasks.ExecutableUserTask.ELEMENTLOCALNAME;


@SuppressWarnings("boxing")
public class TaskSyncAdapter extends RemoteXmlSyncAdapter {

  private static class TaskCVProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;
    final List<GenericItem> mItems;

    public TaskCVProvider(final ContentValues contentValues, final List<GenericItem> items) {
      mContentValues = contentValues;
      mItems = items;
    }

    @Override
    public ContentValues getContentValues() {
      return mContentValues;
    }

    @Override
    public boolean syncDetails() {
      return mItems.size()>0;
    }
  }

  private static final String TAG = TaskSyncAdapter.class.getSimpleName();
  private URI mBase;

  public TaskSyncAdapter(final Context context) {
    super(context, true, false, Tasks.CONTENT_ID_URI_BASE);
  }

  @Override
  public ContentValuesProvider updateItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri,
                                                  final int syncState, final SyncResult syncresult) throws RemoteException, IOException, XmlException {
    final ExecutableUserTask task = TaskProvider.getTask(delegator.getContext(), itemuri);
    final PostRequest postRequest;
    if (! task.getItems().isEmpty()) {
      final StringWriter writer = new StringWriter(0x100);
      final XmlWriter serializer = XmlStreaming.newWriter(writer);
      serializer.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, Constants.USER_MESSAGE_HANDLER_NS);
      serializer.startTag(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, XMLConstants.DEFAULT_NS_PREFIX);
      serializer.attribute(null, "state", XMLConstants.DEFAULT_NS_PREFIX, task.getState().getAttrValue());
      for(final TaskItem item: task.getItems()) {
        if (! item.isReadOnly()) {
          item.serialize(serializer, false);
        }
      }
      serializer.endTag(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, XMLConstants.DEFAULT_NS_PREFIX);
      serializer.flush();
      postRequest = new PostRequest(getListUrl(mBase).resolve(Long.toString(task.getHandle())), writer.toString());
    } else {
      postRequest = new PostRequest(getListUrl(mBase).resolve(Long.toString(task.getHandle())+"?state="+task.getState()),"");
    }
    postRequest.setContentType("text/xml; charset=utf-8");
    final HttpURLConnection result = delegator.getWebClient().execute(postRequest);
    try {
      final int resultCode = result.getResponseCode();
      if (resultCode >= 200 && resultCode < 400) {
        final InputStream inputStream = result.getInputStream();
        final String contentEncoding = result.getContentEncoding();
        XmlReader parser = XmlStreaming.newReader(inputStream, contentEncoding);
        try {
          parser.nextTag(); // Make sure to forward the task.
          return parseItem(parser); // Always an update
        } finally {
          inputStream.close();
        }
      } else {
        throw new IOException("Update request returned an unexpected response: " + resultCode + " " + result.getResponseMessage());
      }
    } finally {
      result.disconnect();
    }
  }

  @Override
  public ContentValuesProvider createItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri,
                                                  final SyncResult syncresult) throws RemoteException, IOException, XmlException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentValuesProvider deleteItemOnServer(final DelegatingResources delegator, final ContentProviderClient provider, final Uri itemuri,
                                                  final SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean resolvePotentialConflict(final ContentProviderClient provider, final Uri uri, final ContentValuesProvider item) throws RemoteException {
    // TODO Do more than take the server state
    return true;
  }

  @Override
  public Collection<ContentProviderOperation> doUpdateItemDetails(final DelegatingResources delegator, final ContentProviderClient provider, final long taskId, final CVPair pair) throws RemoteException, OperationApplicationException, IOException {
    final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
    // TODO support transactions
    boolean updated = false;
    if (pair==null) {
      return Collections.emptyList();
    }
    final List<GenericItem> items = ((TaskCVProvider) pair.mCV).mItems;

    final ArrayList<GenericItem> itemcpy = new ArrayList<>(items);

//    Cursor localItems = pProvider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID, new String[] { Long.toString(pTaskId) }, BaseColumns._ID);
    final Uri itemsUpdateUri = Items.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build();
    final Cursor localItems = provider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID + "=" + Long.toString(taskId), null, BaseColumns._ID);
    try {
      final int nameColIdx = localItems.getColumnIndex(Items.COLUMN_NAME);
      final int idColIdx = localItems.getColumnIndex(BaseColumns._ID);
      final int labelColIdx = localItems.getColumnIndex(Items.COLUMN_LABEL);
      final int typeColIdx = localItems.getColumnIndex(Items.COLUMN_TYPE);
      final int valueColIdx = localItems.getColumnIndex(Items.COLUMN_VALUE);

      final ListIterator<GenericItem> remoteIterator = itemcpy.listIterator();
      updateloop: while(localItems.moveToNext()) {
        final String localName = localItems.getString(nameColIdx);
        final long localItemId = localItems.getLong(idColIdx);
        final String localType = localItems.getString(typeColIdx);
        final String localValue = localItems.getString(valueColIdx);
        final String localLabel = localItems.getString(labelColIdx);

        if (remoteIterator.hasNext()) {
          final GenericItem remoteItem = remoteIterator.next();
          if (StringUtil.isEqual(localName, remoteItem.getName())) {
            remoteIterator.remove();
            final ContentValues cv = new ContentValues(2);
            if (!StringUtil.isEqual(remoteItem.getDBType(), localType)) {
              cv.put(Items.COLUMN_TYPE, String.valueOf(remoteItem.getDBType()));
            }
            if (!StringUtil.isEqual(remoteItem.getValue(), localValue)) {
              cv.put(Items.COLUMN_VALUE, String.valueOf(remoteItem.getValue()));
            }
            if (!StringUtil.isEqual(remoteItem.getLabel(), localLabel)) {
              cv.put(Items.COLUMN_LABEL, String.valueOf(remoteItem.getLabel()));
            }
            if (cv.size() > 0) {
              updated = true;
              batch.add(ContentProviderOperation
                      .newUpdate(ContentUris.withAppendedId(itemsUpdateUri, localItemId))
                      .withValues(cv)
                      .build());
            }
            batch.addAll(updateOptionValues(remoteItem, provider, localItemId));
            continue updateloop;
          } else { // not equal, we need to maintain order, so delete already.
            // not found from server, delete
            batch.addAll(deleteItem(itemsUpdateUri, localItemId));

            remoteIterator.previous(); // Move back so that the next local item may match the remote one.
          }

        }
      }
    } finally {// finished all matches
      localItems.close();
    }

    // These remote items need to be added locally
    for(final GenericItem remoteItem:itemcpy) {
      final ContentValues itemCv = new ContentValues(4);
      itemCv.put(Items.COLUMN_TASKID, taskId);
      itemCv.put(Items.COLUMN_NAME, String.valueOf(remoteItem.getName()));
      if (remoteItem.getType()!=null) { itemCv.put(Items.COLUMN_TYPE, String.valueOf(remoteItem.getDBType())); }
      if (remoteItem.getValue()!=null) { itemCv.put(Items.COLUMN_VALUE, String.valueOf(remoteItem.getValue())); }
      if (remoteItem.getLabel()!=null) { itemCv.put(Items.COLUMN_LABEL, String.valueOf(remoteItem.getLabel())); }
      final int rowitemid = batch.size();
      batch.add(ContentProviderOperation
          .newInsert(itemsUpdateUri)
          .withValues(itemCv)
          .build());
      addOptionsToBatch(batch, rowitemid, remoteItem.getOptions());
    }
    batch.add(ContentProviderOperation
        .newUpdate(Tasks.CONTENT_ID_URI_BASE.buildUpon().appendEncodedPath(Long.toString(taskId)).encodedFragment("nonetnotify").build())
        .withValue(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE)
        .build());
    return batch;
  }

  private static void addOptionsToBatch(final List<ContentProviderOperation> batch, final int previousResult, final List<? extends CharSequence> options) {
    for(final CharSequence option:options) {
      batch.add(ContentProviderOperation
          .newInsert(Options.CONTENT_ID_URI_BASE)
          .withValueBackReference(Options.COLUMN_ITEMID, previousResult)
          .withValue(Options.COLUMN_VALUE, option)
          .build());
    }
  }

  private static List<ContentProviderOperation> deleteItem(final Uri itemsUri, final long localItemId) {
    return Arrays.asList(ContentProviderOperation
        .newDelete(Options.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build())
        .withSelection(Options.COLUMN_ITEMID+"=?", new String[] {Long.toString(localItemId)})
        .build(),
    ContentProviderOperation
        .newDelete(ContentUris.withAppendedId(itemsUri, localItemId))
        .withSelection(BaseColumns._ID+"=?", new String[] {Long.toString(localItemId)})
        .build());
  }

  private static Collection<? extends ContentProviderOperation> updateOptionValues(final GenericItem remoteItem, final ContentProviderClient provider, final long localItemId) throws RemoteException {
    final ArrayList<ContentProviderOperation> result = new ArrayList<>();
    final ArrayList<? extends CharSequence> options = new ArrayList<>(remoteItem.getOptions());
    final Cursor localItems = provider.query(Options.CONTENT_ID_URI_BASE, new String[]{ BaseColumns._ID, Options.COLUMN_VALUE }, Options.COLUMN_ITEMID + "=" + Long.toString(localItemId), null, BaseColumns._ID);
    try {
      final ListIterator<? extends CharSequence> remoteIt = options.listIterator();
      outer:
      while (localItems.moveToNext()) {
        final long localId = localItems.getLong(0);
        final String localOption = localItems.getString(1);
        if (remoteIt.hasNext()) {
          final CharSequence remoteOption = remoteIt.next();
          if (StringUtil.isEqual(remoteOption, localOption)) {
            remoteIt.remove();
            continue outer;
          } else {
            result.add(ContentProviderOperation
                               .newDelete(Options.CONTENT_ID_URI_BASE
                                                  .buildUpon()
                                                  .appendEncodedPath(Long.toString(localId))
                                                  .encodedFragment("nonetnotify")
                                                  .build())
                               .build());
            remoteIt.previous();
          }
        }
      }
    } finally {
      localItems.close();
    }
    for(final CharSequence option: options) {
      if (option!=null) {
        result.add(ContentProviderOperation
            .newInsert(Options.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build())
            .withValue(Options.COLUMN_ITEMID, localItemId)
            .withValue(Options.COLUMN_VALUE, option)
            .build());
      }
    }
    return result;
  }

  private static ContentValues[] getContentValuesForTaskOptions(final GenericItem remoteItem, final long localItemId) {
    final ContentValues[] cvs = new ContentValues[remoteItem.getOptions().size()];
    int i=0;
    for(final CharSequence option: remoteItem.getOptions()) {
      final ContentValues cv2 = new ContentValues(2);
      cv2.put(Options.COLUMN_ITEMID, localItemId);
      cv2.put(Options.COLUMN_VALUE, String.valueOf(option));
      cvs[i++] = cv2;
    }
    return cvs;
  }

  @Override
  public ContentValuesProvider parseItem(final XmlReader in) throws XmlException, IOException {

    in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME);
    final String summary = StringUtil.toString(in.getAttributeValue(null, "summary"));
    final long handle = Long.parseLong(StringUtil.toString(in.getAttributeValue(null, "handle")));
    final long instHandle = Long.parseLong(StringUtil.toString(in.getAttributeValue(null, "instancehandle")));
    final String owner = StringUtil.toString(in.getAttributeValue(null, "owner"));
    final String state = StringUtil.toString(in.getAttributeValue(null, "state"));
    boolean hasItems = false;
    final List<GenericItem> items = new ArrayList<>();
    while ((in.nextTag())==EventType.START_ELEMENT) {
      in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, ExecutableUserTask.TAG_ITEM);
      try {
        items.add(TaskItem.parseTaskGenericItem(in));
      } catch (XmlException e) {
        if (e.getCause() instanceof XmlException) {
          throw (XmlException) e.getCause();
        }
        throw new XmlException(e.getMessage(), in, e);
      }
      in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, ExecutableUserTask.TAG_ITEM);
      hasItems = true;
    }

    final ContentValues result = new ContentValues(6);
    in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME);
    result.put(Tasks.COLUMN_HANDLE, handle);
    result.put(Tasks.COLUMN_SUMMARY, summary);
    result.put(Tasks.COLUMN_OWNER, owner);
    result.put(Tasks.COLUMN_STATE, state);
    result.put(Tasks.COLUMN_INSTANCEHANDLE, instHandle);
    result.put(Tasks.COLUMN_SYNCSTATE, SYNC_UPTODATE);
    return new TaskCVProvider(result, items);
  }

  @Override
  public String getKeyColumn() {
    return Tasks.COLUMN_HANDLE;
  }

  @Override
  public String getSyncStateColumn() {
    return Tasks.COLUMN_SYNCSTATE;
  }

  @Override
  public String getItemNamespace() {
    return Constants.USER_MESSAGE_HANDLER_NS;
  }

  @Override
  public String getItemsTag() {
    return ExecutableUserTask.TAG_TASKS;
  }

  @Override
  public URI getListUrl(final URI base) {
    return base.resolve("pendingTasks/");
  }

  @Override
  public URI getSyncSource() {
    if (mBase==null) {
      String prefBase = SettingsActivity.getSyncSource(getContext()).toString();


      if (prefBase.endsWith("/")) {
        if (prefBase.endsWith("ProcessEngine/")) {
          prefBase = prefBase.substring(0, prefBase.length()-14);
        }
      } else {
        if (prefBase.endsWith("ProcessEngine")) {
          prefBase = prefBase.substring(0, prefBase.length()-13);
        } else {
          prefBase = prefBase+"/";
        }
      }
      mBase = URI.create(prefBase+"PEUserMessageHandler/UserMessageService/");
    }
    return mBase;
  }

  @Override
  public String getListSelection() {
    return null;
  }

  @Override
  public String[] getListSelectionArgs() {
    return new String[0];
  }
}