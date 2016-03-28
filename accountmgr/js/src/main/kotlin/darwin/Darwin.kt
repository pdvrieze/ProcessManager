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

package nl.adaptivity.darwin.gwt.client

import BUTTON_DEFAULT
import encodeURI
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.dom.create
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.p
import kotlinx.html.js.span
import kotlinx.html.span
import org.w3c.dom.*
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest
import removeChildElementIf
import uk.ac.bournemouth.darwin.html.*
import uk.ac.bournemouth.darwin.html.shared.*
import java.io.Closeable
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.*

object Darwin {

  private fun onLoginResult(request: XMLHttpRequest) {
    val text = request.responseText
    val cpos = text.indexOf(':')
    val eolpos = text.indexOf('\n', cpos)
    val result: String
    val payload: String?
    if (cpos >= 0) {
      result = text.substring(0, cpos)
      if (eolpos >= 0) {
        payload = text.substring(cpos + 1, eolpos)
      } else {
        payload = text.substring(cpos + 1)
      }
    } else {
      result = text
      payload = null
    }

    val dialog = mLoginDialog!!

    if ("login" == result && payload != null) {
      mUsername = payload
      closeDialogs()
      updateLoginPanel()
      requestRefreshMenu(mLocation!!)
    } else if ("logout" == result) {
      mUsername = null
      closeDialogs()
      updateLoginPanel()
      requestRefreshMenu(mLocation!!)
      navigateTo("/", true, true)
    } else if ("error" == result) {
      closeDialogs()
      error("Error validating login: " + payload!!, null)
    } else if ("invalid" == result) {
      dialog.errorMsg="Credentials invalid"
      dialog.password=null
    } else {
      closeDialogs()
      error("Invalid response received from login form : ${request.statusText} (${request.status})", null)
    }
  }

  private fun onLoginError(request: XMLHttpRequest) {
    error("Could not login due to request error: ${request.statusText} (${request.status})")
    closeDialogs()
  }

  private fun onLoginDialogConfirm(event:Event) {
    val form = (event.target!! as HTMLInputElement).form!!

    event.stopPropagation()

    val username:String? = (form.get("username") as? HTMLInputElement)?.value
    val password:String? = (form.get("password") as? HTMLInputElement)?.value

    if (username.isNullOrBlank()) { /* set error bit*/ }
    if (password.isNullOrBlank()) { /* set error bit */ }

    val request = XMLHttpRequest().apply {
      setRequestHeader("Accept", "text/plain")
      setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
      open("POST", LOGIN_LOCATION)
      onload = { onLoginResult(this) }
      onerror = { onLoginError(this) }
    }

    val postData = FormData().apply {
      append("username", username)
      append("password", password)
    }

    try {
      request.send(postData)
    } catch (e: Exception) {
      error("Could not send login request", e)
      closeDialogs()
    }

  }

  private fun dialogCloseHandler(event:Event) {
    closeDialogs()
  }

  private fun onLoginOutClicked(event:MouseEvent) {
    if (mUsername == null) {
      loginDialog()
      // Login
    } else {
      val request = XMLHttpRequest().apply {
        setRequestHeader("Accept", "application/binary")
        open("GET", "/accounts/logout")
        onload = { onLoginResult(this) }
        onerror = { error("Error logging out: ${statusText} (${status})")}
      }
      try {
        request.send()
      } catch (e: Exception) {
        error("Could not log out", e)
      }

    }
  }

/*
  private inner class HistoryChangeHandler : ValueChangeHandler<String> {

    fun onValueChange(event: ValueChangeEvent<String>) {
      val newValue = event.getValue()
      navigateTo(newValue, false, false)
    }

  }
*/

  fun onLinkClick(event: MouseEvent) {
    if (event.button == BUTTON_DEFAULT) {
      val target = event.target as? HTMLAnchorElement
      var href = target?.href
      // handle urls to virtual pages
      if (href!=null) {
        if (href.startsWith("/#")) {
          href = href.substring(2)
        }
        navigateTo(href, true, true)
        event.preventDefault()
        event.stopPropagation()
      }
    }

  }

  fun onMenuReceived(request: XMLHttpRequest) {
    if (request.status in 200..399) {
      val text = request.responseText
      val m = menu
      m.innerHTML = text
      val firstChild = m.firstElementChild
      if (firstChild?.nodeName=="menu") {
        m.append(firstChild.children())
        firstChild!!.removeFromParent()
      }
      convertMenuToJS()
    } else {
      error("Error updating the menu: ${request.statusText} (${request.status})")
    }
  }

