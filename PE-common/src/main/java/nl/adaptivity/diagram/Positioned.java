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

package nl.adaptivity.diagram;


public interface Positioned {

  /**
   * Determine whether the element actually has a real position.
   * @return <code>true</code> if it has, <code>false</code> if not.
   */
  boolean hasPos();

  /**
   * Get the X coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.
   *
   * @return The X coordinate
   */
  double getX();

  /**
   * Get the Y coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.
   *
   * @return The Y coordinate
   */
  double getY();

}
