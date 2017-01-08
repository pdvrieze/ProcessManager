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

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.processModel.MutableProcessNode
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet


interface ClientProcessNode : MutableProcessNode<DrawableProcessNode, DrawableProcessModel?> {

    interface Builder : ProcessNode.Builder<DrawableProcessNode, DrawableProcessModel?> {

        var isCompat: Boolean

    }

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

    fun setLabel(label: String?)

}