  private fun onContentPanelReceived(request: XMLHttpRequest) {
    val statusCode:Int = request.status as Int
    if (statusCode == 401) {
      hideBanner()
      loginDialog() // just return
      return
    } else if (statusCode !in 200..399) {
      hideBanner()
      error("Failure to load panel: " + statusCode)
      return
    }
    mContentPanel!!.clear()
    hideBanner()
    val root = request.responseXML?.documentElement
    if (root!=null) {
      var windowtitle: String? = null
      var pagetitle: NodeList? = null
      var body: NodeList? = null
      for(childElement in root.elements()) {
        when (childElement.nodeName) {
          "title" -> if (windowtitle==null) {
            windowtitle = childElement.getAttribute("windowtitle") ?: childElement.textContent
            pagetitle = document.importNode(childElement, true).childNodes
          }
          "body" -> if (body==null) { body = document.importNode(childElement, true).childNodes }
          else error("unexpected child in dynamic content: ${childElement.nodeName}")
        }
      }

      windowtitle?.let { document.title=it }

      if (pagetitle != null) {
        val onPageTitle = document.getElementById("title")
        onPageTitle?.let {
          it.append(pagetitle)
        }
      }
      if (body != null) {
        mContentPanel!!.append(body)
      }
    }
  }

  private fun onContentPanelError(request: XMLHttpRequest) {
    hideBanner()
    error("The requested location is not available: ${request.statusText} (${request.status})")
  }

  //  interface DarwinUiBinder extends UiBinder<Widget, Darwin> { /* Dynamic gwt */}

  private val menu: HTMLElement
    get() { return document.getElementById("menu") as HTMLElement }

  private var context: ServiceContext = JSServiceContext()

  private var mMenu: Element? = menu

  private var mLocation: String? = null

  private var mLoginDialog: LoginDialog? = null

  private var mUsername: String? = null
  private var mPassword: String? = null

  private var mContentPanel: HTMLElement? = null

  internal var dialogTitle: HTMLSpanElement? = null

  private var mLoginoutRegistration: Closeable? = null

  private var mUsernameRegistration: Closeable? = null

  private var mBanner: Element? = null

  fun main(args: Array<String>) {
    val newLocation = window.location.hash.let { if (it.isNullOrBlank()) window.location.pathname else it }

    mMenu = document.getElementById("menu")
//    val newLocation = History.getToken()
    val usernameSpan = document.getElementById("username")

    (document.getElementById("xloginform") as? HTMLFormElement) ?.let { form ->
      mUsername = (form["username"] as? HTMLInputElement)?.value
      mPassword = (form["password"] as? HTMLInputElement)?.value
      form.removeFromParent() // No longer needed
    }

    mContentPanel = document.getElementById("content") as HTMLElement

    if (!window.location.hash.isNullOrBlank()) {
      requestRefreshMenu(newLocation)
    }

    convertMenuToJS()

    registerLoginPanel()

//    History.addValueChangeHandler(HistoryChangeHandler())

    mBanner = document.getElementById("banner")

    // This is not a page that already has it's content.
    if (asInlineLocation(newLocation) == null) {
      showBanner()
      navigateTo(newLocation, false, false)
    } else {
      mLocation = newLocation
    }
  }

  /**
   * @category ui_elements
   */
  private fun hideBanner() = mBanner?.setAttribute("style", "display:none")

  /**
   * @category ui_elements
   */
  private fun showBanner()  = mBanner?.removeAttribute("style")

  /**
   * @category ui_elements
   */
  private fun modalDialog(string: String) {
    dialog("Message") {
      span { +string }
      button () {
        value = "Ok"
        onClickFunction = { dialogCloseHandler(it) };
      }
    }
  }

  /**
   * @category error_handling
   */
  fun error(message: String, exception: Throwable? = null) {
    console.error(message, exception)
    val completeMessage: String
    if (exception == null) {
      completeMessage = message
    } else {
      completeMessage = message + "<br />" + exception.message
    }
    modalDialog(completeMessage)
  }

  private fun loginDialog() {
    val loginDialog = LoginDialog(username = mUsername, password=mPassword, visitConfirm = { onClickFunction = { event -> onLoginDialogConfirm(event) } }, visitCancel = { closeDialogs() })
    mLoginDialog = loginDialog

    mContentPanel!!.appendChild(loginDialog.element)
  }

  /**
   * @category ui_elements
   */
  private fun updateDialogTitle(string: String) {
    dialogTitle?.textContent=string
  }

  /**
   * @category ui_elements
   */
  private fun dialog(title: String, id: String? = null, content: FlowContent.() -> Unit) {
    val dlg: HTMLElement = document.create.darwinDialog(title, id, content)
    mContentPanel!!.appendChild(dlg)
  }

  private fun closeDialogs() {
    val contentPanel = mContentPanel
    if (contentPanel !=null) {
      contentPanel.removeChildElementIf {it.hasClass("dialog")}
    }
    if (mLoginDialog!=null) { mLoginDialog = null }
  }

