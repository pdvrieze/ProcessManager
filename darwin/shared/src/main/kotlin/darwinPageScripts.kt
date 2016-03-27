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


const val FIELD_USERNAME = "username"
const val FIELD_PASSWORD = "password"
const val FIELD_PUBKEY = "pubkey"
const val FIELD_REDIRECT = "redirect"
const val FIELD_KEYID = "keyid"
const val FIELD_APPNAME = "app"
const val FIELD_RESPONSE = "response"
const val FIELD_RESETTOKEN = "resettoken"
const val FIELD_NEWPASSWORD1 = "newpassword1"
const val FIELD_NEWPASSWORD2 = "newpassword2"

/**
 * A class representing the idea of sending sufficient html to replace the content, but not the layout of the page.
 */

class XMLBody(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("body", consumer, initialAttributes, null, false, false), HtmlBlockTag


/** Just inline for now, as this is just a forwarder. */
inline fun <O : Appendable> O.appendXML(prettyPrint: Boolean = true): TagConsumer<O> = this.appendHTML(prettyPrint)

class PartialHTML(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("root", consumer, initialAttributes, null, false, false) {

  fun title(block: TITLE.() -> Unit = {}): Unit = TITLE(emptyMap, consumer).visit(block)
  fun title(content: String = ""): Unit = TITLE(emptyMap, consumer).visit({ +content })

  fun body(block: XMLBody.() -> Unit = {}): Unit = XMLBody(emptyMap, consumer).visit(block)
}


fun <T, C : TagConsumer<T>> C.partialHTML(block: PartialHTML.() -> Unit = {}): T = PartialHTML(emptyMap, this).visitAndFinalize(this, block)

fun <T, C : TagConsumer<T>> C.darwinDialog(title: String, id: String? = null, bodyContent: FlowContent.() -> Unit = {}):T {
  return div(classes = "dialog centerContents") {
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



fun <T, C : TagConsumer<T>> C.loginDialog(errorMsg: String? = null, username: String? = null, password: String?=null, redirect: String? = null, visitConfirm: INPUT.() -> Unit, visitCancel: (INPUT.() -> Unit)?): T {
  return darwinDialog("Log in") {
    div("errorMsg") {
      if (errorMsg==null) style="display:none" else +errorMsg
    }
    if (errorMsg!=null) {
    }
    form(action = "login", method = FormMethod.post, encType = FormEncType.applicationXWwwFormUrlEncoded) {
      acceptCharset="utf8"
      if(redirect!=null) {
        input(name=FIELD_REDIRECT, type = InputType.hidden) { value = redirect }
      }
      table {
        style = "border:none"
        tr {
          td {
            label { for_='#'+FIELD_USERNAME
              +"User name:"
            }
          }
          td {
            input(name=FIELD_USERNAME, type= InputType.text) {
              if (username !=null) { value= username }
            }
          }
        }
        tr {
          td {
            label { for_='#'+FIELD_PASSWORD
              +"Password:"
            }
          }
          td {
            input(name=FIELD_PASSWORD, type= InputType.password) {
              if (password!=null) { value = password }
            }
          }
        }
      } // table
      span {
        style="margin-top: 1em; float: right;"
        input(type= InputType.submit) {
          value="Log in"
          visitConfirm(this)
        }
      }
      div { id="forgotpasswd"
        a(href="/accounts/resetpasswd")
      }
    }
  }

}