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

package nl.adaptivity.util.activation

import javax.activation.DataSource
import javax.activation.MimetypesFileTypeMap

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection


class UrlDataSource @Throws(IOException::class)
constructor(private val url: URL) : DataSource {

    private var contentType: String

    private val inputStream: InputStream

    val headers: Map<String, List<String>>

    init {
        val connection: URLConnection
        connection = url.openConnection()

        this.contentType = connection.contentType.let<String?, String?> {
            if (it == null || it == "content/unknown") getMimeTypeForFileName(
                url.file
                                                                             ) else it
        } ?: "application/binary"

        inputStream = connection.getInputStream()

        headers = connection.headerFields
    }

    override fun getContentType(): String {
        return contentType
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return inputStream
    }

    override fun getName(): String {
        return url.path
    }

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException("Not allowed")

    }

    companion object {

        private var _mimeMap: MimetypesFileTypeMap? = null

        private fun getMimeTypeForFileName(fileName: String): String {
            if (_mimeMap == null) {
                _mimeMap = MimetypesFileTypeMap()
                _mimeMap!!.addMimeTypes("text/css css\ntext/html htm html shtml\nimage/png png\n")
            }
            return _mimeMap!!.getContentType(fileName)
        }
    }

}
