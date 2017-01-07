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
import nl.adaptivity.process.clientProcessModel.ClientProcessNode

typealias DrawableState = Int

interface DrawableProcessNode : ClientProcessNode<DrawableProcessNode, DrawableProcessModel?>, Drawable {

  interface Builder : ClientProcessNode.Builder<DrawableProcessNode, DrawableProcessModel?> {
    override fun build(newOwner: DrawableProcessModel?): DrawableProcessNode

    var state: DrawableState
  }

  //  void setLabel(@Nullable String label);

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?, left: Double, top: Double)

  /** Get the base to use for generating ID's.  */
  override val idBase: String

  override fun clone(): DrawableProcessNode

  override val ownerModel: DrawableProcessModel?

  fun isWithinBounds(x: Double, y: Double): Boolean

  override fun builder(): Builder
}
