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


import org.jetbrains.annotations.NotNull;


public final class Point {
  public final double x;
  public final double y;
  
  public Point(final double x, final double y) {
    this.x = x;
    this.y = y;
  }

  @NotNull
  @Override
  public String toString() {
    return "(" + x + ", " + y + ')';
  }

  public double distanceTo(final Point other) {
    final double dx = other.x - x;
    final double dy = other.y - y;
    return Math.sqrt(dx*dx+dy*dy);
  }

  public static Point of(final double x, final double y) {
    return new Point(x,y);
  }
}
