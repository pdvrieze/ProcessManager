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

package nl.adaptivity.process.diagram;

import net.devrieze.util.CollectionUtil;
import nl.adaptivity.diagram.Positioned;

import java.util.*;


public class LayoutAlgorithm<T extends Positioned> {

  private static class NullAlgorithm extends LayoutAlgorithm<Positioned> {

    @Override
    public boolean layout(List<? extends DiagramNode<Positioned>> nodes) {
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

  private double mVertSeparation = DrawableProcessModel.DEFAULT_VERT_SEPARATION;

  private double mHorizSeparation = DrawableProcessModel.DEFAULT_HORIZ_SEPARATION;

  private double mDefaultNodeWidth = 30d;
  private double mDefaultNodeHeight = 30d;

  private double mGridSize = Double.NaN;

  private boolean mTighten = false;

  private LayoutStepper<T> mLayoutStepper = new LayoutStepper<>();

  public LayoutStepper<T> getLayoutStepper() {
    return mLayoutStepper;
  }

  public void setLayoutStepper(LayoutStepper<T> layoutStepper) {
    mLayoutStepper = layoutStepper !=null ? layoutStepper : new LayoutStepper<T>();
  }



  public double getGridSize() {
    return mGridSize;
  }


  public void setGridSize(double gridSize) {
    mGridSize = gridSize;
  }

  public boolean isTighten() {
    return mTighten;
  }

  public void setTighten(boolean tighten) {
    mTighten = tighten;
  }

  public double getVertSeparation() {
    return mVertSeparation;
  }


  public void setVertSeparation(double vertSeparation) {
    mVertSeparation = vertSeparation;
  }


  public double getHorizSeparation() {
    return mHorizSeparation;
  }


  public void setHorizSeparation(double horizSeparation) {
    mHorizSeparation = horizSeparation;
  }

  public double getDefaultNodeWidth() {
    return mDefaultNodeWidth;
  }


  public void setDefaultNodeWidth(double defaultNodeWidth) {
    mDefaultNodeWidth = defaultNodeWidth;
  }


  public double getDefaultNodeHeight() {
    return mDefaultNodeHeight;
  }


  public void setDefaultNodeHeight(double defaultNodeHeight) {
    mDefaultNodeHeight = defaultNodeHeight;
  }

  /**
   * Layout the given nodes
   * @param nodes The nodes to layout
   * @return Whether the nodes have changed.
   */
  public boolean layout(List<? extends DiagramNode<T>> nodes) {
    boolean changed = false;
    double minY = Double.NEGATIVE_INFINITY;
    for(List<DiagramNode<T>> partition : partition(nodes)) {
      changed = layoutPartition(partition, minY)||changed;
      minY = getMinY(partition, mVertSeparation);
    }
    // TODO if needed, lay out single element partitions differently.
    return changed;
  }

  public boolean layoutPartition(List<DiagramNode<T>> nodes, double minY) {
    boolean changed = false;
    for (final DiagramNode<T> node : nodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = layoutNodeInitial(nodes, node, minY) || changed; // always force as that should be slightly more efficient
      }
      changed |= node.getTarget().getX()!=node.getX() || node.getTarget().getY()!=node.getY();
    }
    if (! changed) {
      if (mTighten) {
        changed = tightenPositions(nodes);
      } else {
        changed = verifyPositions(nodes);
      }
    }
    if (mTighten || changed) {
      ArrayList<DiagramNode<T>> nodeListCopy = new ArrayList<>(nodes);
      boolean nodesChanged = true;
      for (int pass=0; nodesChanged && pass<PASSCOUNT; ++pass) {
        mLayoutStepper.reportPass(pass);
        nodesChanged = false;
        if (pass%2==0) {
//          Collections.sort(nodes, LEFT_TO_RIGHT);
          for(DiagramNode<T> node: nodeListCopy) {
            if (node.getLeftNodes().isEmpty()) {
              nodesChanged|=layoutNodeRight(nodes, node, pass);
            }
          }
        } else {
//          Collections.sort(nodes, RIGHT_TO_LEFT);
          for(DiagramNode<T> node: nodeListCopy) {
            if (node.getRightNodes().isEmpty()) {
              nodesChanged|=layoutNodeLeft(nodes, node, pass);
            }
          }
        }
      }
      changed|=nodesChanged;
    }
    if (changed) {
      double newMinX = Double.MAX_VALUE;
      double newMinY = Double.MAX_VALUE;
      for (final DiagramNode<T> node : nodes) {
        newMinX = Math.min(node.getLeft(), newMinX);
        newMinY = Math.min(node.getTop(), newMinY);
      }
      final double offsetX = 0 - newMinX;
      final double offsetY = 0 - newMinY;

      if (Math.abs(offsetX)>TOLERANCE || Math.abs(offsetY)>TOLERANCE) {
        for (final DiagramNode<T> node : nodes) {
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

  private static int maxPos(double[] array) {
    if (array.length==0) { return -1; }
    double cur = array[0];
    int node = 0;
    for(int i=1; i<array.length;++i) {
      if(array[i]>cur) {
        node = i;
        cur = array[i];
      }
    }
    return node;
  }

  private boolean tightenPositions(List<DiagramNode<T>> nodes) {
    double minX = getValidLeftBound(nodes, 0);
    double minY = getValidTopBound(nodes, 0);

    boolean changed = false;
    for(List<DiagramNode<T>> partition:partition(nodes)) {
      changed = tightenPartitionPositions(partition, minX, minY) | changed;
      minY = getMinY(partition, mVertSeparation); // put the partitions below each other
    }
    return changed;
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param nodes The nodes to try to find the mimimum of.
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected double getValidLeftBound(List<DiagramNode<T>> nodes, double fallback) {
    final double minX = left(leftMost(nodes),fallback);
    mLayoutStepper.reportMinX(nodes, minX);
    return minX;
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param nodes The nodes to try to find the mimimum of.
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected double getValidTopBound(List<DiagramNode<T>> nodes, double fallback) {
    final double minY = top(highest(nodes), fallback);
    mLayoutStepper.reportMinY(nodes, minY);
    return minY;
  }

  protected static <T extends Positioned> List<List<DiagramNode<T>>> partition(List<? extends DiagramNode<T>> nodes) {
    List<List<DiagramNode<T>>> partitions = new ArrayList<>();
    ArrayList<DiagramNode<T>> nodesCopy = new ArrayList<>(nodes);
    while (! nodesCopy.isEmpty()) {
      final ArrayList<DiagramNode<T>> partition = new ArrayList<>();
      addToPartition(nodesCopy.get(0), partition, nodesCopy);
      partitions.add(partition);
    }
    return partitions;
  }

  private static <T extends Positioned> void addToPartition(DiagramNode<T> node, ArrayList<DiagramNode<T>> partition, ArrayList<DiagramNode<T>> nodes) {
    if (! partition.contains(node)) {
      partition.add(node);
      if(nodes.remove(node)) {
        for(DiagramNode<T> left: node.getLeftNodes()) {
          addToPartition(left, partition, nodes);
        }
        for(DiagramNode<T> right: node.getRightNodes()) {
          addToPartition(right, partition, nodes);
        }
      }
    }
  }

  private boolean tightenPartitionPositions(List<DiagramNode<T>> nodes, double leftMostPos, double minY) {
    int leftMostNode;
    int len = nodes.size();
    double[] minXs = new double[len]; unset(minXs);
    double[] maxXs = new double[len]; unset(maxXs);
    double[] newYs = new double[len]; unset(newYs);
    {
      leftMostNode = -1;
      leftMostPos = Double.MAX_VALUE;
      for(int i=0; i<len;++i) {
        DiagramNode<T> node = nodes.get(i);
        if (node.getLeftNodes().isEmpty() && node.getLeft()<leftMostPos) {
          leftMostNode = i;
          leftMostPos = node.getLeft();
        }
      }
      if (leftMostNode>=0) {
        maxXs[leftMostNode] = minXs[leftMostNode] = nodes.get(leftMostNode).getX();
      }
      for(DiagramNode<T> node: nodes.get(leftMostNode).getRightNodes()) {
        double newX = minXs[leftMostNode] + nodes.get(leftMostNode).getRightExtend()+getHorizSeparation()+node.getLeftExtend();
        mLayoutStepper.reportLayoutNode(node);
        mLayoutStepper.reportMinX(Arrays.asList(nodes.get(leftMostNode)) , newX);
        updateXPosLR(newX, node, nodes, minXs);
      }
    }
    int mostRightNodePos = maxPos(minXs);
    DiagramNode<T> mostRightNode = nodes.get(mostRightNodePos);
    maxXs[mostRightNodePos] = minXs[mostRightNodePos];
    boolean changed;
    if (Math.abs(mostRightNode.getX()-minXs[mostRightNodePos])>TOLERANCE) {
      changed = true;
      mLayoutStepper.reportLayoutNode(mostRightNode);
      mLayoutStepper.reportMove(mostRightNode, minXs[mostRightNodePos], mostRightNode.getY());
      nodes.get(mostRightNodePos).setX(minXs[mostRightNodePos]);
    } else {
      changed = false;
    }
    for(DiagramNode<T> node:nodes.get(mostRightNodePos).getLeftNodes()) {
      double newX = minXs[mostRightNodePos]-nodes.get(mostRightNodePos).getLeftExtend()-getHorizSeparation()-node.getRightExtend();
      mLayoutStepper.reportLayoutNode(node);
      mLayoutStepper.reportMaxX(Arrays.asList(nodes.get(mostRightNodePos)) , newX);
      updateXPosRL(newX, node, nodes, minXs, maxXs);
    }
    return updateXPos(nodes, minXs, maxXs) || changed;
  }

  private void updateXPosLR(double newX, DiagramNode<T> node, List<? extends DiagramNode<T>> nodes, double[] newXs) {
    final int len = nodes.size();
    for(int i=0; i< len;++i) {
      if (node==nodes.get(i)) {
        double oldX = newXs[i];
        if (! (newX<=oldX)) { // Use the negative way to handle NaN, don't go on when there is already a value that wasn't changed.
          if (! Double.isNaN(newXs[i])) { mLayoutStepper.reportMaxX(Arrays.asList(node), newXs[i]); }
          newXs[i] = newX;
//          mLayoutStepper.reportMove(pNode, newXs[i], pNode.getY());
          for(DiagramNode<T> rightNode:node.getRightNodes()) {
            double updatedNewX = newX+node.getRightExtend()+getHorizSeparation()+rightNode.getLeftExtend();
            mLayoutStepper.reportLayoutNode(rightNode);
            mLayoutStepper.reportMinX(Arrays.asList(node), updatedNewX);
            updateXPosLR(updatedNewX, rightNode, nodes, newXs);
          }
        } // ignore the rest
        break;
      }
    }
  }

  private void updateXPosRL(double maxX, DiagramNode<T> node, List<? extends DiagramNode<T>> nodes, double[] minXs, double[] maxXs) {
    final int len = nodes.size();
    for(int i=0; i< len;++i) { // loop to find the node position
      if (node==nodes.get(i)) { // found the position, now use stuff
        mLayoutStepper.reportMinX(Arrays.asList(node), minXs[i]);
        if (Double.isNaN(maxXs[i]) || (maxXs[i]-TOLERANCE>maxX)) {
          maxXs[i] = maxX;
          for(DiagramNode<T> leftNode:node.getLeftNodes()) {
            double newX = maxX-node.getLeftExtend()-getHorizSeparation()-leftNode.getRightExtend();
            mLayoutStepper.reportLayoutNode(leftNode);
            mLayoutStepper.reportMaxX(Arrays.asList(node), newX);
            updateXPosRL(newX, leftNode, nodes, minXs, maxXs);
          }
        }
        break;
      }
    }
  }

  private boolean updateXPos(List<? extends DiagramNode<T>> nodes, double[] minXs, double[] maxXs) {
    boolean changed = false;
    int len = nodes.size();
    for(int i=0;i<len;++i) {
      DiagramNode<T> node = nodes.get(i);
      double minX = minXs[i];
      double maxX = maxXs[i];
      mLayoutStepper.reportLayoutNode(node);
      mLayoutStepper.reportMinX(Collections.<DiagramNode<T>>emptyList(), minX);
      mLayoutStepper.reportMaxX(Collections.<DiagramNode<T>>emptyList(), maxX);
      double x = node.getX();
      if (x+TOLERANCE<minX) {
        mLayoutStepper.reportMove(node, minX, node.getY());
        changed = true;
        node.setX(minX);
      } else if (x-TOLERANCE>maxX) {
        mLayoutStepper.reportMove(node, maxX, node.getY());
        changed = true;
        node.setX(maxX);
      }
    }

    return changed;
  }

  /** Just ensure that the positions of all the nodes are valid.
   * This means that all nodes are checked on whether they are at least horizseparation and vertseparation from each other.
   * This method does <b>not</b> take into account the grid. In most cases this method should not change the layout.
   * @param nodes The nodes to verify (or move)
   * @return <code>true</code> if at least one node changed position, <code>false</code> if not.
   */
  private boolean verifyPositions(List<? extends DiagramNode<T>> nodes) {
    boolean changed = false;
    for(DiagramNode<T> node: nodes) {
      // For every node determine the minimum X position
      double minX = right(rightMost(nodesLeftPos(nodes, node)), Double.NEGATIVE_INFINITY)+mHorizSeparation+node.getLeftExtend();
      // If our coordinate is lower than needed, move the node and all "within the area"
      if (minX + TOLERANCE>node.getX()) {
        changed = moveToRight(nodes, node) || changed;
      }
      double minY = bottom(lowest(nodesAbovePos(nodes, node)), Double.NEGATIVE_INFINITY)+mHorizSeparation+node.getTopExtend();
      if (minY + TOLERANCE>node.getY()) {
        changed = moveDown(nodes, node) || changed;
      }
    }
    return changed;
  }

  /**
   * @param nodes The nodes in the diagram that could be layed out.
   * @param baseNode The node to focus on.
   */
  private boolean layoutNodeInitial(List<? extends DiagramNode<T>> nodes, DiagramNode<T> baseNode, double minY) {
    List<DiagramNode<T>> leftNodes = baseNode.getLeftNodes();
    List<DiagramNode<T>> aboveNodes = getPrecedingSiblings(baseNode);

    double origX = baseNode.getX(); // store the initial coordinates
    double origY = baseNode.getY();

    double x = origX;
    double y = origY;

    // set temporary coordinates to prevent infinite recursion
    if (Double.isNaN(origX)) {baseNode.setX(0);x=0;}
    if (Double.isNaN(origY)) {baseNode.setY(0);y=0;}

    // Ensure that both the leftNodes and aboveNodes have set coordinates.
    for(DiagramNode<T> node: CollectionUtil.<DiagramNode<T>>combine(leftNodes, aboveNodes)) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        layoutNodeInitial(nodes, node, minY);
      }
    }

    double newMinY = Math.max(getMinY(aboveNodes, mVertSeparation + baseNode.getTopExtend()), minY + baseNode.getTopExtend());
    double newMinX = getMinX(leftNodes, mHorizSeparation + baseNode.getLeftExtend());

    if (leftNodes.isEmpty()) {
      x = aboveNodes.isEmpty() ? baseNode.getLeftExtend() : averageX(aboveNodes);
      y = aboveNodes.isEmpty() ? baseNode.getTopExtend() : newMinY;
    } else { // leftPoints not empty, minX must be set
      x = newMinX;
      y = Math.max(newMinY, averageY(leftNodes));
    }
//    if (Double.isNaN(x)) { x = 0d; }
//    if (Double.isNaN(y)) { y = 0d; }
    boolean xChanged = changed(x, origX, TOLERANCE);
    boolean yChanged = changed(y, origY, TOLERANCE);
    if (yChanged || xChanged) {
      mLayoutStepper.reportMove(baseNode, x, y);
//      System.err.println("Moving node "+pNode.getTarget()+ "to ("+x+", "+y+')');
      baseNode.setX(x);
      baseNode.setY(y);
      return true;
    }
    return false;
  }

  private boolean layoutNodeRight(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node, int pass) {
    mLayoutStepper.reportLayoutNode(node);
    boolean changed = false;
    List<DiagramNode<T>> leftNodes = node.getLeftNodes();
    List<DiagramNode<T>> rightNodes = node.getRightNodes();
    List<DiagramNode<T>> aboveSiblings = getPrecedingSiblings(node);
    List<DiagramNode<T>> belowSiblings = getFollowingSiblings(node);

    double minY = getMinY(nodesAbove(node), mVertSeparation+node.getTopExtend());
    double maxY = getMaxY(belowSiblings, mVertSeparation + node.getBottomExtend());

    double minX = getMinX(leftNodes, mHorizSeparation+node.getLeftExtend());
    double maxX = getMaxX(rightNodes, mHorizSeparation+node.getRightExtend());

    double x = node.getX();
    double y = node.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesRightPos(nodes, node), missingSpace);
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

    boolean xChanged = changed(x, node.getX(), TOLERANCE);
    boolean yChanged = changed(y, node.getY(), TOLERANCE);

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
      mLayoutStepper.reportMove(node, x, y);
      changed=true;
//      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      node.setX(x);
      node.setY(y);
    }
    for(DiagramNode<T> rightNode:rightNodes) {
      changed |= layoutNodeRight(nodes, rightNode, pass);
    }
    return changed;
  }

  private double getMinY(List<DiagramNode<T>> nodes, double add) {
    double result = bottom(lowest(nodes), Double.NEGATIVE_INFINITY)+add;
    if (!Double.isInfinite(result)) {
      mLayoutStepper.reportMinY(nodes, result);
    }
    return result;
  }

  private double getMaxY(List<DiagramNode<T>> nodes, double subtract) {
    double result = top(highest(nodes), Double.POSITIVE_INFINITY)-subtract;
    if (!Double.isInfinite(result)) {
      mLayoutStepper.reportMaxY(nodes, result);
    }
    return result;
  }

  private double getMinX(List<DiagramNode<T>> nodes, double add) {
    double result = right(rightMost(nodes), Double.NEGATIVE_INFINITY)+add;
    if (!Double.isInfinite(result)) {
      mLayoutStepper.reportMinX(nodes, result);
    }
    return result;
  }

  private double getMaxX(List<DiagramNode<T>> nodes, double subtract) {
    double result = left(leftMost(nodes), Double.POSITIVE_INFINITY)-subtract;
    if (!Double.isInfinite(result)) {
      mLayoutStepper.reportMaxX(nodes, result);
    }
    return result;
  }

  private boolean layoutNodeLeft(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node, int pass) {
    mLayoutStepper.reportLayoutNode(node);
    boolean changed = false;
    List<? extends DiagramNode<T>> leftNodes = node.getLeftNodes();
    List<? extends DiagramNode<T>> rightNodes = node.getRightNodes();
    List<? extends DiagramNode<T>> aboveSiblings = getPrecedingSiblings(node);
    List<? extends DiagramNode<T>> belowSiblings = getFollowingSiblings(node);

    double minY = bottom(lowest(aboveSiblings),Double.NEGATIVE_INFINITY)+mVertSeparation + node.getTopExtend();
    if (aboveSiblings.size()>1) { mLayoutStepper.reportLowest(aboveSiblings, lowest(aboveSiblings)); }
    if (!Double.isInfinite(minY)) { mLayoutStepper.reportMinY(aboveSiblings, minY); }

    List<DiagramNode<T>> nodesBelow = nodesBelow(node);
    double maxY = top(highest(nodesBelow), Double.POSITIVE_INFINITY)-mVertSeparation - node.getBottomExtend();
    if (nodesBelow.size()>1) { mLayoutStepper.reportHighest(nodesBelow, highest(nodesBelow)); }
    if (!Double.isInfinite(minY)) { mLayoutStepper.reportMaxY(nodesBelow, maxY); }

    double minX = right(rightMost(leftNodes), Double.NEGATIVE_INFINITY)+mHorizSeparation + node.getLeftExtend();
    if (leftNodes.size()>1) { mLayoutStepper.reportRightmost(leftNodes, rightMost(leftNodes)); }
    if (!Double.isInfinite(minX)) { mLayoutStepper.reportMinX(leftNodes, minX); }

    double maxX = left(leftMost(rightNodes),Double.POSITIVE_INFINITY)-mHorizSeparation - node.getRightExtend();
    if (leftNodes.size()>1) { mLayoutStepper.reportLeftmost(rightNodes, leftMost(rightNodes)); }
    if (!Double.isInfinite(maxX)) { mLayoutStepper.reportMaxX(rightNodes, maxX); }

    double x = node.getX();
    double y = node.getY();

    { // ensure that there is space for the node. If not, move all right nodes to the right
      double missingSpace = minX - maxX;
      if (missingSpace>TOLERANCE) {
        x = minX;
        moveX(nodesLeftPos(nodes, node), -missingSpace);
        changed = true;
      }
    }

    {
      double missingSpace = minY - maxY;
      if (missingSpace>TOLERANCE) {
        y = minY;
        moveY(nodesAbovePos(nodes, node), -missingSpace);
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

    boolean xChanged = changed(x, node.getX(), TOLERANCE);
    boolean yChanged = changed(y, node.getY(), TOLERANCE);

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
      mLayoutStepper.reportMove(node, x, y);
      changed=true;
      System.err.println("Moving node "+node+ "to ("+x+", "+y+')');
      node.setX(x);
      node.setY(y);
    }
    for(DiagramNode<T> leftNode:leftNodes) {
      changed |= layoutNodeLeft(nodes, leftNode, pass);
    }
    return changed;
  }

  private static double top(DiagramNode<?> node, double fallback) {
    return node==null ? fallback : node.getTop();
  }

  private static double bottom(DiagramNode<?> node, double fallback) {
    return node==null ? fallback : node.getBottom();
  }

  private static double left(DiagramNode<?> node, double fallback) {
    return node==null ? fallback : node.getLeft();
  }

  private static double right(DiagramNode<?> node, double fallback) {
    return node==null ? fallback : node.getRight();
  }

  private static <T extends Positioned> DiagramNode<T> highest(List<? extends DiagramNode<T>> nodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:nodes) {
      if (result==null || node.getTop()<result.getTop()) {
        result = node;
      }
    }
    return result;
  }


  private DiagramNode<T> lowest(List<? extends DiagramNode<T>> nodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:nodes) {
      if (result==null || node.getBottom()>result.getBottom()) {
        result = node;
      }
    }
    if (result!=null) { mLayoutStepper.reportLowest(nodes, result); }
    return result;
  }

  private static <T extends Positioned> DiagramNode<T> leftMost(List<? extends DiagramNode<T>> nodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:nodes) {
      if (result==null || node.getLeft()<result.getLeft()) {
        result = node;
      }
    }
    return result;
  }


