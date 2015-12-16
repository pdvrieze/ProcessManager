package nl.adaptivity.process.tasks.data;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.darwin.AuthenticatedWebClient.PostRequest;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.XMLConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

import static nl.adaptivity.process.tasks.UserTask.NS_TASKS;
import static nl.adaptivity.process.tasks.UserTask.TAG_TASK;


@SuppressWarnings("boxing")
public class TaskSyncAdapter extends RemoteXmlSyncAdapter {

  private static class TaskCVProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;
    final List<GenericItem> mItems;

    public TaskCVProvider(ContentValues contentValues, List<GenericItem> items) {
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

  public TaskSyncAdapter(Context context) {
    super(context, true, false, Tasks.CONTENT_ID_URI_BASE);
  }

  @Override
  public ContentValuesProvider updateItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri,
                                                  int syncState, SyncResult syncresult) throws RemoteException, IOException,
      XmlPullParserException {
    UserTask task = TaskProvider.getTask(delegator.getContext(), itemuri);
    PostRequest request;
    final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    if (! task.getItems().isEmpty()) {
      XmlSerializer serializer = factory.newSerializer();
      StringWriter writer = new StringWriter(0x100);
      serializer.setOutput(writer);
      serializer.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, NS_TASKS);
      serializer.startTag(NS_TASKS, TAG_TASK);
      serializer.attribute(null, "state", task.getState());
      for(TaskItem item: task.getItems()) {
        if (! item.isReadOnly()) {
          item.serialize(serializer, false);
        }
      }
      serializer.endTag(NS_TASKS, TAG_TASK);
      serializer.flush();
      request = new PostRequest(getListUrl(mBase).resolve(Long.toString(task.getHandle())), writer.toString());
    } else {
      request = new PostRequest(getListUrl(mBase).resolve(Long.toString(task.getHandle())+"?state="+task.getState()),"");
    }
    request.setContentType("text/xml; charset=utf-8");
    HttpURLConnection result = delegator.getWebClient().execute(request);
    try {
      int resultCode = result.getResponseCode();
      if (resultCode >= 200 && resultCode < 400) {
        XmlPullParser parser = factory.newPullParser();
        final InputStream inputStream = result.getInputStream();
        final String contentEncoding = result.getContentEncoding();
        parser.setInput(inputStream, contentEncoding);
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
  public ContentValuesProvider createItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri,
                                                  SyncResult syncresult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentValuesProvider deleteItemOnServer(DelegatingResources delegator, ContentProviderClient provider, Uri itemuri,
                                                  SyncResult syncResult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean resolvePotentialConflict(ContentProviderClient provider, Uri uri, ContentValuesProvider item) throws RemoteException {
    // TODO Do more than take the server state
    return true;
  }

  @Override
  public Collection<ContentProviderOperation> doUpdateItemDetails(DelegatingResources delegator, ContentProviderClient provider, long taskId, CVPair pair) throws RemoteException, OperationApplicationException, IOException {
    ArrayList<ContentProviderOperation> batch = new ArrayList<>();
    // TODO support transactions
    boolean updated = false;
    if (pair==null) {
      return Collections.emptyList();
    }
    List<GenericItem> items = ((TaskCVProvider) pair.mCV).mItems;

    ArrayList<GenericItem> itemcpy = new ArrayList<>(items);

//    Cursor localItems = pProvider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID, new String[] { Long.toString(pTaskId) }, BaseColumns._ID);
    final Uri itemsUpdateUri = Items.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build();
    Cursor localItems = provider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID+"="+Long.toString(taskId), null, BaseColumns._ID);
    try {
      int nameColIdx = localItems.getColumnIndex(Items.COLUMN_NAME);
      int idColIdx = localItems.getColumnIndex(BaseColumns._ID);
      int labelColIdx = localItems.getColumnIndex(Items.COLUMN_LABEL);
      int typeColIdx = localItems.getColumnIndex(Items.COLUMN_TYPE);
      int valueColIdx = localItems.getColumnIndex(Items.COLUMN_VALUE);

      ListIterator<GenericItem> remoteIterator = itemcpy.listIterator();
      updateloop: while(localItems.moveToNext()) {
        String localName = localItems.getString(nameColIdx);
        long localItemId = localItems.getLong(idColIdx);
        String localType = localItems.getString(typeColIdx);
        String localValue = localItems.getString(valueColIdx);
        String localLabel = localItems.getString(labelColIdx);

        if (remoteIterator.hasNext()) {
          GenericItem remoteItem = remoteIterator.next();
          if (StringUtil.isEqual(localName, remoteItem.getName())) {
            remoteIterator.remove();
            ContentValues cv = new ContentValues(2);
            if (!StringUtil.isEqual(remoteItem.getDBType(), localType)) {
              cv.put(Items.COLUMN_TYPE, remoteItem.getDBType());
            }
            if (!StringUtil.isEqual(remoteItem.getValue(), localValue)) {
              cv.put(Items.COLUMN_VALUE, remoteItem.getValue());
            }
            if (!StringUtil.isEqual(remoteItem.getLabel(), localLabel)) {
              cv.put(Items.COLUMN_LABEL, remoteItem.getLabel());
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
    for(GenericItem remoteItem:itemcpy) {
      ContentValues itemCv = new ContentValues(4);
      itemCv.put(Items.COLUMN_TASKID, taskId);
      itemCv.put(Items.COLUMN_NAME, remoteItem.getName());
      if (remoteItem.getType()!=null) { itemCv.put(Items.COLUMN_TYPE, remoteItem.getDBType()); }
      if (remoteItem.getValue()!=null) { itemCv.put(Items.COLUMN_VALUE, remoteItem.getValue()); }
      if (remoteItem.getLabel()!=null) { itemCv.put(Items.COLUMN_LABEL, remoteItem.getLabel()); }
      int rowitemid = batch.size();
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

  private static void addOptionsToBatch(List<ContentProviderOperation> batch, int previousResult, List<String> options) {
    for(String option:options) {
      batch.add(ContentProviderOperation
          .newInsert(Options.CONTENT_ID_URI_BASE)
          .withValueBackReference(Options.COLUMN_ITEMID, previousResult)
          .withValue(Options.COLUMN_VALUE, option)
          .build());
    }
  }

  private static List<ContentProviderOperation> deleteItem(final Uri itemsUri, long localItemId) {
    return Arrays.asList(ContentProviderOperation
        .newDelete(Options.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build())
        .withSelection(Options.COLUMN_ITEMID+"=?", new String[] {Long.toString(localItemId)})
        .build(),
    ContentProviderOperation
        .newDelete(ContentUris.withAppendedId(itemsUri, localItemId))
        .withSelection(BaseColumns._ID+"=?", new String[] {Long.toString(localItemId)})
        .build());
  }

  private static Collection<? extends ContentProviderOperation> updateOptionValues(GenericItem remoteItem, ContentProviderClient provider, long localItemId) throws RemoteException {
    ArrayList<ContentProviderOperation> result = new ArrayList<>();
    ArrayList<String> options = new ArrayList<>(remoteItem.getOptions());
    Cursor localItems = provider.query(Options.CONTENT_ID_URI_BASE, new String[]{ BaseColumns._ID, Options.COLUMN_VALUE }, Options.COLUMN_ITEMID+"="+Long.toString(localItemId), null, BaseColumns._ID);
    try {
      ListIterator<String> remoteIt = options.listIterator();
      outer:
      while (localItems.moveToNext()) {
        long localId = localItems.getLong(0);
        String localOption = localItems.getString(1);
        if (remoteIt.hasNext()) {
          String remoteOption = remoteIt.next();
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
    for(String option: options) {
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

  private static ContentValues[] getContentValuesForTaskOptions(GenericItem remoteItem, long localItemId) {
    ContentValues[] cvs = new ContentValues[remoteItem.getOptions().size()];
    int i=0;
    for(String option: remoteItem.getOptions()) {
      ContentValues cv2 = new ContentValues(2);
      cv2.put(Options.COLUMN_ITEMID, localItemId);
      cv2.put(Options.COLUMN_VALUE, option);
      cvs[i++] = cv2;
    }
    return cvs;
  }

  @Override
  public ContentValuesProvider parseItem(XmlPullParser in) throws XmlPullParserException, IOException {

    in.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASK);
    String summary = in.getAttributeValue(null, "summary");
    long handle = Long.parseLong(in.getAttributeValue(null, "handle"));
    String owner = in.getAttributeValue(null, "owner");
    String state = in.getAttributeValue(null, "state");
    boolean hasItems = false;
    List<GenericItem> items = new ArrayList<>();
    while ((in.nextTag())==XmlPullParser.START_TAG) {
      in.require(XmlPullParser.START_TAG, NS_TASKS, UserTask.TAG_ITEM);
      items.add(TaskItem.parseTaskGenericItem(in));
      in.require(XmlPullParser.END_TAG, NS_TASKS, UserTask.TAG_ITEM);
      hasItems = true;
    }

    ContentValues result = new ContentValues(6);
    in.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASK);
    result.put(Tasks.COLUMN_HANDLE, handle);
    result.put(Tasks.COLUMN_SUMMARY, summary);
    result.put(Tasks.COLUMN_OWNER, owner);
    result.put(Tasks.COLUMN_STATE, state);
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
    return UserTask.NS_TASKS;
  }

  @Override
  public String getItemsTag() {
    return UserTask.TAG_TASKS;
  }

  @Override
  public URI getListUrl(URI base) {
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