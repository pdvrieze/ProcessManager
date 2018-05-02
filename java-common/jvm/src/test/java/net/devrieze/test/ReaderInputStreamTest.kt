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

package net.devrieze.test

import net.devrieze.util.ReaderInputStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.io.IOException
import java.io.StringReader
import java.nio.charset.Charset

import java.util.Arrays.copyOf


class ReaderInputStreamTest {

    private lateinit var mSource: StringReader

    private lateinit var mResultBuffer: ByteArray

    private lateinit var mStream: ReaderInputStream

    private lateinit var mExpected: ByteArray

    @BeforeEach
    fun setUp() {
        mResultBuffer = ByteArray(100)
        mSource = StringReader("Glückliche gruße øæ mit €uros")
        mExpected = byteArrayOf(71, 108, 0xc3.toByte(), 188.toByte(), 99, 107, 108, 105, 99, 104, 101, 32, 103, 114,
                                117, 195.toByte(), 159.toByte(), 101, 32, 195.toByte(), 184.toByte(), 195.toByte(),
                                166.toByte(), 32, 109, 105, 116, 32, 226.toByte(), 130.toByte(), 172.toByte(), 117, 114,
                                111, 115)
        mStream = ReaderInputStream(Charset.forName("UTF-8"), mSource)
    }

    @Test
    @Throws(IOException::class)
    fun testRead() {
        for (i in mExpected.indices) {
            assertEquals(mExpected[i], mStream.read(), "Read(#$i)")
        }
        assertEquals(-1, mStream.read(), "Reading EOF")
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray() {
        mResultBuffer[0] = -1
        mResultBuffer[mExpected.size] = -1
        mResultBuffer[mExpected.size + 1] = -1
        val count = mStream.read(mResultBuffer, 1, mResultBuffer.size - 1)
        assertEquals(mExpected.size, count)
        assertEquals(-1, mResultBuffer[0])
        assertEquals(-1, mResultBuffer[mExpected.size + 1])
        assertEquals(mExpected[mExpected.size - 1], mResultBuffer[mExpected.size])
        val actuals = ByteArray(count)
        System.arraycopy(mResultBuffer, 1, actuals, 0, count)
        assertEquals(mExpected, actuals)
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray2() {
        val testLength = 17
        mResultBuffer[0] = -1
        mResultBuffer[testLength] = -1
        mResultBuffer[testLength + 1] = -1
        val count = mStream.read(mResultBuffer, 1, testLength)
        assertEquals(testLength, count)
        assertEquals(-1, mResultBuffer[0])
        assertEquals(-1, mResultBuffer[testLength + 1])
        assertEquals(mExpected[testLength - 1], mResultBuffer[testLength])
        val actuals = ByteArray(count)
        System.arraycopy(mResultBuffer, 1, actuals, 0, count)
        assertEquals(copyOf(mExpected, testLength), actuals)
        assertEquals(mExpected[testLength], mStream.read())
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray3() {
        assertEquals(mExpected[0], mStream.read())
        val testLength = 16
        mResultBuffer[0] = -1
        mResultBuffer[1] = mExpected[0]
        mResultBuffer[testLength + 1] = -1
        mResultBuffer[testLength + 2] = -1
        val count = mStream.read(mResultBuffer, 2, testLength)
        assertEquals(testLength, count)
        assertEquals(-1, mResultBuffer[0])
        assertEquals(-1, mResultBuffer[testLength + 2])
        assertEquals(mExpected[testLength], mResultBuffer[testLength + 1])
        val actuals = ByteArray(count)
        System.arraycopy(mResultBuffer, 1, actuals, 0, count)
        assertEquals(copyOf(mExpected, testLength), actuals)
        assertEquals(mExpected[testLength + 1], mStream.read())
    }

}
