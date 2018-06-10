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

package uk.ac.bournemouth.darwin.sharedhtml

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.w3c.dom.events.Event

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

open class ContextTagConsumer<out T>(val context:ServiceContext, private val myDelegate: TagConsumer<T>): TagConsumer<T> {
  @Suppress("NOTHING_TO_INLINE")
  inline operator fun CharSequence.unaryPlus() = onTagContent(this)

  override fun finalize() = myDelegate.finalize()

  override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) = myDelegate.onTagAttributeChange(tag, attribute, value)

  override fun onTagContent(content: CharSequence) = myDelegate.onTagContent(content)

  override fun onTagComment(content: CharSequence) = myDelegate.onTagComment(content)

  override fun onTagContentEntity(entity: Entities) = myDelegate.onTagContentEntity(entity)

  override fun onTagContentUnsafe(block: Unsafe.() -> Unit) = myDelegate.onTagContentUnsafe(block)

  override fun onTagEnd(tag: Tag) = myDelegate.onTagEnd(tag)

  override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) = myDelegate.onTagEvent(tag, event, value)

  override fun onTagStart(tag: Tag) = myDelegate.onTagStart(tag)
}

open class SharedButton(val label:String, val id:String)

@Suppress("NOTHING_TO_INLINE")
inline fun <T,C:TagConsumer<T>> C.withContext(context:ServiceContext) = ContextTagConsumer(context, this)

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T:Tag> T.withContext(context:ServiceContext) = (consumer as TagConsumer<T>).withContext(context)


/** Just inline for now, as this is just a forwarder. */
fun <O : Appendable> O.appendXML(prettyPrint: Boolean = true): TagConsumer<O>
    = this.appendHTML(prettyPrint)

class PartialHTML(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) : HTMLTag("root", consumer, initialAttributes, null, false, false) {

  fun title(block: TITLE.() -> Unit = {}): Unit = TITLE(emptyMap, consumer).visit(block)
  fun title(content: String = ""): Unit = TITLE(emptyMap, consumer).visit({ +content })

  fun body(block: XMLBody.() -> Unit = {}): Unit = XMLBody(emptyMap, consumer).visit(block)
}


fun <T, C : TagConsumer<T>> C.partialHTML(block: PartialHTML.() -> Unit = {}): T = PartialHTML(emptyMap, this).visitAndFinalize(this, block)

fun <T, C : ContextTagConsumer<T>> C.darwinBaseDialog(title: String, id: String? = null,
                                                      bodyContent: ContextTagConsumer<DIV>.() -> Unit = {}):T {
  return div(classes = "dialog centerContents") {
    if (id != null) {
      this.id = id
    }
    div(classes = "dialogOuter") {
      h1(classes = "dlgTitle") { +title }
      div(classes = "dialogInner centerContents") {
        withContext(context).bodyContent()
      }
    }
  }
}

fun <T, C : ContextTagConsumer<T>> C.darwinDialog(title: String, id: String? = null, positiveButton:SharedButton?=SharedButton("Ok","btn_dlg_ok"), negativeButton:SharedButton?=null, vararg otherButtons:SharedButton, bodyContent: ContextTagConsumer<*>.() -> Unit = {}):T {
  return darwinBaseDialog(title, id) {
    div(classes = "dlgContent") {
      withContext(context).bodyContent()
    }
    dialogButtons(positiveButton, negativeButton, *otherButtons)

  }
}

private fun ContextTagConsumer<DIV>.dialogButtons(positiveButton: SharedButton?,
                              negativeButton: SharedButton?,
                              vararg otherButtons: SharedButton) {
  div {
    div(classes = "dlgButtons") {
      style = "margin-top: 1em; float: right;"
      if (negativeButton != null && negativeButton.label.isNotEmpty()) {
        input(type = InputType.button, classes = "dlgbutton dialogcancel") {
          value = negativeButton.label
          id = negativeButton.id
        }
      }
      for (otherButton in otherButtons) {
        input(type = InputType.button, classes = "dlgbutton dialogother") {
          value = otherButton.label
          id = otherButton.id
        }
      }
      if (positiveButton != null && positiveButton.label.isNotEmpty()) {

        input(type = InputType.submit, classes = "dlgbutton dialogconfirm") {
          value = positiveButton.label
          id = positiveButton.id
        }
      }
    }
  }
}

fun DIV.loginPanelContent(context: ServiceContext, username: String?) {
  consumer.loginPanelContent(context, username)
}

fun <T, C: TagConsumer<T>> C.loginPanelContent(context: ServiceContext, username: String?) {
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
  val jsGlobalPath:String
  val jsLocalPath:String

  fun cssRef(filename: String): String = "$cssPath$filename"
  fun jsGlobalRef(filename: String): String = "$jsGlobalPath$filename"
  fun jsLocalRef(filename: String): String = "$jsLocalPath$filename"
}


fun <T, C : ContextTagConsumer<T>> C.loginDialog(context: ServiceContext, errorMsg: String? = null, username: String? = null, password: String? = null, redirect: String? = null, cancelEnabled: Boolean = true): T {
  return darwinBaseDialog(title="Log in") {
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
            label { htmlFor = "#$FIELD_USERNAME"
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
            label { htmlFor = "#$FIELD_PASSWORD"
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
      dialogButtons(positiveButton = SharedButton("Log in", "btn_login"),
                    negativeButton = if (cancelEnabled) SharedButton("Cancel","btn_login_cancel") else null)
      div { id="forgotpasswd"
        a(href="${context.accountMgrPath}resetpasswd") { +"Forgot password" }
      }
    }
  }
}

fun <T, C:ContextTagConsumer<T>> C.setAliasDialog(oldAlias:String?):T =
      darwinDialog("Set alias", negativeButton = SharedButton("Cancel", "btn_alias_cancel")) {
        form(action="${context.accountMgrPath}setAlias") {
          div {
            label { htmlFor = "#alias"; +"Alias" }
            input(type= InputType.text, name="alias") {
              placeholder="Alias"
              oldAlias?.let { value=oldAlias }
            }
          }
        }
      }

object shared {
  fun <T, C:ContextTagConsumer<T>> setAliasDialog(consumer: C, oldAlias:String?):T {
    return consumer.setAliasDialog(oldAlias)
  }
}