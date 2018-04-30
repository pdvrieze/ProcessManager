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

@file:Suppress("DEPRECATION")

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.diagram.*
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.modelNodes
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.JvmOverloads

abstract class RootClientProcessModel @JvmOverloads constructor(builder: RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>,
                                                                    nodeFactory: NodeFactory<DrawableProcessNode, DrawableProcessModel?>,
                                                                    pedantic: Boolean = builder.defaultPedantic)
  : RootProcessModelBase<DrawableProcessNode, DrawableProcessModel?>(builder, nodeFactory, pedantic),
    RootProcessModel<DrawableProcessNode, DrawableProcessModel?> {

  abstract val layoutAlgorithm: LayoutAlgorithm

  val topPadding = (builder as? DrawableProcessModel.Builder)?.topPadding ?: 5.0

  val leftPadding = (builder as? DrawableProcessModel.Builder)?.leftPadding ?: 5.0

  var bottomPadding = (builder as? DrawableProcessModel.Builder)?.bottomPadding ?: 5.0

  var rightPadding = (builder as? DrawableProcessModel.Builder)?.rightPadding ?: 5.0

  abstract val isInvalid:Boolean

  override abstract fun builder(): RootDrawableProcessModel.Builder


  abstract fun invalidate()

  @Deprecated("Use the version taking an identifier", ReplaceWith("getNode(Identifier(nodeId))", "nl.adaptivity.process.util.Identifier"))
  override fun getNode(nodeId: String) = getNode(Identifier(nodeId))

  val startNodes: Collection<DrawableStartNode>
    get() = modelNodes.filterIsInstance<DrawableStartNode>()

  private fun toDiagramNodes(modelNodes: Collection<DrawableProcessNode>): List<DiagramNode<DrawableProcessNode>> {
    val nodeMap = HashMap<Identified, DiagramNode<DrawableProcessNode>>()
    val result = modelNodes.map { node  ->
      DiagramNode(node).apply { node.identifier?.let { nodeMap[it] = this } ?: Unit }
    }

    for (diagramNode in result) {
      val modelNode = diagramNode.target
      modelNode.successors.asSequence()
          .map { nodeMap[it] }
          .filterNotNullTo(diagramNode.rightNodes)

      modelNode.predecessors.asSequence()
          .map { nodeMap[it] }
          .filterNotNullTo(diagramNode.leftNodes)
    }
    return result
  }

  companion object {

    const val NS_JBI = "http://adaptivity.nl/ProcessEngine/activity"

    const val NS_UMH = Constants.USER_MESSAGE_HANDLER_NS

    const val NS_PM = ProcessConsts.Engine.NAMESPACE

    internal const val PROCESSMODEL_NS = NS_PM
  }

}
