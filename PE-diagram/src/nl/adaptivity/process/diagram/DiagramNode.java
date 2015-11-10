package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Positioned;


public class DiagramNode<T extends Positioned> implements Positioned {

  private T aTarget;

  private double aX;

  private double aY;

  private final double aLeftExtend;

  private final double aRightExtend;

  private final double aTopExtend;

  private final double aBottomExtend;

  private List<DiagramNode<T>> aLeft;

  private List<DiagramNode<T>> aRight;

  public DiagramNode(T target, double leftExtend, double rightExtend, double topExtend, double bottomExtend) {
    aTarget = target;
    aLeft = new ArrayList<>();
    aRight = new ArrayList<>();
    aX = target.getX();
    aY = target.getY();
    aLeftExtend = leftExtend;
    aRightExtend = rightExtend;
    aTopExtend = topExtend;
    aBottomExtend = bottomExtend;
  }

  private DiagramNode(DiagramNode<T> diagramNode, double x, double y) {
    aTarget = diagramNode.aTarget;
    aX = x;
    aY = y;
    aLeftExtend = diagramNode.aLeftExtend;
    aRightExtend = diagramNode.aRightExtend;
    aTopExtend = diagramNode.aTopExtend;
    aBottomExtend = diagramNode.aBottomExtend;
  }

  public T getTarget() {
    return aTarget;
  }

  /** Get the size to the left of the gravity point. */
  public double getLeftExtend() {
    return aLeftExtend;
  }

  /** Get the size to the right of the gravity point. */
  public double getRightExtend() {
    return aRightExtend;
  }

  /** Get the size to the top of the gravity point. */
  public double getTopExtend() {
    return aTopExtend;
  }

  /** Get the size to the bottom of the gravity point. */
  public double getBottomExtend() {
    return aBottomExtend;
  }

  public DiagramNode<T> withX(double x) {
    return new DiagramNode<>(this, x, aY);
  }

  public DiagramNode<T> withY(double y) {
    return new DiagramNode<>(this, aX, y);
  }

  public void setX(double x) {
    aX = x;
  }

  public void setY(double y) {
    aY = y;
  }

  @Override
  public double getX() {
    return aX;
  }

  @Override
  public double getY() {
    return aY;
  }

  public double getLeft() {
    return aX - aLeftExtend;
  }

  public double getRight() {
    return aX + aRightExtend;
  }

  public double getTop() {
    return aY - aTopExtend;
  }

  public double getBottom() {
    return aY + aBottomExtend;
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

  public List<DiagramNode<T>> getLeftNodes() {
    return aLeft;
  }

  public List<DiagramNode<T>> getRightNodes() {
    return aRight;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    if (aTarget!=null) {
      builder.append(aTarget.toString()).append(' ');
    }
    if (! Double.isNaN(aX)) {
      builder.append("x=");
      builder.append(aX);
      builder.append(", ");
    }
    if (! Double.isNaN(aY)) {
      builder.append("y=");
      builder.append(aY);
      builder.append(" - ");
    } else {
      if (!Double.isNaN(aX)) {
        builder.append(" - ");
      }
    }
    builder.append("((");
    builder.append((Double.isNaN(aX) ? 0 : aX) - aLeftExtend);
    builder.append(", ");
    builder.append((Double.isNaN(aY) ? 0 : aY) - aTopExtend);
    builder.append("),(");
    builder.append((Double.isNaN(aX) ? 0 : aX) + aRightExtend);
    builder.append(", ");
    builder.append((Double.isNaN(aY) ? 0 : aY) + aBottomExtend);
    builder.append("))");
    return builder.toString();
  }
}
