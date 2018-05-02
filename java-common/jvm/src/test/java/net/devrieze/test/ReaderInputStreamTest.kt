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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.io.IOException
import java.io.StringReader
import java.nio.charset.Charset

import java.util.Arrays.copyOf


class ReaderInputStreamTest {

    private lateinit var source: StringReader

    private lateinit var resultBuffer: ByteArray

    private lateinit var stream: ReaderInputStream

    private lateinit var expected: ByteArray

    @BeforeEach
    fun setUp() {
        resultBuffer = ByteArray(100)
        source = StringReader("Glückliche gruße øæ mit €uros")
        expected = byteArrayOf(71, 108, 0xc3.toByte(), 188.toByte(), 99, 107, 108, 105, 99, 104, 101, 32, 103, 114,
                               117, 195.toByte(), 159.toByte(), 101, 32, 195.toByte(), 184.toByte(), 195.toByte(),
                               166.toByte(), 32, 109, 105, 116, 32, 226.toByte(), 130.toByte(), 172.toByte(), 117, 114,
                               111, 115)
        stream = ReaderInputStream(Charset.forName("UTF-8"), source)
    }

    @Test
    @Throws(IOException::class)
    fun testRead() {
        for (i in expected.indices) {
            assertEquals(expected[i].toInt(), stream.read(), "Read(#$i)")
        }
        assertEquals(-1, stream.read(), "Reading EOF")
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray() {
        resultBuffer[0] = -1
        resultBuffer[expected.size] = -1
        resultBuffer[expected.size + 1] = -1
        val count = stream.read(resultBuffer, 1, resultBuffer.size - 1)
        assertEquals(expected.size, count)
        assertEquals(-1, resultBuffer[0])
        assertEquals(-1, resultBuffer[expected.size + 1])
        assertEquals(expected[expected.size - 1], resultBuffer[expected.size])
        val actuals = ByteArray(count)
        System.arraycopy(resultBuffer, 1, actuals, 0, count)
        assertArrayEquals(expected, actuals)
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray2() {
        val testLength = 17
        resultBuffer[0] = -1
        resultBuffer[testLength] = -1
        resultBuffer[testLength + 1] = -1
        val count = stream.read(resultBuffer, 1, testLength)
        assertEquals(testLength, count)
        assertEquals(-1, resultBuffer[0])
        assertEquals(-1, resultBuffer[testLength + 1])
        assertEquals(expected[testLength - 1], resultBuffer[testLength])
        val actuals = ByteArray(count)
        System.arraycopy(resultBuffer, 1, actuals, 0, count)
        assertArrayEquals(copyOf(expected, testLength), actuals)
        assertEquals(expected[testLength].toInt(), stream.read())
    }

    @Test
    @Throws(IOException::class)
    fun testReadByteArray3() {
        assertEquals(expected[0].toInt(), stream.read())
        val testLength = 16
        resultBuffer[0] = -1
        resultBuffer[1] = expected[0]
        resultBuffer[testLength + 1] = -1
        resultBuffer[testLength + 2] = -1
        val count = stream.read(resultBuffer, 2, testLength)
        assertEquals(testLength, count)
        assertEquals(-1, resultBuffer[0])
        assertEquals(-1, resultBuffer[testLength + 2])
        assertEquals(expected[testLength], resultBuffer[testLength + 1])
        val actuals = ByteArray(count)
        System.arraycopy(resultBuffer, 1, actuals, 0, count)
        assertArrayEquals(copyOf(expected, testLength), actuals)
        assertEquals(expected[testLength + 1].toInt(), stream.read())
    }

}
