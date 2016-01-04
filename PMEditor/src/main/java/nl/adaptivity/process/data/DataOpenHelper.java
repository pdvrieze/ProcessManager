package nl.adaptivity.process.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessInstances;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.tasks.data.TasksOpenHelper;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


@SuppressWarnings("boxing")
public class DataOpenHelper extends SQLiteOpenHelper {

  public static final String TABLE_NAME_MODELS = "processModels";
  public static final String TABLE_NAME_INSTANCES = "processInstances";
  public static final String TABLE_NAME_TASKS = "tasks";
  public static final String TABLE_NAME_ITEMS = "items";
  public static final String TABLE_NAME_OPTIONS = "options";
  public static final String VIEW_NAME_PROCESSMODELEXT = "processModelsExt";
  public static final String VIEW_NAME_TASKSEXT="tasksExt";

  private static final String DB_NAME = "processmodels.db";
  private static final int DB_VERSION = 8;

  private static final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
                                                       BaseColumns._ID + " INTEGER PRIMARY KEY," +
                                                       Tasks.COLUMN_HANDLE + " LONG," +
                                                       Tasks.COLUMN_SUMMARY + " TEXT," +
                                                       Tasks.COLUMN_OWNER + " TEXT," +
                                                       Tasks.COLUMN_STATE + " TEXT," +
                                                       Tasks.COLUMN_INSTANCEHANDLE + " INT, " +
                                                       Tasks.COLUMN_SYNCSTATE + " INT )";
  private static final String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_NAME_ITEMS + " (" +
                                                       BaseColumns._ID + " INTEGER PRIMARY KEY," +
                                                       Items.COLUMN_TASKID + " INTEGER," +
                                                       Items.COLUMN_NAME + " TEXT," +
                                                       Items.COLUMN_LABEL + " TEXT," +
                                                       Items.COLUMN_TYPE + " TEXT," +
                                                       Items.COLUMN_VALUE + " TEXT )";
  private static final String SQL_CREATE_OPTIONS_TABLE = "CREATE TABLE " + TABLE_NAME_OPTIONS + " (" +
                                                         BaseColumns._ID + " INTEGER PRIMARY KEY," +
                                                         Options.COLUMN_ITEMID + " INTEGER," +
                                                         Options.COLUMN_VALUE + " TEXT )";

  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME_MODELS + " (" +
                                                 BaseColumns._ID + " INTEGER PRIMARY KEY," +
                                                 ProcessModels.COLUMN_HANDLE + " LONG," +
                                                 ProcessModels.COLUMN_NAME + " TEXT," +
                                                 ProcessModels.COLUMN_MODEL + " TEXT," +
                                                 ProcessModels.COLUMN_UUID + " TEXT, " +
                                                 ProcessModels.COLUMN_FAVOURITE + " INT," +
                                                 ProcessModels.COLUMN_SYNCSTATE + " INT )";
  private static final String SQL_CREATE_TABLE_INSTANCES = "CREATE TABLE " + TABLE_NAME_INSTANCES + " (" +
                                                           BaseColumns._ID + " INTEGER PRIMARY KEY," +
                                                           ProcessInstances.COLUMN_HANDLE + " LONG," +
                                                           ProcessInstances.COLUMN_PMHANDLE + " LONG," +
                                                           ProcessInstances.COLUMN_NAME + " TEXT," +
                                                           ProcessInstances.COLUMN_UUID + " TEXT," +
                                                           ProcessInstances.COLUMN_SYNCSTATE + " INT )";

  private static final String SQL_CREATE_TASKSEXT_VIEW = "CREATE VIEW " + VIEW_NAME_TASKSEXT+" AS SELECT t." +
                                                         BaseColumns._ID + ", t." +
                                                         Tasks.COLUMN_HANDLE + ", t." +
                                                         Tasks.COLUMN_SUMMARY + ", t." +
                                                         Tasks.COLUMN_OWNER + ", t." +
                                                         Tasks.COLUMN_STATE + ", t." +
                                                         Tasks.COLUMN_SYNCSTATE + ", i."+
                                                         ProcessInstances.COLUMN_NAME +" AS "+Tasks.COLUMN_INSTANCENAME+" FROM "+
                                                         TABLE_NAME_TASKS + " AS t LEFT JOIN " +
                                                         TABLE_NAME_INSTANCES + " AS i ON ( t."+Tasks.COLUMN_INSTANCEHANDLE+" = i."+ProcessInstances.COLUMN_HANDLE+" )";
          ;

