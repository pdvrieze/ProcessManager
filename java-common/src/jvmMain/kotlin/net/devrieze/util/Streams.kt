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

@file:JvmName("Streams")
package net.devrieze.util

import java.io.*
import java.nio.charset.Charset

@Deprecated("Compatibility with Java interface", ReplaceWith("inputStream.readString(charset)"))
@Throws(IOException::class)
fun toString(inputStream: InputStream, charset: Charset):String = inputStream.readString(charset)

fun InputStream.readString(charset: Charset): String {
  return InputStreamReader(this, charset).readString()
}

@Deprecated("Compatibility with Java interface", ReplaceWith("reader.readString()"))
@Throws(IOException::class)
fun toString(reader: Reader):String = reader.readString()

fun Reader.readString(): String {
  val result = StringBuilder()
  val buffer = CharArray(0x8ff)
  var count = read(buffer)
  while (count >= 0) {
    result.append(buffer, 0, count)
    count = read(buffer)
  }
  return result.toString()
}

fun StringBuilder.writer(): Writer = object : Writer() {
  override fun write(cbuf: CharArray, off: Int, len: Int) {
    append(cbuf, off, len)
  }

  override fun flush() = Unit

  override fun close() = Unit

}