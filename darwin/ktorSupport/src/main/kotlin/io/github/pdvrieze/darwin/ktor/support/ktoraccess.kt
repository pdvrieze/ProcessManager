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

package io.github.pdvrieze.darwin.ktor.support

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.HtmlBlockTag
import uk.ac.bournemouth.darwin.html.RequestInfo
import uk.ac.bournemouth.darwin.html.RequestServiceContext
import uk.ac.bournemouth.darwin.html.ResponseContext
import uk.ac.bournemouth.darwin.html.darwinResponse
import uk.ac.bournemouth.darwin.sharedhtml.ContextTagConsumer
import uk.ac.bournemouth.darwin.sharedhtml.ServiceContext
import java.io.CharArrayWriter
import java.io.Writer
import java.security.Principal

public suspend fun PipelineContext<*, ApplicationCall>.darwinResponse(
    windowTitle: String = "Darwin",
    pageTitle: String? = null,
    includeLogin: Boolean = true,
    context: ServiceContext,
    bodyContent: ContextTagConsumer<HtmlBlockTag>.() -> Unit
) {
    val resp = KtorResponseContext(call)
    val req = KtorServletRequestInfo(call)
    return resp.darwinResponse(req, windowTitle, pageTitle, includeLogin, context, bodyContent)
}

public suspend fun PipelineContext<*, ApplicationCall>.darwinResponse(windowTitle: String = "Darwin",
                                                                      pageTitle: String? = null,
                                                                      includeLogin: Boolean = true,
                                                                      bodyContent: ContextTagConsumer<HtmlBlockTag>.() -> Unit) {
    val resp = KtorResponseContext(call)
    val req = KtorServletRequestInfo(call)
    val context = RequestServiceContext(req)
    return resp.darwinResponse(req, windowTitle, pageTitle, includeLogin, context, bodyContent)

}

public class KtorResponseContext(private val call: ApplicationCall): ResponseContext {
    private var pendingContentType: ContentType = ContentType.parse("text/plain")
    private var pendingCode: HttpStatusCode = HttpStatusCode.OK

    override fun contentType(type: String) {
        pendingContentType = ContentType.parse(type)
//        call.response.header("Content-Type", type)
    }

    override fun setStatus(code: Int) {
        pendingCode = HttpStatusCode.fromValue(code)
//        call.response.status(HttpStatusCode.fromValue(code))
    }

    override fun respondWriter(body: (Writer) -> Unit) {
        val caw = CharArrayWriter().apply { use { body(it) } }
        runBlocking {
            call.respondTextWriter(pendingContentType, pendingCode) { caw.writeTo(this) }
        }
    }
}

public class KtorServletRequestInfo(private val call: ApplicationCall): RequestInfo {
    override fun getHeader(name: String): String? {
        return call.request.header(name)
    }

    override fun isUserInRole(role: String): Boolean {
        return false // TODO use more extensive authentication
    }

    public val ktorPrincipal: UserIdPrincipal? get() {
        return call.principal()
    }

    override val userPrincipal: Principal?
        get() = ktorPrincipal?.let(::KtorPrincipal)

    override val contextPath: String
        get() = call.application.environment.rootPath

}

internal class KtorPrincipal(private val ktorPrincipal: UserIdPrincipal): Principal {
    override fun getName(): String {
        return ktorPrincipal.name
    }
}
