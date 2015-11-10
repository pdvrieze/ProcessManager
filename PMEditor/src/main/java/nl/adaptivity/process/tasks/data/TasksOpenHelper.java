package nl.adaptivity.process.tasks.data;

import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
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
  private static final int DB_VERSION = 2;
  private static final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Tasks.COLUMN_HANDLE +" LONG," +
      Tasks.COLUMN_SUMMARY + " TEXT," +
      Tasks.COLUMN_OWNER + " TEXT," +
      Tasks.COLUMN_STATE + " TEXT," +
      Tasks.COLUMN_SYNCSTATE+ " INT )";
  private static final String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_NAME_ITEMS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Items.COLUMN_TASKID+ " INTEGER," +
      Items.COLUMN_NAME +" TEXT," +
      Items.COLUMN_LABEL + " TEXT," +
      Items.COLUMN_TYPE + " TEXT," +
      Items.COLUMN_VALUE + " TEXT )";
  private static final String SQL_CREATE_OPTIONS_TABLE = "CREATE TABLE " + TABLE_NAME_OPTIONS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      Options.COLUMN_ITEMID +" INTEGER," +
      Options.COLUMN_VALUE + " TEXT )";

  public TasksOpenHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_OPTIONS_TABLE);
    db.execSQL(SQL_CREATE_ITEMS_TABLE);
    db.execSQL(SQL_CREATE_TASKS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.beginTransaction();
    try {
      if (oldVersion==1 && newVersion==2) {
        db.execSQL("ALTER TABLE "+TABLE_NAME_ITEMS+ " ADD COLUMN "+Items.COLUMN_LABEL+ " TEXT");
      } else {
        db.execSQL("DROP TABLE "+TABLE_NAME_TASKS);
        db.execSQL("DROP TABLE "+TABLE_NAME_ITEMS);
        db.execSQL("DROP TABLE "+TABLE_NAME_OPTIONS);
        onCreate(db);
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }



}
