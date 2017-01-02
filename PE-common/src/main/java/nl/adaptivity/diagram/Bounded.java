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


public interface Bounded {

  /**
   * Get a rectangle containing the bounds of the object. Objects should not normally
   * be drawn only inside their bounds. And the bounds are expected to be as small as possible.
   * @return The bounds of the object.
   */
  Rectangle getBounds();

  /**
   * Determine whether the given coordinate lies within the object. As objects may be
   * shaped, this may mean that some points are not part even though they look to be.
   * The method will return the most specific element contained.
   *
   * @param x The X coordinate
   * @param y The Y coordinate
   * @return <code>null</code> if no item could be found, otherwise the item found.
   */
  Bounded getItemAt(double x, double y);

}