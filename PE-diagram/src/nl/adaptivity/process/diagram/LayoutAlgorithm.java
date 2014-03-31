package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.diagram.Positioned;


public class LayoutAlgorithm<T extends Positioned> {

  private static final double TOLERANCE = 0.1d;

  private static final int PASSCOUNT = 9;

  private double aVertSeparation = 30d;

  private double aHorizSeparation = 30d;

  private double aDefaultNodeWidth = 30d;
  private double aDefaultNodeHeight = 30d;

  private LayoutStepper<T> aLayoutStepper = new LayoutStepper<>();

  public LayoutStepper<T> getLayoutStepper() {
    return aLayoutStepper;
  }

  public void setLayoutStepper(LayoutStepper<T> pLayoutStepper) {
    aLayoutStepper = pLayoutStepper !=null ? pLayoutStepper : new LayoutStepper<T>();
  }


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

  public boolean layout(List<? extends DiagramNode<T>> pNodes) {
    boolean changed = false;
    for (final DiagramNode<T> node : pNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = layoutNodeInitial(pNodes, node); // always force as that should be slightly more efficient
      }
    }
    {
      ArrayList<DiagramNode<T>> nodes = new ArrayList<>(pNodes);
      boolean nodesChanged = true;
      for (int pass=0; nodesChanged && pass<PASSCOUNT; ++pass) {
        aLayoutStepper.reportPass(pass);
        nodesChanged = false;
        if (pass%2==0) {
//          Collections.sort(nodes, LEFT_TO_RIGHT);
          for(DiagramNode<T> node: nodes) {
            if (node.getLeftNodes().isEmpty()) {
              nodesChanged|=layoutNodeRight(pNodes, node, pass);
            }
          }
        } else {
//          Collections.sort(nodes, RIGHT_TO_LEFT);
          for(DiagramNode<T> node: nodes) {
            if (node.getRightNodes().isEmpty()) {
              nodesChanged|=layoutNodeLeft(pNodes, node, pass);
            }
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


  /**
   * @param pNodes The nodes in the diagram that could be layed out.
   * @param pNode The node to focus on.
   */
  private boolean layoutNodeInitial(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    boolean changed = false;

    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> aboveNodes = getPrecedingSiblings(pNode);

    double origX = pNode.getX(); // store the initial coordinates
    double origY = pNode.getY();

    double x = origX;
    double y = origY;

    // set temporary coordinates to prevent infinite recursion
    if (Double.isNaN(origX)) {pNode.setX(0);x=0;}
    if (Double.isNaN(origY)) {pNode.setY(0);y=0;}

    // Ensure that both the leftNodes and aboveNodes have set coordinates.
    for(DiagramNode<T> node: CollectionUtil.<DiagramNode<T>>combine(leftNodes, aboveNodes)) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        layoutNodeInitial(pNodes, node);
      }
    }

    double minY = bottom(lowest(aboveNodes), Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
    if (aboveNodes.size()>1) { aLayoutStepper.reportLowest(aboveNodes, lowest(aboveNodes)); }
    if (!Double.isInfinite(minY)) { aLayoutStepper.reportMinY(aboveNodes, minY); }

    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
    if (leftNodes.size()>1) { aLayoutStepper.reportRightmost(leftNodes, rightMost(leftNodes)); }
    if (!Double.isInfinite(minX)) { aLayoutStepper.reportMinX(leftNodes, minX); }

    if (leftNodes.isEmpty()) {
      x = aboveNodes.isEmpty() ? pNode.getLeftExtend() : averageX(aboveNodes);
      y = aboveNodes.isEmpty() ? pNode.getTopExtend() :minY;
    } else { // leftPoints not empty, minX must be set
      x = minX;
      y = Math.max(minY, averageY(leftNodes));
    }
//    if (Double.isNaN(x)) { x = 0d; }
//    if (Double.isNaN(y)) { y = 0d; }
    boolean xChanged = changed(x, origX, TOLERANCE);
    boolean yChanged = changed(y, origY, TOLERANCE);
    if (yChanged || xChanged) {
      aLayoutStepper.reportMove(pNode, x, y);
//      System.err.println("Moving node "+pNode.getTarget()+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
    }
    return changed;
  }

  private boolean layoutNodeRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, int pass) {
    aLayoutStepper.reportLayoutNode(pNode);
    boolean changed = false;
    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> rightNodes = pNode.getRightNodes();
    List<? extends DiagramNode<T>> aboveSiblings = getPrecedingSiblings(pNode);
    List<? extends DiagramNode<T>> belowSiblings = getFollowingSiblings(pNode);

    final List<DiagramNode<T>> nodesAbove = nodesAbove(pNode);
    double minY = bottom(lowest(nodesAbove),Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
    if (nodesAbove.size()>1) { aLayoutStepper.reportLowest(nodesAbove, lowest(nodesAbove)); }
    if (!Double.isInfinite(minY)) { aLayoutStepper.reportMinY(nodesAbove, minY); }

    double maxY = top(highest(belowSiblings), Double.POSITIVE_INFINITY)-aVertSeparation - pNode.getBottomExtend();
    if (belowSiblings.size()>1) { aLayoutStepper.reportHighest(belowSiblings, highest(belowSiblings)); }
    if (!Double.isInfinite(minY)) { aLayoutStepper.reportMaxY(belowSiblings, maxY); }

    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
    if (leftNodes.size()>1) { aLayoutStepper.reportRightmost(leftNodes, rightMost(leftNodes)); }
    if (!Double.isInfinite(minX)) { aLayoutStepper.reportMinX(leftNodes, minX); }

    double maxX = left(leftMost(rightNodes),Double.POSITIVE_INFINITY)-aHorizSeparation - pNode.getRightExtend();
    if (leftNodes.size()>1) { aLayoutStepper.reportLeftmost(rightNodes, leftMost(rightNodes)); }
    if (!Double.isInfinite(maxX)) { aLayoutStepper.reportMaxX(rightNodes, maxX); }

    double x = pNode.getX();
    double y = pNode.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesRightPos(pNodes, pNode), missingSpace);
        changed = true;
      }
    }

    {
      double missingSpace = minY - maxY;
      if (missingSpace>TOLERANCE) {
        y = minY;
        moveY(belowSiblings, missingSpace);
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
    } else if (leftNodes.size()==1 && rightNodes.size()<2) {
      y = leftNodes.get(0).getY();
    }

    x = Math.max(Math.min(maxX, x), minX);
    y = Math.max(Math.min(maxY, y), minY);

    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);

    if (rightNodes.size()>1 && (pass<2 ||yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      double rightCenterY = (highest(rightNodes).getY()+lowest(rightNodes).getY())/2;
      if ((y-rightCenterY)>TOLERANCE) {
        // if the center of the right nodes is above this one, move the right nodes down.
        // the reverse should be handled in the left pass
        moveY(rightNodes, y-rightCenterY);
      }
    }

    if (yChanged || xChanged) {
      aLayoutStepper.reportMove(pNode, x, y);
      changed=true;
//      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
    }
    for(DiagramNode<T> node:rightNodes) {
      changed |= layoutNodeRight(pNodes, node, pass);
    }
    return changed;
  }

  private boolean layoutNodeLeft(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, int pass) {
    aLayoutStepper.reportLayoutNode(pNode);
    boolean changed = false;
    List<? extends DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<? extends DiagramNode<T>> rightNodes = pNode.getRightNodes();
    List<? extends DiagramNode<T>> aboveSiblings = getPrecedingSiblings(pNode);
    List<? extends DiagramNode<T>> belowSiblings = getFollowingSiblings(pNode);

    double minY = bottom(lowest(aboveSiblings),Double.NEGATIVE_INFINITY)+aVertSeparation + pNode.getTopExtend();
    if (aboveSiblings.size()>1) { aLayoutStepper.reportLowest(aboveSiblings, lowest(aboveSiblings)); }
    if (!Double.isInfinite(minY)) { aLayoutStepper.reportMinY(aboveSiblings, minY); }

    List<DiagramNode<T>> nodesBelow = nodesBelow(pNode);
    double maxY = top(highest(nodesBelow), Double.POSITIVE_INFINITY)-aVertSeparation - pNode.getBottomExtend();
    if (nodesBelow.size()>1) { aLayoutStepper.reportHighest(nodesBelow, highest(nodesBelow)); }
    if (!Double.isInfinite(minY)) { aLayoutStepper.reportMaxY(nodesBelow, maxY); }

    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+aHorizSeparation + pNode.getLeftExtend();
    if (leftNodes.size()>1) { aLayoutStepper.reportRightmost(leftNodes, rightMost(leftNodes)); }
    if (!Double.isInfinite(minX)) { aLayoutStepper.reportMinX(leftNodes, minX); }

    double maxX = left(leftMost(rightNodes),Double.POSITIVE_INFINITY)-aHorizSeparation - pNode.getRightExtend();
    if (leftNodes.size()>1) { aLayoutStepper.reportLeftmost(rightNodes, leftMost(rightNodes)); }
    if (!Double.isInfinite(maxX)) { aLayoutStepper.reportMaxX(rightNodes, maxX); }

    double x = pNode.getX();
    double y = pNode.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesLeftPos(pNodes, pNode), -missingSpace);
        changed = true;
      }
    }

    {
      double missingSpace = minY - maxY;
      if (missingSpace>TOLERANCE) {
        y = minY;
        moveY(nodesAbovePos(pNodes, pNode), -missingSpace);
        changed = true;
      }
    }

    // If we have nodes left and right position this one in the middle
    if (! (leftNodes.isEmpty()||rightNodes.isEmpty())) {
      x = (rightMost(leftNodes).getX()+leftMost(rightNodes).getX())/2;
    }

    if (!(aboveSiblings.isEmpty()|| belowSiblings.isEmpty())) {
      y = (lowest(aboveSiblings).getY()+ highest(belowSiblings).getY())/2;
    } else if (rightNodes.size()>1 && leftNodes.size()<2) {
      y = (highest(rightNodes).getY() + lowest(rightNodes).getY())/2;
    } else if (rightNodes.size()==1 && leftNodes.isEmpty()) {
      y = rightNodes.get(0).getY();
    }

    x = Math.max(Math.min(maxX, x), minX);
    y = Math.max(Math.min(maxY, y), minY);

    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);

    if (leftNodes.size()>1 && (pass<2 ||yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      double leftCenterY = (highest(leftNodes).getY()+lowest(leftNodes).getY())/2;
      // if the center of the left nodes is below this one, move the left nodes up.
      // the reverse should be handled in the right pass
      if ((y-leftCenterY)>TOLERANCE) {
        moveY(leftNodes, y-leftCenterY);
      }
    }

    if (yChanged || xChanged) {
      aLayoutStepper.reportMove(pNode, x, y);
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

  private List<DiagramNode<T>> nodesAbove(DiagramNode<T> pNode) {
    LinkedHashSet<DiagramNode<T>> result = new LinkedHashSet<>();
    for(DiagramNode<T> pred : pNode.getLeftNodes()) {
      addNodesAbove(result, pred, pNode);
    }
    removeTransitiveRight(result, pNode);
    return new ArrayList<>(result);
  }

  private List<DiagramNode<T>> nodesBelow(DiagramNode<T> pNode) {
    LinkedHashSet<DiagramNode<T>> result = new LinkedHashSet<>();
    for(DiagramNode<T> pred : pNode.getLeftNodes()) {
      addNodesBelow(result, pred, pNode);
    }
    removeTransitiveRight(result, pNode);
    return new ArrayList<>(result);
  }

  private void addNodesAbove(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> pLeft, DiagramNode<T> pRef) {
    if (pLeft.getY()<pRef.getY()) {
      for(DiagramNode<T> candidate: pLeft.getRightNodes()) {
        if (candidate==pRef) {
          break;
        } else {
          addTransitiveRight(result, candidate);
        }
      }
      for(DiagramNode<T> pred : pLeft.getLeftNodes()) {
        addNodesAbove(result, pred, pLeft);
      }
    }
  }

  private void addNodesBelow(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> pLeft, DiagramNode<T> pRef) {
    if (pLeft.getY()>pRef.getY()) {
      boolean found = false;
      for(DiagramNode<T> candidate: pLeft.getRightNodes()) {
        if (candidate==pRef) {
          found = true;
        } else if (found){
          addTransitiveRight(result, candidate);
        }
      }
      for(DiagramNode<T> pred : pLeft.getLeftNodes()) {
        addNodesBelow(result, pred, pLeft);
      }
    }
  }

  private void addTransitiveRight(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> pNode) {
    if (result.add(pNode)) {
      for(DiagramNode<T> right: pNode.getRightNodes()) {
        addTransitiveRight(result, right);
      }
    }
  }


  private void removeTransitiveRight(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> pNode) {
    result.remove(pNode);
    for(DiagramNode<T> right: pNode.getRightNodes()) {
      removeTransitiveRight(result, right);
    }
  }

  private List<DiagramNode<T>> nodesAbovePos(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: pNodes) {
      if (n!=pNode && n.upOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  @SuppressWarnings("unused")
  private List<DiagramNode<T>> nodesBelowPos(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: pNodes) {
      if (n!=pNode && n.downOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private List<DiagramNode<T>> nodesLeftPos(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: pNodes) {
      if (n!=pNode && n.leftOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private List<DiagramNode<T>> nodesRightPos(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: pNodes) {
      if (n!=pNode && n.rightOverlaps(pNode, aHorizSeparation, aVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private void moveToRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    for(DiagramNode<T> n: pNodes) {
      if (n.rightOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        final double newX = pFreeRegion.getRight()+aHorizSeparation+n.getLeftExtend();
        aLayoutStepper.reportMove(n, newX, n.getY());
        n.setX(newX);
        moveToRight(pNodes, n);
        moveDown(pNodes, n);
      }
    }
  }

  private void moveDown(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    for(DiagramNode<T> n: pNodes) {
      if (n.downOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        final double newY = pFreeRegion.getBottom()+aVertSeparation+n.getTopExtend();
        aLayoutStepper.reportMove(n, n.getX(), newY);
        n.setY(newY);
        moveDown(pNodes, n);
        moveToRight(pNodes, n);
      }
    }
  }

  private void moveX(List<? extends DiagramNode<T>> pNodes, double distance) {
    aLayoutStepper.reportMoveX(pNodes, distance);
    for(DiagramNode<T> n: pNodes) {
      n.setX(n.getX()+distance);
      System.err.println("Moving node "+n+ "to ("+n.getX()+"!, "+n.getY()+')');
    }
  }

  private void moveY(List<? extends DiagramNode<T>> pNodes, double distance) {
    aLayoutStepper.reportMoveY(pNodes, distance);
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

  @SuppressWarnings("unused")
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

  @SuppressWarnings("unused")
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
    List<DiagramNode<T>> result = new ArrayList<>();
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
    List<DiagramNode<T>> result = new ArrayList<>();
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
