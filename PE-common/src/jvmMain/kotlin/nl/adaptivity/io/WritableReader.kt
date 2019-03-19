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

package nl.adaptivity.io

import java.io.CharArrayReader
import java.io.CharArrayWriter
import java.io.IOException
import java.io.Reader
import java.nio.CharBuffer


/**
 * Created by pdvrieze on 19/11/15.
 */
class WritableReader(content: Writable) : Reader() {

    private var content: Writable? = content

    private// No longer needed, discard.
    val delegate: Reader by lazy {
        val caw = CharArrayWriter()
        content.writeTo(caw)
        this.content = null
        CharArrayReader(caw.toCharArray())
    }

    @Throws(IOException::class)
    override fun read(target: CharBuffer): Int {
        return delegate.read(target)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return delegate.read()
    }

    @Throws(IOException::class)
    override fun read(cbuf: CharArray): Int {
        return delegate.read(cbuf)
    }

    @Throws(IOException::class)
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        return delegate.read(cbuf, off, len)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return delegate.skip(n)
    }

    @Throws(IOException::class)
    override fun ready(): Boolean {
        return delegate.ready()
    }

    override fun markSupported(): Boolean {
        try {
            return delegate.markSupported()
        } catch (e: IOException) {
            return false
        }

    }

    @Throws(IOException::class)
    override fun mark(readAheadLimit: Int) {
        delegate.mark(readAheadLimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        delegate.reset()
    }

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
    }
}
