/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine

import nl.adaptivity.util.DomUtil
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.w3c.dom.NodeList

actual val ProcessData.contentFragment: DocumentFragment
    get() = DomUtil.childrenToDocumentFragment(contentStream)

@Suppress("DEPRECATION", "FunctionName")
@Deprecated("")
fun ProcessData(name: String, nodeList: NodeList?) =
    ProcessData(name, node = (if (nodeList == null || nodeList.length <= 1) toNode(nodeList) else DomUtil.toDocFragment(nodeList)))

@Suppress("FunctionName")
@Deprecated("Use compactFragments directly", ReplaceWith("ProcessData(name, DomUtil.nodeToFragment(node))",
                                                         "nl.adaptivity.process.engine.ProcessData", "nl.adaptivity.util.DomUtil"))
fun ProcessData(name: String, node: Node?) = ProcessData(name, DomUtil.nodeToFragment(node))

private fun toNode(value: NodeList?): Node? {
    if (value == null || value.length == 0) {
        return null
    }
    assert(value.length == 1)
    return value.item(0)
}
