package nl.adaptivity.android.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by pdvrieze on 12/08/15.
 */
public class LogUtil {
  private LogUtil() {}

  public static void logResponse(final String pTag, final int pDebugLevel, final String pUrl, final String pStatusLine, final InputStream pContent) throws
          IOException {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append("Error updating the server for url: ")
                .append(pUrl).append("\n    ")
                .append(pStatusLine).append("\n    ");
    BufferedReader contentBuffer = new BufferedReader(new InputStreamReader(pContent));
    String line = contentBuffer.readLine();
    while (line!=null) {
      errorMessage.append("    ").append(line).append('\n');
      line = contentBuffer.readLine();
    }
    Log.d(pTag, errorMessage.toString());

  }
}