  fun navigateTo(newLocation: String?, addHistory: Boolean, doRedirect: Boolean) {
    var location = this.mLocation
    if (location == null && newLocation != null || location != null && location != newLocation) {
      if (location != null && location.startsWith("/accounts/myaccount")) {
        location = newLocation
        updateLoginPanel()
      } else {
        location = newLocation
      }
      updateMenuTabs()

      if (location == "/" || location == "" || location == null) {
        hideBanner()
        setInboxPanel()
      } else if (location == "/actions") {
        hideBanner()
        setActionPanel()
      } else if (location == "/processes") {
        hideBanner()
        setProcessesPanel()
      } else if (location == "/about") {
        hideBanner()
        setAboutPanel()
      } else {
        val inlineLocation = asInlineLocation(location)
        if (inlineLocation != null) {
          location = inlineLocation
          val contentPanel = mContentPanel
          if (contentPanel!=null) {
            contentPanel.clear()
            contentPanel.appendChild(document.create.span("label") { +"Loading" })
          }

          val request = XMLHttpRequest().apply {
            open("GET", inlineLocation)
            setRequestHeader("Accept", "text/html")
            setRequestHeader("X-Darwin", "nochrome")
          }
          request.onload = { onContentPanelReceived(request) }
          request.onerror = { onContentPanelError(request)}
          try {
            request.send()
          } catch (e:Exception) {
            error("Could load requested content", e)
            closeDialogs()
          }

        } else {
          if (doRedirect) {
            // Load the page
            window.location.assign(newLocation!!)
          } else {
            hideBanner()
          }
        }
      }
      if (addHistory) {
        window.history.pushState(data=location, title="location", url=location)
//        History.newItem(location, false)
      }
    }
    this.mLocation = location
  }

  private fun updateMenuTabs() {
    var menuitem = mMenu!!.firstElementChild
    while (menuitem != null) {
      updateLinkItem(menuitem)
      menuitem = menuitem.nextElementSibling
    }
  }

  private fun updateLinkItem(menuitem: Element) {
    var href = menuitem.getAttribute("href")
    if (href!=null && href.length>0) {
      if (href.startsWith("/#")) {
        href = href.substring(2)
      }
      if (href == mLocation) {
        menuitem.addClass("active")
      } else {
        menuitem.removeClass("active")
      }
    }
  }

  /**
   * Make the menu elements active and add an onClick Listener.
   */
  fun convertMenuToJS() { // TODO convert this into a window handler that is a bit smarter.
    val clickHandler = { ev:MouseEvent -> onLinkClick(ev)}
    for(item in mMenu!!.elements()) {
      item.onClick(true, clickHandler)
      updateLinkItem(item)
    }
  }

  inline fun setContent(vararg newContent: Node) {
    setContentHelper(newContent)
  }

  inline fun setContent(newContent: NodeList) {
    setContentHelper(newContent)
  }

  fun setContentHelper(vararg newContent: dynamic) {
    val contentPanel = mContentPanel!!
    contentPanel.clear()
    contentPanel.append(newContent)
  }

  inline fun setContent(block: ()->Node) {
    setContent(block())
  }

  inline fun setContentList(block: ()->NodeList) {
    setContent(block())
  }

  private fun setInboxPanel() {
    setContent(document.create.span { +"Inbox panel - work in progress" })
  }

  private fun setProcessesPanel() {
    setContent(document.create.span { +"Processes panel - work in progress" })
  }

  private fun setActionPanel() {
    setContent(document.create.span { +"Action panel - work in progress" })
  }

  private fun setAboutPanel() {
    setContent {
      document.create.p {
        +"""Welcome to the Darwin server. This server functions as a research prototype as well
            as support for the Web Information Systems and Application Programming units."""
      }
    }
  }


  private fun registerLoginPanel() {
    mLoginoutRegistration = document.getElementById("logout")?.let { it.removeAttribute("href"); it.onClick { ev -> onLoginOutClicked(ev) } }
    mUsernameRegistration = document.getElementById("username")?.let { it.removeAttribute("href"); it.onClick { ev -> onLinkClick(ev) } }
  }

  private fun unregisterLoginPanel() {
    mLoginoutRegistration?.close()
    mLoginoutRegistration = null
    mUsernameRegistration?.close()
    mUsernameRegistration = null
  }


  private fun updateLoginPanel() {
    unregisterLoginPanel()

    val loginPanel = document.getElementById("login") as HTMLDivElement
    loginPanel.clear()
    val content = document.create.loginPanelContent(context, mUsername)
    loginPanel.append(content)
    registerLoginPanel()
  }


  private fun requestRefreshMenu(location: String) {
    val request = XMLHttpRequest().apply {
      open("GET", "/common/menu?location=${encodeURI(location)}")
      onload = { onMenuReceived(this) }
      onerror = { error("Could not update menu: ${statusText} ($status)")}
    }
    try {
      request.send()
    } catch (e: Exception) {
      log("Could not update menu", e)
    }

  }

  private val LOGIN_LOCATION = "/accounts/login.php"

  private val INLINEPREFIXES = arrayOf("/accounts/chpasswd", "/accounts/myaccount")

  private fun asInlineLocation(location: String): String? {
    for (prefix in INLINEPREFIXES) {
      if (location.startsWith(prefix)) {
        return prefix
      }
    }
    return null
  }

  private fun log(message: String, throwable: Throwable) {
    console.warn(message, throwable)
  }

  private fun log(message: String) {
    console.info(message)
  }

}
