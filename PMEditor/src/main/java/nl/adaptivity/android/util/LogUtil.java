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

  public static void logResponse(final String tag, final int debugLevel, final String url, final String statusLine, final InputStream content) throws
          IOException {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append("Error updating the server for url: ")
                .append(url).append("\n    ")
                .append(statusLine).append("\n    ");
    BufferedReader contentBuffer = new BufferedReader(new InputStreamReader(content));
    String line = contentBuffer.readLine();
    while (line!=null) {
      errorMessage.append("    ").append(line).append('\n');
      line = contentBuffer.readLine();
    }
    Log.d(tag, errorMessage.toString());

  }
}
