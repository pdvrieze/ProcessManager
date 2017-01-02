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

package net.devrieze.util

interface StringCache {

  enum class UniqueCaches : StringCache {
    NOP;

    override fun lookup(string: String): String {
      return string
    }

  }

  /**
   * Look up a string in the cache for string reuse.
   * @param string `null` parameters will always return null
   * *
   * @return
   */
  fun lookup(string: String): String

  companion object {

    val NOPCACHE = StringCache.UniqueCaches.NOP

  }

}


@Suppress("NOTHING_TO_INLINE")
inline fun StringCache.lookup(string: String?): String? = string?.let { lookup(it) }
