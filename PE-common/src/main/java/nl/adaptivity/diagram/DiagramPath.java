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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram;

/**
 * An abstraction for paths that can be drawn on a {@link Canvas}.
 * 
 * @author Paul de Vrieze
 * @param <PATH_T> The type of path. This normally is the type itself.
 */
public interface DiagramPath<PATH_T extends DiagramPath<PATH_T>> {

  /**
   * Move to a new point. This will create a new sub-path.
   * 
   * @param x The new X coordinate.
   * @param y The new Y coordinate
   * @return The path itself, to allow for method chaining.
   */
  PATH_T moveTo(double x, double y);

  /**
   * Draw a line from the current point to a new point.
   * 
   * @param x The new X coordinate.
   * @param y The new Y coordinate
   * @return The path itself, to allow for method chaining.
   */
  PATH_T lineTo(double x, double y);


  /**
   * Draw a cubic bezier spline from the current point to a new point.
   * 
   * @param x1 The first control point's x coordinate
   * @param y1 The first control point's y coordinate.
   * @param x2 The second control point's x coordinate
   * @param y2 The second control point's y coordinate.
   * @param x3 The endpoint's x coordinate
   * @param y3 The endpoint's y coordinate.
   * @return The path itself, to allow for method chaining.
   */
  PATH_T cubicTo(double x1, double y1, double x2, double y2, double x3, double y3);

  /**
   * Close the current sub-path by drawing a straight line back to the
   * beginning.
   * 
   * @return The path itself, to allow for method chaining.
   */
  PATH_T close();

  /**
   * Get the bounds of the path
   * @param dest The rectangle to store the bounds in (existing content is discarded)
   * @param stroke The pen that represents the stroke to use.
   * @return The bounds of the path
   */
  Rectangle getBounds(Rectangle dest, Pen<?> stroke);
}
