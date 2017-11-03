/*
 * Copyright (c) 2017.
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
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;


public class ReaderInputStream extends InputStream {

  private final Reader mReader;

  private final CharsetEncoder mEncoder;

  private final CharBuffer mIn;

  private final ByteBuffer mOut;

  public ReaderInputStream(final Charset charset, final Reader reader) {
    mReader = reader;
    mEncoder = charset.newEncoder();
    mIn = CharBuffer.allocate(1024);
    mOut = ByteBuffer.allocate(Math.round((1024 * mEncoder.averageBytesPerChar()) + 0.5f));
    mIn.position(mIn.limit());
    mOut.position(mOut.limit());
  }

  @Override
  public int read() throws IOException {
    if (mOut.remaining() == 0) {

      mOut.rewind();

      updateBuffers();
    }
    if (mOut.remaining() == 0) {
      return -1;
    }
    return mOut.get();
  }


  @Override
  public int read(final byte[] bytes, final int offset, final int len) throws IOException {
    // If we still have stuff in the buffer, flush that first.
    if (mOut.remaining() > 0) {
      final int length = Math.min(mOut.remaining(), len);
      mOut.get(bytes, offset, length);
      return length;
    } else {
      final ByteBuffer out = ByteBuffer.wrap(bytes, offset, len);
      updateBuffers2(out);
      if ((out.remaining() == 0) && (len > 0)) {
        return -1;
      }
      return out.remaining();
    }
  }

  private void updateBuffers() throws IOException {
    mOut.limit(mOut.capacity());
    updateBuffers2(mOut);
  }

  private void updateBuffers2(final ByteBuffer out) throws IOException {
    out.mark();
    if (mIn.remaining() == 0) {
      mIn.rewind();
      mIn.limit(mIn.capacity());
      final int readResult = mReader.read(mIn);
      if (readResult == -1) {
        mIn.limit(mIn.position());
        mIn.position(0);
        CoderResult encodeResult = mEncoder.encode(mIn, out, true);
        if (encodeResult.isError()) { encodeResult.throwException(); }
        out.limit(out.position());
        out.reset();
        return;
      } else {
        mIn.limit(readResult);
        mIn.position(0);
      }
    }
    CoderResult encodeResult = mEncoder.encode(mIn, out, false);
    if (encodeResult.isError()) { encodeResult.throwException();}

    out.limit(out.position());
    out.reset();
  }

  public CodingErrorAction malformedInputAction() {
    return mEncoder.malformedInputAction();
  }

  public final CharsetEncoder onMalformedInput(final CodingErrorAction pNewAction) {
    return mEncoder.onMalformedInput(pNewAction);
  }

  public CodingErrorAction unmappableCharacterAction() {
    return mEncoder.unmappableCharacterAction();
  }

  public final CharsetEncoder onUnmappableCharacter(final CodingErrorAction pNewAction) {
    return mEncoder.onUnmappableCharacter(pNewAction);
  }

  @Override
  public void close() throws IOException {
    mReader.close();
  }

  /**
   * Skip characters, not bytes.
   */
  @Override
  public long skip(final long pN) throws IOException {
    return mReader.skip(pN);
  }

  @Override
  public boolean markSupported() {
    return mReader.markSupported();
  }

  @Override
  public synchronized void mark(final int pReadAheadLimit) {
    try {
      mReader.mark(pReadAheadLimit);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public synchronized void reset() throws IOException {
    mReader.reset();
  }

}
