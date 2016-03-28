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

/**
 * Created by pdvrieze on 27/03/16.
 */

package uk.ac.bournemouth.darwin.html

import kotlinx.html.INPUT
import kotlinx.html.dom.create
import org.w3c.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.NodeFilter
import uk.ac.bournemouth.darwin.html.shared.*
import kotlin.browser.document
import kotlin.dom.appendText
import kotlin.dom.clear
import kotlin.dom.hasClass

class JSServiceContext: ServiceContext {
  override val accountMgrPath: String
    get() = "/accounts/"
  override val assetPath: String
    get() = "/assets/"

}

class LoginDialog private constructor (val element: HTMLElement) {
  constructor(errorMsg: String? = null, username: String? = null, password: String?=null, redirect: String? = null, visitConfirm: INPUT.() -> Unit, visitCancel: (INPUT.() -> Unit)?): this(document.create.loginDialog(errorMsg=errorMsg, username=username, password =password, redirect=redirect, visitConfirm=visitConfirm, visitCancel=visitCancel)) {}

  val form: HTMLFormElement by lazy {
    val treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_ELEMENT, { node -> if (node is HTMLFormElement) NodeFilter.FILTER_ACCEPT else NodeFilter.FILTER_SKIP })
    treeWalker.nextNode() as HTMLFormElement
  }

  val errorMsgElem: HTMLElement by lazy {
    val treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_ELEMENT, { node -> if ((node as Element).hasClass("errorMsg")) NodeFilter.FILTER_ACCEPT else NodeFilter.FILTER_SKIP })
    treeWalker.nextNode() as HTMLElement
  }

  var errorMsg:String?
    get() = errorMsgElem.textContent
    set(value) {
      errorMsgElem.apply {
        clear();
        if (value.isNullOrBlank()) {
          hidden=true
        } else {
          hidden=false
          appendText(value!!)
        }
      }
    }

  var username:String?
    set(value) {
      (form["username"] as? HTMLInputElement) ?.let { it.value=username?:"" }
    }
    get() {
      return (form["username"] as? HTMLInputElement) ?.value
    }

  var password:String?
    set(value) {
      (form["password"] as? HTMLInputElement) ?.let { it.value=username?:"" }
    }
    get() {
      return (form["password"] as? HTMLInputElement) ?.value
    }
}
