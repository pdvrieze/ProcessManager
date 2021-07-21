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

package uk.ac.bournemouth.darwin.html

import kotlinx.html.HtmlBlockTag
import uk.ac.bournemouth.darwin.html.uk.ac.bournemouth.darwin.html.ServletResponseContext
import uk.ac.bournemouth.darwin.sharedhtml.ContextTagConsumer
import uk.ac.bournemouth.darwin.sharedhtml.ServiceContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpServletResponse.darwinResponse(
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

fun HttpServletResponse.darwinResponse(
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

fun HttpServletResponse.darwinError(request: HttpServletRequest,
                                message: String,
                                code: Int = 500,
                                status: String = "Server error",
                                cause: Exception? = null) {
    val resp = ServletResponseContext(this)
    val req = ServletRequestInfo(request)
    resp.darwinError(req, message, code, status, cause)
}
