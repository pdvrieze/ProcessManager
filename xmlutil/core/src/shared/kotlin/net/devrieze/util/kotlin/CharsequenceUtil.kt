/*
 * Copyright (c) 2017.
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
@file:JvmName("CharSequenceUtil")
package net.devrieze.util.kotlin

/**
 * Utility functions on CharSequences.
 */

@Suppress("NOTHING_TO_INLINE")
inline infix fun CharSequence.matches(other:CharSequence):Boolean =
  this.equals(other)

fun equals(first: CharSequence, other: CharSequence):Boolean {
  if (first ===other) return true
  if (first.length!=other.length) return false
  for(i in first.indices) {
    if (first[i] != other[i]) return false
  }
  return true
}


fun CharSequence?.equals(other:Any) =
      if (other is CharSequence) (this?.equals(other) ?: false) else this==null


fun CharSequence?.asString() = this?.toString()
