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

/**
 * Extensions for DarwinPage that only apply for JavaScript.
 * Created by pdvrieze on 27/03/16.
 */

package uk.ac.bournemouth.darwin

import kotlinx.browser.document
import kotlinx.dom.appendText
import kotlinx.dom.clear
import kotlinx.dom.hasClass
import kotlinx.html.dom.create
import org.w3c.dom.*
import uk.ac.bournemouth.darwin.sharedhtml.ServiceContext
import uk.ac.bournemouth.darwin.sharedhtml.loginDialog
import uk.ac.bournemouth.darwin.sharedhtml.withContext
import uk.ac.bournemouth.darwin.util.visitDescendants

const val accountsLoc = "/accountmgr/"

class JSServiceContext : ServiceContext {
    override val accountMgrPath: String
        get() = accountsLoc
    override val assetPath: String
        get() = "/assets/"
    override val cssPath: String
        get() = "/css/"
    override val jsGlobalPath: String
        get() = "/js/"
    override val jsLocalPath: String
        get() = "js/"
}

class LoginDialog private constructor(val element: HTMLElement) {
    constructor(
        context: ServiceContext,
        errorMsg: String? = null,
        username: String? = null,
        password: String? = null,
        redirect: String? = null,
        visitConfirm: (HTMLElement) -> Unit,
        visitCancel: ((HTMLElement) -> Unit)?
    ) :
        this(
            document.create.withContext(context).loginDialog(
                context = context,
                errorMsg = errorMsg,
                username = username,
                password = password,
                redirect = redirect,
                cancelEnabled = visitCancel != null
            )
        ) {
        element.visitDescendants { descendant ->
            if (descendant is HTMLElement) {
                if (descendant.hasClass("dialogconfirm")) visitConfirm(descendant)
                else if (descendant.hasClass("dialogcancel")) visitCancel?.invoke(descendant)
            }
        }
    }

    val form: HTMLFormElement by lazy {
        val treeWalker = document.createTreeWalker(
            element,
            NodeFilter.SHOW_ELEMENT,
            { node -> if (node is HTMLFormElement) NodeFilter.FILTER_ACCEPT else NodeFilter.FILTER_SKIP })
        treeWalker.nextNode() as HTMLFormElement
    }

    val errorMsgElem: HTMLElement by lazy {
        val treeWalker = document.createTreeWalker(
            element,
            NodeFilter.SHOW_ELEMENT,
            { node -> if ((node as Element).hasClass("errorMsg")) NodeFilter.FILTER_ACCEPT else NodeFilter.FILTER_SKIP })
        treeWalker.nextNode() as HTMLElement
    }

    var errorMsg: String?
        get() = errorMsgElem.textContent
        set(value) {
            errorMsgElem.apply {
                clear()
                if (value.isNullOrBlank()) {
                    hidden = true
                } else {
                    hidden = false
                    appendText(value)
                }
            }
        }

    var username: String?
        set(value) {
            (form["username"] as? HTMLInputElement)?.let { it.value = value ?: "" }
        }
        get() {
            return (form["username"] as? HTMLInputElement)?.value
        }

    var password: String?
        set(value) {
            (form["password"] as? HTMLInputElement)?.let { it.value = value ?: "" }
        }
        get() {
            return (form["password"] as? HTMLInputElement)?.value
        }
}
