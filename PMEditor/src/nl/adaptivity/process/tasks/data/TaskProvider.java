package nl.adaptivity.process.tasks.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

@SuppressWarnings("static-access")
public class TaskProvider extends ContentProvider {


  private static class ItemCols {

    public final int colId;
    public final int colName;
    public final int colLabel;
    public final int colType;
    public final int colValue;

    public ItemCols(int pColId, int pColName, int pColLabel, int pColType, int pColValue) {
      colId = pColId;
      colName = pColName;
      colLabel = pColLabel;
      colType = pColType;
      colValue = pColValue;
    }

    public static ItemCols init(Cursor pItemCursor) {
      int colId = pItemCursor.getColumnIndex(Items._ID);
      int colName = pItemCursor.getColumnIndex(Items.COLUMN_NAME);
      int colLabel = pItemCursor.getColumnIndex(Items.COLUMN_LABEL);
      int colType = pItemCursor.getColumnIndex(Items.COLUMN_TYPE);
      int colValue = pItemCursor.getColumnIndex(Items.COLUMN_VALUE);
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
    public static final String COLUMN_LABEL = "name";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_VALUE = "value";
  }

  public static class Options implements BaseColumns {

    public static final Uri CONTENT_ID_URI_BASE = Uri.parse(Tasks.SCHEME+AUTHORITY+Tasks.PATH_OPTIONS);
    public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(Tasks.SCHEME+AUTHORITY+Tasks.PATH_OPTIONS+'#');
    public static final String COLUMN_ITEMID = "itemid";
    public static final String COLUMN_VALUE = "value";

  }

  private static enum QueryTarget{
    TASKS(Tasks.CONTENT_URI, TasksOpenHelper.TABLE_NAME_TASKS),
    TASK(Tasks.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_TASKS),
    TASKITEMS(TaskProvider.Items.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_ITEMS),
    TASKOPTIONS(Options.CONTENT_ID_URI_PATTERN, TasksOpenHelper.TABLE_NAME_OPTIONS);

    private final String path;
    private final String table;

    private QueryTarget(Uri uri, String table) {
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

    private UriHelper(QueryTarget u) {
      this(u, -1);
    }

    private UriHelper(QueryTarget u, long id) {
      mTarget = u;
      mId = id;
      mTable = u.table;
    }

    static UriHelper parseUri(Uri query) {
      int ord = _uriMatcher.match(query);
      if (ord<0) { throw new IllegalArgumentException("Unknown URI: "+query); }
      QueryTarget u = QueryTarget.values()[ord];

      switch (u) {
      case TASK:
      case TASKOPTIONS:
      case TASKITEMS:
        return new UriHelper(u, ContentUris.parseId(query));
      default:
        return new UriHelper(u);
      }
    }
  }

  private static String[] appendArg(String[] pArgs, String pArg) {
    if (pArgs==null || pArgs.length==0) { return new String[] { pArg }; }
    final String[] result = new String[pArgs.length+1];
    System.arraycopy(pArgs, 0, result, 0, pArgs.length);
    result[pArgs.length] = pArg;
    return result;
  }

