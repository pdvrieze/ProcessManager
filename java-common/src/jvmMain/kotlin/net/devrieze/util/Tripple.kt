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

/**
 * This class implements a tupple as is commonly used in many code snippets.
 * Such functionality could be implemented using
 *
 * @param S The type of the first element.
 * @param T The type of the second element.
 * @param U The type of the third element.
 * @see net.devrieze.util.Tupple
 *
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 *
 * @constructor Create a tripple with the specified elements.
 *
 * @property elem1 The first element
 * @property elem2 The second element
 * @property elem3 The third element
 *
 */
class Tripple<S, T, U>(
    var elem1: S,
    var elem2: T,
    var elem3: U
) {

    fun copy(elem1: S = this.elem1, elem2: T = this.elem2, elem3: U = this.elem3) =
        Tripple(elem1, elem2, elem3)

    operator fun component1(): S = elem1

    operator fun component2(): T = elem2

    operator fun component3(): U = elem3

    override fun toString(): String {
        return "($elem1, $elem2, $elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tripple<*, *, *>

        if (elem1 != other.elem1) return false
        if (elem2 != other.elem2) return false
        if (elem3 != other.elem3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elem1?.hashCode() ?: 0
        result = 31 * result + (elem2?.hashCode() ?: 0)
        result = 31 * result + (elem3?.hashCode() ?: 0)
        return result
    }

    companion object {

        fun <X, Y, Z> tripple(elem1: X, elem2: Y, elem3: Z): Tripple<X, Y, Z> {
            return Tripple(elem1, elem2, elem3)
        }
    }
}
