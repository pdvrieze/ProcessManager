/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.diagram

import net.devrieze.util.CollectionUtil
import net.devrieze.util.collection.maxIndex
import nl.adaptivity.diagram.Positioned

import java.util.*


open class LayoutAlgorithm<T : Positioned> {

  object NullAlgorithm : LayoutAlgorithm<Positioned>() {

    override fun layout(nodes: List<DiagramNode<Positioned>>): Boolean {
      return false
    }

  }

  var vertSeparation = RootDrawableProcessModel.DEFAULT_VERT_SEPARATION

  var horizSeparation = RootDrawableProcessModel.DEFAULT_HORIZ_SEPARATION

  var defaultNodeWidth = 30.0
  var defaultNodeHeight = 30.0

  var gridSize = java.lang.Double.NaN

  var isTighten = false

  var layoutStepper: LayoutStepper<T>? = AbstractLayoutStepper()
    set(layoutStepper) {
      field = layoutStepper ?: AbstractLayoutStepper<T>()
    }

  /**
   * Layout the given nodes
   * @param nodes The nodes to layout
   * *
   * @return Whether the nodes have changed.
   */
  open fun layout(nodes: List<DiagramNode<T>>): Boolean {
    var changed = false
    var minY = java.lang.Double.NEGATIVE_INFINITY
    for (partition in partition(nodes)) {
      changed = layoutPartition(partition, minY) || changed
      minY = getMinY(partition, vertSeparation)
    }
    // TODO if needed, lay out single element partitions differently.
    return changed
  }

  fun layoutPartition(nodes: List<DiagramNode<T>>, minY: Double): Boolean {
    var changed = false
    for (node in nodes) {
      if (java.lang.Double.isNaN(node.x) || java.lang.Double.isNaN(node.y)) {
        changed = layoutNodeInitial(nodes, node,
                                    minY) || changed // always force as that should be slightly more efficient
      }
      changed = changed or (node.target.x != node.x || node.target.y != node.y)
    }
    if (!changed) {
      if (isTighten) {
        changed = tightenPositions(nodes)
      } else {
        changed = verifyPositions(nodes)
      }
    }
    if (isTighten || changed) {
      val nodeListCopy = ArrayList(nodes)
      var nodesChanged = true
      var pass = 0
      while (nodesChanged && pass < PASSCOUNT) {
        layoutStepper!!.reportPass(pass)
        nodesChanged = false
        if (pass % 2 == 0) {
          //          Collections.sort(nodes, LEFT_TO_RIGHT);
          for (node in nodeListCopy) {
            if (node.leftNodes.isEmpty()) {
              nodesChanged = nodesChanged or layoutNodeRight(nodes, node, pass)
            }
          }
        } else {
          //          Collections.sort(nodes, RIGHT_TO_LEFT);
          for (node in nodeListCopy) {
            if (node.rightNodes.isEmpty()) {
              nodesChanged = nodesChanged or layoutNodeLeft(nodes, node, pass)
            }
          }
        }
        ++pass
      }
      changed = changed or nodesChanged
    }
    if (changed) {
      var newMinX = java.lang.Double.MAX_VALUE
      var newMinY = java.lang.Double.MAX_VALUE
      for (node in nodes) {
        newMinX = Math.min(node.left, newMinX)
        newMinY = Math.min(node.top, newMinY)
      }
      val offsetX = 0 - newMinX
      val offsetY = 0 - newMinY

      if (Math.abs(offsetX) > TOLERANCE || Math.abs(offsetY) > TOLERANCE) {
        for (node in nodes) {
          node.x = node.x + offsetX
          node.y = node.y + offsetY
        }
      }
    }
    return changed
  }