  private static <T extends Positioned> DiagramNode<T> rightMost(List<? extends DiagramNode<T>> nodes) {
    DiagramNode<T> result = null;
    for(DiagramNode<T> node:nodes) {
      if (result==null || node.getRight()>result.getRight()) {
        result = node;
      }
    }
    return result;
  }

  private List<DiagramNode<T>> nodesAbove(DiagramNode<T> node) {
    LinkedHashSet<DiagramNode<T>> result = new LinkedHashSet<>();
    for(DiagramNode<T> pred : node.getLeftNodes()) {
      addNodesAbove(result, pred, node);
    }
    removeTransitiveRight(result, node);
    return new ArrayList<>(result);
  }

  private List<DiagramNode<T>> nodesBelow(DiagramNode<T> node) {
    LinkedHashSet<DiagramNode<T>> result = new LinkedHashSet<>();
    for(DiagramNode<T> pred : node.getLeftNodes()) {
      addNodesBelow(result, pred, node);
    }
    removeTransitiveRight(result, node);
    return new ArrayList<>(result);
  }

  private void addNodesAbove(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> left, DiagramNode<T> ref) {
    if (left.getY()<ref.getY()) {
      for(DiagramNode<T> candidate: left.getRightNodes()) {
        if (candidate==ref) {
          break;
        } else {
          addTransitiveRight(result, candidate);
        }
      }
      for(DiagramNode<T> pred : left.getLeftNodes()) {
        addNodesAbove(result, pred, left);
      }
    }
  }

