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

import nl.adaptivity.diagram.Positioned

class NewLayoutAlgorithm: LayoutAlgorithm() {

  var alignFraction = 0.25

  private inner class LayoutInstance<T: Positioned> (val layoutStepper: LayoutStepper<T>) {

    var changed = false

    fun layout(nodes: List<DiagramNode<T>>, topY: Double):Boolean {
      val top = if (topY.isFinite()) topY else 0.0
      layoutStepper.reportPass(0)
      if (!initialLayout(nodes, top)) return false

      leftRightRepack(nodes)


      return nodes.none { it.x != it.target.x || it.y != it.target.y }
    }

    private fun leftRightRepack(nodes: List<DiagramNode<T>>) {
      for(node in nodes) {
        if (node.rightNodes.isNotEmpty()) {
          layoutStepper.reportLayoutNode(node)
          val leftMostPosition = node.right + horizSeparation
          val desiredMinX = node.rightNodes.asSequence().map {
            leftMostPosition + it.leftExtent
          }.max()!!

          for (successor in node.rightNodes) {
            if (successor.x < desiredMinX - TOLERANCE) {
              successor.moveTo(desiredMinX, successor.y, nodes)
            }
          }
        }
      }
    }


    private fun initialLayout(nodes: List<DiagramNode<T>>, top: Double): Boolean {
      val layedOutNodes = mutableSetOf<DiagramNode<T>>()
      val nodesToLayout = nodes.toMutableSet()

      fun layoutNode(node: DiagramNode<T>, x: Double, y: Double) {
        layoutStepper.reportLayoutNode(node)
        changed = node.setPos(x, y)
        if(changed) layoutStepper.reportMove(node, x, y)

        nodesToLayout.remove(node)
        layedOutNodes.add(node)
        for (left in node.leftNodes) {
          if (left !in layedOutNodes) {
            val leftX = node.left - horizSeparation - left.rightExtent
            val leftY =  maxOf(y, layedOutNodes.highestAvailablePos((leftX-node.leftExtent)..(leftX+node.rightExtent))+left.topExtent)
            layoutNode(left, leftX, leftY)
          }
        }
        for (right in node.rightNodes) {
          if (right !in layedOutNodes) {
            val rightX = node.right+horizSeparation+right.leftExtent
            val rightY =  maxOf(y, layedOutNodes.highestAvailablePos((rightX-node.leftExtent)..(rightX+node.rightExtent))+right.topExtent)
            layoutNode(right, node.right+horizSeparation+right.leftExtent, y)
          }
        }
      }

      val leftMost = nodes.firstOrNull { it.leftNodes.isEmpty() }
      if (leftMost!=null) {
        layoutNode(leftMost, leftMost.leftExtent, leftMost.topExtent + top)
      }
      assert(nodesToLayout.isEmpty()) { throw IllegalArgumentException("The layout requested has a partition") }
      return changed
    }

    private fun Collection<DiagramNode<T>>.highestAvailablePos(x: ClosedFloatingPointRange<Double>): Double {
      val consideredNodes = filter { node ->
        x overlaps (node.left - minHorizSeparation)..(node.right + minHorizSeparation)
      }

      val lowestNode = consideredNodes.maxBy { it.bottom }
      if (lowestNode!=null) {
        layoutStepper.reportLowest(consideredNodes, lowestNode)
        return lowestNode.bottom + vertSeparation
      }
      return Double.NEGATIVE_INFINITY
    }

    fun DiagramNode<T>.moveTo(x: Double, y:Double, nodes: List<DiagramNode<T>>) {
      setPos(x, y)
      layoutStepper.reportMove(this, x, y)
      val horizExRange = this.horizExRange
      val vertExRange = this.vertExRange
      for (rightNode in rightNodes) {
        if (rightNode.horizRange overlaps horizExRange && rightNode.vertRange overlaps vertExRange) {
          rightNode.moveTo(right+horizSeparation+rightNode.leftExtent, rightNode.y, nodes)
        }
      }
      for (node in nodes) {
        if (node.horizRange overlaps horizExRange && node.y > y && node.vertRange overlaps vertExRange) {
          node.moveTo(node.x, bottom+vertSeparation+node.topExtent, nodes)
        }
      }
    }

  }

  override fun <T : Positioned> layout(nodes: List<DiagramNode<T>>, layoutStepper: LayoutStepper<T>): Boolean {

    return super.layout(nodes, layoutStepper)
  }

  override fun <T : Positioned> layoutPartition(nodes: List<DiagramNode<T>>,
                                                topY: Double,
                                                layoutStepper: LayoutStepper<T>): Boolean {
    return LayoutInstance(layoutStepper).layout(nodes, topY)
  }

  /**
   * The horizontal exclusion range of the node
   */
  val DiagramNode<*>.horizExRange get() = (left-minHorizSeparation)..(right+minHorizSeparation)

  /**
   * The vertical exclusion range of the node
   */
  val DiagramNode<*>.vertExRange get() = (top-minVertSeparation)..(bottom+minVertSeparation)


}

infix fun <T: Comparable<T>> ClosedFloatingPointRange<T>.overlaps(other: ClosedFloatingPointRange<T>):Boolean {
  return when {
    start < other.start -> endInclusive >= other.start
    start <= other.endInclusive -> true
    else -> false
  }
}

fun DiagramNode<*>.setPos(x: Double, y: Double): Boolean {
  if (this.x != x || this.y != y) {
    this.x = x
    this.y = y
    return true
  }
  return false
}

/**
 * The horizontal coordinate range of the node
 */
val DiagramNode<*>.horizRange get() = left..right

/**
 * The vertical coordinate range of the node
 */
val DiagramNode<*>.vertRange get() = top..bottom
