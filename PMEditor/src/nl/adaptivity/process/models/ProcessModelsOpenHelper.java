package nl.adaptivity.process.models;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


public class ProcessModelsOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME = "processModels";
  private static final String DB_NAME = "processmodels.db";
  private static final int DB_VERSION = 3;
  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
      BaseColumns._ID+" LONG," +
      ProcessModels.COLUMN_HANDLE +" LONG," +
      ProcessModels.COLUMN_NAME + " TEXT," +
      ProcessModels.COLUMN_MODEL + " TEXT )";
  private Context mContext;

  public ProcessModelsOpenHelper(Context pContext) {
    super(pContext, DB_NAME, null, DB_VERSION);
    mContext = pContext;
  }

  @Override
  public void onCreate(SQLiteDatabase pDb) {
    pDb.execSQL(SQL_CREATE_TABLE);
    ContentValues cv = new ContentValues();
    InputStream in = mContext.getResources().openRawResource(R.raw.processmodel);
    StringBuilder out = new StringBuilder();
    try {
      try {
        Reader reader = new InputStreamReader(in, "utf8");
        CharBuffer buffer = CharBuffer.allocate(4096);
        int cnt;
        while ((cnt=reader.read(buffer))>=0) {
          out.append(buffer.array(),buffer.arrayOffset(),cnt);
          buffer.rewind();
        }

      } finally {
        in.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    cv.put(ProcessModels.COLUMN_MODEL, out.toString());
    cv.put(ProcessModels.COLUMN_NAME, mContext.getString(R.string.example_1_name));
    pDb.insert(TABLE_NAME, ProcessModels.COLUMN_MODEL, cv);
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
