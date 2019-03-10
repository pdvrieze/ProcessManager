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

/*
 * Created on Feb 4, 2004
 *
 */

package net.devrieze.util

import org.jetbrains.annotations.Contract


/**
 * A linked list based stack implementation for characters.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
class IntStack {

    private var top: Node? = null

    /**
     * Returns `true` if the stack is empty.
     *
     * @return boolean
     */
    val isEmpty: Boolean get() = top == null

    /**
     * @constructor Create a new node.
     *
     * @param value The value in the node.
     * @param next The next element. This may be `NULL`if there is no
     * next element.
     */
    private class Node constructor(
        @JvmField val value: Int,
        @JvmField val next: Node?) {

        /**
         * A string representation of the node.
         *
         * @return A string representation
         */
        override fun toString(): String {
            return if (next != null) {
                next.toString() + ", " + Integer.toString(value)
            } else Integer.toString(value)

        }
    }

    /**
     * Get the value at the top of the stack.
     *
     * @return The value at the top
     */
    fun peek(): Int {
        return top!!.value
    }

    /**
     * Get the top character from the stack.
     *
     * @return the top character
     */
    fun pop(): Int {
        return top!!.apply {
            top = next
        }.value
    }

    /**
     * Push a value onto the stack.
     *
     * @param value the value to be pushed
     */
    fun push(value: Int) {
        top = Node(value, top)
    }

    /**
     * Get a String representation for the stack.
     *
     * @return The string representation
     */
    @Contract(pure = true)
    override fun toString(): String {
        return "[$top]"
    }
}
