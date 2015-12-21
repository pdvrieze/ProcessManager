package nl.adaptivity.process.tasks.data;

import android.accounts.Account;
import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.android.ProviderHelper;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.UserTask.TaskState;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@SuppressWarnings("static-access")
public class TaskProvider extends ContentProvider {


  private static class ItemCols {

    public final int colId;
    public final int colName;
    public final int colLabel;
    public final int colType;
    public final int colValue;

    public ItemCols(int colId, int colName, int colLabel, int colType, int colValue) {
      this.colId = colId;
      this.colName = colName;
      this.colLabel = colLabel;
      this.colType = colType;
      this.colValue = colValue;
    }

    public static ItemCols init(Cursor itemCursor) {
      int colId = itemCursor.getColumnIndex(Items._ID);
      int colName = itemCursor.getColumnIndex(Items.COLUMN_NAME);
      int colLabel = itemCursor.getColumnIndex(Items.COLUMN_LABEL);
      int colType = itemCursor.getColumnIndex(Items.COLUMN_TYPE);
      int colValue = itemCursor.getColumnIndex(Items.COLUMN_VALUE);
      return new ItemCols(colId, colName, colLabel, colType, colValue);
    }

  }

  public static final String AUTHORITY = "nl.adaptivity.process.tasks";

  public static class Tasks implements BaseColumns {

    private Tasks(){}

    public static final String COLUMN_HANDLE="handle";
    public static final String COLUMN_SUMMARY = "summary";
    public static final String COLUMN_OWNER = "owner";
    public static final String COLUMN_STATE = "state";


    public static final String COLUMN_SYNCSTATE = "syncstate";

    private static final String SCHEME = "content://";

    private static final String PATH_MODELS = "/tasks";
    private static final String PATH_MODEL_ID = PATH_MODELS+'/';
    private static final String PATH_ITEMS = "/items/";
    private static final String PATH_OPTIONS = "/options/";

