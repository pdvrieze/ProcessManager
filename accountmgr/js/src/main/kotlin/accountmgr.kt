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
 * Created by pdvrieze on 26/03/16.
 */
import nl.adaptivity.darwin.html.appendContent
import nl.adaptivity.darwin.html.context
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import org.w3c.xhr.XMLHttpRequest
import uk.ac.bournemouth.darwin.sharedhtml.shared.setAliasDialog
import uk.ac.bournemouth.darwin.util.encodeURI
import uk.ac.bournemouth.darwin.util.foreach
import kotlin.browser.document

fun updateLinks() {
  (document.getElementById("accountmgr.setAlias") as HTMLElement).onclick = ::displaySetAliasFormClicked
  document.getElementsByClassName("forget_key_class").foreach { e ->
    (e as HTMLElement).onclick = ::forgetClicked
  }
}

@JsName("displaySetAliasFormClicked")
fun displaySetAliasFormClicked(event: Event) {
  event.preventDefault()
  event.stopPropagation()
  val aliasValue = (event.target as HTMLElement).attributes["alias"]?.value ?: ""
  displaySetAliasForm(aliasValue)
}


fun displaySetAliasForm(oldName: String) {
  appendContent { setAliasDialog(this, oldName) }
}

val accountsLoc = context.accountMgrPath

/**
 * Forget the key with the given id.
 */
@JsName("forgetClicked")
fun forgetClicked(event: Event) {
  (event.target as HTMLElement).attributes["keyid"]?.value?.toIntOrNull()?.let { keyid ->
    event.preventDefault()
    event.stopPropagation()
    forget(event as MouseEvent, keyid)
  }

}

fun forget(event: MouseEvent, keyId: Int) {
  val request = XMLHttpRequest().apply {
    open("GET", "$accountsLoc/forget?keyid=${encodeURI(keyId.toString())}")
    onload = {
      (event.target as Element).parentElement?.parentElement?.remove()
    }
    onerror = { error("Could forget authorization: $statusText ($status)") }
  }
  try {
    request.send()
  } catch (e: Exception) {
    console.warn("Could not update menu", e)
  }

}
