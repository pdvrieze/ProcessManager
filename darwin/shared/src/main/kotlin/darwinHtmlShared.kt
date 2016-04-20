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

package uk.ac.bournemouth.darwin.html.shared

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

/**
 * A helper class to allow the context to be passed along within contexts.
 */
//class ContextHtmlBlockTag(val context:ServiceContext, private val delegate:HtmlBlockTag): HtmlBlockTag by delegate
//class ContextHtmlInlineTag(val context:ServiceContext, private val delegate:HtmlInlineTag): HtmlInlineTag by delegate
//class ContextCommonAttributeGroupFacade(val context:ServiceContext, private val delegate:CommonAttributeGroupFacade): CommonAttributeGroupFacade by delegate

//@Suppress("NOTHING_TO_INLINE")
//inline fun CommonAttributeGroupFacade.withContext(context:ServiceContext) = ContextCommonAttributeGroupFacade(context, this)
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContextHtmlBlockTag.withContext(context:ServiceContext) = ContextHtmlBlockTag(context, this)
//@Suppress("NOTHING_TO_INLINE")
//inline fun ContextHtmlInlineTag.withContext(context:ServiceContext) = ContextHtmlInlineTag(context, this)


class ContextTagConsumer<out T>(val context:ServiceContext, private val delegate: TagConsumer<out T>): TagConsumer<T> by delegate {
  @Suppress("NOTHING_TO_INLINE")
  inline final operator fun CharSequence.unaryPlus() = onTagContent(this)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T,C:TagConsumer<out T>> C.withContext(context:ServiceContext) = ContextTagConsumer<T>(context, this)

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T:Tag> T.withContext(context:ServiceContext) = (consumer as TagConsumer<out T>).withContext(context)


/** Just inline for now, as this is just a forwarder. */
@Suppress("NOTHING_TO_INLINE")
inline fun <O : Appendable> O.appendXML(prettyPrint: Boolean = true): TagConsumer<O> = this.appendHTML(prettyPrint)

class PartialHTML(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("root", consumer, initialAttributes, null, false, false) {

  fun title(block: TITLE.() -> Unit = {}): Unit = TITLE(emptyMap, consumer).visit(block)
  fun title(content: String = ""): Unit = TITLE(emptyMap, consumer).visit({ +content })

  fun body(block: XMLBody.() -> Unit = {}): Unit = XMLBody(emptyMap, consumer).visit(block)
}


fun <T, C : TagConsumer<out T>> C.partialHTML(block: PartialHTML.() -> Unit = {}): T = PartialHTML(emptyMap, this).visitAndFinalize(this, block)

fun <T, C : ContextTagConsumer<T>> C.darwinDialog(title: String, id: String? = null, positiveButton:String?="Ok", negativeButton:String?=null, vararg otherButtons:String, bodyContent: ContextTagConsumer<*>.() -> Unit = {}):T {
  return div(classes = "dialog centerContents") {
    if (id != null) {
      this.id = id
    }
    div(classes = "dialogOuter") {
      h1(classes = "dlgTitle") { +title }
      div(classes = "dialogInner centerContents") {
        div(classes = "dlgContent") {
          withContext(context).bodyContent()
        }
        div(classes = "dlgButtons") {
          style="margin-top: 1em; float: right;"
          if (negativeButton!=null && negativeButton.isNotEmpty()) {
            input(type = InputType.button, classes = "dialogcancel") {
              value = negativeButton
            }
          }
          for(otherButton in otherButtons) {
            input(type= InputType.button, classes = "dialogother") {
              value = otherButton
            }
          }
          if (positiveButton!=null && positiveButton.isNotEmpty()) {
            input(type = InputType.submit, classes = "dialogconfirm") {
              value = positiveButton
            }
          }
        }

      }
    }
  }
}

fun DIV.loginPanelContent(context: ServiceContext, username: String?) {
  consumer.loginPanelContent(context, username)
}

fun <T, C: TagConsumer<out T>> C.loginPanelContent(context: ServiceContext, username: String?) {
  if (username == null) {
    a(href = context.accountMgrPath + "login") {
      id = "logout"
      +"login"
    }
  } else {
    a(href = context.accountMgrPath + "myaccount") { id = "username"; +username }
    span("hide")
    a(href = context.accountMgrPath + "logout") { id = "logout"; +"logout" }
  }
}

interface ServiceContext {
  val accountMgrPath:String
  val assetPath:String
  val cssPath:String
  val jsPath:String

  fun cssRef(filename: String): String = "${cssPath}${filename}"
  fun jsRef(filename: String): String = "${jsPath}${filename}"
}


fun <T, C : ContextTagConsumer<out T>> C.loginDialog(context: ServiceContext, errorMsg: String? = null, username: String? = null, password: String? = null, redirect: String? = null, cancelEnabled: Boolean = true): T {
  return darwinDialog(title="Log in",
                      positiveButton = "Log in",
                      negativeButton = if (cancelEnabled) "Cancel" else null) {
    div("errorMsg") {
      if (errorMsg==null) style="display:none" else +errorMsg
    }
    if (errorMsg!=null) {
    }
    form(action = "${context.accountMgrPath}login", method = FormMethod.post, encType = FormEncType.applicationXWwwFormUrlEncoded) {
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
      div { id="forgotpasswd"
        a(href="${context.accountMgrPath}resetpasswd") { +"Forgot password" }
      }
    }
  }
}

fun <T, C:ContextTagConsumer<out T>> C.setAliasDialog(oldAlias:String?):T =
  darwinDialog("Set alias") {
    form(action="${context.accountMgrPath}setAlias") {
      div {
        label { for_= "#alias"; +"Alias" }
        input(type= InputType.text, name="alias") {
          placeholder="Alias"
          oldAlias?.let { value=oldAlias }
        }
      }
    }
  }
