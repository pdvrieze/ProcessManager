package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.diagram.Positioned;


public class LayoutAlgorithm {

  private static final double TOLERANCE = 0.1d;

  private static final int PASSCOUNT = 1;

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

  public <T extends Positioned> boolean layout(List<? extends DiagramNode<T>> pNodes) {
    boolean changed = false;
    for (final DiagramNode<T> node : pNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = layoutNodeInitial(pNodes, node); // always force as that should be slightly more efficient
      }
    }
    {
      boolean nodesChanged = true;
      for (int pass=0; nodesChanged && pass<PASSCOUNT; ++pass) {
        nodesChanged = false;
        for(DiagramNode<T> node: pNodes) {
          if (node.getLeftNodes().isEmpty()) {
            nodesChanged|=layoutNodeRight(pNodes, node, pass);
          }
        }
        for(DiagramNode<T> node: pNodes) {
          if (node.getRightNodes().isEmpty()) {
            nodesChanged|=layoutNodeLeft(pNodes, node, pass);
          }
        }
      }
      changed|=nodesChanged;
    }
    if (changed) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      for (final DiagramNode<T> node : pNodes) {
        minX = Math.min(node.getLeft(), minX);
        minY = Math.min(node.getTop(), minY);
      }
      final double offsetX = 0 - minX;
      final double offsetY = 0 - minY;

