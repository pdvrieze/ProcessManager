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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by pdvrieze on 12/08/15.
 */
public final class LogUtil {
  private LogUtil() {}

  public static void logResponse(final String tag, final int debugLevel, final String url, final String statusLine, final InputStream content) throws
          IOException {
    final StringBuilder errorMessage = new StringBuilder();
    errorMessage.append("Error updating the server for url: ")
                .append(url).append("\n    ")
                .append(statusLine).append("\n    ");
    final BufferedReader contentBuffer = new BufferedReader(new InputStreamReader(content));
    String               line          = contentBuffer.readLine();
    while (line!=null) {
      errorMessage.append("    ").append(line).append('\n');
      line = contentBuffer.readLine();
    }
    Log.d(tag, errorMessage.toString());

  }
}