  private static final String SQL_CREATE_VIEW_MODELS_EXT = "CREATE VIEW " + VIEW_NAME_PROCESSMODELEXT + " AS SELECT m." +
                                                           BaseColumns._ID + ", m." +
                                                           ProcessModels.COLUMN_HANDLE + ", m." +
                                                           ProcessModels.COLUMN_NAME + ", m." +
                                                           ProcessModels.COLUMN_MODEL + ", m." +
                                                           ProcessModels.COLUMN_UUID + ", m." +
                                                           ProcessModels.COLUMN_FAVOURITE + ", m." +
                                                           ProcessModels.COLUMN_SYNCSTATE + ", COUNT( i." +
                                                           BaseColumns._ID + ") AS " + ProcessModels.COLUMN_INSTANCECOUNT + " FROM " +
                                                           TABLE_NAME_MODELS + " AS m LEFT JOIN " + TABLE_NAME_INSTANCES + " AS i ON ( m." +
                                                           ProcessModels.COLUMN_HANDLE + " = i." + ProcessInstances.COLUMN_PMHANDLE + " ) GROUP BY m." +
                                                           ProcessModels.COLUMN_HANDLE +
                                                           ";";
  private static final boolean CREATE_DEFAULT_MODEL = false;

  private Context mContext;

