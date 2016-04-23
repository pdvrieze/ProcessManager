/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;

import java.io.*;


public final class ContentProviderHelper {

  private static final String TAG = ContentProviderHelper.class.getSimpleName();

  private static class ProcessModelThread extends Thread {

    protected SQLiteOpenHelper mDbHelper;
    protected String mTable;
    protected String mColumn;
    protected long mId;
    protected ParcelFileDescriptor mFileDescriptor;

    public ProcessModelThread(SQLiteOpenHelper dbHelper, String table, String column, long id, ParcelFileDescriptor parcelFileDescriptor) {
      mDbHelper = dbHelper;
      mTable = table;
      mColumn = column;
      mId = id;
      mFileDescriptor = parcelFileDescriptor;
    }

  }

  private static class ProcessModelWriteThread extends ProcessModelThread {

    public ProcessModelWriteThread(SQLiteOpenHelper dbHelper, String table, String column, long id, ParcelFileDescriptor parcelFileDescriptor) {
      super(dbHelper, table, column, id, parcelFileDescriptor);
    }

    @Override
    public void run() {
      SQLiteDatabase db = mDbHelper.getReadableDatabase();
      Cursor cursor = db.query(mTable, new String[] {mColumn}, BaseColumns._ID+" = ?", new String[]{Long.toString(mId)}, null, null, null);
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

    private Context mContext;
    private final String mSyncStateColumn;
    private boolean mNotifyNet;

    public ProcessModelReadThread(final Context context, SQLiteOpenHelper dbHelper, String table, String column, long id, String syncStateColumn, ParcelFileDescriptor parcelFileDescriptor, final boolean notifyNet) {
      super(dbHelper, table, column, id, parcelFileDescriptor);
      mContext = context;
      mSyncStateColumn = syncStateColumn;
      mNotifyNet = notifyNet;
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
        if (data.length()>0) {
          ContentValues values = new ContentValues(mSyncStateColumn == null ? 1 : 2);
          values.put(mColumn, data.toString());
          SQLiteDatabase db = mDbHelper.getWritableDatabase();
          db.beginTransaction();
          try {
            if (mSyncStateColumn != null) {
              values.put(mSyncStateColumn, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_UPDATE_SERVER));
            }
            int updateCount = db.update(mTable, values, BaseColumns._ID + " = ?", new String[]{Long.toString(mId)});
            if (updateCount != 1) {
              Log.e(TAG, "Failure to update the database");
              Compat.closeWithError(mFileDescriptor, "Database update failure");
            } else {
              db.setTransactionSuccessful();
            }
          } finally {
            db.endTransaction();
          }
          final ContentResolver contentResolver = mContext.getContentResolver();
          contentResolver.notifyChange(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, mId), null, mNotifyNet);
          contentResolver.notifyChange(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, mId), null, mNotifyNet);
        } else {
          Compat.closeWithError(mFileDescriptor, "Empty process model provided. This is not valid");
        }
      } catch (IOException e) {
        Log.e(TAG, "Failure to excute pipe", e);
        Compat.closeWithError(mFileDescriptor, e.getMessage());
      }
    }

  }

  public static ParcelFileDescriptor createPipe(ProcessModelProvider processModelProvider, SQLiteOpenHelper dbHelper, String table, String column, long id, String syncStateColumn, String mode, final boolean notifyNet) {
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
      th = new ProcessModelWriteThread(dbHelper, table, column, id, pair[1]);
      th.run();
    } else {
      th = new ProcessModelReadThread(processModelProvider.getContext(), dbHelper, table, column, id, syncStateColumn, pair[0], notifyNet);
      th.start();
    }

    return readMode ? pair[0] : pair[1];
  }

}
