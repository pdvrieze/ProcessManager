package nl.adaptivity.process.tasks.data;

import static nl.adaptivity.process.tasks.UserTask.NS_TASKS;
import static nl.adaptivity.process.tasks.UserTask.TAG_TASK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.devrieze.util.StringUtil;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.UserTask.TaskItem;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
    final List<TaskItem> mItems;

    public TaskCVProvider(ContentValues pContentValues, List<TaskItem> pItems) {
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
//
//  @Override
//  public void onPerformSync(Account pAccount, Bundle pExtras, String pAuthority, ContentProviderClient pProvider, SyncResult pSyncResult) {
//    try {
//      mXpf = XmlPullParserFactory.newInstance();
//    } catch (XmlPullParserException e1) {
//      pSyncResult.stats.numParseExceptions++;
//      return;
//    }
//    mXpf.setNamespaceAware(true);
//
//    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//    mBase = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/PEUserMessageHandler/UserMessageService/");
//    if (! mBase.endsWith("/")) {mBase = mBase+'/'; }
//    {
//      String authbase = AuthenticatedWebClient.getAuthBase(mBase);
//      mHttpClient = new AuthenticatedWebClient(getContext(), authbase);
//    }
//    HttpGet getProcessesRequest = new HttpGet(mBase+"pendingTasks");
//    HttpResponse result;
//    try {
//      result = mHttpClient.execute(getProcessesRequest);
//    } catch (IOException e) {
//      pSyncResult.stats.numIoExceptions++;
//      return;
//    }
//    if (result!=null) {
//      final int statusCode = result.getStatusLine().getStatusCode();
//      if (statusCode>=200 && statusCode<400) {
//        try {
//          updateTaskList(pProvider, pSyncResult, result.getEntity().getContent());
//        } catch (IllegalStateException|XmlPullParserException e) {
//          pSyncResult.stats.numParseExceptions++;
//          Log.e(TAG, "Error parsing process model list", e);
//        } catch (IOException e) {
//          pSyncResult.stats.numIoExceptions++;
//          Log.e(TAG, "Error parsing process model list", e);
//        } catch (RemoteException e) {
//          pSyncResult.databaseError=true;
//          Log.e(TAG, "Error parsing process model list", e);
//        }
//      } else {
//        pSyncResult.stats.numIoExceptions++;
//      }
//    } else {
//      pSyncResult.stats.numAuthExceptions++;
//    }
//  }

  @Override
  protected ContentValuesProvider updateItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri, SyncResult pSyncresult)
      throws RemoteException, IOException, XmlPullParserException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ContentValuesProvider createItemOnServer(ContentProviderClient pProvider, AuthenticatedWebClient pHttpClient, Uri pItemuri,
                                                     SyncResult pSyncresult) throws RemoteException, IOException, XmlPullParserException {
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
    List<TaskItem> items = ((TaskCVProvider) pPair.mCV).mItems;
    ListIterator<TaskItem> itemIterator = items.listIterator();

    final Uri itemsUri = ContentUris.withAppendedId(Items.CONTENT_ID_URI_BASE,  pTaskId);
    Cursor localItems = pProvider.query(itemsUri, null, null, null, BaseColumns._ID);
    int nameColIdx = localItems.getColumnIndex(Items.COLUMN_NAME);
    int idColIdx = localItems.getColumnIndex(Items._ID);
    int typeColIdx = localItems.getColumnIndex(Items._ID);
    int valueColIdx = localItems.getColumnIndex(Items._ID);
    long deleteMinId = 0;
    updateloop: while(localItems.moveToNext() && itemIterator.hasNext()) {
      TaskItem remoteItem = itemIterator.next();
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
      if (!StringUtil.isEqual(remoteItem.getType(),localType)) {
        cv.put(Items.COLUMN_TYPE, remoteItem.getType());
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
      TaskItem remoteItem = itemIterator.next();
      ContentValues itemCv = new ContentValues(4);
      itemCv.put(Items.COLUMN_TASKID, pTaskId);
      itemCv.put(Items.COLUMN_NAME, remoteItem.getName());
      if (remoteItem.getType()!=null) { itemCv.put(Items.COLUMN_TYPE, remoteItem.getType()); }
      if (remoteItem.getValue()!=null) { itemCv.put(Items.COLUMN_VALUE, remoteItem.getValue()); }
      long taskItemId = ContentUris.parseId(pProvider.insert(itemsUri, itemCv));
      Uri optionsUri = ContentUris.withAppendedId(Options.CONTENT_ID_URI_BASE, taskItemId);
      ContentValues[] cvs = getContentValuesForTaskOptions(remoteItem, taskItemId);
      updated=true;
      pProvider.bulkInsert(optionsUri, cvs);
    }
    return updated;
  }
  private ContentValues[] getContentValuesForTaskOptions(TaskItem remoteItem, long localItemId) {
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
    List<TaskItem> items = new ArrayList<>();
    while ((pIn.nextTag())==XmlPullParser.START_TAG) {
      pIn.require(XmlPullParser.START_TAG, NS_TASKS, UserTask.TAG_ITEM);
      items.add(UserTask.parseTaskItem(pIn));
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
      mBase = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, "https://darwin.bournemouth.ac.uk/PEUserMessageHandler/UserMessageService/");
      if (! mBase.endsWith("/")) {mBase = mBase+'/'; }
    }
    return mBase;
  }


}