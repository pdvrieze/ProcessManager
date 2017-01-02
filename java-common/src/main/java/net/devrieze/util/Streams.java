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

package net.devrieze.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;


public final class Streams {

  private Streams() {}

  public static String toString(final InputStream pInputStream, final Charset pCharset) throws IOException {
    return toString(new InputStreamReader(pInputStream, pCharset));
  }

  public static String toString(final Reader pReader) throws IOException {
    final StringBuilder result = new StringBuilder();
    final char[] buffer = new char[0x8ff];
    int count = pReader.read(buffer);
    while (count >= 0) {
      result.append(buffer, 0, count);
      count = pReader.read(buffer);
    }
    return result.toString();
  }

}
