package nl.adaptivity.process.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class ProcessModelsOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME = "processModels";
  private static final String DB_NAME = "processmodels.db";
  private static final int DB_VERSION = 1;
  @SuppressWarnings("static-access")
  private static final String COLUMN_ID = ProcessModelProvider.ProcessModels._ID;
  private static final String COLUMN_HANDLE = ProcessModelProvider.ProcessModels.COLUMN_HANDLE;
  private static final String COLUMN_NAME = ProcessModelProvider.ProcessModels.COLUMN_NAME;

  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
      COLUMN_ID+" LONG," +
      COLUMN_HANDLE +" LONG," +
      COLUMN_NAME + " TEXT )";

  public ProcessModelsOpenHelper(Context pContext) {
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
      pDb.execSQL("DROP TABLE "+TABLE_NAME);
      onCreate(pDb);
      pDb.setTransactionSuccessful();
    } finally {
      pDb.endTransaction();
    }
  }

}