  private fun tightenPositions(nodes: List<DiagramNode<T>>): Boolean {
    val minX = getValidLeftBound(nodes, 0.0)
    var minY = getValidTopBound(nodes, 0.0)

    var changed = false
    for (partition in partition(nodes)) {
      changed = tightenPartitionPositions(partition, minX, minY) or changed
      minY = getMinY(partition, vertSeparation) // put the partitions below each other
    }
    return changed
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param nodes The nodes to try to find the mimimum of.
   * *
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected fun getValidLeftBound(nodes: List<DiagramNode<T>>, fallback: Double): Double {
    val minX = left(leftMost(nodes), fallback)
    layoutStepper?.reportMinX(nodes, minX)
    return minX
  }

  /**
   * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
   * @param nodes The nodes to try to find the mimimum of.
   * *
   * @return The minimum left of all the nodes, or 0 if none exists.
   */
  protected fun getValidTopBound(nodes: List<DiagramNode<T>>, fallback: Double): Double {
    val minY = top(highest(nodes), fallback)
    layoutStepper?.reportMinY(nodes, minY)
    return minY
  }

  private fun tightenPartitionPositions(nodes: List<DiagramNode<T>>, leftMostPos: Double, minY: Double): Boolean {
    var leftMostPos = leftMostPos
    var leftMostNode: Int
    val len = nodes.size
    val minXs = DoubleArray(len)
    unset(minXs)
    val maxXs = DoubleArray(len)
    unset(maxXs)
    val newYs = DoubleArray(len)
    unset(newYs)
    run {
      leftMostNode = -1
      leftMostPos = java.lang.Double.MAX_VALUE
      for (i in 0..len - 1) {
        val node = nodes[i]
        if (node.leftNodes.isEmpty() && node.left < leftMostPos) {
          leftMostNode = i
          leftMostPos = node.left
        }
      }
      if (leftMostNode >= 0) {
        minXs[leftMostNode] = nodes[leftMostNode].x
        maxXs[leftMostNode] = minXs[leftMostNode]
      }
      for (node in nodes[leftMostNode].rightNodes) {
        val newX = minXs[leftMostNode] + nodes[leftMostNode].rightExtent + horizSeparation + node.leftExtent
        layoutStepper?.reportLayoutNode(node)
        layoutStepper?.reportMinX(Arrays.asList(nodes[leftMostNode]), newX)
        updateXPosLR(newX, node, nodes, minXs)
      }
    }
    val mostRightNodePos = maxPos(minXs)
    val mostRightNode = nodes[mostRightNodePos]
    maxXs[mostRightNodePos] = minXs[mostRightNodePos]
    val changed: Boolean
    if (Math.abs(mostRightNode.x - minXs[mostRightNodePos]) > TOLERANCE) {
      changed = true
      layoutStepper?.reportLayoutNode(mostRightNode)
      layoutStepper?.reportMove(mostRightNode, minXs[mostRightNodePos], mostRightNode.y)
      nodes[mostRightNodePos].x = minXs[mostRightNodePos]
    } else {
      changed = false
    }
    for (node in nodes[mostRightNodePos].leftNodes) {
      val newX = minXs[mostRightNodePos] - nodes[mostRightNodePos].leftExtent - horizSeparation - node.rightExtent
      layoutStepper?.reportLayoutNode(node)
      layoutStepper?.reportMaxX(Arrays.asList(nodes[mostRightNodePos]), newX)
      updateXPosRL(newX, node, nodes, minXs, maxXs)
    }
    return updateXPos(nodes, minXs, maxXs) || changed
  }

  private fun updateXPosLR(newX: Double, node: DiagramNode<T>, nodes: List<DiagramNode<T>>, newXs: DoubleArray) {
    val len = nodes.size
    for (i in 0..len - 1) {
      if (node == nodes[i]) {
        val oldX = newXs[i]
        if (newX > oldX) { // Use the negative way to handle NaN, don't go on when there is already a value that wasn't changed.
          if (!java.lang.Double.isNaN(newXs[i])) {
            layoutStepper?.reportMaxX(Arrays.asList(node), newXs[i])
          }
          newXs[i] = newX
          //          mLayoutStepper.reportMove(pNode, newXs[i], pNode.getY());
          for (rightNode in node.rightNodes) {
            val updatedNewX = newX + node.rightExtent + horizSeparation + rightNode.leftExtent
            layoutStepper?.reportLayoutNode(rightNode)
            layoutStepper?.reportMinX(Arrays.asList(node), updatedNewX)
            updateXPosLR(updatedNewX, rightNode, nodes, newXs)
          }
        } // ignore the rest
        break
      }
    }
  }

  private fun updateXPosRL(maxX: Double,
                           node: DiagramNode<T>,
                           nodes: List<DiagramNode<T>>,
                           minXs: DoubleArray,
                           maxXs: DoubleArray) {
    val len = nodes.size
    for (i in 0..len - 1) { // loop to find the node position
      if (node == nodes[i]) { // found the position, now use stuff
        layoutStepper!!.reportMinX(Arrays.asList(node), minXs[i])
        if (java.lang.Double.isNaN(maxXs[i]) || maxXs[i] - TOLERANCE > maxX) {
          maxXs[i] = maxX
          for (leftNode in node.leftNodes) {
            val newX = maxX - node.leftExtent - horizSeparation - leftNode.rightExtent
            layoutStepper!!.reportLayoutNode(leftNode)
            layoutStepper!!.reportMaxX(Arrays.asList(node), newX)
            updateXPosRL(newX, leftNode, nodes, minXs, maxXs)
          }
        }
        break
      }
    }
  }

  private fun updateXPos(nodes: List<DiagramNode<T>>, minXs: DoubleArray, maxXs: DoubleArray): Boolean {
    var changed = false
    val len = nodes.size
    for (i in 0..len - 1) {
      val node = nodes[i]
      val minX = minXs[i]
      val maxX = maxXs[i]
      layoutStepper!!.reportLayoutNode(node)
      layoutStepper!!.reportMinX(emptyList<DiagramNode<T>>(), minX)
      layoutStepper!!.reportMaxX(emptyList<DiagramNode<T>>(), maxX)
      val x = node.x
      if (x + TOLERANCE < minX) {
        layoutStepper!!.reportMove(node, minX, node.y)
        changed = true
        node.x = minX
      } else if (x - TOLERANCE > maxX) {
        layoutStepper!!.reportMove(node, maxX, node.y)
        changed = true
        node.x = maxX
      }
    }

    return changed
  }

  /** Just ensure that the positions of all the nodes are valid.
   * This means that all nodes are checked on whether they are at least horizseparation and vertseparation from each other.
   * This method does **not** take into account the grid. In most cases this method should not change the layout.
   * @param nodes The nodes to verify (or move)
   * *
   * @return `true` if at least one node changed position, `false` if not.
   */
  private fun verifyPositions(nodes: List<DiagramNode<T>>): Boolean {
    var changed = false
    for (node in nodes) {
      // For every node determine the minimum X position
      val minX = right(rightMost(nodesLeftPos(nodes, node)),
                       java.lang.Double.NEGATIVE_INFINITY) + horizSeparation + node.leftExtent
      // If our coordinate is lower than needed, move the node and all "within the area"
      if (minX + TOLERANCE > node.x) {
        changed = moveToRight(nodes, node) || changed
      }
      val minY = bottom(lowest(nodesAbovePos(nodes, node)),
                        java.lang.Double.NEGATIVE_INFINITY) + horizSeparation + node.topExtent
      if (minY + TOLERANCE > node.y) {
        changed = moveDown(nodes, node) || changed
      }
    }
    return changed
  }

  /**
   * @param nodes The nodes in the diagram that could be layed out.
   * *
   * @param baseNode The node to focus on.
   */
  private fun layoutNodeInitial(nodes: List<DiagramNode<T>>, baseNode: DiagramNode<T>, minY: Double): Boolean {
    val leftNodes = baseNode.leftNodes
    val aboveNodes = getPrecedingSiblings(baseNode)

    val origX = baseNode.x // store the initial coordinates
    val origY = baseNode.y

    var x = origX
    var y = origY

    // set temporary coordinates to prevent infinite recursion
    if (java.lang.Double.isNaN(origX)) {
      baseNode.x = 0.0
      x = 0.0
    }
    if (java.lang.Double.isNaN(origY)) {
      baseNode.y = 0.0
      y = 0.0
    }

    // Ensure that both the leftNodes and aboveNodes have set coordinates.
    for (node in CollectionUtil.combine(leftNodes, aboveNodes)) {
      if (java.lang.Double.isNaN(node.x) || java.lang.Double.isNaN(node.y)) {
        layoutNodeInitial(nodes, node, minY)
      }
    }

    val newMinY = Math.max(getMinY(aboveNodes, vertSeparation + baseNode.topExtent), minY + baseNode.topExtent)
    val newMinX = getMinX(leftNodes, horizSeparation + baseNode.leftExtent)

    if (leftNodes.isEmpty()) {
      x = if (aboveNodes.isEmpty()) baseNode.leftExtent else averageX(aboveNodes)
      y = if (aboveNodes.isEmpty()) baseNode.topExtent else newMinY
    } else { // leftPoints not empty, minX must be set
      x = newMinX
      y = Math.max(newMinY, averageY(leftNodes))
    }
    //    if (Double.isNaN(x)) { x = 0d; }
    //    if (Double.isNaN(y)) { y = 0d; }
    val xChanged = changed(x, origX, TOLERANCE)
    val yChanged = changed(y, origY, TOLERANCE)
    if (yChanged || xChanged) {
      layoutStepper!!.reportMove(baseNode, x, y)
      //      System.err.println("Moving node "+pNode.getTarget()+ "to ("+x+", "+y+')');
      baseNode.x = x
      baseNode.y = y
      return true
    }
    return false
  }

  private fun layoutNodeRight(nodes: List<DiagramNode<T>>, node: DiagramNode<T>, pass: Int): Boolean {
    layoutStepper!!.reportLayoutNode(node)
    var changed = false
    val leftNodes = node.leftNodes
    val rightNodes = node.rightNodes
    val aboveSiblings = getPrecedingSiblings(node)
    val belowSiblings = getFollowingSiblings(node)

    val minY = getMinY(nodesAbove(node), vertSeparation + node.topExtent)
    val maxY = getMaxY(belowSiblings, vertSeparation + node.bottomExtent)

    val minX = getMinX(leftNodes, horizSeparation + node.leftExtent)
    val maxX = getMaxX(rightNodes, horizSeparation + node.rightExtent)

    var x = node.x
    var y = node.y

    run {
      // ensure that there is space for the node. If not, move all right nodes to the right
      val missingSpace = minX - maxX
      if (missingSpace > TOLERANCE) {
        x = minX
        moveX(nodesRightPos(nodes, node), missingSpace)
        changed = true
      }
    }

    run {
      val missingSpace = minY - maxY
      if (missingSpace > TOLERANCE) {
        y = minY
        moveY(belowSiblings, missingSpace)
        changed = true
      }
    }

    // If we have nodes left and right position this one in the middle
    if (!(leftNodes.isEmpty() || rightNodes.isEmpty())) {
      x = (rightMost(leftNodes)!!.x + leftMost(rightNodes)!!.x) / 2
    }
    if (!(aboveSiblings.isEmpty() || belowSiblings.isEmpty())) {
      y = (lowest(aboveSiblings)!!.y + highest(belowSiblings)!!.y) / 2
    } else if (leftNodes.size > 1) {
      y = (highest(leftNodes)!!.y + lowest(leftNodes)!!.y) / 2
    } else if (leftNodes.size == 1 && rightNodes.size < 2) {
      y = leftNodes[0].y
    }

    x = Math.max(Math.min(maxX, x), minX)
    y = Math.max(Math.min(maxY, y), minY)

    val xChanged = changed(x, node.x, TOLERANCE)
    val yChanged = changed(y, node.y, TOLERANCE)

    if (rightNodes.size > 1 && (pass < 2 || yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      val rightCenterY = (highest(rightNodes)!!.y + lowest(rightNodes)!!.y) / 2
      if (y - rightCenterY > TOLERANCE) {
        // if the center of the right nodes is above this one, move the right nodes down.
        // the reverse should be handled in the left pass
        moveY(rightNodes, y - rightCenterY)
      }
    }

    if (yChanged || xChanged) {
      layoutStepper!!.reportMove(node, x, y)
      changed = true
      //      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      node.x = x
      node.y = y
    }
    for (rightNode in rightNodes) {
      changed = changed or layoutNodeRight(nodes, rightNode, pass)
    }
    return changed
  }

  private fun getMinY(nodes: List<DiagramNode<T>>, add: Double): Double {
    val result = bottom(lowest(nodes), java.lang.Double.NEGATIVE_INFINITY) + add
    if (!java.lang.Double.isInfinite(result)) {
      layoutStepper!!.reportMinY(nodes, result)
    }
    return result
  }

  private fun getMaxY(nodes: List<DiagramNode<T>>, subtract: Double): Double {
    val result = top(highest(nodes), java.lang.Double.POSITIVE_INFINITY) - subtract
    if (!java.lang.Double.isInfinite(result)) {
      layoutStepper!!.reportMaxY(nodes, result)
    }
    return result
  }

  private fun getMinX(nodes: List<DiagramNode<T>>, add: Double): Double {
    val result = right(rightMost(nodes), java.lang.Double.NEGATIVE_INFINITY) + add
    if (!java.lang.Double.isInfinite(result)) {
      layoutStepper!!.reportMinX(nodes, result)
    }
    return result
  }

  private fun getMaxX(nodes: List<DiagramNode<T>>, subtract: Double): Double {
    val result = left(leftMost(nodes), java.lang.Double.POSITIVE_INFINITY) - subtract
    if (!java.lang.Double.isInfinite(result)) {
      layoutStepper!!.reportMaxX(nodes, result)
    }
    return result
  }

  private fun layoutNodeLeft(nodes: List<DiagramNode<T>>, node: DiagramNode<T>, pass: Int): Boolean {
    layoutStepper!!.reportLayoutNode(node)
    var changed = false
    val leftNodes = node.leftNodes
    val rightNodes = node.rightNodes
    val aboveSiblings = getPrecedingSiblings(node)
    val belowSiblings = getFollowingSiblings(node)

    val minY = bottom(lowest(aboveSiblings), java.lang.Double.NEGATIVE_INFINITY) + vertSeparation + node.topExtent
    if (minY.isFinite()) {
      layoutStepper?.reportMinY(aboveSiblings, minY)
    }

    val nodesBelow = nodesBelow(node)
    val maxY = top(highest(nodesBelow), java.lang.Double.POSITIVE_INFINITY) - vertSeparation - node.bottomExtent
    if (minY.isFinite()) {
      layoutStepper?.reportMaxY(nodesBelow, maxY)
    }

    val minX = right(rightMost(leftNodes), java.lang.Double.NEGATIVE_INFINITY) + horizSeparation + node.leftExtent
    if (minX.isFinite()) {
      layoutStepper?.reportMinX(leftNodes, minX)
    }

    val maxX = left(leftMost(rightNodes), java.lang.Double.POSITIVE_INFINITY) - horizSeparation - node.rightExtent
    if (maxX.isFinite()) {
      layoutStepper?.reportMaxX(rightNodes, maxX)
    }

    var x = node.x
    var y = node.y

    run {
      // ensure that there is space for the node. If not, move all right nodes to the right
      val missingSpace = minX - maxX
      if (missingSpace > TOLERANCE) {
        x = minX
        moveX(nodesLeftPos(nodes, node), -missingSpace)
        changed = true
      }
    }

    run {
      val missingSpace = minY - maxY
      if (missingSpace > TOLERANCE) {
        y = minY
        moveY(nodesAbovePos(nodes, node), -missingSpace)
        changed = true
      }
    }

    // If we have nodes left and right position this one in the middle
    if (!(leftNodes.isEmpty() || rightNodes.isEmpty())) {
      x = (rightMost(leftNodes)!!.x + leftMost(rightNodes)!!.x) / 2
    }

    if (!(aboveSiblings.isEmpty() || belowSiblings.isEmpty())) {
      y = (lowest(aboveSiblings)!!.y + highest(belowSiblings)!!.y) / 2
    } else if (rightNodes.size > 1 && leftNodes.size < 2) {
      y = (highest(rightNodes)!!.y + lowest(rightNodes)!!.y) / 2
    } else if (rightNodes.size == 1 && leftNodes.isEmpty()) {
      y = rightNodes[0].y
    }

    x = Math.max(Math.min(maxX, x), minX)
    y = Math.max(Math.min(maxY, y), minY)

    val xChanged = changed(x, node.x, TOLERANCE)
    val yChanged = changed(y, node.y, TOLERANCE)

    if (leftNodes.size > 1 && (pass < 2 || yChanged)) {
      /* If we have multiple nodes branching of this one determine the center. Move that
       * so that this node is the vertical center.
       */
      val leftCenterY = (highest(leftNodes)!!.y + lowest(leftNodes)!!.y) / 2
      // if the center of the left nodes is below this one, move the left nodes up.
      // the reverse should be handled in the right pass
      if (y - leftCenterY > TOLERANCE) {
        moveY(leftNodes, y - leftCenterY)
      }
    }

    if (yChanged || xChanged) {
      layoutStepper?.reportMove(node, x, y)
      changed = true
      System.err.println("Moving node " + node + "to (" + x + ", " + y + ')')
      node.x = x
      node.y = y
    }
    for (leftNode in leftNodes) {
      changed = changed or layoutNodeLeft(nodes, leftNode, pass)
    }
    return changed
  }


  private fun lowest(nodes: List<DiagramNode<T>>, report:Boolean = true)
    = nodes.minBy { it.bottom }?.apply { if (report) layoutStepper?.reportLowest(nodes, this) }


  private fun highest(nodes: List<DiagramNode<T>>, report:Boolean = true)
    = nodes.maxBy { it.top }?.apply { if (report) layoutStepper?.reportHighest(nodes, this) }

  private fun leftMost(nodes: List<DiagramNode<T>>, report:Boolean = true)
    = nodes.minBy { it.left }?.apply { if (report) layoutStepper?.reportLeftmost(nodes, this) }


  private fun rightMost(nodes: List<DiagramNode<T>>, report:Boolean = true)
    = nodes.maxBy { it.right }?.apply { if (report) layoutStepper?.reportRightmost(nodes, this) }

  private fun nodesAbove(node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = LinkedHashSet<DiagramNode<T>>()
    for (pred in node.leftNodes) {
      addNodesAbove(result, pred, node)
    }
    removeTransitiveRight(result, node)
    return result.toList()
  }

  private fun nodesBelow(node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = LinkedHashSet<DiagramNode<T>>()
    for (pred in node.leftNodes) {
      addNodesBelow(result, pred, node)
    }
    removeTransitiveRight(result, node)
    return result.toList()
  }

  private fun addNodesAbove(result: LinkedHashSet<DiagramNode<T>>, left: DiagramNode<T>, ref: DiagramNode<T>) {
    if (left.y < ref.y) {
      for (candidate in left.rightNodes) {
        if (candidate == ref) {
          break
        } else {
          addTransitiveRight(result, candidate)
        }
      }
      for (pred in left.leftNodes) {
        addNodesAbove(result, pred, left)
      }
    }
  }

  private fun addNodesBelow(result: LinkedHashSet<DiagramNode<T>>, left: DiagramNode<T>, ref: DiagramNode<T>) {
    if (left.y > ref.y) {
      var found = false
      for (candidate in left.rightNodes) {
        if (candidate == ref) {
          found = true
        } else if (found) {
          addTransitiveRight(result, candidate)
        }
      }
      for (pred in left.leftNodes) {
        addNodesBelow(result, pred, left)
      }
    }
  }

  private fun addTransitiveRight(result: LinkedHashSet<DiagramNode<T>>, node: DiagramNode<T>) {
    if (result.add(node)) {
      for (right in node.rightNodes) {
        addTransitiveRight(result, right)
      }
    }
  }


  private fun removeTransitiveRight(result: LinkedHashSet<DiagramNode<T>>, node: DiagramNode<T>) {
    result.remove(node)
    for (right in node.rightNodes) {
      removeTransitiveRight(result, right)
    }
  }

  private fun nodesAbovePos(nodes: List<DiagramNode<T>>, node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = ArrayList<DiagramNode<T>>()
    for (n in nodes) {
      if (n != node && n.upOverlaps(node, horizSeparation, vertSeparation)) {
        result.add(n)
      }
    }
    return result
  }

  private fun nodesBelowPos(nodes: List<DiagramNode<T>>, node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = ArrayList<DiagramNode<T>>()
    for (n in nodes) {
      if (n != node && n.downOverlaps(node, horizSeparation, vertSeparation)) {
        result.add(n)
      }
    }
    return result
  }

  private fun nodesLeftPos(nodes: List<DiagramNode<T>>, node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = ArrayList<DiagramNode<T>>()
    for (n in nodes) {
      if (n != node && n.leftOverlaps(node, horizSeparation, vertSeparation)) {
        result.add(n)
      }
    }
    return result
  }

  private fun nodesRightPos(nodes: List<DiagramNode<T>>, node: DiagramNode<T>): List<DiagramNode<T>> {
    val result = ArrayList<DiagramNode<T>>()
    for (n in nodes) {
      if (n != node && n.rightOverlaps(node, horizSeparation, vertSeparation)) {
        result.add(n)
      }
    }
    return result
  }

  private fun moveToRight(nodes: List<DiagramNode<T>>, freeRegion: DiagramNode<T>): Boolean {
    var changed = false
    for (n in nodes) {
      if (n.rightOverlaps(freeRegion, horizSeparation, vertSeparation)) {
        changed = true
        val newX = freeRegion.right + horizSeparation + n.leftExtent
        layoutStepper?.reportMove(n, newX, n.y)
        n.x = newX
        moveToRight(nodes, n)
        moveDown(nodes, n)
      }
    }
    return changed
  }

  private fun moveDown(nodes: List<DiagramNode<T>>, freeRegion: DiagramNode<T>): Boolean {
    var changed = false
    for (n in nodes) {
      if (n.downOverlaps(freeRegion, horizSeparation, vertSeparation)) {
        changed = true
        val newY = freeRegion.bottom + vertSeparation + n.topExtent
        layoutStepper!!.reportMove(n, n.x, newY)
        n.y = newY
        moveDown(nodes, n)
        moveToRight(nodes, n)
      }
    }
    return changed
  }

  private fun moveX(nodes: List<DiagramNode<T>>, distance: Double) {
    layoutStepper!!.reportMoveX(nodes, distance)
    for (n in nodes) {
      n.x = n.x + distance
      System.err.println("Moving node " + n + "to (" + n.x + "!, " + n.y + ')')
    }
  }

  private fun moveY(nodes: List<DiagramNode<T>>, distance: Double) {
    layoutStepper!!.reportMoveY(nodes, distance)
    for (n in nodes) {
      n.y = n.y + distance
      System.err.println("Moving node " + n + "to (" + n.x + ", " + n.y + "!)")
    }
  }

  // TODO Change to all nodes in the graph that are not smaller or bigger
  private fun getPrecedingSiblings(node: DiagramNode<T>): List<DiagramNode<T>> {
    return getSiblings(node, true)
  }

  private fun getSiblings(node: DiagramNode<T>, above: Boolean): List<DiagramNode<T>> {
    val result = ArrayList<DiagramNode<T>>()
    val y = node.y
    run {
      var seenNode = false
      for (pred in node.leftNodes) {
        if (pred.rightNodes.contains(node)) {
          for (sibling in pred.rightNodes) {
            if (sibling == node) {
              seenNode = true
            }
            if (java.lang.Double.isNaN(sibling.y) || java.lang.Double.isNaN(y)) { // no coordinate
              if (above xor seenNode) {
                result.add(sibling)
              }
            } else {
              if (if (above) sibling.y < y else sibling.y > y) {
                result.add(sibling)
              }
            }
          }
        }
      }
    }
    run {
      var seenNode = false
      for (pred in node.rightNodes) {
        if (pred.leftNodes.contains(node)) {
          for (sibling in pred.leftNodes) {
            if (sibling == node) {
              seenNode = true
            }
            if (java.lang.Double.isNaN(sibling.y) || java.lang.Double.isNaN(y)) { // no coordinate
              if (above xor seenNode) {
                result.add(sibling)
              }
            } else {
              if (if (above) sibling.y < y else sibling.y > y) {
                result.add(sibling)
              }
            }
          }
        }
      }
    }
    if (result.size > 0) {
      layoutStepper!!.reportSiblings(node, result, above)
    }
    return result
  }

  private fun getFollowingSiblings(node: DiagramNode<T>): List<DiagramNode<T>> {
    return getSiblings(node, false)
  }

  companion object {

    @JvmField
    val NULLALGORITHM: LayoutAlgorithm<*> = NullAlgorithm

    // We know that nullalgorithm does nothing and doesn't care about types.
    @JvmStatic
    fun <T : Positioned> nullalgorithm(): LayoutAlgorithm<T> {
      return NullAlgorithm as LayoutAlgorithm<T>
    }

    private const val TOLERANCE = 0.1

    private const val PASSCOUNT = 9

    private fun unset(array: DoubleArray) = array.fill(Double.NaN)

    @Deprecated("Use directly", ReplaceWith("array.maxIndex", "net.devrieze.util.collection.maxIndex"))
    private fun maxPos(array: DoubleArray) = array.maxIndex()

    protected fun <T : Positioned> partition(nodes: List<DiagramNode<T>>): List<List<DiagramNode<T>>> {
      val partitions = ArrayList<List<DiagramNode<T>>>()
      val nodesCopy = nodes.toMutableList()

      fun addToPartition(node: DiagramNode<T>,
                                          partition: ArrayList<DiagramNode<T>>) {
        if (!partition.contains(node)) {
          partition.add(node)
          if (nodesCopy.remove(node)) {
            for (left in node.leftNodes) {
              addToPartition(left, partition)
            }
            for (right in node.rightNodes) {
              addToPartition(right, partition)
            }
          }
        }
      }


      while (!nodesCopy.isEmpty()) {
        val partition = ArrayList<DiagramNode<T>>()
        addToPartition(nodesCopy[0], partition)
        partitions.add(partition)
      }
      return partitions
    }

    private fun top(node: DiagramNode<*>?, fallback: Double): Double {
      return node?.top ?: fallback
    }

    private fun bottom(node: DiagramNode<*>?, fallback: Double): Double {
      return node?.bottom ?: fallback
    }

    private fun left(node: DiagramNode<*>?, fallback: Double): Double {
      return node?.left ?: fallback
    }

    private fun right(node: DiagramNode<*>?, fallback: Double): Double {
      return node?.right ?: fallback
    }


    private fun changed(a: Double, b: Double, tolerance: Double): Boolean {
      if (java.lang.Double.isNaN(a)) {
        return !java.lang.Double.isNaN(b)
      }
      if (java.lang.Double.isNaN(b)) {
        return true
      }
      return Math.abs(a - b) > tolerance
    }

    private fun <T : Positioned> averageY(nodes: List<DiagramNode<T>>): Double {
      if (nodes.isEmpty()) {
        return java.lang.Double.NaN
      } else {
        var total = 0.0
        for (p in nodes) {
          total += p.y
        }
        return total / nodes.size
      }
    }

    private fun <T : Positioned> averageY(nodes1: List<DiagramNode<T>>,
                                          nodes2: List<DiagramNode<T>>,
                                          fallback: Double): Double {
      if (nodes1.isEmpty() && nodes2.isEmpty()) {
        return fallback
      } else {
        var total = 0.0
        for (p in nodes1) {
          total += p.y
        }
        for (p in nodes2) {
          total += p.y
        }
        return total / (nodes1.size + nodes2.size)
      }
    }


    private fun <T : Positioned> averageX(nodes: List<DiagramNode<T>>): Double {
      if (nodes.isEmpty()) {
        return java.lang.Double.NaN
      } else {
        var total = 0.0
        for (p in nodes) {
          total += p.x
        }
        return total / nodes.size
      }
    }

    private fun <T : Positioned> averageX(nodes1: List<DiagramNode<T>>,
                                          nodes2: List<DiagramNode<T>>,
                                          fallback: Double): Double {
      if (nodes1.isEmpty() && nodes2.isEmpty()) {
        return fallback
      } else {
        var total = 0.0
        for (p in nodes1) {
          total += p.x
        }
        for (p in nodes2) {
          total += p.y
        }
        return total / (nodes1.size + nodes2.size)
      }
    }
  }

}
