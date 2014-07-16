package nl.adaptivity.process.tasks.data;

import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


public class TasksOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME_TASKS = "tasks";
  static final String TABLE_NAME_ITEMS = "items";
  static final String TABLE_NAME_OPTIONS = "options";
  private static final String DB_NAME = "tasks.db";
  private static final int DB_VERSION = 0;
  private static final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Tasks.COLUMN_HANDLE +" LONG," +
      Tasks.COLUMN_SUMMARY + " TEXT," +
      Tasks.COLUMN_OWNER + " TEXT," +
      Tasks.COLUMN_STATE + " TEXT," +
      Tasks.COLUMN_SYNCSTATE+ " INT )";
  private static final String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Items.COLUMN_TASKID+ " INTEGER," +
      Items.COLUMN_NAME +" TEXT," +
      Items.COLUMN_TYPE + " TEXT," +
      Items.COLUMN_VALUE + " TEXT )";
  private static final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Tasks.COLUMN_HANDLE +" LONG," +
      Tasks.COLUMN_SUMMARY + " TEXT," +
      Tasks.COLUMN_OWNER + " TEXT," +
      Tasks.COLUMN_STATE + " TEXT," +
      Tasks.COLUMN_SYNCSTATE+ " INT )";

  public TasksOpenHelper(Context pContext) {
    super(pContext, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase pDb) {
    pDb.execSQL(SQL_CREATE_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase pDb, int pOldVersion, int pNewVersion) {
    pDb.beginTransaction();
    try {
      pDb.execSQL("DROP TABLE "+TABLE_NAME_TASKS);
      onCreate(pDb);

      pDb.setTransactionSuccessful();
    } finally {
      pDb.endTransaction();
    }
  }

  @Override
  public void onDowngrade(SQLiteDatabase pDb, int pOldVersion, int pNewVersion) {
    onUpgrade(pDb, pOldVersion, pNewVersion);
  }



}
