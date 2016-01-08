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
