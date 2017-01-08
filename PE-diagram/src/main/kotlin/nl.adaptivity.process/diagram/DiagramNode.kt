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

package nl.adaptivity.process.diagram

import nl.adaptivity.diagram.Bounded
import nl.adaptivity.diagram.HasExtent
import nl.adaptivity.diagram.Positioned

@Suppress("NOTHING_TO_INLINE")
inline fun <T> DiagramNode(orig: DiagramNode<T>,
                           x: Double = orig.x,
                           y: Double = orig.y,
                           target: T = orig.target,
                           leftExtent: Double = orig.leftExtent,
                           rightExtent: Double = orig.rightExtent,
                           topExtent: Double = orig.topExtent,
                           bottomExtent: Double = orig.bottomExtent) =
  DiagramNode(target, x, y, leftExtent, rightExtent, topExtent, bottomExtent)


fun <T> DiagramNode(target: T) : DiagramNode<T> where T: HasExtent, T: Positioned {
  return DiagramNode(target= target, x=target.x, y=target.y,
                        leftExtent = target.leftExtent, rightExtent = target.rightExtent,
                        topExtent = target.topExtent, bottomExtent = target.bottomExtent)
}


fun <T : Positioned> DiagramNode(positionedTarget: T,
                                 leftExtent: Double,
                                 rightExtent: Double,
                                 topExtent: Double,
                                 bottomExtent: Double,
                                 x: Double = positionedTarget.x,
                                 y: Double = positionedTarget.y): DiagramNode<T> {
  return DiagramNode(target = positionedTarget,
                     leftExtent = leftExtent,
                     rightExtent = rightExtent,
                     topExtent = topExtent,
                     bottomExtent = bottomExtent,
                     x = positionedTarget.x,
                     y = positionedTarget.y)
}

fun <T : Bounded> DiagramNode(boundedTarget: T,
                              x: Double = boundedTarget.x,
                              y: Double = boundedTarget.y,
                              leftExtent: Double = boundedTarget.leftExtent,
                              rightExtent: Double = boundedTarget.rightExtent,
                              topExtent: Double = boundedTarget.topExtent,
                              bottomExtent: Double = boundedTarget.bottomExtent): DiagramNode<T> {
  return DiagramNode<T>(target = boundedTarget,
                        x= x,
                        y= y,
                        leftExtent = leftExtent,
                        rightExtent = rightExtent,
                        topExtent = topExtent,
                        bottomExtent = bottomExtent)
}

fun <T : HasExtent> DiagramNode(extentTarget: T,
                                x: Double,
                                y: Double,
                              leftExtent: Double = extentTarget.leftExtent,
                              rightExtent: Double = extentTarget.rightExtent,
                              topExtent: Double = extentTarget.topExtent,
                              bottomExtent: Double = extentTarget.bottomExtent): DiagramNode<T> {
  return DiagramNode(target = extentTarget,
                     x = x,
                     y = y,
                     leftExtent = leftExtent,
                     rightExtent = rightExtent,
                     topExtent = topExtent,
                     bottomExtent = bottomExtent)
}


/**
 * Class that represents a single node in the diagram. The node represents an element with extents, and a logical point
 * position. It is important to realise that the layout algorithm assumes that nodes are part of a digraph (that may
 * have multiple disconnected groups.
 *
 * @param T               The type of the item for layout, or any handle to it.
 * @property leftExtent   Get the size to the left of the gravity point.
 * @property rightExtent  Get the size to the right of the gravity point.
 * @property topExtent    Get the size to the top of the gravity point.
 * @property bottomExtent Get the size to the bottom of the gravity point.
 * @property target       The node that is layed out
 * @property x            The x coordinate of the item
 * @property y            The y coordinate of the item
 * @property leftNodes    The nodes that are logically smaller than the current one in the partial order/digraph
 * @property rightNodes   The nodes that are logically larger than the current one in the partial order/digraph
 */
class DiagramNode<out T>(val target: T, override var x:Double, override var y: Double, val leftExtent: Double, val rightExtent: Double, val topExtent: Double, val bottomExtent: Double) : Positioned {
//
//  constructor(orig: DiagramNode<T>, x: Double = orig.x, y: Double = orig.y) :
//    this(target = orig.target, x = x, y = y,
//         leftExtent = orig.leftExtent, rightExtent = orig.rightExtent,
//         topExtent = orig.topExtent, bottomExtent = orig.bottomExtent)

  val leftNodes: MutableList<DiagramNode<@UnsafeVariance T>> = mutableListOf()

  val rightNodes: MutableList<DiagramNode<@UnsafeVariance T>> = mutableListOf()

  val left get() = x - leftExtent

  val right get() = x + rightExtent

  val top get() = y - topExtent

  val bottom get() = y + bottomExtent

  @Deprecated("As these nodes are for modfying the positions they don't need this function")
  fun withX(x: Double) = DiagramNode(orig=this, x=x)

  @Deprecated("As these nodes are for modfying the positions they don't need this function")
  fun withY(y: Double) = DiagramNode(orig=this, y=y)

  /**
   *  Determine whether the region overlaps this node and is not positioned to its right.
   */
  fun leftOverlaps(region: DiagramNode<*>, xSep: Double, ySep: Double): Boolean {
    return overlaps(region, left - xSep, top - ySep, right + xSep, bottom + ySep) && region.x < x
  }

  fun rightOverlaps(region: DiagramNode<*>, xSep: Double, ySep: Double): Boolean {
    return overlaps(region, left - xSep, top - ySep, right + xSep, bottom + ySep) && region.x > x
  }

  fun upOverlaps(region: DiagramNode<*>, xSep: Double, ySep: Double): Boolean {
    return overlaps(region, left - xSep, top - ySep, right + xSep, bottom + ySep) && region.y < y
  }

  fun downOverlaps(region: DiagramNode<*>, xSep: Double, ySep: Double): Boolean {
    return overlaps(region, left - xSep, top - ySep, right + xSep, bottom + ySep) && region.y > y
  }

  private fun overlaps(region: DiagramNode<*>, left: Double, top: Double, right: Double, bottom: Double): Boolean {
    return region.right > left &&
           region.left < right &&
           region.top < bottom &&
           region.bottom > top
  }

  override fun hasPos() = x.isFinite() && y.isFinite()

  override fun toString() = buildString {
    append("[$target ")

    if (x.isFinite()) append("x=$x, ")

    if (y.isFinite()) {
      append("y=$y - ")
    } else {
      if (x.isFinite()) append(" - ")
    }

    val effX = if (x.isNaN()) 0.0 else x
    val effY = if (y.isNaN()) 0.0 else y
    append("((${effX - leftExtent}, ${effY - topExtent}),(${effX + rightExtent}, ${effY + bottomExtent}))")
  }
}
