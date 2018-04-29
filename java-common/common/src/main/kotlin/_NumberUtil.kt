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
/**
 * Determine whether the value is between the two bounds. The bounds can be in any order (either [bound1] or [bound2] 
 * can be smaller. Both bounds are inclusive
 */
fun Double.isBetween(bound1:Double, bound2: Double) = when {
  bound1<bound2 -> this >= bound1 && this<=bound2
  else          -> this >= bound2 && this<=bound1
}

/**
 * Determine whether the value is between the two bounds. The bounds can be in any order (either [bound1] or [bound2]
 * can be smaller. Both bounds are inclusive
 */
fun Float.isBetween(bound1:Float, bound2: Float) = when {
  bound1<bound2 -> this >= bound1 && this<=bound2
  else          -> this >= bound2 && this<=bound1
}

/**
 * Determine whether the value is between the two bounds. The bounds can be in any order (either [bound1] or [bound2]
 * can be smaller. Both bounds are inclusive
 */
fun Int.isBetween(bound1:Int, bound2: Int) = when {
  bound1<bound2 -> this >= bound1 && this<=bound2
  else          -> this >= bound2 && this<=bound1
}

/**
 * Determine whether the value is between the two bounds. The bounds can be in any order (either [bound1] or [bound2]
 * can be smaller. Both bounds are inclusive
 */
fun Long.isBetween(bound1:Long, bound2: Long) = when {
  bound1<bound2 -> this >= bound1 && this<=bound2
  else          -> this >= bound2 && this<=bound1
}
