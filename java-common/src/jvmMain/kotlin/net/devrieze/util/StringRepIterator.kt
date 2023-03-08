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
 * Created on Jan 20, 2004
 *
 */

package net.devrieze.util

/**
 * An iterator for walking over string parts consequtively.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
@Deprecated("Stale class that shouldn't be used")
@Suppress("DEPRECATION")
interface StringRepIterator {

    /**
     * Does the iterator have more elements.
     *
     * @return `true` if there is more
     */
    operator fun hasNext(): Boolean

    /**
     * Get the item that is currently being displayed.
     *
     * @return The current item
     */
    fun last(): StringRep

    /**
     * The next string item.
     *
     * @return if the list is full
     */
    operator fun next(): StringRep
}
