/*
 * Copyright (c) 2017.
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

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import uk.ac.bournemouth.darwin.sharedhtml.*
import java.net.URI
import java.security.Principal
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/** Should Javascript initialization be delayed to the footer of the page. */
private const val DELAY_JS = true

fun HttpServletResponse.contentType(type: String) {
    addHeader("Content-Type", type)
}

val HttpServletRequest.isNoChromeRequested: Boolean
    get() = getHeader("X-Darwin")?.contains("nochrome") ?: false

/**
 * Method that encapsulates the darwin web template.
 *
 * @param request The request to respond to
 * @param windowTitle The title that should be in the head element
 * @param pageTitle The title to display on page (if not the same as windowTitle)
 * @param bodyContent The closure that creates the actual body content of the document.
 */
fun HttpServletResponse.darwinResponse(request: HttpServletRequest,
                                       windowTitle: String = "Darwin",
                                       pageTitle: String? = null,
                                       includeLogin: Boolean = true,
                                       context: ServiceContext = RequestServiceContext(request),
                                       bodyContent: ContextTagConsumer<HtmlBlockTag>.() -> Unit) {
    val result = writer

    if (request.isNoChromeRequested) {
        contentType("text/xml")
        result.append("<?xml version=\"1.0\" ?>\n")
        result.appendHTML().partialHTML {
            title(windowTitle, pageTitle)
            script(context.jsLocalRef("main.js"))
            body {
                withContext(context).bodyContent()
            }
        }
    } else {
        contentType("text/html")
        result.append("<!DOCTYPE html>\n")
        result.appendHTML().html {

            head {
                title(windowTitle)
                styleLink(context.cssRef("darwin.css"))
                meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                if (!DELAY_JS) script(type = ScriptType.textJavaScript, src = context.jsGlobalRef(
                    "require.js")) { this.attributes["data-main"] = context.jsLocalRef("main.js") }
            }
            body {
                h1 {
                    id = "header"
                    a(href = "/") {
                        id = "logo"
                        +"Darwin"
                    }
                    span {
                        id = "title"
                        +(pageTitle ?: windowTitle)
                    }
                }

                darwinMenu(request)

                div {
                    id = "login"
                    loginPanelContent(context, request.userPrincipal?.name)
                }

                div {
                    id = "content"
                    withContext(context).bodyContent()
                }
                if (includeLogin) {
                    // A mini form that we use to get username/password out of the account manager
                    form(action = "${context.accountMgrPath}login", method = FormMethod.post) {
                        id = "xloginform"; style = "display:none"
                        input(type = InputType.text, name = "username")
                        input(type = InputType.password, name = "password")
                    }
                }
                div {
                    id = "footer"
                    span { id = "divider" }
                    +"Darwin is a Bournemouth University Project"
                }
                if (DELAY_JS) script(type = ScriptType.textJavaScript, src = context.jsGlobalRef(
                    "require.js")) { this.attributes["data-main"] = context.jsLocalRef("main.js") }
//        script(type= ScriptType.textJavaScript, src="/js/darwin.js")
            }
        }
    }
}

fun HttpServletResponse.darwinError(req: HttpServletRequest,
                                    message: String,
                                    code: Int = 500,
                                    status: String = "Server error",
                                    cause: Exception? = null) {
    setStatus(code)
    this.darwinResponse(req, windowTitle = "$code $status") {
        h2 { +status }
        p {
            style = "margin-top: 2em"
            +message.trim().replace("\n", "<br />")
        }
        if (cause != null && System.getProperty("DEBUG")?.let { it != "false" } == true) {
            p {
                this.printCauseAsHtml(cause)
            }
        }
    }
}

private fun HtmlBlockTag.printCauseAsHtml(cause: Throwable) {
    b { +cause.javaClass.simpleName }
    +": ${cause.message}"
    div {
        style = "margin-left: 1em"
        ul {
            for (elem in cause.stackTrace) {
                li { +elem.toString() }
            }
        }
        cause.cause?.let {
            i { +"caused by " }
            printCauseAsHtml(it)
        }
        for (suppressed in cause.suppressed) {
            i { +"suppressing " }
            printCauseAsHtml(suppressed)
        }
    }
}

