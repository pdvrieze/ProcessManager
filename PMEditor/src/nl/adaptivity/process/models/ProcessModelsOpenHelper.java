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
import nl.adaptivity.process.models.ProcessModelProvider.ProcessInstances;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


@SuppressWarnings("boxing")
public class ProcessModelsOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME = "processModels";
  static final String TABLE_INSTANCES_NAME = "processInstances";
  private static final String DB_NAME = "processmodels.db";
  private static final int DB_VERSION = 5;
  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      ProcessModels.COLUMN_HANDLE +" LONG," +
      ProcessModels.COLUMN_NAME + " TEXT," +
      ProcessModels.COLUMN_MODEL + " TEXT," +
      ProcessModels.COLUMN_UUID + " TEXT, " +
      ProcessModels.COLUMN_SYNCSTATE+ " INT )";
  private static final String SQL_CREATE_TABLE_INSTANCES = "CREATE TABLE " + TABLE_INSTANCES_NAME + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      ProcessInstances.COLUMN_HANDLE +" LONG," +
      ProcessInstances.COLUMN_PMHANDLE + " LONG," +
      ProcessInstances.COLUMN_NAME + " TEXT," +
      ProcessInstances.COLUMN_UUID + " TEXT," +
      ProcessInstances.COLUMN_SYNCSTATE+ " INT )";

  private static final boolean CREATE_DEFAULT_MODEL = false;

  private Context mContext;

  public ProcessModelsOpenHelper(Context pContext) {
    super(pContext, DB_NAME, null, DB_VERSION);
    mContext = pContext;
  }

  @Override
  public void onCreate(SQLiteDatabase pDb) {
    pDb.execSQL(SQL_CREATE_TABLE);
    pDb.execSQL(SQL_CREATE_TABLE_INSTANCES);
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
        } catch (IOException | XmlPullParserException e) {
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
      pDb.insert(TABLE_NAME, ProcessModels.COLUMN_MODEL, cv);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase pDb, int pOldVersion, int pNewVersion) {
    pDb.beginTransaction();
    try {
      if (pNewVersion==5 && pOldVersion==3) {
        pDb.execSQL(SQL_CREATE_TABLE_INSTANCES);
      } else if (pNewVersion==5 && pOldVersion==4) {
        pDb.execSQL("ALTER TABLE "+TABLE_INSTANCES_NAME+" ADD COLUMN "+ProcessInstances.COLUMN_UUID+" TEXT");
        pDb.delete(TABLE_INSTANCES_NAME, XmlBaseColumns.COLUMN_SYNCSTATE+"="+RemoteXmlSyncAdapter.SYNC_UPTODATE,null);
      } else {
        pDb.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        pDb.execSQL("DROP TABLE IF EXISTS "+TABLE_INSTANCES_NAME);
        onCreate(pDb);
      }

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
