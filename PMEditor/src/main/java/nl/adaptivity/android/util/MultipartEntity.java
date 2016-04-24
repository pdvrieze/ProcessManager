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

package nl.adaptivity.android.util;

import android.util.Pair;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MultipartEntity extends AbstractHttpEntity {

  private static final String MIMETYPE_BASE = "multipart/formdata";
  private static final String CRLF="\r\n";
  private static final String CONTENT_DISPOSITION = "Content-Disposition: form-data; name=";

  private final List<Pair<String, HttpEntity>> mContent = new ArrayList<>();

  private String mBoundary;

  public MultipartEntity() {
    mBoundary = generateBoundary();
  }

  private static String generateBoundary() {
    final Random rnd = new Random();
    return "--"+MultipartEntity.class.getSimpleName()+'-'+Long.toHexString(rnd.nextLong());
  }

  @Override
  public InputStream getContent() throws IOException, IllegalStateException {
    // TODO do this in a less memory intensive way
    final long                  cl  = getContentLength();
    final ByteArrayOutputStream bos = cl < 0 ? new ByteArrayOutputStream() :  new ByteArrayOutputStream((int) cl);
    writeTo(bos);
    return new ByteArrayInputStream(bos.toByteArray());
  }

  @SuppressWarnings("EmptyMethod")
  private void ensureBoundary() {
    // TODO do nothing for now. Should verify the viability of the boundary
  }

  @Override
  public String getContentType() {
    return MIMETYPE_BASE+"; boundary="+mBoundary.substring(2);
  }

  @Override
  public void setContentType(final String contentType) {
    if (contentType.startsWith(MIMETYPE_BASE)) {
      int i = contentType.indexOf(';', MIMETYPE_BASE.length())+1;
      while (i>=0) {
        final int    j = contentType.indexOf(';', i);
        final String param;
        if (j>=0) {
          param = contentType.substring(i).trim();
        } else {
          param = contentType.substring(i, j).trim();
        }
        final int e = param.indexOf('=');
        if (e>=0) {
          final String parName  = param.substring(0, e).trim();
          final String parValue = param.substring(e + 1).trim();
          if ("boundary".equals(parName)) {
            if (parValue.startsWith("--")) {
              mBoundary = parValue;
            } else {
              mBoundary = "--"+parValue;
            }
          }
        }
        i=j+1;
      }
    } else {
      throw new UnsupportedOperationException("The type must be multipart/formdata");
    }
  }

  @Override
  public long getContentLength() {
    if (mContent.isEmpty()) { return 0; }
    long result = -2; // First boundary does not include a CRLF
    for (final Pair<String, HttpEntity> content: mContent) {
      final long cl = getContentLength(content.first, content.second);
      if (cl<0) { return cl; } // we don't know if a child doesn't know
      result+=cl;
    }
    result+=6+mBoundary.length();
    return result;
  }

  private long getContentLength(final String name, final HttpEntity entity) {
    final long el = entity.getContentLength();
    if (el>=0) {
      return getBoundary(name, entity).length()+el;
    } else {
      return el;
    }
  }

  private String getBoundary(final String name, final HttpEntity entity) {
    final StringBuilder result = new StringBuilder();
    result.append(CRLF).append(mBoundary).append(CRLF);
    result.append(CONTENT_DISPOSITION).append(name).append(CRLF);
    {
      final String ct = entity.getContentType();
      if (ct!=null) {
        result.append("Content-Type: ").append(ct).append(CRLF);
      }
    }
    {
      final String ce = entity.getContentEncoding();
      if (ce!=null) {
        result.append("Content-Transfer-Encoding: ").append(ce).append(CRLF);
      }
    }
    final long cl = entity.getContentLength();
    if (cl>=0) {
      result.append("Content-Length: ").append(cl).append(CRLF);
    }
    result.append(CRLF);
    return result.toString();
  }

  @Override
  public boolean isRepeatable() {
    for(final Pair<String, HttpEntity> elem:mContent) {
      if (!elem.second.isRepeatable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isStreaming() {
    for(final Pair<String, HttpEntity> elem:mContent) {
      if (elem.second.isStreaming()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void writeTo(final OutputStream outstream) throws IOException {
    ensureBoundary();
    final Charset cs   = Charset.forName("UTF8");
    int           skip = 2;
    for(final Pair<String, HttpEntity> elem:mContent) {
      final ByteBuffer boundary = cs.encode(getBoundary(elem.first, elem.second));
      final int        pos      = boundary.position();
      if (pos>0) {
        boundary.flip();
      }
      outstream.write(boundary.array(), boundary.arrayOffset()+skip, boundary.limit()-skip);
      elem.second.writeTo(outstream);
      skip = 0;
    }
    final ByteBuffer boundary = cs.encode(CRLF+mBoundary+"--"+CRLF);
    outstream.write(boundary.array(), boundary.arrayOffset(), boundary.limit());
    outstream.flush();
  }

  public void add(final String name, final HttpEntity childEntity) {
    mContent.add(new Pair<>(name, childEntity));
  }

}