  public DataOpenHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
    mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_TABLE);
    db.execSQL(SQL_CREATE_TABLE_INSTANCES);
    db.execSQL(SQL_CREATE_VIEW_MODELS_EXT);
    db.execSQL(SQL_CREATE_OPTIONS_TABLE);
    db.execSQL(SQL_CREATE_ITEMS_TABLE);
    db.execSQL(SQL_CREATE_TASKS_TABLE);

    if (CREATE_DEFAULT_MODEL) {
      final String modelName = mContext.getString(R.string.example_1_name);
      ContentValues cv = new ContentValues();
      InputStream in = mContext.getResources().openRawResource(R.raw.processmodel);
      final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<DrawableProcessNode>();
      DrawableProcessModel model = PMParser.parseProcessModel(in, layoutAlgorithm, layoutAlgorithm);
      model.setName(modelName);
      CharArrayWriter out = new CharArrayWriter();
      try {
        try {
          PMParser.serializeProcessModel(out, model);
        } catch (IOException | XmlPullParserException | XmlException e) {
          throw new RuntimeException(e);
        }
      } finally {
        out.flush();
        out.close();
      }
      cv.put(ProcessModels.COLUMN_MODEL, out.toString());
      cv.put(ProcessModels.COLUMN_NAME, modelName);
      cv.put(XmlBaseColumns.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER);
      UUID uuid = model.getUuid();
      if (uuid==null) { uuid = UUID.randomUUID(); }
      cv.put(ProcessModels.COLUMN_UUID, uuid.toString());
      db.insert(TABLE_NAME_MODELS, ProcessModels.COLUMN_MODEL, cv);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.beginTransaction();
    try {
      if (oldVersion==8) {
        db.endTransaction();
        return;
      }
      if (newVersion!=8 || !upgradeTo8(db, oldVersion, newVersion)) {
        recreateDB(db);
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    if (oldVersion<8) {
      final File oldDatabasePath = mContext.getDatabasePath(TasksOpenHelper.DB_NAME);
      oldDatabasePath.delete(); // Delete the old database as no longer needed, but only after successful migration
    }
  }

  private boolean upgradeTo8(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    if (oldVersion==7 || upgradeTo7(db, oldVersion, newVersion)) {
      db.execSQL(SQL_CREATE_OPTIONS_TABLE);
      db.execSQL(SQL_CREATE_ITEMS_TABLE);
      db.execSQL(SQL_CREATE_TASKS_TABLE);
      db.execSQL(SQL_CREATE_TASKSEXT_VIEW);
      final File oldDatabasePath = mContext.getDatabasePath(TasksOpenHelper.DB_NAME);
      if(oldDatabasePath.exists()) {
        SQLiteDatabase tasksDb = new TasksOpenHelper(mContext).getReadableDatabase();

        try {
          copyTable(tasksDb, db, TasksOpenHelper.TABLE_NAME_OPTIONS, TABLE_NAME_OPTIONS);
          copyTable(tasksDb, db, TasksOpenHelper.TABLE_NAME_ITEMS, TABLE_NAME_ITEMS);
          copyTable(tasksDb, db, TasksOpenHelper.TABLE_NAME_TASKS, TABLE_NAME_TASKS);
        } finally {
          tasksDb.close();
        }
      }
      return true;
    }
    return false;
  }

  private static boolean upgradeTo7(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    if (oldVersion==6 || upgradeTo6(db, oldVersion, newVersion)) {
      db.execSQL(SQL_CREATE_VIEW_MODELS_EXT);
    } else {
      return false;
    }
    return true;
  }

  private static boolean upgradeTo6(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    if (oldVersion==5 || upgradeTo5(db, oldVersion, newVersion)) {
      db.execSQL("ALTER TABLE " + TABLE_NAME_MODELS + " ADD COLUMN " + ProcessModels.COLUMN_FAVOURITE + " INTEGER");
      return true;
    } else {
      return false;
    }
  }

  private static boolean upgradeTo5(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    if (newVersion>=5 && oldVersion==3) {
      db.execSQL(SQL_CREATE_TABLE_INSTANCES);
      return true;
    } else if (newVersion>=5 && oldVersion==4) {
      db.execSQL("ALTER TABLE " + TABLE_NAME_INSTANCES + " ADD COLUMN " + ProcessInstances.COLUMN_UUID + " TEXT");
      db.delete(TABLE_NAME_INSTANCES, XmlBaseColumns.COLUMN_SYNCSTATE + "=" + RemoteXmlSyncAdapter.SYNC_UPTODATE, null);
      return true;
    } else {
      return false;
    }
  }

  private void recreateDB(final SQLiteDatabase db) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_MODELS);
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_INSTANCES);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME_PROCESSMODELEXT);
    db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME_TASKS);
    db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME_ITEMS);
    db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME_OPTIONS);
    db.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME_TASKSEXT);
    onCreate(db);
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }

  private static void copyTable(SQLiteDatabase sourceDb, SQLiteDatabase targetDb, String sourceTable, String targetTable) {
    Cursor source = sourceDb.query(sourceTable, null, null, null, null, null, null);
    try {
      while(source.moveToNext()) {
        targetDb.insert(targetTable, null, toContentValues(source));
      }
    } finally {
      source.close();
    }
  }

  private static ContentValues toContentValues(final Cursor cursor) {
    final int columnCount = cursor.getColumnCount();
    ContentValues result = new ContentValues(columnCount);
    for (int i = 0; i < columnCount; i++) {
      final String columnName = cursor.getColumnName(i);
      switch (cursor.getType(i)) {
        case Cursor.FIELD_TYPE_BLOB:
          result.put(columnName, cursor.getBlob(i));
          break;
        case Cursor.FIELD_TYPE_FLOAT:
          result.put(columnName, cursor.getDouble(i));
          break;
        case Cursor.FIELD_TYPE_INTEGER:
          result.put(columnName, cursor.getLong(i));
          break;
        case Cursor.FIELD_TYPE_NULL:
          result.putNull(columnName);
          break;
        case Cursor.FIELD_TYPE_STRING:
          result.put(columnName, cursor.getString(i));
          break;
      }
    }
    return result;
  }



}
