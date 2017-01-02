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

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Positioned;

import java.util.ArrayList;
import java.util.List;


public class DiagramNode<T extends Positioned> implements Positioned {

  private T mTarget;

  private double mX;

  private double mY;

  private final double mLeftExtend;

  private final double mRightExtend;

  private final double mTopExtend;

  private final double mBottomExtend;

  private List<DiagramNode<T>> mLeft;

  private List<DiagramNode<T>> mRight;

  public DiagramNode(T target, double leftExtend, double rightExtend, double topExtend, double bottomExtend) {
    mTarget = target;
    mLeft = new ArrayList<>();
    mRight = new ArrayList<>();
    mX = target.getX();
    mY = target.getY();
    mLeftExtend = leftExtend;
    mRightExtend = rightExtend;
    mTopExtend = topExtend;
    mBottomExtend = bottomExtend;
  }

  private DiagramNode(DiagramNode<T> diagramNode, double x, double y) {
    mTarget = diagramNode.mTarget;
    mX = x;
    mY = y;
    mLeftExtend = diagramNode.mLeftExtend;
    mRightExtend = diagramNode.mRightExtend;
    mTopExtend = diagramNode.mTopExtend;
    mBottomExtend = diagramNode.mBottomExtend;
  }

  public T getTarget() {
    return mTarget;
  }

  /** Get the size to the left of the gravity point. */
  public double getLeftExtend() {
    return mLeftExtend;
  }

  /** Get the size to the right of the gravity point. */
  public double getRightExtend() {
    return mRightExtend;
  }

  /** Get the size to the top of the gravity point. */
  public double getTopExtend() {
    return mTopExtend;
  }

  /** Get the size to the bottom of the gravity point. */
  public double getBottomExtend() {
    return mBottomExtend;
  }

  public DiagramNode<T> withX(double x) {
    return new DiagramNode<>(this, x, mY);
  }

  public DiagramNode<T> withY(double y) {
    return new DiagramNode<>(this, mX, y);
  }

  public void setX(double x) {
    mX = x;
  }

  public void setY(double y) {
    mY = y;
  }

  @Override
  public double getX() {
    return mX;
  }

  @Override
  public double getY() {
    return mY;
  }

  public double getLeft() {
    return mX - mLeftExtend;
  }

  public double getRight() {
    return mX + mRightExtend;
  }

  public double getTop() {
    return mY - mTopExtend;
  }

  public double getBottom() {
    return mY + mBottomExtend;
  }

  /** Determine whether the region overlaps this node and is not positioned to its right. */
  public boolean leftOverlaps(DiagramNode<?> region, double xSep, double ySep) {
    return overlaps(region, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
           region.getX()<getX();
  }

  public boolean rightOverlaps(DiagramNode<?> region, double xSep, double ySep) {
    return overlaps(region, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        region.getX()>getX();
  }

  public boolean upOverlaps(DiagramNode<?> region, double xSep, double ySep) {
    return overlaps(region, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        region.getY()<getY();
  }

  public boolean downOverlaps(DiagramNode<?> region, double xSep, double ySep) {
    return overlaps(region, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        region.getY()>getY();
  }

  private static boolean overlaps(DiagramNode<?> region, double left, double top, double right, double bottom) {
    return (region.getRight()>left) &&
           (region.getLeft()<right) &&
           (region.getTop()<bottom) &&
           (region.getBottom()>top);
  }

  @Override
  public boolean hasPos() {
    return !(Double.isInfinite(mX) || Double.isInfinite(mY));
  }

  public List<DiagramNode<T>> getLeftNodes() {
    return mLeft;
  }

  public List<DiagramNode<T>> getRightNodes() {
    return mRight;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    if (mTarget!=null) {
      builder.append(mTarget.toString()).append(' ');
    }
    if (! Double.isNaN(mX)) {
      builder.append("x=");
      builder.append(mX);
      builder.append(", ");
    }
    if (! Double.isNaN(mY)) {
      builder.append("y=");
      builder.append(mY);
      builder.append(" - ");
    } else {
      if (!Double.isNaN(mX)) {
        builder.append(" - ");
      }
    }
    builder.append("((");
    builder.append((Double.isNaN(mX) ? 0 : mX) - mLeftExtend);
    builder.append(", ");
    builder.append((Double.isNaN(mY) ? 0 : mY) - mTopExtend);
    builder.append("),(");
    builder.append((Double.isNaN(mX) ? 0 : mX) + mRightExtend);
    builder.append(", ");
    builder.append((Double.isNaN(mY) ? 0 : mY) + mBottomExtend);
    builder.append("))");
    return builder.toString();
  }
}
