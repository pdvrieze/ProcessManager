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
 * Created on Oct 9, 2004
 *
 */

package net.devrieze.util

import nl.adaptivity.util.multiplatform.JvmStatic

/**
 * This class implements a tupple as is commonly used in many code snippets.
 * Such functionality could be implemented using
 *
 * @param S The type of the first element in the tupple
 * @param T The type of the second element in the tupple
 * @property elem1 The first element
 * @property elem2 The second element
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
@Deprecated("Use pair", ReplaceWith("Pair", "kotlin.Pair"))
data class Tupple<S, T>(var first: S, var second: T) {

    @Deprecated("Match Pair", ReplaceWith("first"))
    var elem1
        get() = first
        set(value) { first = value }
    
    @Deprecated("Match Pair", ReplaceWith("second"))
    var elem2
        get() = second
        set(value) { second = value }
    
    override fun toString(): String {
        return "($elem1, $elem2)"
    }

    companion object {

        @JvmStatic
        fun <S, T> tupple(pElem1: S, pElem2: T): Tupple<S, T> {
            return Tupple(pElem1, pElem2)
        }

        /**
         * Create a new array that has the value from pElem1 at each first postition
         *
         * @param elem1
         * @param elem2
         */
        @JvmStatic
        fun <S, T> pack1st(elem1: S, elem2: Array<T>): Array<Tupple<S, T>> {
            return Array(elem2.size) { idx -> Tupple(elem1, elem2[idx]) }
        }
    }
}
