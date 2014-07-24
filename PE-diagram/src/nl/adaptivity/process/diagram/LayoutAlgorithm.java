package nl.adaptivity.process.diagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.diagram.Positioned;


public class LayoutAlgorithm<T extends Positioned> {

  private static class NullAlgorithm extends LayoutAlgorithm<Positioned> {

    @Override
    public boolean layout(List<? extends DiagramNode<Positioned>> pNodes) {
      return false;
    }

    public static final NullAlgorithm INSTANCE = new NullAlgorithm();

  }

  public static final LayoutAlgorithm<?> NULLALGORITHM = NullAlgorithm.INSTANCE;

  // We know that nullalgorithm does nothing and doesn't care about types.
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends Positioned> LayoutAlgorithm<T> nullalgorithm() { return (LayoutAlgorithm) NULLALGORITHM; }

  private static final double TOLERANCE = 0.1d;

  private static final int PASSCOUNT = 9;

  private double aVertSeparation = DrawableProcessModel.DEFAULT_VERT_SEPARATION;

  private double aHorizSeparation = DrawableProcessModel.DEFAULT_HORIZ_SEPARATION;

  private double aDefaultNodeWidth = 30d;
  private double aDefaultNodeHeight = 30d;

  private double aGridSize = Double.NaN;

  private boolean aTighten = false;

  private LayoutStepper<T> aLayoutStepper = new LayoutStepper<>();

  public LayoutStepper<T> getLayoutStepper() {
    return aLayoutStepper;
  }

  public void setLayoutStepper(LayoutStepper<T> pLayoutStepper) {
    aLayoutStepper = pLayoutStepper !=null ? pLayoutStepper : new LayoutStepper<T>();
  }



  public double getGridSize() {
    return aGridSize;
  }


  public void setGridSize(double pGridSize) {
    aGridSize = pGridSize;
  }

  public boolean isTighten() {
    return aTighten;
  }

