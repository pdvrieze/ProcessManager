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

import nl.adaptivity.diagram.HasExtent
import nl.adaptivity.diagram.Positioned

import java.util.ArrayList

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Positioned> DiagramNode(orig: DiagramNode<T>,
                                        x: Double = orig.x,
                                        y: Double = orig.y,
                                        target: T = orig.target,
                                        leftExtent: Double = orig.leftExtent,
                                        rightExtent: Double = orig.rightExtent,
                                        topExtent: Double = orig.topExtent,
                                        bottomExtent: Double = orig.bottomExtent) =
  DiagramNode(target, x, y, leftExtent, rightExtent, topExtent, bottomExtent)


fun <T> DiagramNode(target: T) : DiagramNode<T> where T: HasExtent, T: Positioned {
  return DiagramNode<T>(target, target.x, target.y, target.leftExtent, target.rightExtent, target.topExtent, target.bottomExtent)
}


/**
 * @property leftExtent   Get the size to the left of the gravity point.
 * @property rightExtent  Get the size to the right of the gravity point.
 * @property topExtent    Get the size to the top of the gravity point.
 * @property bottomExtent Get the size to the bottom of the gravity point.
 */
class DiagramNode<out T : Positioned>(val target: T, override var x:Double = 0.0, override var y: Double = 0.0, val leftExtent: Double, val rightExtent: Double, val topExtent: Double, val bottomExtent: Double) : Positioned {


  val leftNodes: MutableList<DiagramNode<@UnsafeVariance T>> = mutableListOf()

  val rightNodes: MutableList<DiagramNode<@UnsafeVariance T>> = mutableListOf()

  val left get() = x - leftExtent

  val right get() = x + rightExtent

  val top get() = y - topExtent

  val bottom get() = y + bottomExtent

  constructor(target: T, leftExtent: Double, rightExtent: Double, topExtent: Double, bottomExtent: Double):
    this(target, target.x, target.y, leftExtent, rightExtent, topExtent, bottomExtent)

  fun withX(x: Double) = DiagramNode(this, x, y)

  fun withY(y: Double) = DiagramNode(this, x, y)

  /** Determine whether the region overlaps this node and is not positioned to its right.  */
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