class MenuItem(val label: String, val target: URI) {
    constructor(label: String, target: String) : this(label, URI.create(target))
}

fun <T, C : TagConsumer<T>> C.darwinMenu(request: HttpServletRequest): T {
    val user = request.userPrincipal

    return div {
        id = "menu"
        var first = true
        for (menuItem in getMenuItems(request, user)) {
            if (!first) +"\n" else first = false
            a(href = menuItem.target.toString(), classes = "menuitem") {
                +menuItem.label
            }
        }
    }
}

fun FlowContent.darwinMenu(request: HttpServletRequest) {
    consumer.darwinMenu(request)
}


private fun getMenuItems(request: HttpServletRequest, user: Principal?): List<MenuItem> {
    val menuItems: MutableList<MenuItem> = ArrayList()
    // Pages with /#/... urls are virtual pages. They don't have valid other urls

    if (user == null) {
        menuItems += MenuItem("Welcome", "/")
    } else {
        menuItems += MenuItem("Home", "/")
        if (request.isUserInRole("admin") || request.isUserInRole("appprogramming")) {
            menuItems += MenuItem("Trac", user.name + "/trac/")
        }
    }
    menuItems += MenuItem("About", "/#/about")
    return menuItems
}


/**
 * A class representing the idea of sending sufficient html to replace the content, but not the layout of the page.
 */
class PartialHTML(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("root",
                                                                                                           consumer,
                                                                                                           initialAttributes,
                                                                                                           null, false,
                                                                                                           false) {

    fun title(block: TITLE.() -> Unit = {}): Unit = TITLE(emptyMap, consumer).visit(block)
    fun title(windowTitle: String = "", pageTitle: String? = null): Unit = TITLE(emptyMap, consumer).visit({
                                                                                                               attributes["windowtitle"] = windowTitle
                                                                                                               +(pageTitle
                                                                                                                 ?: windowTitle)
                                                                                                           })

    fun script(src: String) {
        SCRIPT(emptyMap, consumer).visit {
            this.type = ScriptType.textJavaScript
            this.src = src
        }
    }

    fun body(block: XMLBody.() -> Unit = {}): Unit = XMLBody(emptyMap, consumer).visit(block)
}

class XMLBody(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("body", consumer,
                                                                                                       initialAttributes,
                                                                                                       null, false,
                                                                                                       false), HtmlBlockTag


fun <T, C : TagConsumer<T>> C.partialHTML(block: PartialHTML.() -> Unit = {}): T = PartialHTML(emptyMap,
                                                                                               this).visitAndFinalize(
    this, block)

val HttpServletRequest.htmlAccepted: Boolean
    get() {
        return getHeader("Accept")?.let { it.contains("text/html") || it.contains("application/nochrome") } ?: false
    }

fun ContextTagConsumer<HtmlBlockTag>.darwinDialog(title: String,
                                                  id: String? = null,
                                                  bodyContent: FlowContent.() -> Unit = {}) {
    div(classes = "dialog centerContents") {
        if (id != null) {
            this.id = id
        }
        div(classes = "dialogOuter") {
            h1(classes = "dlgTitle") { +title }
            div(classes = "dialogInner centerContents") {
                div(classes = "dlgContent") {
                    bodyContent()
                }
            }
        }
    }

}

class RequestServiceContext(private val request: HttpServletRequest) : ServiceContext {
    override val accountMgrPath: String
        get() = "/accountmgr/"
    override val assetPath: String
        get() = "/assets/"
    override val cssPath: String
        get() = "/css/"
    override val jsGlobalPath: String
        get() = "/js/"
    override val jsLocalPath: String
        get() = "${request.contextPath}/js/"
}