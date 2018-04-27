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

package net.devrieze.test;

import net.devrieze.util.ReaderInputStream;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import static java.util.Arrays.copyOf;
import static org.testng.Assert.assertEquals;


public class ReaderInputStreamTest {

  private StringReader mSource;

  private byte[] mResultBuffer;

  private ReaderInputStream mStream;

  private byte[] mExpected;

  @BeforeMethod
  public void setUp() {
    mResultBuffer = new byte[100];
    mSource = new StringReader("Glückliche gruße øæ mit €uros");
    mExpected = new byte[] { 71, 108, (byte) 0xc3, (byte) 188, 99, 107, 108, 105, 99, 104, 101, 32, 103, 114, 117, (byte) 195, (byte) 159,
                            101, 32, (byte) 195, (byte) 184, (byte) 195, (byte) 166, 32, 109, 105, 116, 32, (byte) 226, (byte) 130,
                            (byte) 172, 117, 114, 111, 115 };
    mStream = new ReaderInputStream(Charset.forName("UTF-8"), mSource);
  }

  @Test
  public void testRead() throws IOException {
    for (int i = 0; i < mExpected.length; ++i) {
      Assert.assertEquals(mStream.read(), mExpected[i], "Read(#" + i + ")");
    }
    Assert.assertEquals(mStream.read(), -1, "Reading EOF");
  }

  @Test
  public void testReadByteArray() throws IOException {
    mResultBuffer[0] = -1;
    mResultBuffer[mExpected.length] = -1;
    mResultBuffer[mExpected.length + 1] = -1;
    final int count = mStream.read(mResultBuffer, 1, mResultBuffer.length - 1);
    assertEquals(count, mExpected.length);
    assertEquals(mResultBuffer[0], -1);
    assertEquals(mResultBuffer[mExpected.length + 1], -1);
    assertEquals(mResultBuffer[mExpected.length], mExpected[mExpected.length - 1]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertEquals(actuals, mExpected);;
  }

  @Test
  public void testReadByteArray2() throws IOException {
    final int testLength = 17;
    mResultBuffer[0] = -1;
    mResultBuffer[testLength] = -1;
    mResultBuffer[testLength + 1] = -1;
    final int count = mStream.read(mResultBuffer, 1, testLength);
    assertEquals(count, testLength);
    assertEquals(mResultBuffer[0], -1);
    assertEquals(mResultBuffer[testLength + 1], -1);
    assertEquals(mResultBuffer[testLength], mExpected[testLength - 1]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertEquals(actuals, copyOf(mExpected, testLength));;
    assertEquals(mStream.read(), mExpected[testLength]);
  }

  @Test
  public void testReadByteArray3() throws IOException {
    assertEquals(mStream.read(), mExpected[0]);
    final int testLength = 16;
    mResultBuffer[0] = -1;
    mResultBuffer[1] = mExpected[0];
    mResultBuffer[testLength + 1] = -1;
    mResultBuffer[testLength + 2] = -1;
    final int count = mStream.read(mResultBuffer, 2, testLength);
    assertEquals(count, testLength);
    assertEquals(mResultBuffer[0], -1);
    assertEquals(mResultBuffer[testLength + 2], -1);
    assertEquals(mResultBuffer[testLength + 1], mExpected[testLength]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertEquals(actuals, copyOf(mExpected, testLength));;
    assertEquals(mStream.read(), mExpected[testLength + 1]);
  }

}
