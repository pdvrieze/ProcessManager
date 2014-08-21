package nl.adaptivity.process.tasks.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import javax.xml.XMLConstants;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import net.devrieze.util.StringUtil;

import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapterDelegate.DelegatingResources;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import static nl.adaptivity.process.tasks.UserTask.*;

@SuppressWarnings("boxing")
public class TaskSyncAdapter extends RemoteXmlSyncAdapter {


  private static class TaskCVProvider implements ContentValuesProvider {

    private final ContentValues mContentValues;
    final List<GenericItem> mItems;

    public TaskCVProvider(ContentValues pContentValues, List<GenericItem> pItems) {
      mContentValues = pContentValues;
      mItems = pItems;
    }

    @Override
    public ContentValues getContentValues() {
      return mContentValues;
    }

  }

  private static final String TAG = TaskSyncAdapter.class.getSimpleName();
  private String mBase;

  public TaskSyncAdapter(Context pContext) {
    super(pContext, true, false, Tasks.CONTENT_ID_URI_BASE);
  }

  @Override
  public ContentValuesProvider updateItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri,
                                                  int pSyncState, SyncResult pSyncresult) throws RemoteException, IOException,
      XmlPullParserException {
    UserTask task = TaskProvider.getTask(getContext(), pItemuri);
    HttpPost request;
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
      request = new HttpPost(getListUrl(mBase)+'/'+task.getHandle());

      request.setEntity(new StringEntity(writer.toString(), "UTF-8"));
    } else {
      request = new HttpPost(getListUrl(mBase)+'/'+task.getHandle()+"?state="+task.getState());
    }
    HttpResponse result = pDelegator.getWebClient().execute(request);
    int resultCode = result.getStatusLine().getStatusCode();
    if (resultCode>=200 && resultCode<400) {
      XmlPullParser parser = factory.newPullParser();
      final InputStream inputStream = result.getEntity().getContent();
      final Header contentEncoding = result.getEntity().getContentEncoding();
      parser.setInput(inputStream, contentEncoding==null ? null : contentEncoding.getValue());
      try {
        parser.nextTag(); // Make sure to forward the task.
        return parseItem(parser);
      } finally {
        inputStream.close();
      }
    } else {
      throw new IOException("Update request returned an unexpected response: "+resultCode+" "+result.getStatusLine().getReasonPhrase());
    }
  }

  @Override
  public ContentValuesProvider createItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri,
                                                  SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentValuesProvider deleteItemOnServer(DelegatingResources pDelegator, ContentProviderClient pProvider, Uri pItemuri,
                                                  SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException {
    // TODO Do more than take the server state
    return true;
  }

  @Override
  public boolean doUpdateItemDetails(DelegatingResources pDelegator, ContentProviderClient pProvider, long pTaskId, CVPair pPair) throws RemoteException, OperationApplicationException, IOException {
    ArrayList<ContentProviderOperation> batch = new ArrayList<>();
    // TODO support transactions
    boolean updated = false;
    if (pPair==null) {
      return false;
    }
    List<GenericItem> items = ((TaskCVProvider) pPair.mCV).mItems;

//    Cursor localItems = pProvider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID, new String[] { Long.toString(pTaskId) }, BaseColumns._ID);
    Cursor localItems = pProvider.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID+"="+Long.toString(pTaskId), null, BaseColumns._ID);
    int nameColIdx = localItems.getColumnIndex(Items.COLUMN_NAME);
    int idColIdx = localItems.getColumnIndex(BaseColumns._ID);
    int labelColIdx = localItems.getColumnIndex(Items.COLUMN_LABEL);
    int typeColIdx = localItems.getColumnIndex(Items.COLUMN_TYPE);
    int valueColIdx = localItems.getColumnIndex(Items.COLUMN_VALUE);

    ArrayList<GenericItem> itemcpy = new ArrayList<>(items);

    final Uri itemsUpdateUri = Items.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build();
    ListIterator<GenericItem> remoteIterator = itemcpy.listIterator();
    updateloop: while(localItems.moveToNext()) {
      String localName = localItems.getString(nameColIdx);
      long localItemId = localItems.getLong(idColIdx);
      String localType = localItems.getString(typeColIdx);
      String localValue = localItems.getString(valueColIdx);
      String localLabel = localItems.getString(labelColIdx);

      if (remoteIterator.hasNext()) {
        GenericItem remoteItem = remoteIterator.next();
        if (StringUtil.isEqual(localName,remoteItem.getName())) {
          remoteIterator.remove();
          ContentValues cv = new ContentValues(2);
          if (!StringUtil.isEqual(remoteItem.getDBType(),localType)) {
            cv.put(Items.COLUMN_TYPE, remoteItem.getDBType());
          }
          if (!StringUtil.isEqual(remoteItem.getValue(),localValue)){
            cv.put(Items.COLUMN_VALUE, remoteItem.getValue());
          }
          if (!StringUtil.isEqual(remoteItem.getLabel(),localLabel)){
            cv.put(Items.COLUMN_LABEL, remoteItem.getLabel());
          }
          if (cv.size()>0) {
            updated=true;
            batch.add(ContentProviderOperation
                .newUpdate(ContentUris.withAppendedId(itemsUpdateUri,localItemId))
                .withValues(cv)
                .build());
          }
          batch.addAll(updateOptionValues(remoteItem, pProvider, localItemId));
          continue updateloop;
        } else { // not equal, we need to maintain order, so delete already.
          // not found from server, delete
          batch.addAll(deleteItem(itemsUpdateUri, localItemId));

          remoteIterator.previous(); // Move back so that the next local item may match the remote one.
        }

      }

    } // finished all matches

    // These remote items need to be added locally
    for(GenericItem remoteItem:itemcpy) {
      ContentValues itemCv = new ContentValues(4);
      itemCv.put(Items.COLUMN_TASKID, pTaskId);
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
        .newUpdate(Tasks.CONTENT_ID_URI_BASE.buildUpon().appendEncodedPath(Long.toString(pTaskId)).encodedFragment("nonetnotify").build())
        .withValue(XmlBaseColumns.COLUMN_SYNCSTATE, SYNC_UPTODATE)
        .build());
    pProvider.applyBatch(batch);
    return updated;
  }

  private static void addOptionsToBatch(List<ContentProviderOperation> pBatch, int pPreviousResult, List<String> pOptions) {
    for(String option:pOptions) {
      pBatch.add(ContentProviderOperation
          .newInsert(Options.CONTENT_ID_URI_BASE)
          .withValueBackReference(Options.COLUMN_ITEMID, pPreviousResult)
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

  private static Collection<? extends ContentProviderOperation> updateOptionValues(GenericItem pRemoteItem, ContentProviderClient pProvider, long pLocalItemId) throws RemoteException {
    ArrayList<ContentProviderOperation> result = new ArrayList<>();
    ArrayList<String> options = new ArrayList<>(pRemoteItem.getOptions());
    Cursor localItems = pProvider.query(Options.CONTENT_ID_URI_BASE, new String[]{ BaseColumns._ID, Options.COLUMN_VALUE }, Options.COLUMN_ITEMID+"="+Long.toString(pLocalItemId), null, BaseColumns._ID);
    ListIterator<String> remoteIt = options.listIterator();
    outer: while (localItems.moveToNext()) {
      long localId = localItems.getLong(0);
      String localOption = localItems.getString(1);
      if(remoteIt.hasNext()) {
        String remoteOption = remoteIt.next();
        if (StringUtil.isEqual(remoteOption,localOption)) {
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
    for(String option: options) {
      if (option!=null) {
        result.add(ContentProviderOperation
            .newInsert(Options.CONTENT_ID_URI_BASE.buildUpon().encodedFragment("nonetnotify").build())
            .withValue(Options.COLUMN_ITEMID, pLocalItemId)
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
  public ContentValuesProvider parseItem(XmlPullParser pIn) throws XmlPullParserException, IOException {

    pIn.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASK);
    String summary = pIn.getAttributeValue(null, "summary");
    long handle = Long.parseLong(pIn.getAttributeValue(null, "handle"));
    String owner = pIn.getAttributeValue(null, "owner");
    String state = pIn.getAttributeValue(null, "state");
    boolean hasItems = false;
    List<GenericItem> items = new ArrayList<>();
    while ((pIn.nextTag())==XmlPullParser.START_TAG) {
      pIn.require(XmlPullParser.START_TAG, NS_TASKS, UserTask.TAG_ITEM);
      items.add(TaskItem.parseTaskGenericItem(pIn));
      pIn.require(XmlPullParser.END_TAG, NS_TASKS, UserTask.TAG_ITEM);
      hasItems = true;
    }

    ContentValues result = new ContentValues(6);
    pIn.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASK);
    result.put(Tasks.COLUMN_SYNCSTATE, hasItems ? SYNC_DETAILSPENDING : SYNC_UPTODATE);
    result.put(Tasks.COLUMN_HANDLE, handle);
    result.put(Tasks.COLUMN_SUMMARY, summary);
    result.put(Tasks.COLUMN_OWNER, owner);
    result.put(Tasks.COLUMN_STATE, state);
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
  public String getListUrl(String pBase) {
    return pBase+"pendingTasks";
  }

  @Override
  public String getSyncSource() {
    if (mBase==null) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
      String prefBase = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/PEUserMessageHandler/UserMessageService");
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
      mBase = prefBase+"PEUserMessageHandler/UserMessageService/";
    }
    return mBase;
  }


}