      if (Math.abs(offsetX)>TOLERANCE || Math.abs(offsetY)>TOLERANCE) {
        for (final DiagramNode<T> node : pNodes) {
          node.setX(node.getX()+offsetX);
          node.setY(node.getY()+offsetY);
        }
      }
    }
    return changed;
  }


  private <T extends Positioned> boolean layoutNodeInitial(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    boolean changed = false;

    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> aboveNodes = getPrecedingSiblings(pNode);

//    List<Point> leftPoints = getLeftPoints(pNode);
//    List<Point> abovePoints = getAbovePoints(pNode);
//    List<Point> rightPoints = getRightPoints(pNode);
//    List<Point> belowPoints = getBelowPoints(pNode);
//
    double minY = bottom(lowest(aboveNodes), Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
//    double maxY = minY(belowPoints)-aVertSeparation - bottomDistance(pNode);
    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
//    double maxX = minX(rightPoints)-aHorizSeparation - rightDistance(pNode);
//
//    { // ensure that there is space for the node. If not, move all right nodes to the right
//      double missingSpace = minX+pNode.getLeftExtend()+pNode.getRightExtend() - maxX;
//      if (missingSpace>0) {
//        moveToRight(pNodes, pNode.withX(minX));
//        changed = true;
//      }
//    }
//
//    {
//      double missingSpace = minY+pNode.getTopExtend()+pNode.getBottomExtend() - maxY;
//      if (missingSpace>0) {
//        moveDown(pNodes, pNode.withY(minY));
//        changed = true;
//      }
//    }

    double x = pNode.getX();
    double y = pNode.getY();

    if (leftNodes.isEmpty()) {
      x = aboveNodes.isEmpty() ? pNode.getLeftExtend() : averageX(aboveNodes);
      y = aboveNodes.isEmpty() ? pNode.getTopExtend() :minY;
    } else { // leftPoints not empty, minX must be set
      x = minX;
      y = Math.max(minY, averageY(leftNodes));
    }
//    if (Double.isNaN(x)) { x = 0d; }
//    if (Double.isNaN(y)) { y = 0d; }
    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);
    if (yChanged || xChanged) {
      System.err.println("Moving node "+pNode.getTarget()+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
    }
    return changed;
  }

  private <T extends Positioned> boolean layoutNodeRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, int pass) {
    boolean changed = false;
    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> rightNodes = pNode.getRightNodes();
    List<? extends DiagramNode<T>> aboveSiblings = getPrecedingSiblings(pNode);
    List<? extends DiagramNode<T>> belowSiblings = getFollowingSiblings(pNode);

    double minY = bottom(lowest(nodesAbove(pNodes, pNode)),Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
    double maxY = top(highest(belowSiblings), Double.POSITIVE_INFINITY)-aVertSeparation - pNode.getBottomExtend();
    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
    double maxX = left(leftMost(rightNodes),Double.POSITIVE_INFINITY)-aHorizSeparation - pNode.getRightExtend();

    double x = pNode.getX();
    double y = pNode.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesRight(pNodes, pNode), missingSpace);
        changed = true;
      }
    }

    {
      double missingSpace = minY - maxY;
      if (missingSpace>TOLERANCE) {
        y = minY;
        moveY(nodesBelow(pNodes, pNode), missingSpace);
        changed = true;
      }
    }

    // If we have nodes left and right position this one in the middle
    if (! (leftNodes.isEmpty()||rightNodes.isEmpty())) {
      x = (rightMost(leftNodes).getX()+leftMost(rightNodes).getX())/2;
    }
    if (!(aboveSiblings.isEmpty()|| belowSiblings.isEmpty())) {
      y = (lowest(aboveSiblings).getY()+ highest(belowSiblings).getY())/2;
    } else if (leftNodes.size()>1) {
      y = (highest(leftNodes).getY() + lowest(leftNodes).getY())/2;
    } else if (leftNodes.size()==1) {
      y = leftNodes.get(0).getY();
    }

    x = Math.max(Math.min(maxX, x), minX);
    y = Math.max(Math.min(maxY, y), minY);

    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);

    if (rightNodes.size()>1 && (pass==0 ||yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      double rightCenterY = (highest(rightNodes).getY()+lowest(rightNodes).getY())/2;
      if ((y-rightCenterY)>TOLERANCE) {
        // if the center of the right nodes is above this one, move the right nodes down.
        // the reverse should be handled in the left pass
        moveY(rightNodes, rightCenterY-y);
      }
    }

    if (yChanged || xChanged) {
      changed=true;
      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
    }
    for(DiagramNode<T> node:rightNodes) {
      changed |= layoutNodeRight(pNodes, node, pass);
    }
    return changed;
  }

  private <T extends Positioned> boolean layoutNodeLeft(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, int pass) {
    boolean changed = false;
    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> rightNodes = pNode.getRightNodes();
    List<? extends DiagramNode<T>> aboveSiblings = getPrecedingSiblings(pNode);
    List<? extends DiagramNode<T>> belowSiblings = getFollowingSiblings(pNode);

    double minY = bottom(lowest(aboveSiblings),Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
    double maxY = top(highest(nodesBelow(pNodes, pNode)), Double.POSITIVE_INFINITY)-aVertSeparation - pNode.getBottomExtend();
    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
    double maxX = left(leftMost(rightNodes),Double.POSITIVE_INFINITY)-aHorizSeparation - pNode.getRightExtend();

    double x = pNode.getX();
    double y = pNode.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesLeft(pNodes, pNode), -missingSpace);
        changed = true;
      }
    }

    {
      double missingSpace = minY - maxY;
      if (missingSpace>TOLERANCE) {
        y = minY;
        moveY(nodesAbove(pNodes, pNode), -missingSpace);
        changed = true;
      }
    }

    // If we have nodes left and right position this one in the middle
    if (! (leftNodes.isEmpty()||rightNodes.isEmpty())) {
      x = (rightMost(leftNodes).getX()+leftMost(rightNodes).getX())/2;
    }

    if (!(aboveSiblings.isEmpty()|| belowSiblings.isEmpty())) {
      y = (lowest(aboveSiblings).getY()+ highest(belowSiblings).getY())/2;
    } else if (rightNodes.size()>1) {
      y = (highest(rightNodes).getY() + lowest(rightNodes).getY())/2;
    } else if (rightNodes.size()==1) {
      y = rightNodes.get(0).getY();
    }

    x = Math.max(Math.min(maxX, x), minX);
    y = Math.max(Math.min(maxY, y), minY);

    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);

    if (leftNodes.size()>1 && (pass==0 ||yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      double leftCenterY = (highest(leftNodes).getY()+lowest(leftNodes).getY())/2;
      // if the center of the left nodes is below this one, move the left nodes up.
      // the reverse should be handled in the right pass
      if ((leftCenterY-y)>TOLERANCE) {
        moveY(leftNodes, y-leftCenterY);
      }
    }

    if (yChanged || xChanged) {
      changed=true;
      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
    }
    for(DiagramNode<T> node:leftNodes) {
      changed |= layoutNodeLeft(pNodes, node, pass);
    }
    return changed;
  }

  private static double top(DiagramNode<?> pNode, double pFallback) {
    return pNode==null ? pFallback : pNode.getTop();
  }

  private static double bottom(DiagramNode<?> pNode, double pFallback) {
    return pNode==null ? pFallback : pNode.getBottom();
  }

  private static double left(DiagramNode<?> pNode, double pFallback) {
    return pNode==null ? pFallback : pNode.getLeft();
  }

  private static double right(DiagramNode<?> pNode, double pFallback) {
    return pNode==null ? pFallback : pNode.getRight();
  }

  private static <T extends Positioned> DiagramNode<T> highest(List<? extends DiagramNode<T>> pNodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:pNodes) {
      if (result==null || node.getTop()<result.getTop()) {
        result = node;
      }
    }
    return result;
  }


  private static <T extends Positioned> DiagramNode<T> lowest(List<? extends DiagramNode<T>> pNodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:pNodes) {
      if (result==null || node.getBottom()>result.getBottom()) {
        result = node;
      }
    }
    return result;
  }

  private static <T extends Positioned> DiagramNode<T> leftMost(List<? extends DiagramNode<T>> pNodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:pNodes) {
      if (result==null || node.getLeft()<result.getLeft()) {
        result = node;
      }
    }
    return result;
  }


  private static <T extends Positioned> DiagramNode<T> rightMost(List<? extends DiagramNode<T>> pNodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:pNodes) {
      if (result==null || node.getRight()>result.getRight()) {
        result = node;
      }
    }
    return result;
  }

  private <T extends Positioned> List<DiagramNode<T>> nodesAbove(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> n: pNodes) {
      if (n.upOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private <T extends Positioned> List<DiagramNode<T>> nodesBelow(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> n: pNodes) {
      if (n.downOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private <T extends Positioned> List<DiagramNode<T>> nodesLeft(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> n: pNodes) {
      if (n.leftOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private <T extends Positioned> List<DiagramNode<T>> nodesRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> n: pNodes) {
      if (n.rightOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private <T extends Positioned> void moveToRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    for(DiagramNode<T> n: pNodes) {
      if (n.rightOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        n.setX(pFreeRegion.getRight()+aHorizSeparation+n.getLeftExtend());
        moveToRight(pNodes, n);
        moveDown(pNodes, n);
      }
    }
  }

  private <T extends Positioned> void moveDown(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    for(DiagramNode<T> n: pNodes) {
      if (n.downOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        n.setY(pFreeRegion.getBottom()+aVertSeparation+n.getTopExtend());
        moveDown(pNodes, n);
        moveToRight(pNodes, n);
      }
    }
  }

  private static <T extends Positioned> void moveX(List<? extends DiagramNode<T>> pNodes, double distance) {
    for(DiagramNode<T> n: pNodes) {
      n.setX(n.getX()+distance);
      System.err.println("Moving node "+n+ "to ("+n.getX()+"!, "+n.getY()+')');
    }
  }

  private static <T extends Positioned> void moveY(List<? extends DiagramNode<T>> pNodes, double distance) {
    for(DiagramNode<T> n: pNodes) {
      n.setY(n.getY()+distance);
      System.err.println("Moving node "+n+ "to ("+n.getX()+", "+n.getY()+"!)");
    }
  }


  private static boolean changed(double pA, double pB, double pTolerance) {
    if (Double.isNaN(pA)) { return !Double.isNaN(pB); }
    if (Double.isNaN(pB)) { return true; }
    return Math.abs(pA-pB)>pTolerance;
  }

  private static <T extends Positioned> double averageY(List<? extends DiagramNode<T>> pNodes) {
    if (pNodes.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(DiagramNode<T> p: pNodes) { total+=p.getY(); }
      return total/pNodes.size();
    }
  }

  private static <T extends Positioned> double averageY(List<? extends DiagramNode<T>> pNodes1, List<? extends DiagramNode<T>> pNodes2, double fallback) {
    if (pNodes1.isEmpty() && pNodes2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(DiagramNode<T> p: pNodes1) { total+=p.getY(); }
      for(DiagramNode<T> p: pNodes2) { total+=p.getY(); }
      return total/(pNodes1.size()+pNodes2.size());
    }
  }


  private static <T extends Positioned> double averageX(List<? extends DiagramNode<T>> pNodes) {
    if (pNodes.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(DiagramNode<T> p: pNodes) { total+=p.getX(); }
      return total/pNodes.size();
    }
  }

  private static <T extends Positioned> double averageX(List<? extends DiagramNode<T>> pNodes1, List<? extends DiagramNode<T>> pNodes2, double fallback) {
    if (pNodes1.isEmpty() && pNodes2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(DiagramNode<T> p: pNodes1) { total+=p.getX(); }
      for(DiagramNode<T> p: pNodes2) { total+=p.getY(); }
      return total/(pNodes1.size()+pNodes2.size());
    }
  }

  // TODO Change to all nodes in the graph that are not smaller or bigger
  private static <T extends Positioned> List<? extends DiagramNode<T>> getPrecedingSiblings(DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> pred:pNode.getLeftNodes()) {
      if (pred.getRightNodes().contains(pNode)) {
        for(DiagramNode<T> sibling: pred.getRightNodes()) {
          if (sibling==pNode) {
            break;
          } else {
            result.add(sibling);
          }
        }
      }
    }
    for(DiagramNode<T> pred:pNode.getRightNodes()) {
      if (pred.getLeftNodes().contains(pNode)) {
        for(DiagramNode<T> sibling: pred.getLeftNodes()) {
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

  private static <T extends Positioned> List<? extends DiagramNode<T>> getFollowingSiblings(DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<DiagramNode<T>>();
    for(DiagramNode<T> successor:pNode.getLeftNodes()) {
      if (successor.getRightNodes().contains(pNode)) {
        boolean following = false;
        for(DiagramNode<T> sibling: successor.getRightNodes()) {
          if (sibling==pNode) {
            following = true;
          } else if (following){
            result.add(sibling);
          }
        }
      }
    }
    for(DiagramNode<T> successor:pNode.getRightNodes()) {
      if (successor.getLeftNodes().contains(pNode)) {
        boolean following = false;
        for(DiagramNode<T> sibling: successor.getLeftNodes()) {
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
