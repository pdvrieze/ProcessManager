/*
 * Copyright (c) 2016.
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

/**
 * Created by pdvrieze on 28/03/16.
 */

package example

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.html.img
import kotlinx.html.stream.appendHTML
import nl.adaptivity.xmlutil.serialization.ktor.xml
import uk.ac.bournemouth.darwin.html.darwinDialog
import uk.ac.bournemouth.darwin.html.darwinMenu
import java.io.File

fun main() {
    embeddedServer(Netty, 9090) {
        install(ContentNegotiation) {
            json()
//            xml()
        }
        install(Compression) {
            gzip()
        }
        routing {
            get("/") { mainPage() }
            get("/common/menu") { menu() }
            static("/") {
                resources("")
            }
            route("/js") {
                get("{static-content-path-parameter...}") {
//                val packageName = staticBasePackage.combinePackage(resourcePackage)
                    val relativePath = call.parameters.getAll("static-content-path-parameter")?.joinToString(File.separator) ?: return@get
                    val content = call.resolveResource(relativePath, "js") ?:
                        call.resolveResource("META-INF/resources/webjars/requirejs/2.3.5/"+relativePath, "")
                    if (content != null) {
                        call.respond(content)
                    }
                }
            }
        }

    }.start(wait = true)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.mainPage() {
    darwinResponse {
        darwinDialog(title = "loading", id="banner") {
            img(alt="loading...", src="/assets/progress_large.gif") { width="192"; height="192"}
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.menu() {
    call.respondTextWriter {
        appendHTML().darwinMenu(KtorServletRequestInfo(call))
    }
}

