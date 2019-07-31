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
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.util.multiplatform.JvmDefault

/**
 * Drawable version of the process model.
 */
interface DrawableProcessModel : ProcessModel<DrawableProcessNode> {
    interface Builder : ProcessModel.Builder, IDrawableProcessModel {
        var layoutAlgorithm: LayoutAlgorithm

        override val nodes: MutableList<ProcessNode.Builder>

        override val childElements: List<DrawableProcessNode.Builder<*>>

        override val rootBuilder: RootDrawableProcessModel.Builder

        @JvmDefault
        override fun compositeActivityBuilder(): Activity.CompositeActivityBuilder {
            TODO("DrawableChildModels still need to be implemented")
        }

        override fun getNode(nodeId: String): DrawableProcessNode.Builder<*>? {
            return super.getNode(nodeId) as DrawableProcessNode.Builder<*>?
        }

        fun hasUnpositioned() = nodes.any { !(it as Positioned).hasPos() }

        fun layout(layoutStepper: LayoutStepper<DrawableProcessNode.Builder<*>> = AbstractLayoutStepper())


        fun notifyNodeChanged(node: DrawableProcessNode) = Unit

    }

    fun builder(): Builder

    val layoutAlgorithm: LayoutAlgorithm

    override val modelNodes: List<DrawableProcessNode>

    override val rootModel: RootDrawableProcessModel
}
