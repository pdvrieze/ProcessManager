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


public final class Rectangle implements Cloneable{

  public double left;
  public double top;
  public double width;
  public double height;

  public Rectangle(final double left, final double top, final double width, final double height) {
    this.top = top;
    this.left = left;
    this.height = height;
    this.width = width;
  }

  @NotNull
  @Override
  public Rectangle clone() {
    try {
      return (Rectangle) super.clone();
    } catch (@NotNull final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public final float leftf() {
    return (float) left;
  }

  public final float topf() {
    return (float) top;
  }

  public final double right() {
    return left+width;
  }

  public final float rightf() {
    return (float) right();
  }

  public final double bottom() {
    return top+height;
  }

  public final float bottomf() {
    return (float) bottom();
  }

  public final float widthf() {
    return (float) width;
  }

  public final float heightf() {
    return (float) height;
  }

  /**
   * Create an offsetted rectangle. The offsets should not be prescaled. They will be scaled in the method.
   * The scaling is from the top left of the rectangle.
   * @param xOffset The x offset.
   * @param yOffset The y offset.
   * @param scale The scaling needed.
   * @return A new rectangle that is moved from the original one.
   */
  @NotNull
  public final Rectangle offsetScaled(final double xOffset, final double yOffset, final double scale) {
    return new Rectangle((left+xOffset)*scale, (top+yOffset)*scale, width*scale, height*scale);
  }

  @NotNull
  @Override
  public final String toString() {
    return "Rectangle [l=" + left + ", t=" + top + ", w=" + width + ", h=" + height + "]";
  }

  public final void set(final double left, final double top, final double width, final double height) {
    this.left = left;
    this.top = top;
    this.width = width;
    this.height = height;
  }

  public final void set(@NotNull final Rectangle bounds) {
    left = bounds.left;
    top = bounds.top;
    width = bounds.width;
    height = bounds.height;
  }

  public final void extendBounds(@NotNull final Rectangle bounds) {
    final double newleft = Math.min(left, bounds.left);
    final double newtop = Math.min(top, bounds.left);
    width = Math.max(right(),  bounds.right())-newleft;
    height = Math.max(bottom(),  bounds.bottom())-newtop;
    left = newleft;
    top = newtop;
  }

  public final boolean contains(final double x, final double y) {
    return (x>=left) &&
           (y>=top) &&
           (x<=left+width) &&
           (y<=top+height);
  }

}
