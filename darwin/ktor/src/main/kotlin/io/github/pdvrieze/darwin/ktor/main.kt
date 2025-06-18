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

package io.github.pdvrieze.darwin.ktor

import io.github.pdvrieze.darwin.ktor.support.KtorServletRequestInfo
import io.github.pdvrieze.darwin.ktor.support.darwinResponse
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.html.img
import kotlinx.html.stream.appendHTML
import uk.ac.bournemouth.darwin.html.darwinDialog
import uk.ac.bournemouth.darwin.html.darwinMenu
import java.io.File

fun main() {
    embeddedServer(Netty, 9090, module = Application::myApplicationModule).start(wait = true)
}

private suspend fun RoutingContext.mainPage() {
    darwinResponse {
        darwinDialog(title = "loading", id = "banner") {
            img(alt = "loading...", src = "/assets/progress_large.gif") { width = "192"; height = "192" }
        }
    }
}

private suspend fun RoutingContext.menu() {
    call.respondTextWriter {
        appendHTML().darwinMenu(KtorServletRequestInfo(call))
    }
}

fun Application.myApplicationModule() {
    install(ContentNegotiation) {
        json()
//            xml()
    }
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
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
                val relativePath =
                    call.parameters.getAll("static-content-path-parameter")?.joinToString(File.separator)
                        ?: return@get
                val content = call.resolveResource(relativePath, "js")
                    ?: call.resolveResource("META-INF/resources/webjars/requirejs/2.3.5/" + relativePath, "")
                if (content != null) {
                    call.respond(content)
                }
            }
        }
    }
}

