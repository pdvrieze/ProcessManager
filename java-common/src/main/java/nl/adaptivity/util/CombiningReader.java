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

package nl.adaptivity.util;

import java.io.IOException;
import java.io.Reader;


/**
 * Reader that combines multiple "component" readers into one.
 * Created by pdvrieze on 01/11/15.
 */
public class CombiningReader extends Reader {

  private final Reader[] mSources;
  private int mCurrentSource;

  public CombiningReader(final Reader... pSources) {
    mSources = pSources;
    mCurrentSource = 0;
  }

  @Override
  public int read(final char[] cbuf, final int off, final int len) throws IOException {
    if (mCurrentSource>=mSources.length) { return -1; }
    Reader source = mSources[mCurrentSource];
    int i = source.read(cbuf, off, len);
    if (i<0) { source.close(); ++mCurrentSource; return read(cbuf, off, len); }
    return i;
  }

  @Override
  public void close() throws IOException {
    for(int i=mCurrentSource; i<mSources.length; ++i) {
      mSources[i].close();
    }
  }

  @Override
  public boolean ready() throws IOException {
    if (mCurrentSource>=mSources.length) { return false; }
    return mSources[mCurrentSource].ready();
  }

  @Override
  public boolean markSupported() {
    return super.markSupported();
  }

  @Override
  public void mark(final int readAheadLimit) throws IOException {
    throw new IOException("Mark not supported");
  }

  @Override
  public void reset() throws IOException {
    for(int i=mCurrentSource; i>=0; --i) {
      mSources[i].reset();
      mCurrentSource = i;
    }
  }
}
