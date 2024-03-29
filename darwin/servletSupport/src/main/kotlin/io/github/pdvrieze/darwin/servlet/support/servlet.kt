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

package io.github.pdvrieze.darwin.servlet.support

import kotlinx.html.HtmlBlockTag
import uk.ac.bournemouth.darwin.html.RequestInfo
import uk.ac.bournemouth.darwin.html.RequestServiceContext
import uk.ac.bournemouth.darwin.html.darwinError
import uk.ac.bournemouth.darwin.html.darwinResponse
import uk.ac.bournemouth.darwin.sharedhtml.ContextTagConsumer
import uk.ac.bournemouth.darwin.sharedhtml.ServiceContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

public fun HttpServletResponse.darwinResponse(
    request: HttpServletRequest,
    windowTitle: String = "Darwin",
    pageTitle: String? = null,
    includeLogin: Boolean = true,
    context: ServiceContext,
    bodyContent: ContextTagConsumer<HtmlBlockTag>.() -> Unit
) {
    val resp = ServletResponseContext(this)
    val req = ServletRequestInfo(request)
    return resp.darwinResponse(req, windowTitle, pageTitle, includeLogin, context, bodyContent)
}

public fun HttpServletResponse.darwinResponse(
    request: HttpServletRequest,
    windowTitle: String = "Darwin",
    pageTitle: String? = null,
    includeLogin: Boolean = true,
    bodyContent: ContextTagConsumer<HtmlBlockTag>.() -> Unit
) {
    val resp = ServletResponseContext(this)
    val req = ServletRequestInfo(request)
    val context = RequestServiceContext(req)
    return resp.darwinResponse(req, windowTitle, pageTitle, includeLogin, context, bodyContent)
}

public fun HttpServletResponse.darwinError(request: HttpServletRequest,
                                           message: String,
                                           code: Int = 500,
                                           status: String = "Server error",
                                           cause: Exception? = null) {
    val resp = ServletResponseContext(this)
    val req = ServletRequestInfo(request)
    resp.darwinError(req, message, code, status, cause)
}


public val HttpServletRequest.htmlAccepted: Boolean
    get() {
        return getHeader("Accept")?.let { it.contains("text/html") || it.contains("application/nochrome") } ?: false
    }
