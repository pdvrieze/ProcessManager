/*
 * Copyright (c) 2018.
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

/*
 * HttpRequest.java
 *
 * Created on 15 May 2001, 14:34
 */

package net.devrieze.util.webServer;

import net.devrieze.lang.Const;
import net.devrieze.util.DebugTool;

import javax.activation.DataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import java.io.*;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class is an abstraction of an http request. It takes care of reading the
 * request.
 *
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
public class HttpRequest {

  private static class BytesDatasource implements DataSource {

    private final String name;
    private final byte[]   content;
    private final MimeType contentType;

    public BytesDatasource(final byte[] content, final String name, final MimeType contentType) {
      this.name = name;
      this.content = content;
      this.contentType = contentType;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(content);
    }

    @Override
    public String getContentType() {
      return contentType.toString();
    }
  }

  public static final MimeType TEXT_PLAIN = mimeType("text/plain");

  /** Enumeration for the various HTTP methods supported. */
  public enum Method {
    OPTIONS("OPTIONS"),
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    TRACE("TRACE"),
    STOP("STOP");

    private final String mString;

    Method(final String pString) {
      mString = pString;
    }

    @Override
    public String toString() {
      return mString;
    }

    public static Method find(final String pName) {
      final String name = pName.toUpperCase();
      for (final Method m : Method.values()) {
        if (name.equals(m.mString)) {
          return m;
        }
      }
      return null;
    }
  }

  private HashMap<String, String> mHeaders;

  private final HashMap<String, String> mQueries;

  private String mURI; /* the request-uri */

  private Method mMethod; /*
                           * the method that is requested
                           */

  private String mVersion = "HTTP/1.0"; /*
                                         * either this (if omitted) or HTTP/1.1
                                         */

  private int mResponse = 0;

  /*
   * public int getHeaderCount(){ return headers.size(); }
   */

  /**
   * Creates new HttpRequest. It parses the text that is available from the in
   * stream, and uses that to initialize itself.
   *
   * @param pIn The stream that get read
   */
  public HttpRequest(final BufferedReader pIn) {
    mQueries = new HashMap<>();

    if (pIn != null) {
      mHeaders = new HashMap<>();

      try {
        String s;
        do {
          s = pIn.readLine();
        } while (s.trim().isEmpty());

        processFirstLine(s);

        /* now process the other parts of the request */
        s = pIn.readLine();
        while ((s != null) && (!s.trim().isEmpty())) {
          /* add headers */
          int i = s.indexOf(':');

          if (i >= 0) {
            final String t = s.substring(0, i).trim().toLowerCase();
            s = s.substring(i + 1).trim();
            mHeaders.put(t, s);
          }
          s = pIn.readLine();
          i = s.indexOf(':');
        }

        final String o = mHeaders.get("content-length");

        if (o != null) {
          final int cl = Integer.parseInt(o);
          final char[] mContents = new char[cl];

          for (int i = 0; i < cl; i++) {
            mContents[i] = (char) pIn.read();
          }

          final String ct = mHeaders.get("content-type");

          if (ct.toLowerCase().equals("application/x-www-form-urlencoded")) {
            final String query = new String(mContents);
            parseUrlEncodedHelper(mQueries, query);
          }
        } else {
          String query = null;
          final int i = mURI.indexOf('?');

          if (i >= 0) {
            query = mURI.substring(i);
          }

          if (query != null) {
            parseUrlEncodedHelper(mQueries, query);
          }
        }
      } catch (final Exception e) {
        DebugTool.handle(e);
      }
    }
  }

