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
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModelBase.NodeFactory
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.JvmOverloads
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal

abstract class RootClientProcessModel @JvmOverloads constructor(builder: RootProcessModelBase.Builder,
                                                                nodeFactory: NodeFactory<DrawableProcessNode, DrawableProcessNode, ChildProcessModelBase<DrawableProcessNode>>,
                                                                pedantic: Boolean)
    : RootProcessModelBase<DrawableProcessNode>(builder, nodeFactory, pedantic),
      RootProcessModel<DrawableProcessNode> {

    abstract val layoutAlgorithm: LayoutAlgorithm

    val topPadding = (builder as? DrawableProcessModel.Builder)?.topPadding ?: 5.0

    val leftPadding = (builder as? DrawableProcessModel.Builder)?.leftPadding ?: 5.0

    val bottomPadding = (builder as? DrawableProcessModel.Builder)?.bottomPadding ?: 5.0

    val rightPadding = (builder as? DrawableProcessModel.Builder)?.rightPadding ?: 5.0

    abstract val isInvalid: Boolean

    val startNodes: Collection<DrawableStartNode>
        get() = modelNodes.filterIsInstance<DrawableStartNode>()

    @Deprecated("Use full version", level = DeprecationLevel.HIDDEN)
    final     override fun copy(imports: Collection<IXmlResultType>,
                                exports: Collection<IXmlDefineType>,
                                nodes: Collection<ProcessNode>,
                                name: String?,
                                uuid: UUID?,
                                roles: Set<String>,
                                owner: Principal,
                                childModels: Collection<ChildProcessModel<DrawableProcessNode>>): RootDrawableProcessModel {
        return copy(imports, exports, nodes, name, uuid, roles, owner, childModels, this.handleValue, this.layoutAlgorithm)
    }

    abstract fun copy(imports: Collection<IXmlResultType> = this.imports,
                      exports: Collection<IXmlDefineType> = this.exports,
                      nodes: Collection<ProcessNode> = modelNodes,
                      name: String? = this.name,
                      uuid: UUID? = this.uuid,
                      roles: Set<String> = this.roles,
                      owner: Principal = this.owner,
                      childModels: Collection<ChildProcessModel<DrawableProcessNode>> = this.childModels,
                      handle: Long = this.handleValue,
                      layoutAlgorithm: LayoutAlgorithm = this.layoutAlgorithm): RootDrawableProcessModel


    override abstract fun builder(): RootDrawableProcessModel.Builder

    @Deprecated("Use the version taking an identifier",
                ReplaceWith("getNode(Identifier(nodeId))", "nl.adaptivity.process.util.Identifier"))
    override fun getNode(nodeId: String) = getNode(Identifier(nodeId))

    private fun toDiagramNodes(modelNodes: Collection<DrawableProcessNode.Builder<*>>): List<DiagramNode<DrawableProcessNode.Builder<*>>> {
        val nodeMap = HashMap<Identified, DiagramNode<DrawableProcessNode.Builder<*>>>()
        val result = modelNodes.map { node ->
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
