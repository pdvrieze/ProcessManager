package nl.adaptivity.process.tasks.data;

import static nl.adaptivity.process.tasks.UserTask.NS_TASKS;
import static nl.adaptivity.process.tasks.UserTask.TAG_TASK;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.devrieze.util.StringUtil;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.tasks.items.GenericItem;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

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
import android.provider.BaseColumns;

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
  protected ContentValuesProvider updateItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, int pSyncState, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException {
    UserTask task = TaskProvider.getTask(getContext(), pItemuri);
    HttpPost request;
    final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    if (! task.getItems().isEmpty()) {
      XmlSerializer serializer = factory.newSerializer();
      StringWriter writer = new StringWriter(0x100);
      serializer.setOutput(writer);
      serializer.startTag(NS_TASKS, TAG_TASK);
      serializer.attribute(null, "state", task.getState());
      for(TaskItem item: task.getItems()) {
        if (! item.isReadOnly()) {
          item.serialize(serializer, false);
        }
      }
      request = new HttpPost(getListUrl(mBase)+'/'+task.getHandle());
      request.setEntity(new StringEntity(writer.toString(), "UTF-8"));
    } else {
      request = new HttpPost(getListUrl(mBase)+'/'+task.getHandle()+"?state="+task.getState());
    }
    HttpResponse result = pHttpClient.execute(request);
    int resultCode = result.getStatusLine().getStatusCode();
    if (resultCode>=200 && resultCode<400) {
      XmlPullParser parser = factory.newPullParser();
      final InputStream inputStream = result.getEntity().getContent();
      parser.setInput(inputStream, result.getEntity().getContentEncoding().getValue());
      try {
        return parseItem(parser);
      } finally {
        inputStream.close();
      }
    } else {
      throw new IOException("Update request returned an unexpected response: "+resultCode+" "+result.getStatusLine().getReasonPhrase());
    }
  }

  @Override
  protected ContentValuesProvider createItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ContentValuesProvider deleteItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri,
                                                     SyncResult pSyncResult) throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean resolvePotentialConflict(ContentProviderClient pProvider, Uri pUri, ContentValuesProvider pItem) throws RemoteException {
    // TODO Do more than take the server state
    return true;
  }

  @Override
  protected boolean doUpdateItemDetails(AuthenticatedWebClient pHttpClient, ContentProviderClient pProvider, long pTaskId, CVPair pPair) throws RemoteException, IOException {
    // TODO support transactions
    boolean updated = false;
    if (pPair==null) {
      return false;
    }
    List<GenericItem> items = ((TaskCVProvider) pPair.mCV).mItems;
    ListIterator<GenericItem> itemIterator = items.listIterator();

    final Uri itemsUri = ContentUris.withAppendedId(Items.CONTENT_ID_URI_BASE,  pTaskId);
    Cursor localItems = pProvider.query(itemsUri, null, null, null, BaseColumns._ID);
    int nameColIdx = localItems.getColumnIndex(Items.COLUMN_NAME);
    int idColIdx = localItems.getColumnIndex(Items._ID);
    int labelColIdx = localItems.getColumnIndex(Items.COLUMN_LABEL);
    int typeColIdx = localItems.getColumnIndex(Items.COLUMN_TYPE);
    int valueColIdx = localItems.getColumnIndex(Items.COLUMN_VALUE);
    long deleteMinId = 0;
    updateloop: while(localItems.moveToNext() && itemIterator.hasNext()) {
      GenericItem remoteItem = itemIterator.next();
      String localName = localItems.getString(nameColIdx);
      long localItemId = localItems.getLong(idColIdx);
      long deleteMaxId=0;
      while (!remoteItem.getName().equals(localName)) {
        deleteMaxId = localItemId;
        if(localItems.moveToNext()) {
          localName = localItems.getString(nameColIdx);
          localItemId = localItems.getLong(idColIdx);
        } else {
          break updateloop;
        }
      }
      if (deleteMaxId>0) {
        updated=true;
        pProvider.delete(itemsUri, Items._ID+" > ? AND "+Items._ID+" <= ?", new String[] {Long.toString(deleteMinId), Long.toString(deleteMaxId)});
      }
      deleteMinId=localItemId;
      String localType = localItems.getString(typeColIdx);
      String localValue = localItems.getString(valueColIdx);
      ContentValues cv = new ContentValues(2);
      if (!StringUtil.isEqual(remoteItem.getDBType(),localType)) {
        cv.put(Items.COLUMN_TYPE, remoteItem.getDBType());
      }
      if (remoteItem.getValue()!=null && (! remoteItem.getValue().equals(localValue))){
        cv.put(Items.COLUMN_VALUE, remoteItem.getValue());
      }
      if (cv.size()>0) {
        updated=true;
        pProvider.update(itemsUri, cv, BaseColumns._ID+" = ? ", new String[] {Long.toString(localItemId)} );
      }
      List<String> localOptions = new ArrayList<>();
      Uri optionsUri = ContentUris.withAppendedId(Options.CONTENT_ID_URI_BASE, localItemId);
      Cursor cursor = pProvider.query(optionsUri, new String[] {Options.COLUMN_VALUE}, null, null, null);
      try {
        while (cursor.moveToNext()) {
          localOptions.add(cursor.getString(0));
        }
      } finally {
        cursor.close();
      }
      if (! remoteItem.getOptions().equals(localOptions)) {
        pProvider.delete(optionsUri, null, null);
        ContentValues[] cvs = getContentValuesForTaskOptions(remoteItem, localItemId);
        pProvider.bulkInsert(optionsUri, cvs);
      }
    } // finished all matches
    // Delete items present locally but not remotely
    if (! localItems.isAfterLast()) {
      updated=true;
      pProvider.delete(itemsUri, Items._ID+" > ?", new String[] {Long.toString(deleteMinId)});
    }
    while(itemIterator.hasNext()) {
      GenericItem remoteItem = itemIterator.next();
      ContentValues itemCv = new ContentValues(4);
      itemCv.put(Items.COLUMN_TASKID, pTaskId);
      itemCv.put(Items.COLUMN_NAME, remoteItem.getName());
      if (remoteItem.getType()!=null) { itemCv.put(Items.COLUMN_TYPE, remoteItem.getDBType()); }
      if (remoteItem.getValue()!=null) { itemCv.put(Items.COLUMN_VALUE, remoteItem.getValue()); }
      if (remoteItem.getLabel()!=null) { itemCv.put(Items.COLUMN_LABEL, remoteItem.getLabel()); }
      long taskItemId = ContentUris.parseId(pProvider.insert(itemsUri, itemCv));
      Uri optionsUri = ContentUris.withAppendedId(Options.CONTENT_ID_URI_BASE, taskItemId);
      ContentValues[] cvs = getContentValuesForTaskOptions(remoteItem, taskItemId);
      updated=true;
      pProvider.bulkInsert(optionsUri, cvs);
    }
    return updated;
  }

  private ContentValues[] getContentValuesForTaskOptions(GenericItem remoteItem, long localItemId) {
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
  protected ContentValuesProvider parseItem(XmlPullParser pIn) throws XmlPullParserException, IOException {

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
  protected String getKeyColumn() {
    return Tasks.COLUMN_HANDLE;
  }

  @Override
  protected String getSyncStateColumn() {
    return Tasks.COLUMN_SYNCSTATE;
  }

  @Override
  protected String getItemNamespace() {
    return UserTask.NS_TASKS;
  }

  @Override
  protected String getItemsTag() {
    return UserTask.TAG_TASKS;
  }

  @Override
  protected String getListUrl(String pBase) {
    return pBase+"pendingTasks";
  }

  @Override
  protected String getSyncSource() {
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