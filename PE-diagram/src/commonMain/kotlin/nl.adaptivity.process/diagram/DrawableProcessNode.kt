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

import nl.adaptivity.diagram.Drawable
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode

typealias DrawableState = Int


interface DrawableProcessNode : ProcessNode {

    open class Delegate(builder: ProcessNode.IBuilder) {

        var state: Int = (builder as? DrawableProcessNode.Builder<*>)?.state ?: Drawable.STATE_DEFAULT
        var isCompat: Boolean = (builder as? DrawableProcessNode.Builder<*>)?.isCompat ?: false

    }

    interface Builder<out R : DrawableProcessNode> : ProcessNode.IBuilder, IDrawableProcessNode {

        class Delegate(var state: Int, var isCompat: Boolean) {
            constructor(node: ProcessNode) :
                this((node as? Drawable)?.state ?: Drawable.STATE_DEFAULT,
                     (node as? DrawableProcessNode)?.isCompat ?: false
                    )
        }

        val _delegate: Delegate

        var isCompat: Boolean
            get() = _delegate.isCompat
            set(value) {
                _delegate.isCompat = value
            }

        override var state: DrawableState
            get() = _delegate.state
            set(value) {
                _delegate.state = value
            }

        override fun copy(): Builder<R>

        override fun translate(dX: Double, dY: Double) {
            x += dX
            y += dY
        }
    }

    //  void setLabel(@Nullable String label);

    val _delegate: Delegate

    val isCompat: Boolean get() = _delegate.isCompat

    override val maxSuccessorCount: Int
    override val maxPredecessorCount: Int
/*

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?, left: Double, top: Double) {
    // TODO implement a proper drawLabel system. Perhaps that needs more knowledge of node bounds
  }
*/

    override fun builder(): Builder<DrawableProcessNode>

}
