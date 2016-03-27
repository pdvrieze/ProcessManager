
import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlin.dom.childElements
import kotlin.dom.children
import kotlin.dom.removeFromParent

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