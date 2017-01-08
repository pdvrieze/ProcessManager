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

package nl.adaptivity.diagram


interface Positioned {

  /**
   * Determine whether the element actually has a real position.
   * @return `true` if it has, `false` if not.
   */
  val hasPos get() = !x.isNaN() && !y.isNaN()

  fun hasPos() = x.isFinite() && y.isFinite()

  /**
   * Get the X coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.

   * @return The X coordinate
   */
  val x: Double

  /**
   * Get the Y coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.

   * @return The Y coordinate
   */
  val y: Double

}