  public static MimeType mimeType(final String pRawData) {
    try {
      return new MimeType(pRawData);
    } catch (final MimeTypeParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Get's the value of the header with the specified name. If there is no such
   * header in the request, returns NULL
   *
   * @param pKey The header name
   * @return the value
   */
  public final String getHeader(final String pKey) {
    return mHeaders.get(pKey);
  }

  /**
   * Returns the HTTP method that is requested.
   *
   * @return the http method that has been requested
   */
  public Method getMethod() {
    return mMethod;
  }

  /**
   * Returns all queries made in this request. Often {@link #getQuery}is easier
   * though
   *
   * @return A Hashtable with all queries.
   */
  public Map<String,String> getQueries() {
    return Collections.unmodifiableMap(mQueries);
  }

  /**
   * Returns the value of a certain query. This is usefull for forms, and using
   * the values returned by them.
   *
   * @param pKey The query that is requested. If the post method has been used,
   *          only the values submitted by that method are parsed. When not
   *          used, the url arguments are parsed.
   * @return The value of that key, or NULL if there is no value.
   */
  public String getQuery(final String pKey) {
    return mQueries.get(pKey);
  }

  /**
   * Sets the response that this request is going to get. This makes it possible
   * to keep track of this code.
   *
   * @param pResponse the http response code
   */
  public void setResponse(final int pResponse) {
    mResponse = pResponse;
  }

  /**
   * The response that this request is going to get.
   *
   * @return the http result code for this response
   */
  public int getResponse() {
    return mResponse;
  }

  /**
   * This method provides the possibility to change the request to request a
   * different resource.
   *
   * @param pURI the new uri
   */
  public void setURI(final String pURI) {
    mURI = pURI;
  }

  /*
   * private void putHeader(String key, String value){ headers.put(key,value); }
   */

  /**
   * Returns the uri that is specified in the request.
   *
   * @return The uri specified
   */
  public String getURI() {
    return mURI;
  }

  /**
   * Returns the http version requested.
   *
   * @return the http version __Identifier (like HTTP/1.0)
   */
  public String getVersion() {
    return mVersion;
  }

  private void setMethod(final Method pMethod) {
    mMethod = pMethod;
  }

  /**
   * @deprecated use {@link #parseUrlEncodedHelper(Map, CharSequence)} directly.
   */
  @Deprecated
  private void procesUrlEncoded(final String pQuery) {
    parseUrlEncodedHelper(mQueries, pQuery);
  }

  /**
   * @param pIn
   * @param pContentType The mime type that is the content. This contains
   *          relevant parameters for parsing.
   * @param pEncoding The character encoding of the content.
   * @return
   * @throws IOException When a read error occurs.
   */
  public static Map<String, DataSource> parseMultipartFormdata(final InputStream pIn, final MimeType pContentType, final String pEncoding) throws IOException {
    if (pContentType == null) {
      throw new NullPointerException("Content type must be given");
    }
    final String boundary = pContentType.getParameter("boundary");
    if (boundary == null) {
      throw new IllegalArgumentException("Content type does not specify a boundary");
    }

    try (final BufferedInputStream in = new BufferedInputStream(pIn)) {

      int b = in.read();
      if (b == Const.INSTANCE.get_CR()) {
        b = in.read();
        if (b == Const.INSTANCE.get_LF()) {
          b = in.read();
        }
      }
      int curPos = 0; // We just optionally read a CRLF
      int stage = 2;
      MimeType contentDisposition = null;
      @SuppressWarnings("resource")
      ByteArrayOutputStream content = null;
      ByteArrayOutputStream wsBuffer = null;
      StringBuilder headerLine = null;
      MimeType contentType = TEXT_PLAIN;
      final Map<String, DataSource> result = new HashMap<>();
      while (b >= 0) {


        if ((stage == 0) && (b == Const.INSTANCE.get_CR())) {
          stage = 1;
        } else if ((stage == 1) && (b == Const.INSTANCE.get_LF())) {
          stage = 2;
        } else if (((stage == 2) || (stage == 3)) && (b == '-')) { // Reading two hyphens
          ++stage;
        } else if ((stage == 4) && (b == boundary.charAt(curPos))) { // Reading the actual boundary
          ++curPos;
          if (curPos == boundary.length()) {
            stage = 5;
            curPos = 0;
          }
        } else if ((stage == 5) && ((b == ' ') || (b == '\t'))) {
          if (wsBuffer == null) {
            wsBuffer = new ByteArrayOutputStream();
          } // Remember to be able to replay
          wsBuffer.write(b);
        } else if ((stage == 5) && (b == '-') && (wsBuffer == null)) {
          b = in.read();
          if (b != '-') {
            wsBuffer = new ByteArrayOutputStream();
            wsBuffer.write('-');
            continue; // This will fail in the next loop iteration
          }
          // We found an end of all data.
          if (content != null) { // First time don't do this
            final String contentName = contentDisposition==null ? null : contentDisposition.getParameter("name");
            if (contentName == null) {
              result.put(Integer.toString(result.size()), toDataSource(content, Integer.toString(result.size()), contentType));
            } else {
              result.put(contentName, toDataSource(content, contentName, contentType));
            }
          }
          break; // Go out of the loop.
        } else if ((stage == 5) && (b == Const.INSTANCE.get_CR())) {
          stage = 6;
        } else if ((stage == 6) && (b == Const.INSTANCE.get_LF())) {
          stage = 7; // We completed, next step is to finish previous block, and then read headers
          wsBuffer = null;
          if (content != null) { // First time don't do this
            final String contentName = contentDisposition==null ? null : contentDisposition.getParameter("name");
            if (contentName == null) {
              result.put(Integer.toString(result.size()), toDataSource(content, Integer.toString(result.size()), contentType));
            } else {
              result.put(contentName, toDataSource(content, contentName, contentType));
            }
          }
          contentDisposition = null;
          headerLine = new StringBuilder();
        } else if (stage == 7) {
          if (b == Const.INSTANCE.get_CR()) {
            b = in.read();
            if (b != Const.INSTANCE.get_LF()) {
              if (content!=null) { content.close(); }
              throw new IllegalArgumentException("Header lines should be separated by CRLF, not CR only");
            }
            if (headerLine == null) {
              if (content!=null) { content.close(); }
              throw new AssertionError("Headerline is null, but never should be");
            }
            if (headerLine.length() == 0) {
              headerLine = null;
              content = new ByteArrayOutputStream();
              stage = 0;
            } else {
              final String s = headerLine.toString();
              headerLine = new StringBuilder();
              final int colonPos = s.indexOf(':');
              if (colonPos >= 1) {
                final String name = s.substring(0, colonPos).trim();
                final String val = s.substring(colonPos + 1).trim();
                final String nmLC = name.toLowerCase();
                if ("content-disposition".equals(nmLC)) {
                  try {
                    if (val.startsWith("form-data")) {
                      contentDisposition = new MimeType("multipart/" + val);
                    } else {
                      contentDisposition = new MimeType(val);
                    }
                  } catch (final MimeTypeParseException ex) {
                    // Just ignore invalid content dispositions
                  }
                } else if ("content-type".equals(nmLC)) {
                  try {
                    contentType = new MimeType(val);
                  } catch (final MimeTypeParseException ex) {
                    // Just ignore invalid content dispositions
                  }
                }
              }
            }
          } else {
            if (headerLine==null) { throw new AssertionError("Headerline is null, but never should be"); }
            headerLine.append((char) b);
          }
        } else {
          if (content != null) { // Ignore any preamble (it's legal that there is stuff so ignore it
            // Reset
            if (stage > 0) {
              content.write(Const.INSTANCE.get_CR());
              if (stage > 1) {
                content.write(Const.INSTANCE.get_LF());
                if (stage > 2) {
                  content.write('-');
                  if (stage > 3) {
                    content.write('-');
                    for (int i = 0; i < curPos; ++i) {
                      content.write(boundary.charAt(i));
                    }
                    if (stage > 4) {
                      final byte[] ws = wsBuffer ==null ? new byte[0] : wsBuffer.toByteArray();
                      for (final byte w : ws) {
                        content.write(ws);
                      }
                      if (stage > 5) {
                        content.write(Const.INSTANCE.get_CR());
                      }
                    }
                  }
                }
              }
            }

            content.write(b);
          }
          curPos = 0;
        }
        b = in.read();

      }


      return result;
    }
  }

  private static DataSource toDataSource(final ByteArrayOutputStream content, final String name, final MimeType contentType) {
    return new BytesDatasource(content.toByteArray(), name, contentType);
  }

  public static Map<String, String> parseUrlEncoded(final CharSequence pSource) {
    if (pSource == null) {
      return Collections.emptyMap();
    }

    return parseUrlEncodedHelper(new TreeMap<String, String>(), pSource);
  }

  private static Map<String, String> parseUrlEncodedHelper(final Map<String, String> pResult, final CharSequence pSource) {
    if (pSource == null) {
      return pResult;
    }
    CharSequence query = pSource;
    if ((query.length() > 0) && (query.charAt(0) == '?')) {
      /* strip questionmark */
      query = query.subSequence(1, query.length());
    }

    int startPos = 0;
    CharSequence key = null;
    for (int i = 0; i < query.length(); ++i) {
      if (key == null) {
        if ('=' == query.charAt(i)) {
          key = query.subSequence(startPos, i);
          startPos = i + 1;
        }
      } else {
        if (('&' == query.charAt(i)) || (';' == query.charAt(i))) {
          final String value;
          try {
            value = URLDecoder.decode(query.subSequence(startPos, i).toString(), "UTF-8");
          } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
          pResult.put(key.toString(), value.toString());
          key = null;
        }
      }
    }
    if (key == null) {
      key = query.subSequence(startPos, query.length());
      if (key.length() > 0) {
        pResult.put(key.toString(), "");
      }
    } else {
      try {
        final String value = URLDecoder.decode(query.subSequence(startPos, query.length()).toString(), "UTF-8");
        pResult.put(key.toString(), value);
      } catch (final UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

    }
    return pResult;
  }

  private void processFirstLine(final String pLine) {
    String s = pLine.trim();

    int i = s.indexOf(' ');

    if (i > 0) {
      setMethod(Method.find(s.substring(0, i)));
      s = s.substring(i + 1);

      i = s.indexOf(' ');
      if (i > 0) {
        mURI = s.substring(0, i);
        s = s.substring(i + 1);
        mVersion = s;
      } else {
        mURI = s;
      }
    } else {
      mMethod = Method.find(s);
    }
  }
}
