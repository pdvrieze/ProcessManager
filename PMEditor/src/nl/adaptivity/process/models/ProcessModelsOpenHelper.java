package nl.adaptivity.process.models;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParserException;

import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


@SuppressWarnings("boxing")
public class ProcessModelsOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME = "processModels";
  private static final String DB_NAME = "processmodels.db";
  private static final int DB_VERSION = 3;
  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      ProcessModels.COLUMN_HANDLE +" LONG," +
      ProcessModels.COLUMN_NAME + " TEXT," +
      ProcessModels.COLUMN_MODEL + " TEXT," +
      ProcessModels.COLUMN_UUID + " TEXT, " +
      ProcessModels.COLUMN_SYNCSTATE+ " INT )";
  private Context mContext;

  public ProcessModelsOpenHelper(Context pContext) {
    super(pContext, DB_NAME, null, DB_VERSION);
    mContext = pContext;
  }

  @Override
  public void onCreate(SQLiteDatabase pDb) {
    pDb.execSQL(SQL_CREATE_TABLE);
    final String modelName = mContext.getString(R.string.example_1_name);
    ContentValues cv = new ContentValues();
    InputStream in = mContext.getResources().openRawResource(R.raw.processmodel);
    DrawableProcessModel model = PMParser.parseProcessModel(in, new LayoutAlgorithm<DrawableProcessNode>());
    model.setName(modelName);
    CharArrayWriter out = new CharArrayWriter();
    try {
      try {
        PMParser.serializeProcessModel(out, model);
      } catch (IOException | XmlPullParserException e) {
        throw new RuntimeException(e);
      }
    } finally {
      out.flush();
      out.close();
    }
    cv.put(ProcessModels.COLUMN_MODEL, out.toString());
    cv.put(ProcessModels.COLUMN_NAME, modelName);
    cv.put(ProcessModels.COLUMN_SYNCSTATE, RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER);
    UUID uuid = model.getUuid();
    if (uuid==null) { uuid = UUID.randomUUID(); }
    cv.put(ProcessModels.COLUMN_UUID, uuid.toString());
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

  @Override
  public void onDowngrade(SQLiteDatabase pDb, int pOldVersion, int pNewVersion) {
    onUpgrade(pDb, pOldVersion, pNewVersion);
  }



}
