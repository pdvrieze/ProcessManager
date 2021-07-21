/*
 * Copyright (c) 2021.
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

package uk.ac.bournemouth.darwin.html.uk.ac.bournemouth.darwin.html

import uk.ac.bournemouth.darwin.html.ResponseContext
import uk.ac.bournemouth.darwin.sharedhtml.ServiceContext
import java.io.Writer
import javax.servlet.http.HttpServletResponse

class ServletResponseContext(val servletResponse: HttpServletResponse): ResponseContext {
    override fun contentType(type: String) {
        servletResponse.contentType = type
    }

    override fun setStatus(code: Int) {
        servletResponse.status = code
    }

    override fun respondWriter(body: (Writer) -> Unit) {
        servletResponse.writer.use { body(it) }
    }

}
