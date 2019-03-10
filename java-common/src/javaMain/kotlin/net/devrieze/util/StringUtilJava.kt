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

@file:JvmName("StringUtilJava")

package net.devrieze.util

import net.devrieze.lang.Const
import nl.adaptivity.util.CharArraySequence
import java.io.IOException
import java.io.Writer

private val FORMAT_SLACK = 20

private class IndentingWriter(level: Int, private val target: Writer) : Writer() {

    private val currentLineBuffer: CharArray = CharArray(level) { ' ' }

    private var lastSeenWasNewline: Boolean = true

    @Throws(IOException::class)
    override fun close() {
        target.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        target.flush()
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        val end = off + len
        var nextToWrite = off
        var i = off
        while (i < end) {
            val c = cbuf[i]
            if (c == Const._CR || c == Const._LF) {
                val lastChar = i - 1
                val d: Char = if (i + 1 >= end) 0.toChar() else cbuf[i + 1]
                if (c != d && (d == Const._CR || d == Const._LF)) {
                    ++i
                }
                if (lastChar != nextToWrite) {
                    // Skip indent in case newline follows directly
                    if (lastSeenWasNewline) {
                        target.write(currentLineBuffer)
                    }
                }
                target.write(cbuf, nextToWrite, i - nextToWrite + 1)
                nextToWrite = i + 1
                lastSeenWasNewline = true
            }
            ++i
        }
        if (nextToWrite < end) {
            if (lastSeenWasNewline) {
                target.write(currentLineBuffer)
            }
            target.write(cbuf, nextToWrite, end - nextToWrite)
            lastSeenWasNewline = false
        }
    }

}

@Deprecated("Use extension version", ReplaceWith("source.indent(level)"))
fun indent(level: Int, source: Writer): Writer = source.indent(level)

fun Writer.indent(level: Int): Writer {
    return IndentingWriter(level, this)
}


/**
 * Create a quoted version of the buffer.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
@Deprecated("Just use quotation directly")
fun quote(buffer: CharSequence): StringRep {
    return StringRep.createRep(buffer.quoted())
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Suppress("DEPRECATION")
@Deprecated("Just use quotation directly")
fun quote(buffer: CharArray): StringRep {
    return quote(CharArraySequence(buffer))
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param start The start index in the buffer that needs to be quoted
 * @param end The end index in the buffer to be quoted (exclusive)
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Suppress("DEPRECATION")
@Deprecated("Just use quotation directly")
fun quote(start: Int, end: Int, buffer: CharArray): StringRep {
    return quote(CharArraySequence(buffer, start, end))
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quoteBuf(buffer: String): StringBuffer {
    @Suppress("DEPRECATION")
    return quoteBuf(buffer.toCharArray())
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quoteBuf(buffer: StringBuffer): StringBuffer {
    val charArray = CharArray(buffer.length)
    buffer.getChars(0, charArray.size, charArray, 0)

    @Suppress("DEPRECATION")
    return quoteBuf(charArray)
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just bad. Don't use, StringBuffers are deprecated")
fun quoteBuf(buffer: CharArray): StringBuffer {
    @Suppress("DEPRECATION")
    return quoteBuf(0, buffer.size, buffer)
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param start The start index in the buffer that needs to be quoted
 * @param end The end index in the buffer to be quoted (exclusive)
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just bad. Don't use, StringBuffers are deprecated")
fun quoteBuf(start: Int, end: Int, buffer: CharArray): StringBuffer {
    val result = StringBuffer(buffer.size + FORMAT_SLACK)
    result.append('"')

    for (i in start until end) {
        when (buffer[i]) {
            '"'  -> {
                result.append("\\\"")
            }

            '\\' -> {
                result.append("\\\\")
            }

            else -> result.append(buffer[i])
        }
    }

    result.append('"')

    return result
}

fun Class<*>.simpleClassName(): String {
    return name.substringAfterLast('.')
}
