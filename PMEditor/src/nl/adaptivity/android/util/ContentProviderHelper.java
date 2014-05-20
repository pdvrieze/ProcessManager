package nl.adaptivity.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.process.models.ProcessModelProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;


public class ContentProviderHelper {

  private static final String TAG = ContentProviderHelper.class.getSimpleName();

  private static class ProcessModelThread extends Thread {

    protected SQLiteDatabase mDb;
    protected String mTable;
    protected String mColumn;
    protected long mId;
    protected ParcelFileDescriptor mFileDescriptor;

    public ProcessModelThread(SQLiteDatabase pDb, String pTable, String pColumn, long pId, ParcelFileDescriptor pParcelFileDescriptor) {
      mDb = pDb;
      mTable = pTable;
      mColumn = pColumn;
      mId = pId;
      mFileDescriptor = pParcelFileDescriptor;
    }

  }

  private static class ProcessModelWriteThread extends ProcessModelThread {

    public ProcessModelWriteThread(SQLiteDatabase pDb, String pTable, String pColumn, long pId, ParcelFileDescriptor pParcelFileDescriptor) {
      super(pDb, pTable, pColumn, pId, pParcelFileDescriptor);
    }

    @Override
    public void run() {
      Cursor cursor = mDb.query(mTable, new String[] {mColumn}, BaseColumns._ID+" = ?", new String[]{Long.toString(mId)}, null, null, null);
      String data;
      if (! cursor.moveToFirst()) {
        data = "";
      } else {
        try {
          data = cursor.getString(cursor.getColumnIndex(mColumn));
        } finally {
          cursor.close();
        }
      }
      OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(mFileDescriptor);
      try {
        try {
          Writer out = new OutputStreamWriter(os, "UTF8");
          try {
            out.append(data);
          } finally {
            out.close();
          }
        } finally {
          os.close();
        }
      } catch (IOException e) {
        Log.e(ContentProviderHelper.class.getSimpleName(), "Failure to excute pipe", e);
        Compat.closeWithError(mFileDescriptor, e.getMessage());
      }
    }

  }

  private static class ProcessModelReadThread extends ProcessModelThread {

    public ProcessModelReadThread(SQLiteDatabase pDb, String pTable, String pColumn, long pId, ParcelFileDescriptor pParcelFileDescriptor) {
      super(pDb, pTable, pColumn, pId, pParcelFileDescriptor);
    }

    @Override
    public void run() {
      StringBuilder data = new StringBuilder();
      InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(mFileDescriptor);
      try {
        try {
          Reader in = new InputStreamReader(is, "UTF8");
          try {
            char[] buffer = new char[2048];
            int cnt;
            while ((cnt=in.read(buffer))>=0) {
              data.append(buffer,0,cnt);
            }
          } finally {
            in.close();
          }
        } finally {
          is.close();
        }
        ContentValues values = new ContentValues();
        values.put(mColumn, data.toString());
        mDb.update(mTable, values , BaseColumns._ID+" = ?", new String[] {Long.toString(mId)});
      } catch (IOException e) {
        Log.e(TAG, "Failure to excute pipe", e);
        Compat.closeWithError(mFileDescriptor, e.getMessage());
      }
    }

  }

  public static ParcelFileDescriptor createPipe(ProcessModelProvider pProcessModelProvider, SQLiteDatabase db, String table, String column, long id, String mode) {
    final boolean readMode;
    switch  (mode) {
      case "r":
        readMode=true;
        break;
      case "w":
        readMode=false;
        break;
      default: {
        ParcelFileDescriptor[] pair;
        try {
          pair = ParcelFileDescriptor.createPipe();
          Compat.closeWithError(pair[0], "The given mode is not available");
          return pair[1];
        } catch (IOException e) {
          Log.e(TAG, "Failure to forward error", e);
          return null;
        }
      }
    }
    ParcelFileDescriptor[] pair;
    try {
      pair = ParcelFileDescriptor.createPipe();
    } catch (IOException e) {
      Log.e(TAG, "Failure to create pipe", e);
      return null;
    }

    Thread th;
    if (readMode) {
      th = new ProcessModelWriteThread(db, table, column, id, pair[1]);
      th.run();
    } else {
      th = new ProcessModelReadThread(db, table, column, id, pair[0]);
      th.start();
    }

    return readMode ? pair[0] : pair[1];
  }

}
