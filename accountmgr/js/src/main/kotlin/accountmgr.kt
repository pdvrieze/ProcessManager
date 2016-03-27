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
package accountmgr

import kotlin.browser.document


fun main(args: Array<String>) {
  val el = document.createElement("div")
  el.appendChild(document.createTextNode("Hello!"))
  document.body!!.appendChild(el)

  val counterDiv = document.createElement("div")
  val counterText = document.createTextNode("Counter!")
  counterDiv.appendChild(counterText)
  document.body!!.appendChild(counterDiv)
}