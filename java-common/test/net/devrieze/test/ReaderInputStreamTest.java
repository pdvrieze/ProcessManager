package net.devrieze.test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import net.devrieze.util.ReaderInputStream;


public class ReaderInputStreamTest {

  private StringReader mSource;

  private byte[] mResultBuffer;

  private ReaderInputStream mStream;

  private byte[] mExpected;

  @Before
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
      assertEquals("Read(#" + i + ")", mExpected[i], mStream.read());
    }
    assertEquals("Reading EOF", -1, mStream.read());
  }

  @Test
  public void testReadByteArray() throws IOException {
    mResultBuffer[0] = -1;
    mResultBuffer[mExpected.length] = -1;
    mResultBuffer[mExpected.length + 1] = -1;
    final int count = mStream.read(mResultBuffer, 1, mResultBuffer.length - 1);
    assertEquals(mExpected.length, count);
    assertEquals(-1, mResultBuffer[0]);
    assertEquals(-1, mResultBuffer[mExpected.length + 1]);
    assertEquals(mExpected[mExpected.length - 1], mResultBuffer[mExpected.length]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertArrayEquals(mExpected, actuals);
  }

  @Test
  public void testReadByteArray2() throws IOException {
    final int testLength = 17;
    mResultBuffer[0] = -1;
    mResultBuffer[testLength] = -1;
    mResultBuffer[testLength + 1] = -1;
    final int count = mStream.read(mResultBuffer, 1, testLength);
    assertEquals(testLength, count);
    assertEquals(-1, mResultBuffer[0]);
    assertEquals(-1, mResultBuffer[testLength + 1]);
    assertEquals(mExpected[testLength - 1], mResultBuffer[testLength]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertArrayEquals(Arrays.copyOf(mExpected, testLength), actuals);
    assertEquals(mExpected[testLength], mStream.read());
  }

  @Test
  public void testReadByteArray3() throws IOException {
    assertEquals(mExpected[0], mStream.read());
    final int testLength = 16;
    mResultBuffer[0] = -1;
    mResultBuffer[1] = mExpected[0];
    mResultBuffer[testLength + 1] = -1;
    mResultBuffer[testLength + 2] = -1;
    final int count = mStream.read(mResultBuffer, 2, testLength);
    assertEquals(testLength, count);
    assertEquals(-1, mResultBuffer[0]);
    assertEquals(-1, mResultBuffer[testLength + 2]);
    assertEquals(mExpected[testLength], mResultBuffer[testLength + 1]);
    final byte[] actuals = new byte[count];
    System.arraycopy(mResultBuffer, 1, actuals, 0, count);
    assertArrayEquals(Arrays.copyOf(mExpected, testLength), actuals);
    assertEquals(mExpected[testLength + 1], mStream.read());
  }

}