  private static boolean mimetypeMatches(String pMimetype, String pMimeTypeFilter) {
    if (pMimeTypeFilter==null) { return true; }
    int splitIndex = pMimetype.indexOf('/');
    String typeLeft = pMimetype.substring(0, splitIndex);
    String typeRight = pMimetype.substring(splitIndex+1);

    splitIndex = pMimeTypeFilter.indexOf('/');
    String filterLeft = pMimeTypeFilter.substring(0, splitIndex);
    String filterRight = pMimeTypeFilter.substring(splitIndex+1);

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
  public String getType(Uri pUri) {
    UriHelper helper = UriHelper.parseUri(pUri);
    switch (helper.mTarget) {
    case TASK:
      return "vnd.android.cursor.item/vnd.nl.adaptivity.process.task";
    case TASKS:
      return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task";
    case TASKITEMS:
      return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item";
    case TASKOPTIONS:
      return "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item.option";
    }
    return null;
  }

  @Override
  public String[] getStreamTypes(Uri pUri, String pMimeTypeFilter) {
    UriHelper helper = UriHelper.parseUri(pUri);
    String mimetype;
    switch (helper.mTarget) {
      case TASK:
        mimetype = "vnd.android.cursor.item/vnd.nl.adaptivity.process.task";break;
      case TASKS:
        mimetype = "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task";break;
      case TASKITEMS:
        mimetype = "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item";break;
      case TASKOPTIONS:
        mimetype = "vnd.android.cursor.dir/vnd.nl.adaptivity.process.task.item.option";break;
      default:
        return null;
    }
    if (mimetypeMatches(mimetype, pMimeTypeFilter)) {
      return new String[] { mimetype };
    } else {
      return null;
    }
  }

  @Override
  public Cursor query(Uri pUri, String[] pProjection, String pSelection, String[] pSelectionArgs, String pSortOrder) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mTarget==QueryTarget.TASK) {
      if (pSelection==null || pSelection.length()==0) {
        pSelection = Tasks._ID+" = ?";
      } else {
        pSelection = "( "+pSelection+" ) AND ( "+Tasks._ID+" = ? )";
      }
      pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    final Cursor result = db.query(helper.mTable, pProjection, pSelection, pSelectionArgs, null, null, pSortOrder);
    result.setNotificationUri(getContext().getContentResolver(), pUri);
    return result;
  }

  @Override
  public Uri insert(Uri pUri, ContentValues pValues) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mTarget==QueryTarget.TASK && helper.mId>=0) {
      pValues.put(Tasks._ID, Long.valueOf(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    long id = db.insert(helper.mTable, null, pValues);
    final Uri result = ContentUris.withAppendedId(Tasks.CONTENT_ID_URI_PATTERN, id);
    getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null);
    getContext().getContentResolver().notifyChange(result, null);
    return result;
  }

  @Override
  public int delete(Uri pUri, String pSelection, String[] pSelectionArgs) {
    UriHelper helper = UriHelper.parseUri(pUri);
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      if (helper.mTarget==QueryTarget.TASK) {
        if (pSelection==null || pSelection.length()==0) {
          pSelection = Tasks._ID+" = ?";
        } else {
          pSelection = "( "+pSelection+" ) AND ( "+Tasks._ID+" = ? )";
        }
        pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
        String optionSelection = Options.COLUMN_ITEMID + " IN ( " +
                                    "SELECT " + Items._ID+
                                    " FROM " + TasksOpenHelper.TABLE_NAME_ITEMS+
                                    " WHERE " + Items.COLUMN_TASKID+" IN (" +
                                      " SELECT " + Tasks._ID+
                                      " FROM " + TasksOpenHelper.TABLE_NAME_TASKS +
                                      " WHERE " + pSelection + " ) )";
        db.delete(TasksOpenHelper.TABLE_NAME_OPTIONS, optionSelection, pSelectionArgs);
        String itemSelection = Items.COLUMN_TASKID+" IN (" +
              " SELECT " + Tasks._ID+
              " FROM " + TasksOpenHelper.TABLE_NAME_TASKS +
              " WHERE " + pSelection + " )";
        db.delete(TasksOpenHelper.TABLE_NAME_ITEMS, itemSelection, pSelectionArgs);
      } else if (helper.mTarget==QueryTarget.TASKITEMS) {
        String optionSelection = Options.COLUMN_ITEMID + " IN ( " +
            "SELECT " + Items._ID+
            " FROM " + TasksOpenHelper.TABLE_NAME_ITEMS+
            " WHERE " + pSelection + " )";
        db.delete(TasksOpenHelper.TABLE_NAME_OPTIONS, optionSelection, pSelectionArgs);
      }
      getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null);
      final int result = db.delete(helper.mTable, pSelection, pSelectionArgs);
      if (result>0) {
        getContext().getContentResolver().notifyChange(Tasks.CONTENT_ID_URI_BASE, null);
      }
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public int update(Uri pUri, ContentValues pValues, String pSelection, String[] pSelectionArgs) {
    UriHelper helper = UriHelper.parseUri(pUri);
    if (helper.mTarget==QueryTarget.TASK) {
      if (pSelection==null || pSelection.length()==0) {
        pSelection = Tasks._ID+" = ?";
      } else {
        pSelection = "( "+pSelection+" ) AND ( "+Tasks._ID+" = ? )";
      }
      pSelectionArgs = appendArg(pSelectionArgs, Long.toString(helper.mId));
    }
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    final int result = db.update(helper.mTable, pValues, pSelection, pSelectionArgs);
    if (result>0) {
      getContext().getContentResolver().notifyChange(pUri, null);
    }
    return result;
  }

  /**
   * This implementation of applyBatch wraps the operations into a database transaction that fails on exceptions.
   */
  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> pOperations) throws OperationApplicationException {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentProviderResult[] result = super.applyBatch(pOperations);
      db.setTransactionSuccessful();
      return result;
    } finally {
      db.endTransaction();
    }
  }

