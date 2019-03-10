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

package net.devrieze.util

import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.*


/**
 * Inputstream that reads out of a reader
 */
class ReaderInputStream(charset: Charset, private val reader: Reader) : InputStream() {

    private val encoder: CharsetEncoder = charset.newEncoder()

    private val input: CharBuffer = CharBuffer.allocate(1024)

    private val output: ByteBuffer = ByteBuffer.allocate(Math.round(1024 * encoder.averageBytesPerChar() + 0.5f))

    init {
        input.position(input.limit())
        output.position(output.limit())
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (output.remaining() == 0) {
            output.rewind()

            updateBuffers()
        }

        return when (output.remaining()) {
            0    -> -1
            else -> output.get().toInt()
        }
    }


    @Throws(IOException::class)
    override fun read(bytes: ByteArray, offset: Int, len: Int): Int {
        // If we still have stuff in the buffer, flush that first.
        return when {
            output.remaining() > 0 -> {
                minOf(output.remaining(), len).also {length ->
                    output.get(bytes, offset, length)
                }
            }
            else                   -> {
                val out = ByteBuffer.wrap(bytes, offset, len)
                updateBuffers2(out)
                if (out.remaining() == 0 && len > 0) {
                    -1
                } else out.remaining()
            }
        }
    }

    @Throws(IOException::class)
    private fun updateBuffers() {
        output.limit(output.capacity())
        updateBuffers2(output)
    }

    @Throws(IOException::class)
    private fun updateBuffers2(out: ByteBuffer) {
        out.mark()
        if (input.remaining() == 0) {
            input.rewind()
            input.limit(input.capacity())
            val readResult = reader.read(input)
            if (readResult == -1) {
                input.limit(input.position())
                input.position(0)
                val encodeResult = encoder.encode(input, out, true)
                if (encodeResult.isError) {
                    encodeResult.throwException()
                }
                out.limit(out.position())
                out.reset()
                return
            } else {
                input.limit(readResult)
                input.position(0)
            }
        }
        val encodeResult = encoder.encode(input, out, false)
        if (encodeResult.isError) {
            encodeResult.throwException()
        }

        out.limit(out.position())
        out.reset()
    }

    fun malformedInputAction(): CodingErrorAction {
        return encoder.malformedInputAction()
    }

    fun onMalformedInput(pNewAction: CodingErrorAction): CharsetEncoder {
        return encoder.onMalformedInput(pNewAction)
    }

    fun unmappableCharacterAction(): CodingErrorAction {
        return encoder.unmappableCharacterAction()
    }

    fun onUnmappableCharacter(pNewAction: CodingErrorAction): CharsetEncoder {
        return encoder.onUnmappableCharacter(pNewAction)
    }

    @Throws(IOException::class)
    override fun close() {
        reader.close()
    }

    /**
     * Skip characters, not bytes.
     */
    @Throws(IOException::class)
    override fun skip(pN: Long): Long {
        return reader.skip(pN)
    }

    override fun markSupported(): Boolean {
        return reader.markSupported()
    }

    @Synchronized
    override fun mark(pReadAheadLimit: Int) {
        try {
            reader.mark(pReadAheadLimit)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        reader.reset()
    }

}
