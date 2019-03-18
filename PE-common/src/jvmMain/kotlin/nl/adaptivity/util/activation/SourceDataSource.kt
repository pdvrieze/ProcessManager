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

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.activation.DataSource
import javax.xml.transform.Source


class SourceDataSource @JvmOverloads constructor(private val contentType: String, private val content: Source, private val name:String? = null) : DataSource {

  override fun getContentType() = contentType

  @Throws(IOException::class)
  override fun getInputStream(): InputStream = content.toInputStream()

  override fun getName()= name

  @Throws(IOException::class)
  override fun getOutputStream(): OutputStream
    = throw UnsupportedOperationException("Can not write to sources")

}
