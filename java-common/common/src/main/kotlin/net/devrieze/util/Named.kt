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
 * Created on Nov 3, 2003
 *
 */

package net.devrieze.util


/**
 * The Named interface groups classes that have a name. As result of that name
 * they can be elements of a nameSet, the can be sorted based on their name,
 * etc.
 *
 * @author Paul de Vrieze
 * @version 1.1 $Revision$
 */
interface Named {

    /**
     * Get the name of the object.
     *
     * @return The name of the object
     */
    val name: String?

    /**
     * This class provides a comparator for objects that implement the Named
     * interface.
     *
     * @param T Type of the objects to compare
     * @author Paul de Vrieze
     * @version 1.1 $Revision$
     */
    class NameCompare<T : Named> : Comparator<T> {

        /**
         * This comparator compares the name according to simple string comparison.
         *
         * @param a The first Item that should be compared
         * @param b The item that pObj1 needs to be compared to
         * @return
         * - <0 iff pObj1 < pObj2
         * - ==0 iff pObj1 == pObj2
         * - &gt;0 iff pObj1 > pObj2
         */
        override fun compare(a: T, b: T): Int {
            val n1 = a.name
            val n2 = b.name
            return when {
                n1 == null && n2 == null -> 0
                n1 == null -> -1
                n2 == null -> 1
                else -> n1.compareTo(n2)
            }
        }
    }

    /**
     * Create a new object that is a clone of this one, except that it has the
     * given name.
     *
     * @param name The new attribute of the name property.
     * @return The new object
     */
    @Deprecated("Use copy", ReplaceWith("copy(name)"))
    fun newWithName(name: String): Named

    fun copy(name: String? = this.name): Named
}
