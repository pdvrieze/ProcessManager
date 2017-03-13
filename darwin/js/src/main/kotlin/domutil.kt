
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
 * Created by pdvrieze on 27/03/16.
 */

package uk.ac.bournemouth.darwin.util

import kotlinx.html.TagConsumer
import org.w3c.dom.*
import kotlin.coroutines.experimental.buildIterator
import kotlin.dom.clear
import kotlinx.html.dom.append as kotlinxAppend

const val BUTTON_DEFAULT: Short=0

fun Element.childElements():Iterable<Element> = object:Iterable<Element>
{
  override fun iterator() = buildIterator<Element> {
    this@childElements.childNodes.forEach { child ->
      if (child is Element) yield(child)
    }
  }
}

inline fun Element.removeChildElementIf(predicate:(Element)-> Boolean) {
  for(childElement in childElements()) {
    if(predicate(childElement)) {
      childElement.remove()
    }
  }
}

external fun encodeURI(uri: dynamic):String? = definedExternally

inline fun Element.removeChildIf(predicate:(Node)-> Boolean) {
  childNodes.forEach { childNode ->
    if(predicate(childNode)) {
      childNode.removeFromParent()
    }
  }
}

inline fun Node.removeFromParent() {
  parentElement!!.removeChild(this)
}

inline fun NodeList.forEach(visitor: (Node)->Unit) {
  var i=0
  val len = this.length
  while(i<len) {
    visitor(this[i]!!)
    i+=1
  }
}

fun Element.setChildren(children:NodeList?, alternative: () -> Node? = {null}) {
  this.clear()
  val elem = this
  if (children==null) {
    alternative()?.let { elem.appendChild(it) }
  } else {
    while(children.length>0) { elem.appendChild(children.item(0)!!) }
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