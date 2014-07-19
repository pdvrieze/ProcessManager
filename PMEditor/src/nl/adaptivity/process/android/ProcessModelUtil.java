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

  public static String suggestNewName(final Context pContext, String pPreviousName) {


    return Util.suggestNewName(pPreviousName, new NameChecker() {

      ContentResolver resolver = pContext.getContentResolver();

      @Override
      public boolean isAvailable(String pString) {
        Cursor result = resolver.query(ProcessModels.CONTENT_ID_URI_BASE, new String[] { BaseColumns._ID }, "name = ?", new String[] { pString} , null);
        if (result.moveToFirst()) {
          return false;
        } else {
          return true;
        }
      }
    });
  }

}
