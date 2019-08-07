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

package net.devrieze.util

import kotlin.reflect.KClass

class TypecheckingCollection<E:Any>(private val type: KClass<out E>, private val delegate: MutableCollection<E>): AbstractMutableCollection<E>() {

    override val size: Int get() = delegate.size

    override fun add(element: E): Boolean {
        if (!type.isInstance(element)) throw ClassCastException("The element $element is not of type $type")
        return delegate.add(element)
    }

    override fun iterator(): MutableIterator<E> = delegate.iterator()
}

class TypecheckingList<E:Any>(private val type: KClass<out E>, private val delegate: MutableList<E>): AbstractMutableList<E>() {

    override val size: Int get() = delegate.size

    override fun add(element: E): Boolean {
        if (!type.isInstance(element)) throw ClassCastException("The element $element is not of type $type")
        return delegate.add(element)
    }

    override fun add(index: Int, element: E) {
        if (!type.isInstance(element)) throw ClassCastException("The element $element is not of type $type")
        delegate.add(index, element)
    }

    override fun set(index: Int, element: E): E {
        if (!type.isInstance(element)) throw ClassCastException("The element $element is not of type $type")
        return delegate.set(index, element)
    }

    override fun iterator(): MutableIterator<E> = delegate.iterator()
    override fun get(index: Int): E = delegate.get(index)
    override fun removeAt(index: Int): E = delegate.removeAt(index)
}
