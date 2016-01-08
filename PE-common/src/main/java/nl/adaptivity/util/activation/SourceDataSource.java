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
import org.jetbrains.annotations.Nullable;

import javax.activation.DataSource;
import javax.xml.transform.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO do this better still.
public class SourceDataSource implements DataSource {

  private final String mContentType;

  private final Source mContent;

  public SourceDataSource(final String contentType, final Source content) {
    mContentType = contentType;
    mContent = content;
  }

  @Override
  public String getContentType() {
    return mContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return Sources.toInputStream(mContent);
  }

  @Nullable
  @Override
  public String getName() {
    return null;
  }

  @NotNull
  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Can not write to sources");
  }

}
