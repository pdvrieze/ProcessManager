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

import nl.adaptivity.diagram.*
import nl.adaptivity.process.processModel.MutableProcessNode
import nl.adaptivity.process.processModel.ProcessNode

typealias DrawableState = Int

interface DrawableProcessNode : MutableProcessNode<DrawableProcessNode, DrawableProcessModel?>, Drawable {

  interface Builder : ProcessNode.Builder<DrawableProcessNode, DrawableProcessModel?> {
    override fun build(newOwner: DrawableProcessModel?): DrawableProcessNode

    var isCompat: Boolean

    var state: DrawableState
  }

  //  void setLabel(@Nullable String label);

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?, left: Double, top: Double)

  val isCompat: Boolean

  /**
   * Set the X coordinate of the reference point of the element. This is
   * normally the center.

   * @param x The x coordinate
   */
  @Deprecated("Use builders")
  fun setX(x: Double)

  /**
   * Set the Y coordinate of the reference point of the element. This is
   * normally the center of the symbol (excluding text).

   * @param y
   */
  @Deprecated("Use builders")
  fun setY(y: Double)

  /** Get the base to use for generating ID's.  */
  override val idBase: String

  override fun clone(): DrawableProcessNode

  override val ownerModel: DrawableProcessModel?

  override fun builder(): Builder

  override fun setPos(left: Double, top: Double)  {
    setX(left + leftExtent)
    setY(top + topExtent)
  }

  override fun getItemAt(x: Double, y: Double) = if (isWithinBounds(x, y)) this else null
}
