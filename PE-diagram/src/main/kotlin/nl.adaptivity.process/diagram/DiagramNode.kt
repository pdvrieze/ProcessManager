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

import nl.adaptivity.diagram.Positioned

import java.util.ArrayList

/**
 * @property leftExtend   Get the size to the left of the gravity point.
 * @property rightExtend  Get the size to the right of the gravity point.
 * @property topExtend    Get the size to the top of the gravity point.
 * @property bottomExtend Get the size to the bottom of the gravity point.
 */
class DiagramNode<T : Positioned>(val target: T, private var x:Double = 0.0, private var y: Double = 0.0, val leftExtend: Double, val rightExtend: Double, val topExtend: Double, val bottomExtend: Double) : Positioned {

  val leftNodes: MutableList<DiagramNode<T>> = mutableListOf()

  val rightNodes: MutableList<DiagramNode<T>> = mutableListOf()

  val left get() = x - leftExtend

  val right get() = x + rightExtend

  val top get() = y - topExtend

  val bottom get() = y + bottomExtend

  constructor(target: T, leftExtend: Double, rightExtend: Double, topExtend: Double, bottomExtend: Double):
    this(target, target.x, target.y, leftExtend, rightExtend, topExtend, bottomExtend)

  private constructor(diagramNode: DiagramNode<T>, x: Double, y: Double):
    this(diagramNode.target, x, y, diagramNode.leftExtend, diagramNode.rightExtend, diagramNode.topExtend, diagramNode.bottomExtend)

  fun withX(x: Double) = DiagramNode(this, x, y)

  fun withY(y: Double) = DiagramNode(this, x, y)

  fun setX(x: Double) {
    this.x = x
  }

  override fun getX() = x

  fun setY(y: Double) {
    this.y = y
  }

  override fun getY() = y

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

  override fun toString(): String {
    return buildString {
      append('[')
      target?.let { append(it).append(' ')}
      if (x.isFinite()) append("x=$x, ")

      if (y.isFinite()) {
        append("y=$y - ")
      } else {
        if (x.isFinite()) append(" - ")
      }

      val effX = if (x.isNaN()) 0.0 else x
      val effY = if (y.isNaN()) 0.0 else y
      append("((${effX - leftExtend}, ${effY - topExtend}),(${effX + rightExtend}, ${effY + bottomExtend}))")
    }
  }
}
