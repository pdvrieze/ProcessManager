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

package nl.adaptivity.util.activation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;


public class UrlDataSource implements DataSource {

  private static MimetypesFileTypeMap _mimeMap;

  private final String mContentType;

  @NotNull private final URL mURL;

  private final InputStream mInputStream;

  private final Map<String, List<String>> mHeaders;

  public UrlDataSource(@NotNull final URL finalUrl) throws IOException {
    final URLConnection connection;
    connection = finalUrl.openConnection();
    {

      String contentType = connection.getContentType();
      if ("content/unknown".equals(contentType)) {
        contentType = getMimeTypeForFileName(finalUrl.getFile());
      }
      mContentType = contentType;
    }

    mInputStream = connection.getInputStream();

    mHeaders = connection.getHeaderFields();

    mURL = finalUrl;
  }

  @Override
  public String getContentType() {
    return mContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return mInputStream;
  }

  @Override
  public String getName() {
    return mURL.getPath();
  }

  @NotNull
  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not allowed");

  }

  private static String getMimeTypeForFileName(@NotNull final String fileName) {
    if (_mimeMap == null) {
      _mimeMap = new MimetypesFileTypeMap();
      _mimeMap.addMimeTypes("text/css css\ntext/html htm html shtml\nimage/png png\n");
    }
    return _mimeMap.getContentType(fileName);
  }

  public Map<String, List<String>> getHeaders() {
    return mHeaders;
  }

}
