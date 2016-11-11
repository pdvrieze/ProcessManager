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
 * Created by pdvrieze on 26/03/16.
 */
import org.w3c.dom.Element
import org.w3c.dom.events.MouseEvent
import org.w3c.xhr.XMLHttpRequest
import uk.ac.bournemouth.darwin.sharedhtml.FIELD_KEYID
import uk.ac.bournemouth.darwin.util.encodeURI

@Suppress("UnsafeCastFromDynamic")
val html:nl.adaptivity.darwin.html get() = js("darwin.nl.adaptivity.darwin.html")

object accountmgr {

  @JsName("setAliasForm")
  fun setAliasForm(oldName: String) {
    html.appendContent { html.shared.setAliasDialog(this, oldName) }
  }

  val accountsLoc = html.context.accountMgrPath

  /**
   * Forget the key with the given id.
   */
  @JsName("forget")
  fun forget(event: MouseEvent, keyId: Int) {
    event.preventDefault()
    event.stopPropagation()
    val request = XMLHttpRequest().apply {
      open("GET", "${accountsLoc}/forget?keyid=${encodeURI(keyId.toString())}")
      onload = {
        (event.target as Element).parentElement?.parentElement?.remove()
      }
      onerror = { html.error("Could forget authorization: ${statusText} ($status)") }
    }
    try {
      request.send()
    } catch (e: Exception) {
      console.warn("Could not update menu", e)
    }

  }
}