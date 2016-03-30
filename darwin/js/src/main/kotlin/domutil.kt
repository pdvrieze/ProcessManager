
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

package uk.ac.bournemouth.darwin.util

import kotlinx.html.TagConsumer
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.NodeFilter
import kotlin.dom.childElements
import kotlin.dom.children
import kotlin.dom.removeFromParent
import kotlinx.html.dom.append as kotlinxAppend

const val BUTTON_DEFAULT: Short=0

inline fun Element.removeChildElementIf(predicate:(Element)-> Boolean) {
  for(childElement in childElements()) {
    if(predicate(childElement)) {
      childElement.removeFromParent()
    }
  }
}

@native("encodeURI") fun encodeURI(uri: dynamic):String? = noImpl

inline fun Element.removeChildIf(predicate:(Node)-> Boolean) {
  for(childNode in children()) {
    if(predicate(childNode)) {
      childNode.removeFromParent()
    }
  }
}

inline fun Node.appendHtml(crossinline block : TagConsumer<HTMLElement>.() -> Unit) : List<HTMLElement> = kotlinxAppend({ ConsumerExt(this).block() })

val HTMLElement.appendHtml : TagConsumer<HTMLElement>
  get() = ConsumerExt(kotlinxAppend)

class ConsumerExt<T>(val parent:TagConsumer<T>): TagConsumer<T> by parent {
  inline operator fun CharSequence.unaryPlus() { parent.onTagContent(this)}
}

fun Element.visitDescendants(filter:(Node)->Short = {node -> NodeFilter.FILTER_ACCEPT}, visitor: (Node)->Unit) {
  val walker = ownerDocument!!.createTreeWalker(root=this, whatToShow= NodeFilter.SHOW_ALL, filter=filter)
  while (walker.nextNode()!=null) {
    visitor(walker.currentNode)
  }
}

inline operator fun <T, C: TagConsumer<T>> C.plus(text:String) = this.onTagContent(text)