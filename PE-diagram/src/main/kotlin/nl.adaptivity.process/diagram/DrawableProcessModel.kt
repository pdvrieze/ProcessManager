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

/**
 * Drawable version of the process model.
 */
interface DrawableProcessModel : ProcessModel<DrawableProcessNode, DrawableProcessModel?>, IDrawableProcessModel {
  interface Builder : ProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?>, IDrawableProcessModel {
    var  layoutAlgorithm: LayoutAlgorithm

    override val nodes: MutableList<ProcessNode.IBuilder<DrawableProcessNode, DrawableProcessModel?>>

    override val childElements: List<DrawableProcessNode.Builder>

    override fun compositeActivityBuilder(): Activity.ChildModelBuilder<DrawableProcessNode, DrawableProcessModel?> {
      TODO("DrawableChildModels still need to be implemented")
    }

    override fun getNode(nodeId: String): DrawableProcessNode.Builder? {
      return super.getNode(nodeId) as DrawableProcessNode.Builder?
    }

    fun hasUnpositioned() = nodes.any { !(it as Positioned).hasPos }

    fun build(): DrawableProcessModel

    fun layout(layoutStepper: LayoutStepper<DrawableProcessNode.Builder> = AbstractLayoutStepper())
  }

  fun builder(): Builder

  val layoutAlgorithm: LayoutAlgorithm

  override val x: Double get() = 0.0

  override val y: Double get() = 0.0

  override fun getModelNodes(): List<DrawableProcessNode>

  override val childElements: List<DrawableProcessNode> get() = getModelNodes()

  override val rootModel: RootDrawableProcessModel

  fun notifyNodeChanged(node: DrawableProcessNode) = Unit
}