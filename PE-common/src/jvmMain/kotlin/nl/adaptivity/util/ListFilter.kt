/*
 * Copyright (c) 2018.
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

package nl.adaptivity.util

import java.util.AbstractList
import java.util.ArrayList


class ListFilter<T>(private val clazz: Class<T>, private val isLax: Boolean) : AbstractMutableList<T>() {

    private val source: MutableList<T> = mutableListOf()


    override fun get(index: Int): T {
        return source[index]
    }

    override val size: Int
        get() {
            return source.size
        }

    override fun add(index: Int, element: T) {
        if (clazz.isInstance(element)) {
            source.add(index, clazz.cast(element))
        } else if (!isLax) {
            clazz.cast(element)
        }
    }

    override fun add(elem: T): Boolean {
        return if (clazz.isInstance(elem)) {
            super.add(clazz.cast(elem))
        } else {
            false
        }
    }

    override fun removeAt(index: Int): T {
        return source.removeAt(index)
    }

    /**
     * Add all the objects in the collection to this one as they match the type
     * @param c The source collection
     * @return `true` if the souce list was changed, `false` if not.
     */
    fun addAllFiltered(c: Collection<T>): Boolean {
        var result = false

        if (source is ArrayList<*>) {
            (source as ArrayList<*>).ensureCapacity(source.size + c.size)
        }

        if (c is ListFilter && clazz.isAssignableFrom(c.clazz)) {
            // We can take a shortcut here
            return source.addAll(c)
        }

        for (elem in c) {
            result = add(elem) || result
        }
        return result
    }

    override fun set(index: Int, element: T): T {
        if (clazz.isInstance(element)) {
            return source.set(index, element)
        } else if (!isLax) {
            clazz.cast(element)
        }
        return source[index]
    }

}
