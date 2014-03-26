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

  public DiagramNode(T pTarget, double pLeftExtend, double pRightExtend, double pTopExtend, double pBottomExtend) {
    aTarget = pTarget;
    aLeft = new ArrayList<>();
    aRight = new ArrayList<>();
    aX = pTarget.getX();
    aY = pTarget.getY();
    aLeftExtend = pLeftExtend;
    aRightExtend = pRightExtend;
    aTopExtend = pTopExtend;
    aBottomExtend = pBottomExtend;
  }

  private DiagramNode(DiagramNode<T> pDiagramNode, double pX, double pY) {
    aTarget = pDiagramNode.aTarget;
    aX = pX;
    aY = pY;
    aLeftExtend = pDiagramNode.aLeftExtend;
    aRightExtend = pDiagramNode.aRightExtend;
    aTopExtend = pDiagramNode.aTopExtend;
    aBottomExtend = pDiagramNode.aBottomExtend;
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

  public DiagramNode<T> withX(double pX) {
    return new DiagramNode<>(this, pX, aY);
  }

  public DiagramNode<T> withY(double pY) {
    return new DiagramNode<>(this, aX, pY);
  }

  public void setX(double pX) {
    aX = pX;
  }

  public void setY(double pY) {
    aY = pY;
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
  public boolean leftOverlaps(DiagramNode<?> pRegion, double xSep, double ySep) {
    return overlaps(pRegion, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
           pRegion.getX()<getX();
  }

  public boolean rightOverlaps(DiagramNode<?> pRegion, double xSep, double ySep) {
    return overlaps(pRegion, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        pRegion.getX()>getX();
  }

  public boolean upOverlaps(DiagramNode<?> pRegion, double xSep, double ySep) {
    return overlaps(pRegion, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        pRegion.getY()<getY();
  }

  public boolean downOverlaps(DiagramNode<?> pRegion, double xSep, double ySep) {
    return overlaps(pRegion, getLeft()-xSep, getTop()-ySep, getRight()+xSep, getBottom()+ySep) &&
        pRegion.getY()>getY();
  }

  private static boolean overlaps(DiagramNode<?> pRegion, double left, double top, double right, double bottom) {
    return (pRegion.getRight()>left) &&
           (pRegion.getLeft()<right) &&
           (pRegion.getTop()<bottom) &&
           (pRegion.getBottom()>top);
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