  public void setTighten(boolean pTighten) {
    aTighten = pTighten;
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

  /**
   * Layout the given nodes
   * @param pNodes The nodes to layout
   * @return Whether the nodes have changed.
   */
  public boolean layout(List<? extends DiagramNode<T>> pNodes) {
    boolean changed = false;
    double minY = Double.NEGATIVE_INFINITY;
    for(List<DiagramNode<T>> partition : partition(pNodes)) {
      changed = layoutPartition(partition, minY)||changed;
      minY = getMinY(partition, aVertSeparation);
    }
    // TODO if needed, lay out single element partitions differently.
    return changed;
  }

  public boolean layoutPartition(List<DiagramNode<T>> pNodes, double pMinY) {
    boolean changed = false;
    for (final DiagramNode<T> node : pNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = layoutNodeInitial(pNodes, node, pMinY) || changed; // always force as that should be slightly more efficient
      }
      changed |= node.getTarget().getX()!=node.getX() || node.getTarget().getY()!=node.getY();
    }
    if (! changed) {
      if (aTighten) {
        changed = tightenPositions(pNodes);
      } else {
        changed = verifyPositions(pNodes);
      }
    }
    if (aTighten || changed) {
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

  private static void unset(double[] array) {
    for(int i=0; i<array.length; ++i) {
      array[i]=Double.NaN;
    }
  }

  private static int maxPos(double[] pArray) {
    if (pArray.length==0) { return -1; }
    double cur = pArray[0];
    int node = 0;
    for(int i=1; i<pArray.length;++i) {
      if(pArray[i]>cur) {
        node = i;
        cur = pArray[i];
      }
    }
    return node;
  }

  private boolean tightenPositions(List<DiagramNode<T>> pNodes) {
    double minX = getValidLeftBound(pNodes, 0);
    double minY = getValidTopBound(pNodes, 0);

    boolean changed = false;
    for(List<DiagramNode<T>> partition:partition(pNodes)) {
      changed = tightenPartitionPositions(partition, minX, minY) | changed;
      minY = getMinY(partition, aVertSeparation); // put the partitions below each other
    }
    return changed;
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param pNodes The nodes to try to find the mimimum of.
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected double getValidLeftBound(List<DiagramNode<T>> pNodes, double pFallback) {
    final double minX = left(leftMost(pNodes),pFallback);
    aLayoutStepper.reportMinX(pNodes, minX);
    return minX;
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param pNodes The nodes to try to find the mimimum of.
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected double getValidTopBound(List<DiagramNode<T>> pNodes, double pFallback) {
    final double minY = top(highest(pNodes), pFallback);
    aLayoutStepper.reportMinY(pNodes, minY);
    return minY;
  }

  protected static <T extends Positioned> List<List<DiagramNode<T>>> partition(List<? extends DiagramNode<T>> pNodes) {
    List<List<DiagramNode<T>>> partitions = new ArrayList<>();
    ArrayList<DiagramNode<T>> nodes = new ArrayList<>(pNodes);
    while (! nodes.isEmpty()) {
      final ArrayList<DiagramNode<T>> partition = new ArrayList<>();
      addToPartition(nodes.get(0), partition, nodes);
      partitions.add(partition);
    }
    return partitions;
  }

  private static <T extends Positioned> void addToPartition(DiagramNode<T> pNode, ArrayList<DiagramNode<T>> pPartition, ArrayList<DiagramNode<T>> pNodes) {
    if (! pPartition.contains(pNode)) {
      pPartition.add(pNode);
      if(pNodes.remove(pNode)) {
        for(DiagramNode<T> left: pNode.getLeftNodes()) {
          addToPartition(left, pPartition, pNodes);
        }
        for(DiagramNode<T> right: pNode.getRightNodes()) {
          addToPartition(right, pPartition, pNodes);
        }
      }
    }
  }

  private boolean tightenPartitionPositions(List<DiagramNode<T>> pNodes, double leftMostPos, double minY) {
    int leftMostNode;
    int len = pNodes.size();
    double[] minXs = new double[len]; unset(minXs);
    double[] maxXs = new double[len]; unset(maxXs);
    double[] newYs = new double[len]; unset(newYs);
    {
      leftMostNode = -1;
      leftMostPos = Double.MAX_VALUE;
      for(int i=0; i<len;++i) {
        DiagramNode<T> node = pNodes.get(i);
        if (node.getLeftNodes().isEmpty() && node.getLeft()<leftMostPos) {
          leftMostNode = i;
          leftMostPos = node.getLeft();
        }
      }
      if (leftMostNode>=0) {
        maxXs[leftMostNode] = minXs[leftMostNode] = pNodes.get(leftMostNode).getX();
      }
      for(DiagramNode<T> node: pNodes.get(leftMostNode).getRightNodes()) {
        double newX = minXs[leftMostNode] + pNodes.get(leftMostNode).getRightExtend()+getHorizSeparation()+node.getLeftExtend();
        aLayoutStepper.reportLayoutNode(node);
        aLayoutStepper.reportMinX(Arrays.asList(pNodes.get(leftMostNode)) , newX);
        updateXPosLR(newX, node, pNodes, minXs);
      }
    }
    int mostRightNodePos = maxPos(minXs);
    DiagramNode<T> mostRightNode = pNodes.get(mostRightNodePos);
    maxXs[mostRightNodePos] = minXs[mostRightNodePos];
    boolean changed;
    if (Math.abs(mostRightNode.getX()-minXs[mostRightNodePos])>TOLERANCE) {
      changed = true;
      aLayoutStepper.reportLayoutNode(mostRightNode);
      aLayoutStepper.reportMove(mostRightNode, minXs[mostRightNodePos], mostRightNode.getY());
      pNodes.get(mostRightNodePos).setX(minXs[mostRightNodePos]);
    } else {
      changed = false;
    }
    for(DiagramNode<T> node:pNodes.get(mostRightNodePos).getLeftNodes()) {
      double newX = minXs[mostRightNodePos]-pNodes.get(mostRightNodePos).getLeftExtend()-getHorizSeparation()-node.getRightExtend();
      aLayoutStepper.reportLayoutNode(node);
      aLayoutStepper.reportMaxX(Arrays.asList(pNodes.get(mostRightNodePos)) , newX);
      updateXPosRL(newX, node, pNodes, minXs, maxXs);
    }
    return updateXPos(pNodes, minXs, maxXs) || changed;
  }

  private void updateXPosLR(double pNewX, DiagramNode<T> pNode, List<? extends DiagramNode<T>> pNodes, double[] newXs) {
    final int len = pNodes.size();
    for(int i=0; i< len;++i) {
      if (pNode==pNodes.get(i)) {
        double oldX = newXs[i];
        if (! (pNewX<=oldX)) { // Use the negative way to handle NaN, don't go on when there is already a value that wasn't changed.
          if (! Double.isNaN(newXs[i])) { aLayoutStepper.reportMaxX(Arrays.asList(pNode), newXs[i]); }
          newXs[i] = pNewX;
//          aLayoutStepper.reportMove(pNode, newXs[i], pNode.getY());
          for(DiagramNode<T> rightNode:pNode.getRightNodes()) {
            double newX = pNewX+pNode.getRightExtend()+getHorizSeparation()+rightNode.getLeftExtend();
            aLayoutStepper.reportLayoutNode(rightNode);
            aLayoutStepper.reportMinX(Arrays.asList(pNode), newX);
            updateXPosLR(newX, rightNode, pNodes, newXs);
          }
        } // ignore the rest
        break;
      }
    }
  }

  private void updateXPosRL(double pMaxX, DiagramNode<T> pNode, List<? extends DiagramNode<T>> pNodes, double[] minXs, double[] maxXs) {
    final int len = pNodes.size();
    for(int i=0; i< len;++i) { // loop to find the node position
      if (pNode==pNodes.get(i)) { // found the position, now use stuff
        aLayoutStepper.reportMinX(Arrays.asList(pNode), minXs[i]);
        if (Double.isNaN(maxXs[i]) || (maxXs[i]-TOLERANCE>pMaxX)) {
          maxXs[i] = pMaxX;
          for(DiagramNode<T> leftNode:pNode.getLeftNodes()) {
            double newX = pMaxX-pNode.getLeftExtend()-getHorizSeparation()-leftNode.getRightExtend();
            aLayoutStepper.reportLayoutNode(leftNode);
            aLayoutStepper.reportMaxX(Arrays.asList(pNode), newX);
            updateXPosRL(newX, leftNode, pNodes, minXs, maxXs);
          }
        }
        break;
      }
    }
  }

  private boolean updateXPos(List<? extends DiagramNode<T>> pNodes, double[] minXs, double[] maxXs) {
    boolean changed = false;
    int len = pNodes.size();
    for(int i=0;i<len;++i) {
      DiagramNode<T> node = pNodes.get(i);
      double minX = minXs[i];
      double maxX = maxXs[i];
      aLayoutStepper.reportLayoutNode(node);
      aLayoutStepper.reportMinX(Collections.<DiagramNode<T>>emptyList(), minX);
      aLayoutStepper.reportMaxX(Collections.<DiagramNode<T>>emptyList(), maxX);
      double x = node.getX();
      if (x+TOLERANCE<minX) {
        aLayoutStepper.reportMove(node, minX, node.getY());
        changed = true;
        node.setX(minX);
      } else if (x-TOLERANCE>maxX) {
        aLayoutStepper.reportMove(node, maxX, node.getY());
        changed = true;
        node.setX(maxX);
      }
    }

    return changed;
  }

  /** Just ensure that the positions of all the nodes are valid.
   * This means that all nodes are checked on whether they are at least horizseparation and vertseparation from each other.
   * This method does <b>not</b> take into account the grid. In most cases this method should not change the layout.
   * @param pNodes The nodes to verify (or move)
   * @return <code>true</code> if at least one node changed position, <code>false</code> if not.
   */
  private boolean verifyPositions(List<? extends DiagramNode<T>> pNodes) {
    boolean changed = false;
    for(DiagramNode<T> node: pNodes) {
      // For every node determine the minimum X position
      double minX = right(rightMost(nodesLeftPos(pNodes, node)), Double.NEGATIVE_INFINITY)+aHorizSeparation+node.getLeftExtend();
      // If our coordinate is lower than needed, move the node and all "within the area"
      if (minX + TOLERANCE>node.getX()) {
        changed = moveToRight(pNodes, node) || changed;
      }
      double minY = bottom(lowest(nodesAbovePos(pNodes, node)), Double.NEGATIVE_INFINITY)+aHorizSeparation+node.getTopExtend();
      if (minY + TOLERANCE>node.getY()) {
        changed = moveDown(pNodes, node) || changed;
      }
    }
    return changed;
  }

  /**
   * @param pNodes The nodes in the diagram that could be layed out.
   * @param pNode The node to focus on.
   */
  private boolean layoutNodeInitial(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, double pMinY) {
    List<DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<DiagramNode<T>> aboveNodes = getPrecedingSiblings(pNode);

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
        layoutNodeInitial(pNodes, node, pMinY);
      }
    }

    double minY = Math.max(getMinY(aboveNodes, aVertSeparation + pNode.getTopExtend()), pMinY + pNode.getTopExtend());
    double minX = getMinX(leftNodes, aHorizSeparation + pNode.getLeftExtend());

    if (leftNodes.isEmpty()) {
      x = aboveNodes.isEmpty() ? pNode.getLeftExtend() : averageX(aboveNodes);
      y = aboveNodes.isEmpty() ? pNode.getTopExtend() : minY;
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
      return true;
    }
    return false;
  }

  private boolean layoutNodeRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode, int pass) {
    aLayoutStepper.reportLayoutNode(pNode);
    boolean changed = false;
    List<DiagramNode<T>> leftNodes = pNode.getLeftNodes();
    List<DiagramNode<T>> rightNodes = pNode.getRightNodes();
    List<DiagramNode<T>> aboveSiblings = getPrecedingSiblings(pNode);
    List<DiagramNode<T>> belowSiblings = getFollowingSiblings(pNode);

    double minY = getMinY(nodesAbove(pNode), aVertSeparation+pNode.getTopExtend());
    double maxY = getMaxY(belowSiblings, aVertSeparation + pNode.getBottomExtend());

    double minX = getMinX(leftNodes, aHorizSeparation+pNode.getLeftExtend());
    double maxX = getMaxX(rightNodes, aHorizSeparation+pNode.getRightExtend());

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

  private double getMinY(List<DiagramNode<T>> pNodes, double pAdd) {
    double result = bottom(lowest(pNodes), Double.NEGATIVE_INFINITY)+pAdd;
    if (!Double.isInfinite(result)) {
      aLayoutStepper.reportMinY(pNodes, result);
    }
    return result;
  }

  private double getMaxY(List<DiagramNode<T>> pNodes, double pSubtract) {
    double result = top(highest(pNodes), Double.POSITIVE_INFINITY)-pSubtract;
    if (!Double.isInfinite(result)) {
      aLayoutStepper.reportMaxY(pNodes, result);
    }
    return result;
  }

  private double getMinX(List<DiagramNode<T>> pNodes, double pAdd) {
    double result = right(rightMost(pNodes), Double.NEGATIVE_INFINITY)+pAdd;
    if (!Double.isInfinite(result)) {
      aLayoutStepper.reportMinX(pNodes, result);
    }
    return result;
  }

  private double getMaxX(List<DiagramNode<T>> pNodes, double pSubtract) {
    double result = left(leftMost(pNodes), Double.POSITIVE_INFINITY)-pSubtract;
    if (!Double.isInfinite(result)) {
      aLayoutStepper.reportMaxX(pNodes, result);
    }
    return result;
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


  private DiagramNode<T> lowest(List<? extends DiagramNode<T>> pNodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:pNodes) {
      if (result==null || node.getBottom()>result.getBottom()) {
        result = node;
      }
    }
    if (result!=null) { aLayoutStepper.reportLowest(pNodes, result); }
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

  private boolean moveToRight(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    boolean changed = false;
    for(DiagramNode<T> n: pNodes) {
      if (n.rightOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        changed = true;
        final double newX = pFreeRegion.getRight()+aHorizSeparation+n.getLeftExtend();
        aLayoutStepper.reportMove(n, newX, n.getY());
        n.setX(newX);
        moveToRight(pNodes, n);
        moveDown(pNodes, n);
      }
    }
    return changed;
  }

  private boolean moveDown(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pFreeRegion) {
    boolean changed = false;
    for(DiagramNode<T> n: pNodes) {
      if (n.downOverlaps(pFreeRegion, aHorizSeparation, aVertSeparation)) {
        changed = true;
        final double newY = pFreeRegion.getBottom()+aVertSeparation+n.getTopExtend();
        aLayoutStepper.reportMove(n, n.getX(), newY);
        n.setY(newY);
        moveDown(pNodes, n);
        moveToRight(pNodes, n);
      }
    }
    return changed;
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
  private List<DiagramNode<T>> getPrecedingSiblings(DiagramNode<T> pNode) {
    return getSiblings(pNode, true);
  }

  private List<DiagramNode<T>> getSiblings(DiagramNode<T> pNode, boolean above) {
    List<DiagramNode<T>> result = new ArrayList<>();
    double y = pNode.getY();
    {
      boolean seenNode = false;
      for(DiagramNode<T> pred:pNode.getLeftNodes()) {
        if (pred.getRightNodes().contains(pNode)) {
          for(DiagramNode<T> sibling: pred.getRightNodes()) {
            if(sibling==pNode) {
              seenNode = true;
            }
            if(Double.isNaN(sibling.getY()) || Double.isNaN(y)) { // no coordinate
              if (above ^ seenNode) {
                result.add(sibling);
              }
            } else {
              if ( above ? (sibling.getY()<y) : (sibling.getY()>y)) {
                result.add(sibling);
              }
            }
          }
        }
      }
    }
    {
      boolean seenNode = false;
      for(DiagramNode<T> pred:pNode.getRightNodes()) {
        if (pred.getLeftNodes().contains(pNode)) {
          for(DiagramNode<T> sibling: pred.getLeftNodes()) {
            if(sibling==pNode) {
              seenNode = true;
            }
            if(Double.isNaN(sibling.getY()) || Double.isNaN(y)) { // no coordinate
              if (above ^ seenNode) {
                result.add(sibling);
              }
            } else {
              if ( above ? (sibling.getY()<y) : (sibling.getY()>y)) {
                result.add(sibling);
              }
            }
          }
        }
      }
    }
    if (result.size()>0) {
      aLayoutStepper.reportSiblings(pNode, result, above);
    }
    return result;
  }

  private List<DiagramNode<T>> getFollowingSiblings(DiagramNode<T> pNode) {
    return getSiblings(pNode, false);
  }

}
