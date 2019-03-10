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

package net.devrieze.util

import java.util.WeakHashMap


/**
 * A cache that will help with replacing strings with a canonical version.
 *
 * @author Paul de Vrieze
 */
open class StringCacheImpl : StringCache {

    internal val cache = WeakHashMap<String, String>()

    /**
     *
     * @author Paul de Vrieze
     */
    @Deprecated("The purpose of this is not clear")
    class SafeStringCache : StringCacheImpl() {

        @Synchronized
        override fun lookup(string: String?): String? {
            return super.lookup(string)
        }

    }

    /*
   * (non-Javadoc)
   * @see net.devrieze.util.StringCache#lookup(java.lang.String)
   */
    override fun lookup(string: String?): String? {
        if (string == null) {
            return null
        }
        cache[string]?.let { return it }

        // Use a narrow stringbuilder such as not to carry extra bytes into the cache.
        val builder = StringBuilder(string.length)
        builder.append(string)
        builder.trimToSize()

        val result = builder.toString()
        cache[result] = result
        return result
    }

}