  private void addNodesBelow(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> left, DiagramNode<T> ref) {
    if (left.getY()>ref.getY()) {
      boolean found = false;
      for(DiagramNode<T> candidate: left.getRightNodes()) {
        if (candidate==ref) {
          found = true;
        } else if (found){
          addTransitiveRight(result, candidate);
        }
      }
      for(DiagramNode<T> pred : left.getLeftNodes()) {
        addNodesBelow(result, pred, left);
      }
    }
  }

  private void addTransitiveRight(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> node) {
    if (result.add(node)) {
      for(DiagramNode<T> right: node.getRightNodes()) {
        addTransitiveRight(result, right);
      }
    }
  }


  private void removeTransitiveRight(LinkedHashSet<DiagramNode<T>> result, DiagramNode<T> node) {
    result.remove(node);
    for(DiagramNode<T> right: node.getRightNodes()) {
      removeTransitiveRight(result, right);
    }
  }

  private List<DiagramNode<T>> nodesAbovePos(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: nodes) {
      if (n!=node && n.upOverlaps(node, mHorizSeparation, mVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  @SuppressWarnings("unused")
  private List<DiagramNode<T>> nodesBelowPos(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: nodes) {
      if (n!=node && n.downOverlaps(node, mHorizSeparation, mVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private List<DiagramNode<T>> nodesLeftPos(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: nodes) {
      if (n!=node && n.leftOverlaps(node, mHorizSeparation, mVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private List<DiagramNode<T>> nodesRightPos(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    List<DiagramNode<T>> result = new ArrayList<>();
    for(DiagramNode<T> n: nodes) {
      if (n!=node && n.rightOverlaps(node, mHorizSeparation, mVertSeparation)) {
        result.add(n);
      }
    }
    return result;
  }

  private boolean moveToRight(List<? extends DiagramNode<T>> nodes, DiagramNode<T> freeRegion) {
    boolean changed = false;
    for(DiagramNode<T> n: nodes) {
      if (n.rightOverlaps(freeRegion, mHorizSeparation, mVertSeparation)) {
        changed = true;
        final double newX = freeRegion.getRight()+mHorizSeparation+n.getLeftExtend();
        mLayoutStepper.reportMove(n, newX, n.getY());
        n.setX(newX);
        moveToRight(nodes, n);
        moveDown(nodes, n);
      }
    }
    return changed;
  }

  private boolean moveDown(List<? extends DiagramNode<T>> nodes, DiagramNode<T> freeRegion) {
    boolean changed = false;
    for(DiagramNode<T> n: nodes) {
      if (n.downOverlaps(freeRegion, mHorizSeparation, mVertSeparation)) {
        changed = true;
        final double newY = freeRegion.getBottom()+mVertSeparation+n.getTopExtend();
        mLayoutStepper.reportMove(n, n.getX(), newY);
        n.setY(newY);
        moveDown(nodes, n);
        moveToRight(nodes, n);
      }
    }
    return changed;
  }

  private void moveX(List<? extends DiagramNode<T>> nodes, double distance) {
    mLayoutStepper.reportMoveX(nodes, distance);
    for(DiagramNode<T> n: nodes) {
      n.setX(n.getX()+distance);
      System.err.println("Moving node "+n+ "to ("+n.getX()+"!, "+n.getY()+')');
    }
  }

  private void moveY(List<? extends DiagramNode<T>> nodes, double distance) {
    mLayoutStepper.reportMoveY(nodes, distance);
    for(DiagramNode<T> n: nodes) {
      n.setY(n.getY()+distance);
      System.err.println("Moving node "+n+ "to ("+n.getX()+", "+n.getY()+"!)");
    }
  }


  private static boolean changed(double a, double b, double tolerance) {
    if (Double.isNaN(a)) { return !Double.isNaN(b); }
    if (Double.isNaN(b)) { return true; }
    return Math.abs(a-b)>tolerance;
  }

  private static <T extends Positioned> double averageY(List<? extends DiagramNode<T>> nodes) {
    if (nodes.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(DiagramNode<T> p: nodes) { total+=p.getY(); }
      return total/nodes.size();
    }
  }

  @SuppressWarnings("unused")
  private static <T extends Positioned> double averageY(List<? extends DiagramNode<T>> nodes1, List<? extends DiagramNode<T>> nodes2, double fallback) {
    if (nodes1.isEmpty() && nodes2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(DiagramNode<T> p: nodes1) { total+=p.getY(); }
      for(DiagramNode<T> p: nodes2) { total+=p.getY(); }
      return total/(nodes1.size()+nodes2.size());
    }
  }


  private static <T extends Positioned> double averageX(List<? extends DiagramNode<T>> nodes) {
    if (nodes.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(DiagramNode<T> p: nodes) { total+=p.getX(); }
      return total/nodes.size();
    }
  }

  @SuppressWarnings("unused")
  private static <T extends Positioned> double averageX(List<? extends DiagramNode<T>> nodes1, List<? extends DiagramNode<T>> nodes2, double fallback) {
    if (nodes1.isEmpty() && nodes2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(DiagramNode<T> p: nodes1) { total+=p.getX(); }
      for(DiagramNode<T> p: nodes2) { total+=p.getY(); }
      return total/(nodes1.size()+nodes2.size());
    }
  }

  // TODO Change to all nodes in the graph that are not smaller or bigger
  private List<DiagramNode<T>> getPrecedingSiblings(DiagramNode<T> node) {
    return getSiblings(node, true);
  }

  private List<DiagramNode<T>> getSiblings(DiagramNode<T> node, boolean above) {
    List<DiagramNode<T>> result = new ArrayList<>();
    double y = node.getY();
    {
      boolean seenNode = false;
      for(DiagramNode<T> pred:node.getLeftNodes()) {
        if (pred.getRightNodes().contains(node)) {
          for(DiagramNode<T> sibling: pred.getRightNodes()) {
            if(sibling==node) {
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
      for(DiagramNode<T> pred:node.getRightNodes()) {
        if (pred.getLeftNodes().contains(node)) {
          for(DiagramNode<T> sibling: pred.getLeftNodes()) {
            if(sibling==node) {
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
      mLayoutStepper.reportSiblings(node, result, above);
    }
    return result;
  }

  private List<DiagramNode<T>> getFollowingSiblings(DiagramNode<T> node) {
    return getSiblings(node, false);
  }

}
