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

package nl.adaptivity.diagram

import isBetween


interface Bounded : Positioned, HasExtent {

  /**
   * Get a rectangle containing the bounds of the object. Objects should normally
   * be drawn only inside their bounds. And the bounds are expected to be as small as possible.
   *
   * @return The bounds of the object.
   */
  val bounds: Rectangle get() = when {
    hasPos -> Rectangle(x - leftExtent, y - topExtent, leftExtent + rightExtent, topExtent + bottomExtent)
    else   -> Rectangle(-leftExtent, -topExtent, leftExtent + rightExtent, topExtent + bottomExtent)
  }

  /**
   * Determine whether the given coordinate lies within the object. As objects may be
   * shaped, this may mean that some points are not part even though they look to be.
   * The method will return the most specific element contained, itself or nothing.
   *
   * The default implementation returns this item if [isWithinBounds] evaluates to true
   *
   * @param x The X coordinate
   *
   * @param y The Y coordinate
   *
   * @return `null` if no item could be found, otherwise the item found.
   */
  fun getItemAt(x: Double, y: Double): Bounded? = if (isWithinBounds(x, y)) this else null

  /**
   * Determine whether the coordinates are within the bounds of the item. This particular implementation is naive
   * and assumes that the item is a rectangle equal to the extents of the item.
   */
  fun isWithinBounds(x: Double, y: Double): Boolean =
    hasPos && ((x - this.x).isBetween(-leftExtent, rightExtent) &&
               (y - this.y).isBetween(-topExtent, bottomExtent))

}