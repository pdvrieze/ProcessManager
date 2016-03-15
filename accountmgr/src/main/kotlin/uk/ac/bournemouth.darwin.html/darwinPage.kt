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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.darwin.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal
import java.net.URI
import java.security.Principal
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpServletResponse.contentType(type:String) { addHeader("Content-Type", type)}

/**
 * Method that encapsulates the darwin web template.
 */
fun HttpServletResponse.darwinResponse(request: HttpServletRequest, title: String="Darwin", pageTitle: String?=null, checkuser:Boolean=true, lightweight:Boolean=false, bodyContent: HtmlBlockTag.() -> Unit) : Unit  {
    val result = writer
    val appRoot = request.servletPath

    if (request.getHeader("X-Darwin")!=null) {
        contentType("text/xml")
        result.append("<?xml version=\"1.0\" ?>\n")
        result.appendXML().partialHTML {
            title(title)
            body() { bodyContent }
        }
    } else {
        contentType("text/html")
        result.append("<!DOCTYPE html>\n")
        result.appendHTML().html() {

            head() {
                title(title)
                link(rel="stylesheet", href=appRoot + "css/darwin.css")
                meta(name="viewport", content="width=device-width, initial-scale=1.0")
                if (! lightweight) {
                    this.script(src=appRoot+"darwinjs/darwinjs.nocache.js")
                    //                result.append("      <script type=\"text/javascript\" src=\"${request.servletPath}\"darwinjs/darwinjs.nocache.js\"></script>\n")
                }
            }
            body() {
                h1() {
                    id="header"
                    a(href=appRoot) {
                        id="logo"
                        +"Darwin"
                    }
                    span() {
                        id="title"
                        + (pageTitle ?: title)
                    }
                }

                darwinMenu(request)

                div { id = "login"
                    val user = request.userPrincipal
                    if (user==null) {
                        a(href=appRoot+"/accounts/login") { id="logout" }
                    } else {
                        a(href=appRoot+"/accounts/myaccount") {
                            id="username"
                            +user.name
                        }
                        span {
                            classes=setOf("hide")
                        }
                        a(href=appRoot+"/accounts/logout.php") { id="logout" }
                    }

                }

                div { id = "content"
                    bodyContent()
                }
                div { id = "footer"
                    span { id = "divider" }
                    +"Darwin is a Bournemouth University Project"
                }
            }
        }
    }
}

fun HttpServletResponse.darwinError(req: HttpServletRequest, message: String, code: Int = 500, status: String="Server error", cause: Exception?=null) {
    this.darwinResponse(req, title="$code $status", checkuser = false) {
        h2 { +status }
        p { style="margin-top: 2em"
            +message.trim().replace("\n", "<br />")
        }
        // TODO print backtrace, but only in debug
    }
}

class MenuItem(val label: String, val target: URI) { constructor(label:String, target: String) : this(label, URI.create(target))}

fun BODY.darwinMenu(request: HttpServletRequest, wrapper: HtmlBlockTag? = null): Unit {
    val user = request.userPrincipal


    val content : HtmlBlockTag.() -> Unit = {
        for (menuItem in getMenuItems(user)) {
            a(href = menuItem.target.toString(), classes = "menuitem") {
                +menuItem.label
            }
        }
    }

    if (wrapper == null) {
        div() {
            id = "menu"
            content()
        }
    } else {
        wrapper.visitAndFinalize(consumer, content)
    }

}


private fun getMenuItems(user: Principal?) : List<MenuItem> {
    val menuItems: MutableList<MenuItem> = ArrayList()
    // Pages with /#/... urls are virtual pages. They don't have valid other urls

    if (user == null) {
        menuItems += MenuItem("Welcome", "/")
    } else {
        menuItems += MenuItem("Home", "/")
        if (user is DarwinPrincipal) {
            if (user.isAdmin || user.hasRole("appprogramming")) {
                menuItems += MenuItem("Trac", user.name + "/trac/")
            }
        }
    }
    menuItems += MenuItem("About", "/#/about")
    return menuItems
}


/**
 * A class representing the idea of sending sufficient html to replace the content, but not the layout of the page.
 */
class PartialHTML(initialAttributes : Map<String, String>, override val consumer : TagConsumer<*>) : HTMLTag("root", consumer, initialAttributes, null, false, false) {

    fun title(block : TITLE.() -> Unit = {}) : Unit = TITLE(emptyMap, consumer).visit(block)
    fun title(content : String = "") : Unit = TITLE(emptyMap, consumer).visit({+content})

    fun body(block: XMLBody.() -> Unit = {}) : Unit = XMLBody(emptyMap, consumer).visit(block)
}

class XMLBody(initialAttributes : Map<String, String>, override val consumer : TagConsumer<*>) : HTMLTag("body", consumer, initialAttributes, null, false, false), HtmlBlockTag


/** Just inline for now, as this is just a forwarder. */
inline fun <O : Appendable> O.appendXML(prettyPrint : Boolean = true) : TagConsumer<O> = this.appendHTML(prettyPrint)

fun <T, C : TagConsumer<T>> C.partialHTML(block : PartialHTML.() -> Unit = {}) : T = PartialHTML(emptyMap, this).visitAndFinalize(this, block)

val HttpServletRequest.htmlAccepted: Boolean
    get() {
        return getHeader("Accept")?.contains("text/html") ?: false
    }

fun HtmlBlockTag.darwinDialog(title: String, id:String? = null, bodyContent: FlowContent.() -> Unit ={}) {
    div(classes="dialog centerContents") {
        if (id!=null) { this.id=id }
        div(classes="dialogOuter") {
            h1(classes="dlgTitle") { +title }
            div(classes="dialogInner centerContents") {
                div(classes = "dlgContent") {
                    bodyContent()
                }
            }
        }
    }

}