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

package nl.adaptivity.process.tasks.data;

import nl.adaptivity.process.tasks.data.TaskProvider.Items;
import nl.adaptivity.process.tasks.data.TaskProvider.Options;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

@Deprecated
public class TasksOpenHelper extends SQLiteOpenHelper {

  public static final String DB_NAME = "tasks.db";
  private static final int DB_VERSION = 2;
  public static final String TABLE_NAME_TASKS = "tasks";
  public static final String TABLE_NAME_ITEMS = "items";
  public static final String TABLE_NAME_OPTIONS = "options";
  private static final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_NAME_TASKS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY )";
  private static final String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_NAME_ITEMS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY )";
  private static final String SQL_CREATE_OPTIONS_TABLE = "CREATE TABLE " + TABLE_NAME_OPTIONS + " (" +
      BaseColumns._ID+" INTEGER PRIMARY KEY )";

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
  public SQLiteDatabase getWritableDatabase() {
    throw new UnsupportedOperationException("This is no longer supported, use the merged database");
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
