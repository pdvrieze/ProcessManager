package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Point;


public class LayoutAlgorithm {

  private double aVertSeparation = 30d;

  private double aHorizSeparation = 30d;

  private double aDefaultNodeWidth = 30d;
  private double aDefaultNodeHeight = 30d;

  public double getVertSeparation() {
    return aVertSeparation;
  }


  public void setVertSeparation(double pVertSeparation) {
    aVertSeparation = pVertSeparation;
  }


  public double getHorizSeparation() {
    return aHorizSeparation;
  }


  public void setHorizSeparation(double pHorizSeparation) {
    aHorizSeparation = pHorizSeparation;
  }

  public double getDefaultNodeWidth() {
    return aDefaultNodeWidth;
  }


  public void setDefaultNodeWidth(double pDefaultNodeWidth) {
    aDefaultNodeWidth = pDefaultNodeWidth;
  }


  public double getDefaultNodeHeight() {
    return aDefaultNodeHeight;
  }


  public void setDefaultNodeHeight(double pDefaultNodeHeight) {
    aDefaultNodeHeight = pDefaultNodeHeight;
  }

  public boolean layout(List<DiagramNode> aNodes) {
    boolean changed = false;
    for (final DiagramNode node : aNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = layoutNode(aNodes, node, true, true); // always force as that should be slightly more efficient
      }
    }
    return changed;
//    if (changed) {
//      double minX = Double.MAX_VALUE;
//      double minY = Double.MAX_VALUE;
//      double maxX = Double.MIN_VALUE;
//      double maxY = Double.MIN_VALUE;
//      for (final DiagramNode node : aNodes) {
//        minX = Math.min(node.getX()-leftDistance(node), minX);
//        minY = Math.min(node.getY()-topDistance(node), minY);
//        maxX = Math.max(node.getX()+rightDistance(node), maxX);
//        maxY = Math.max(node.getY()+bottomDistance(node), maxY);
//      }
//      final double offsetX = aLeftPadding - minX;
//      final double offsetY = aTopPadding - minY;
//
//      aInnerWidth = maxX - minX;
//      aInnerHeight = maxY - minY;
//
//      if (Math.abs(offsetX)>TOLERANCE || Math.abs(offsetY)>TOLERANCE) {
//        for (final DiagramNode node : aNodes) {
//          node.setX(node.getX()+offsetX);
//          node.setY(node.getY()+offsetY);
//        }
//      }
//    }
  }


  private boolean layoutNode(List<DiagramNode> pNodes, DiagramNode pNode, boolean forceX, boolean forceY) {
    boolean changed = false;
    List<Point> leftPoints = getLeftPoints(pNode);
    List<Point> abovePoints = getAbovePoints(pNode);
    List<Point> rightPoints = getRightPoints(pNode);
    List<Point> belowPoints = getBelowPoints(pNode);

    double minY = maxY(abovePoints)+aVertSeparation + topDistance(pNode);
    double maxY = minY(belowPoints)-aVertSeparation - bottomDistance(pNode);
    double minX = maxX(leftPoints)+aHorizSeparation + leftDistance(pNode);
    double maxX = minX(rightPoints)-aHorizSeparation - rightDistance(pNode);

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX+pNode.getLeftExtend()+pNode.getRightExtend() - maxX;
      if (missingSpace>0) {
        moveToRight(pNodes, pNode.withX(minX));
        changed = true;
      }
    }

    {
      double missingSpace = minY+pNode.getTopExtend()+pNode.getBottomExtend() - maxY;
      if (missingSpace>0) {
        moveDown(pNodes, pNode.withY(minY));
        changed = true;
      }
    }

    double x = pNode.getX();
    double y = pNode.getY();

    if (leftPoints.isEmpty()) {
      if (rightPoints.isEmpty()) {
        if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
          x = averageX(abovePoints, belowPoints, 0d);
        }
        if (forceY || Double.isNaN(y) || y<minY || y>maxY) {
          if (abovePoints.isEmpty()) {
            if (! belowPoints.isEmpty()) {
              y = maxY;
            } // otherwise keep it where it is
          } else { // abovePoints not empty
            if (belowPoints.isEmpty()) {
              y = minY;
            } else {
              y = Math.max(minY, (minY+maxY)/2);
            }
          }
        }

      } else { // leftPoints empty, rightPoints not empty
        if (forceY || Double.isNaN(y)|| y<minY || y>maxY) {
          y = Math.max(minY, averageY(rightPoints));
        }
        if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
          x = Math.min(averageX(abovePoints, belowPoints,Double.POSITIVE_INFINITY),maxX);
        }
      }
    } else { // leftPoints not empty
      if (forceY || Double.isNaN(y) || y<minY || y>maxY) {
        if (leftPoints.size()==1) {
          y = Math.max(minY, averageY(leftPoints));
        } else {
          if (rightPoints.size()==1) {
            y = Math.max(minY, averageY(rightPoints));
          } else {
            y = Math.max(minY, averageY(leftPoints, rightPoints, 0));
          }
        }
      }
      if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
        if (rightPoints.isEmpty()) {
          x = Math.max(averageX(abovePoints, belowPoints,Double.NEGATIVE_INFINITY),minX);
        } else {
          x = Math.max(minX, (minX+maxX)/2);
        }
      }
    }
    if (Double.isNaN(x)) { x = 0d; }
    if (Double.isNaN(y)) { y = 0d; }
    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);
    if (yChanged || xChanged) {
      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
      for(DiagramNode n:getPrecedingSiblings(pNode)) {
        layoutNode(n, xChanged, yChanged && y<minY);
      }
      for(DiagramNode n:getFollowingSiblings(pNode)) {
        layoutNode(n, xChanged, yChanged && y>maxY);
      }
      for(DiagramNode n:pNode.getPredecessors()) {
        layoutNode(n, xChanged && x<minX, yChanged);
      }
      for(DiagramNode n:pNode.getSuccessors()) {
        layoutNode(n, xChanged && x>maxX, yChanged);
      }
    }
  }

  private void moveToRight(List<DiagramNode> pNodes, DiagramNode pFreeRegion) {
    for(DiagramNode n: pNodes) {
      if (n.rightOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        n.setX(pFreeRegion.getRight()+aHorizSeparation+n.getLeftExtend());
        moveToRight(pNodes, n);
        moveDown(pNodes, n);
      }
    }
  }

  private void moveDown(List<DiagramNode> pNodes, DiagramNode pFreeRegion) {
    for(DiagramNode n: pNodes) {
      if (n.downOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        n.setY(pFreeRegion.getBottom()+aVertSeparation+n.getTopExtend());
        moveDown(pNodes, n);
        moveToRight(pNodes, n);
      }
    }
  }


  private static boolean changed(double pA, double pB, double pTolerance) {
    if (Double.isNaN(pA)) { return !Double.isNaN(pB); }
    if (Double.isNaN(pB)) { return true; }
    return Math.abs(pA-pB)>pTolerance;
  }

  private double topDistance(DiagramNode pNode) {
    if ((!Double.isNaN(pNode.getY())) && pNode instanceof Bounded) {
      return pNode.getY()-((Bounded) pNode).getBounds().top;
    }
    return aDefaultNodeHeight/2;
  }

  private double bottomDistance(DiagramNode pNode) {
    if ((!Double.isNaN(pNode.getY()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().bottom()-pNode.getY();
    }
    return aDefaultNodeHeight/2;
  }

  private double leftDistance(DiagramNode pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return pNode.getX()-((Bounded) pNode).getBounds().left;
    }
    return aDefaultNodeWidth/2;
  }

  private double rightDistance(DiagramNode pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().right()-pNode.getX();
    }
    return aDefaultNodeWidth/2;
  }

  private static double averageY(List<Point> pPoints) {
    if (pPoints.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(Point p: pPoints) { total+=p.y; }
      return total/pPoints.size();
    }
  }

  private static double averageY(List<Point> pPoints1, List<Point> pPoints2, double fallback) {
    if (pPoints1.isEmpty() && pPoints2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(Point p: pPoints1) { total+=p.y; }
      for(Point p: pPoints2) { total+=p.y; }
      return total/(pPoints1.size()+pPoints2.size());
    }
  }

  private static double averageX(List<Point> pPoints) {
    if (pPoints.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(Point p: pPoints) { total+=p.x; }
      return total/pPoints.size();
    }
  }

  private static double averageX(List<Point> pPoints1, List<Point> pPoints2, double fallback) {
    if (pPoints1.isEmpty() && pPoints2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(Point p: pPoints1) { total+=p.x; }
      for(Point p: pPoints2) { total+=p.x; }
      return total/(pPoints1.size()+pPoints2.size());
    }
  }

  private static double maxY(List<Point> pPoints) {
    double result = Double.NEGATIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.y>result) {
        result = p.y;
      }
    }
    return result;
  }

  private static double minY(List<Point> pPoints) {
    double result = Double.POSITIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.y<result) {
        result = p.y;
      }
    }
    return result;
  }

  private static double maxX(List<Point> pPoints) {
    double result = Double.NEGATIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.x>result) {
        result = p.x;
      }
    }
    return result;
  }

  private static double minX(List<Point> pPoints) {
    double result = Double.POSITIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.x<result) {
        result = p.x;
      }
    }
    return result;
  }

  private List<Point> getLeftPoints(DiagramNode pNode) {
    List<Point> result = new ArrayList<Point>();
    for(DiagramNode n:pNode.getPredecessors()) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x;
        double y = n.getY();
        if (n instanceof Bounded) {
          x = ((Bounded) n).getBounds().right();
        } else {
          x = n.getX()+(aDefaultNodeWidth/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getRightPoints(DiagramNode pNode) {
    List<Point> result = new ArrayList<Point>();
    for(DiagramNode n:pNode.getSuccessors()) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x;
        double y = n.getY();
        if (n instanceof Bounded) {
          x = ((Bounded) n).getBounds().left;
        } else {
          x = n.getX()-(aDefaultNodeWidth/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getAbovePoints(DiagramNode pNode) {
    List<Point> result = new ArrayList<Point>();
    for(DiagramNode n:getPrecedingSiblings(pNode)) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x = n.getX();
        double y;
        if (n instanceof Bounded) {
          y = ((Bounded) n).getBounds().bottom();
        } else {
          y = n.getY()+(aDefaultNodeHeight/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getBelowPoints(DiagramNode pNode) {
    List<Point> result = new ArrayList<Point>();
    for(DiagramNode n:getFollowingSiblings(pNode)) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x = n.getX();
        double y;
        if (n instanceof Bounded) {
          y = ((Bounded) n).getBounds().top;
        } else {
          y = n.getY()-(aDefaultNodeHeight/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<DiagramNode> getPrecedingSiblings(DiagramNode pNode) {
    List<DiagramNode> result = new ArrayList<DiagramNode>();
    for(DiagramNode pred:pNode.getPredecessors()) {
      if (pred.getSuccessors().contains(pNode)) {
        for(DiagramNode sibling: pred.getSuccessors()) {
          if (sibling==pNode) {
            break;
          } else {
            result.add(sibling);
          }
        }
      }
    }
    for(DiagramNode pred:pNode.getSuccessors()) {
      if (pred.getPredecessors().contains(pNode)) {
        for(DiagramNode sibling: pred.getPredecessors()) {
          if (sibling==pNode) {
            break;
          } else {
            result.add(sibling);
          }
        }
      }
    }
    return result;
  }

  private List<DiagramNode> getFollowingSiblings(DiagramNode pNode) {
    List<DiagramNode> result = new ArrayList<DiagramNode>();
    for(DiagramNode successor:pNode.getPredecessors()) {
      if (successor.getSuccessors().contains(pNode)) {
        boolean following = false;
        for(DiagramNode sibling: successor.getSuccessors()) {
          if (sibling==pNode) {
            following = true;
          } else if (following){
            result.add(sibling);
          }
        }
      }
    }
    for(DiagramNode successor:pNode.getSuccessors()) {
      if (successor.getPredecessors().contains(pNode)) {
        boolean following = false;
        for(DiagramNode sibling: successor.getPredecessors()) {
          if (sibling==pNode) {
            following = true;
          } else if (following){
            result.add(sibling);
          }
        }
      }
    }
    return result;
  }

}