  public static UserTask getTaskForHandle(Context pContext, long pHandle) {
    final ContentResolver contentResolver = pContext.getContentResolver();
    Cursor idresult = contentResolver.query(Tasks.CONTENT_URI, null, Tasks.COLUMN_HANDLE+" = ?", new String[] { Long.toString(pHandle)} , null);
    try {
      if (! idresult.moveToFirst()) { return null; }
      return getTask(contentResolver, idresult);
    } finally {
      idresult.close();
    }
  }

  public static UserTask getTaskForId(Context pContext, long pId) {
    Uri uri = ContentUris.withAppendedId(Tasks.CONTENT_ID_URI_BASE, pId);
    return getTask(pContext, uri);
  }

  public static UserTask getTask(Context pContext, Uri pUri) {
    final ContentResolver contentResolver = pContext.getContentResolver();
    Cursor cursor = contentResolver.query(pUri, null, null, null, null);
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

  private static UserTask getTask(ContentResolver pContentResolver, Cursor pCursor) {
    long id = pCursor.getLong(pCursor.getColumnIndex(BaseColumns._ID));
    String summary = pCursor.getString(pCursor.getColumnIndexOrThrow(Tasks.COLUMN_SUMMARY));
    long handle = pCursor.getLong(pCursor.getColumnIndexOrThrow(Tasks.COLUMN_HANDLE));
    String owner = pCursor.getString(pCursor.getColumnIndexOrThrow(Tasks.COLUMN_OWNER));
    String state  =  pCursor.getString(pCursor.getColumnIndexOrThrow(Tasks.COLUMN_STATE));


    List<TaskItem> items = new ArrayList<>();
    Cursor itemCursor = pContentResolver.query(ContentUris.withAppendedId(Items.CONTENT_ID_URI_BASE, id), null, null, null, null);
    try {
      while (itemCursor.moveToNext()) {
        ItemCols itemCols = ItemCols.init(itemCursor);
        items.add(getItem(pContentResolver, itemCols, itemCursor));
      }
    } finally {
      itemCursor.close();
    }

    return new UserTask(summary, handle, owner, state, items);
  }

  private static TaskItem getItem(ContentResolver pContentResolver, ItemCols pItemCols, Cursor pCursor) {
    long id = pCursor.getLong(pItemCols.colId);
    String name = pCursor.getString(pItemCols.colName);
    String label = pCursor.getString(pItemCols.colLabel);
    String type = pCursor.getString(pItemCols.colType);
    String value = pCursor.getString(pItemCols.colValue);

    List<String> options = new ArrayList<>();
    Cursor optionCursor = pContentResolver.query(ContentUris.withAppendedId(Options.CONTENT_ID_URI_BASE, id), new String[] { Options.COLUMN_VALUE }, null, null, null);
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

}