    public static final Uri CONTENT_URI = Uri.parse(SCHEME+AUTHORITY+PATH_MODELS);

    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_ID);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME+AUTHORITY+PATH_MODEL_ID+'#');

    public static final String[] BASE_PROJECTION = new String[] { BaseColumns._ID, COLUMN_HANDLE, COLUMN_SUMMARY };
    public static final String SELECT_HANDLE = COLUMN_HANDLE+" = ?";

  }

  public static class Items implements BaseColumns {
    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(Tasks.SCHEME+TaskProvider.AUTHORITY+Tasks.PATH_ITEMS);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(Tasks.SCHEME+TaskProvider.AUTHORITY+Tasks.PATH_ITEMS+'#');
    public static final String COLUMN_TASKID = "taskid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LABEL = "label";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_VALUE = "value";
  }

  public static class Options implements BaseColumns {

    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(Tasks.SCHEME+AUTHORITY+Tasks.PATH_OPTIONS);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(Tasks.SCHEME+AUTHORITY+Tasks.PATH_OPTIONS+'#');
    public static final String COLUMN_ITEMID = "itemid";
    public static final String COLUMN_VALUE = "value";

  }

  private enum QueryTarget{
    TASKS(Tasks.CONTENT_URI, TasksOpenHelper.TABLE_NAME_TASKS),
    TASK(Tasks.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_TASKS),
    TASKITEMS(TaskProvider.Items.CONTENT_ID_URI_BASE, TasksOpenHelper.TABLE_NAME_ITEMS),
    TASKITEM(TaskProvider.Items.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_ITEMS),
    TASKOPTIONS(Options.CONTENT_ID_URI_BASE, TasksOpenHelper.TABLE_NAME_OPTIONS),
    TASKOPTION(Options.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_OPTIONS);

    private final String path;
    private final String table;

    QueryTarget(Uri uri, String table) {
      String frag = uri.getFragment();
      if (frag != null) {
        path = uri.getPath() + '#' + frag;
      } else {
        path = uri.getPath();
      }
      this.table = table;
    }
  }

  private static class UriHelper {

    private static UriMatcher _uriMatcher;

    static {
      _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
      for(QueryTarget u:QueryTarget.values()) {
        String path = u.path;
        if (path.startsWith("/")) { path = path.substring(1); }
        _uriMatcher.addURI(AUTHORITY, path, u.ordinal() );
      }
    }

    final QueryTarget mTarget;
    final long mId;
    final public String mTable;
    private boolean mNetNotify;

    private UriHelper(QueryTarget u, boolean netNotify) {
      this(u, -1, netNotify);
    }

    private UriHelper(QueryTarget u, long id, boolean netNotify) {
      mTarget = u;
      mId = id;
      mNetNotify = netNotify;
      mTable = u.table;
    }

    static UriHelper parseUri(Uri query) {
      boolean netNotify;
      if ("nonetnotify".equals(query.getFragment())) {
        query = query.buildUpon().encodedFragment(null).build();
        netNotify=false;
      } else {
        netNotify=true;
      }
      int ord = _uriMatcher.match(query);
      if (ord<0) { throw new IllegalArgumentException("Unknown URI: "+query); }
      QueryTarget u = QueryTarget.values()[ord];

      switch (u) {
      case TASK:
      case TASKOPTION:
      case TASKITEM:
        return new UriHelper(u, ContentUris.parseId(query), netNotify);
      default:
        return new UriHelper(u, netNotify);
      }
    }
  }

  public static boolean isSyncActive(final Account account) {
    return ContentResolver.isSyncActive(account, AUTHORITY);
  }

  public static boolean isSyncPending(final Account account) {
    return ContentResolver.isSyncPending(account, AUTHORITY);
  }

  public static void requestSyncTaskList(Account account, boolean expedited) {
    ProviderHelper.requestSync(account, AUTHORITY, expedited);
  }

  public static void requestSyncTaskList(Activity context, boolean expedited) {
    ProviderHelper.requestSync(context, AUTHORITY, expedited);
  }

  private static String[] appendArg(String[] args, String arg) {
    if (args==null || args.length==0) { return new String[] { arg }; }
    final String[] result = new String[args.length+1];
    System.arraycopy(args, 0, result, 0, args.length);
    result[args.length] = arg;
    return result;
  }

  private static boolean mimetypeMatches(String mimetype, String mimeTypeFilter) {
    if (mimeTypeFilter==null) { return true; }
    int splitIndex = mimetype.indexOf('/');
    String typeLeft = mimetype.substring(0, splitIndex);
    String typeRight = mimetype.substring(splitIndex+1);

    splitIndex = mimeTypeFilter.indexOf('/');
    String filterLeft = mimeTypeFilter.substring(0, splitIndex);
    String filterRight = mimeTypeFilter.substring(splitIndex+1);

    if (! (filterLeft.equals(typeLeft)||"*".equals(filterLeft))) {
      return false;
    }

    if (! (filterRight.equals(typeRight)||"*".equals(filterRight))) {
      return false;
    }

    return true;
  }

  private TasksOpenHelper mDbHelper;

  @Override
  public boolean onCreate() {
    mDbHelper = new TasksOpenHelper(getContext());
    return true;
  }

  @Override
  public String getType(Uri uri) {
    UriHelper helper = UriHelper.parseUri(uri);
    switch (helper.mTarget) {
      case TASK:
        return "vnd.android.cursor.item/vnd.nl.adaptivity.process.task";
      case TASKS:
        return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task";
      case TASKITEM:
        return "vnd.android.cursor.item/vnd.nl.adaptivity.process.task.item";
      case TASKITEMS:
        return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item";
      case TASKOPTION:
        return "vnd.android.cursor.item/vnd.nl.adaptivity.process.task.item.option";
      case TASKOPTIONS:
        return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item.option";
    }
    return null;
  }

  @Override
  public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
    String mimetype = getType(uri);
    if (mimetypeMatches(mimetype, mimeTypeFilter)) {
      return new String[] { mimetype };
    } else {
      return null;
    }
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = BaseColumns._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+Tasks._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    final Cursor result = db.query(helper.mTable, projection, selection, selectionArgs, null, null, sortOrder);
    result.setNotificationUri(getContext().getContentResolver(), uri);
    return result;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mTarget==QueryTarget.TASK && helper.mId>=0) {
      values.put(Tasks._ID, Long.valueOf(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    long id = db.insert(helper.mTable, null, values);
    final Uri result = ContentUris.withAppendedId(uri, id);
    getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null, false);
    getContext().getContentResolver().notifyChange(result, null, helper.mNetNotify);
    return result;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    UriHelper helper = UriHelper.parseUri(uri);
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      if (helper.mTarget==QueryTarget.TASK) {
        if (selection==null || selection.length()==0) {
          selection = Tasks._ID+" = ?";
        } else {
          selection = "( "+selection+" ) AND ( "+Tasks._ID+" = ? )";
        }
        selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
        String optionSelection = Options.COLUMN_ITEMID + " IN ( " +
                                    "SELECT " + Items._ID+
                                    " FROM " + TasksOpenHelper.TABLE_NAME_ITEMS+
                                    " WHERE " + Items.COLUMN_TASKID+" IN (" +
                                      " SELECT " + Tasks._ID+
                                      " FROM " + TasksOpenHelper.TABLE_NAME_TASKS +
                                      " WHERE " + selection + " ) )";
        db.delete(TasksOpenHelper.TABLE_NAME_OPTIONS, optionSelection, selectionArgs);
        String itemSelection = Items.COLUMN_TASKID+" IN (" +
              " SELECT " + Tasks._ID+
              " FROM " + TasksOpenHelper.TABLE_NAME_TASKS +
              " WHERE " + selection + " )";
        db.delete(TasksOpenHelper.TABLE_NAME_ITEMS, itemSelection, selectionArgs);
      } else if (helper.mTarget==QueryTarget.TASKITEMS) {
        String optionSelection = Options.COLUMN_ITEMID + " IN ( " +
            "SELECT " + Items._ID+
            " FROM " + TasksOpenHelper.TABLE_NAME_ITEMS+
            " WHERE " + selection + " )";
        db.delete(TasksOpenHelper.TABLE_NAME_OPTIONS, optionSelection, selectionArgs);
      } else if (helper.mId>=0) {
        if (selection==null || selection.length()==0) {
          selection = Tasks._ID+" = ?";
        } else {
          selection = "( "+selection+" ) AND ( "+Tasks._ID+" = ? )";
        }
        selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
      }
      getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null, false);
      final int result = db.delete(helper.mTable, selection, selectionArgs);
      if (result>0) {
        getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null, helper.mNetNotify);
      }
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    UriHelper helper = UriHelper.parseUri(uri);
    if (helper.mId>=0) {
      if (selection==null || selection.length()==0) {
        selection = Tasks._ID+" = ?";
      } else {
        selection = "( "+selection+" ) AND ( "+Tasks._ID+" = ? )";
      }
      selectionArgs = appendArg(selectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.update(helper.mTable, values, selection, selectionArgs);
    if (result>0) {
      getContext().getContentResolver().notifyChange(uri, null, helper.mNetNotify);
    }
    return result;
  }

  /**
   * This implementation of applyBatch wraps the operations into a database transaction that fails on exceptions.
   */
  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentProviderResult[] result = super.applyBatch(operations);
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static UserTask getTaskForHandle(Context context, long handle) {
    final ContentResolver contentResolver = context.getContentResolver();
    Cursor idresult = contentResolver.query(Tasks.CONTENT_URI, null, Tasks.COLUMN_HANDLE+" = ?", new String[] { Long.toString(handle)} , null);
    try {
      if (! idresult.moveToFirst()) { return null; }
      return getTask(contentResolver, idresult);
    } finally {
      idresult.close();
    }
  }

  public static UserTask getTaskForId(Context context, long id) {
    Uri uri = ContentUris.withAppendedId(Tasks.CONTENT_ID_URI_BASE, id);
    return getTask(context, uri);
  }

  public static UserTask getTask(Context context, Uri uri) {
    final ContentResolver contentResolver = context.getContentResolver();
    return getTask(contentResolver, uri);
  }

  private static UserTask getTask(final ContentResolver contentResolver, Uri uri) {
    Cursor cursor = contentResolver.query(uri, null, null, null, null);
    try {
      if (cursor.moveToFirst()) {
        return getTask(contentResolver, cursor);
      } else {
        return null;
      }
    } finally {
      cursor.close();
    }
  }

  private static UserTask getTask(ContentResolver contentResolver, Cursor cursor) {
    long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
    String summary = cursor.getString(cursor.getColumnIndexOrThrow(Tasks.COLUMN_SUMMARY));
    long handle = cursor.getLong(cursor.getColumnIndexOrThrow(Tasks.COLUMN_HANDLE));
    String owner = cursor.getString(cursor.getColumnIndexOrThrow(Tasks.COLUMN_OWNER));
    String state  =  cursor.getString(cursor.getColumnIndexOrThrow(Tasks.COLUMN_STATE));


    List<TaskItem> items = new ArrayList<>();
    Cursor itemCursor = contentResolver.query(Items.CONTENT_ID_URI_BASE, null, Items.COLUMN_TASKID+" = "+id, null, null);
    try {
      while (itemCursor.moveToNext()) {
        ItemCols itemCols = ItemCols.init(itemCursor);
        items.add(getItem(contentResolver, itemCols, itemCursor));
      }
    } finally {
      itemCursor.close();
    }

    return new UserTask(summary, handle, owner, TaskState.fromString(state), items);
  }

  private static TaskItem getItem(ContentResolver contentResolver, ItemCols itemCols, Cursor cursor) {
    long id = cursor.getLong(itemCols.colId);
    String name = cursor.getString(itemCols.colName);
    String label = cursor.getString(itemCols.colLabel);
    String type = cursor.getString(itemCols.colType);
    String value = cursor.getString(itemCols.colValue);

    List<String> options = new ArrayList<>();
    Cursor optionCursor = contentResolver.query(Options.CONTENT_ID_URI_BASE, new String[] { Options.COLUMN_VALUE }, Options.COLUMN_ITEMID+" = "+id, null, null);
    try {
      while (optionCursor.moveToNext()) {
        options.add(optionCursor.getString(0));
      }
    } finally {
      optionCursor.close();
    }

    return TaskItem.defaultFactory().create(name, label, type, value, options);
  }

  private static List<UserTask> getTasks(InputStream in) throws XmlPullParserException, IOException {
    return UserTask.parseTasks(in);
  }

  public static int updateTaskState(Context context, long taskId, TaskState newState) {
    Uri taskUri = ContentUris.withAppendedId(Tasks.CONTENT_ID_URI_BASE, taskId);
    final ContentResolver contentResolver = context.getContentResolver();
    ContentValues values = new ContentValues(2);
    values.put(XmlBaseColumns.COLUMN_SYNCSTATE, Long.valueOf(RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER));
    values.put(Tasks.COLUMN_STATE, newState.getAttrValue());
    return contentResolver.update(taskUri, values, null, null); // no additional where needed
  }

  public static void updateValuesAndState(Context context, long taskId, UserTask updatedTask) throws RemoteException, OperationApplicationException {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    Uri taskUri = ContentUris.withAppendedId(Tasks.CONTENT_ID_URI_BASE, taskId);

    final ContentResolver contentResolver = context.getContentResolver();
    UserTask oldTask = getTask(contentResolver, taskUri);
    if (oldTask==null) {
      throw new NoSuchElementException("The task with id "+taskId+" to update could not be found");
    }

    updateTaskValues(operations, taskId, oldTask, updatedTask);

    ContentValues newValues = new ContentValues(3);
    if(oldTask.getState()!=updatedTask.getState()) {
      newValues.put(Tasks.COLUMN_STATE, updatedTask.getState().getAttrValue());
    }
    if(!StringUtil.isEqual(oldTask.getSummary(), updatedTask.getSummary())) {
      newValues.put(Tasks.COLUMN_SUMMARY, updatedTask.getSummary());
    }
    if (operations.size()>0 || newValues.size()>0) {
      newValues.put(XmlBaseColumns.COLUMN_SYNCSTATE, Long.valueOf(RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER));
      operations.add(ContentProviderOperation
          .newUpdate(taskUri)
          .withValues(newValues)
          .build());
    }

    if (operations.size()>0) {
      contentResolver.applyBatch(AUTHORITY, operations);
    }
  }

  private static void updateTaskValues(ArrayList<ContentProviderOperation> operations, long taskId, UserTask oldTask, UserTask updatedTask) {
    for(TaskItem newItem: updatedTask.getItems()) {
      if (newItem.getName()!=null) { // no name, no value
        TaskItem oldItem = null;
        for(TaskItem candidate: oldTask.getItems()) {
          if (newItem.getName().equals(candidate.getName())) {
            oldItem = candidate;
            break;
          }
        }
        if (oldItem!=null) {
          if (!StringUtil.isEqual(oldItem.getValue(), newItem.getValue())) {
            operations.add(ContentProviderOperation
                .newUpdate(Items.CONTENT_ID_URI_BASE)
                .withSelection(Items.COLUMN_TASKID+"=? AND "+Items.COLUMN_NAME+" = ?", new String[] {Long.toString(taskId), newItem.getName() })
                .withValue(Items.COLUMN_VALUE, newItem.getValue())
                .build());
          }
        }
      }
    }
  }

}
