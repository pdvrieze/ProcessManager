package nl.adaptivity.process.models;

import android.content.ContentValues;
import android.content.Context;
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
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.xml.XmlException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


@SuppressWarnings("boxing")
public class ProcessModelsOpenHelper extends SQLiteOpenHelper {

  static final String TABLE_NAME = "processModels";
  static final String TABLE_INSTANCES_NAME = "processInstances";
  private static final String DB_NAME = "processmodels.db";
  public static final String VIEW_NAME_PROCESSMODELEXT = "processModelsExt";
  private static final int DB_VERSION = 7;
  private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      ProcessModels.COLUMN_HANDLE +" LONG," +
      ProcessModels.COLUMN_NAME + " TEXT," +
      ProcessModels.COLUMN_MODEL + " TEXT," +
      ProcessModels.COLUMN_UUID + " TEXT, " +
      ProcessModels.COLUMN_FAVOURITE+ " INT," +
      ProcessModels.COLUMN_SYNCSTATE+ " INT )";
  private static final String SQL_CREATE_TABLE_INSTANCES = "CREATE TABLE " + TABLE_INSTANCES_NAME + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY," +
      ProcessInstances.COLUMN_HANDLE +" LONG," +
      ProcessInstances.COLUMN_PMHANDLE + " LONG," +
      ProcessInstances.COLUMN_NAME + " TEXT," +
      ProcessInstances.COLUMN_UUID + " TEXT," +
      ProcessInstances.COLUMN_SYNCSTATE+ " INT )";

  private static final String SQL_CREATE_VIEW_MODELS_EXT = "CREATE VIEW " +VIEW_NAME_PROCESSMODELEXT + " AS SELECT m." +
                                                           BaseColumns._ID + ", m." +
                                                           ProcessModels.COLUMN_HANDLE + ", m." +
                                                           ProcessModels.COLUMN_NAME + ", m." +
                                                           ProcessModels.COLUMN_MODEL + ", m." +
                                                           ProcessModels.COLUMN_UUID + ", m." +
                                                           ProcessModels.COLUMN_FAVOURITE + ", m." +
                                                           ProcessModels.COLUMN_SYNCSTATE + ", COUNT( i." +
                                                           BaseColumns._ID + ") AS "+ ProcessModels.COLUMN_INSTANCECOUNT+" FROM " +
                                                           TABLE_NAME+ " AS m LEFT JOIN "+ TABLE_INSTANCES_NAME+ " AS i ON ( m." +
                                                           BaseColumns._ID+" = i."+ ProcessInstances.COLUMN_PMHANDLE+" ) GROUP BY m." +
                                                           BaseColumns._ID+
                                                           ";";
  private static final boolean CREATE_DEFAULT_MODEL = false;

  private Context mContext;

  public ProcessModelsOpenHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
    mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_TABLE);
    db.execSQL(SQL_CREATE_TABLE_INSTANCES);
    db.execSQL(SQL_CREATE_VIEW_MODELS_EXT);

    if (CREATE_DEFAULT_MODEL) {
      final String modelName = mContext.getString(R.string.example_1_name);
      ContentValues cv = new ContentValues();
      InputStream in = mContext.getResources().openRawResource(R.raw.processmodel);
      final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<DrawableProcessNode>();
      DrawableProcessModel model = PMParser.parseProcessModelFallback(in, layoutAlgorithm, layoutAlgorithm);
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
      db.insert(TABLE_NAME, ProcessModels.COLUMN_MODEL, cv);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.beginTransaction();
    try {
      if (newVersion==6 && oldVersion==7) {
        db.execSQL("DROP VIEW "+VIEW_NAME_PROCESSMODELEXT+";");
      } else if (oldVersion==6 || upgradeTo6(db, oldVersion, newVersion)) {
        db.execSQL(SQL_CREATE_VIEW_MODELS_EXT);
      } else {
        recreateDB(db);
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private static boolean upgradeTo6(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    if (oldVersion==5 || upgradeTo5(db, oldVersion, newVersion)) {
      db.execSQL("ALTER TABLE "+TABLE_NAME+" ADD COLUMN "+ProcessModels.COLUMN_FAVOURITE+" INTEGER");
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
      db.execSQL("ALTER TABLE "+TABLE_INSTANCES_NAME+" ADD COLUMN "+ProcessInstances.COLUMN_UUID+" TEXT");
      db.delete(TABLE_INSTANCES_NAME, XmlBaseColumns.COLUMN_SYNCSTATE+"="+RemoteXmlSyncAdapter.SYNC_UPTODATE,null);
      return true;
    } else {
      return false;
    }
  }

  private void recreateDB(final SQLiteDatabase db) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    db.execSQL("DROP TABLE IF EXISTS "+TABLE_INSTANCES_NAME);
    db.execSQL("DROP VIEW IF EXISTS "+VIEW_NAME_PROCESSMODELEXT);
    onCreate(db);
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }



}
