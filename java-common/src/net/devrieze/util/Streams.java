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
