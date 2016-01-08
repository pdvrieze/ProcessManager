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

package nl.adaptivity.process.android;

import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.util.Util;
import nl.adaptivity.util.Util.NameChecker;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;


public final class ProcessModelUtil {
  private ProcessModelUtil() {}

  public static String suggestNewName(final Context context, CharSequence previousName) {


    return Util.suggestNewName(previousName, new NameChecker() {

      ContentResolver resolver = context.getContentResolver();

      @Override
      public boolean isAvailable(String string) {
        Cursor result = resolver.query(ProcessModels.CONTENT_ID_URI_BASE, new String[] { BaseColumns._ID }, "name = ?", new String[] { string} , null);
        try {
          if (result.moveToFirst()) {
            return false;
          } else {
            return true;
          }
        } finally {
          result.close();
        }
      }
    });
  }

}
