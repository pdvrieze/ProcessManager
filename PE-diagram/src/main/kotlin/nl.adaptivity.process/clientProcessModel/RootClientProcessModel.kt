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

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.diagram.*
import nl.adaptivity.process.processModel.MutableRootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.modelNodes
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import java.util.*

@Suppress("OverridingDeprecatedMember")
abstract class RootClientProcessModel @JvmOverloads constructor(builder: RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>,
                                                                nodeFactory: NodeFactory<DrawableProcessNode, DrawableProcessModel?>,
                                                                pedantic: Boolean = builder.defaultPedantic) : RootProcessModelBase<DrawableProcessNode, DrawableProcessModel?>(
  builder, nodeFactory, pedantic), MutableRootProcessModel<DrawableProcessNode, DrawableProcessModel?> {

  abstract val layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

  var topPadding = 5.0
    set(topPadding) {
      val offset = topPadding - this.topPadding
      for (n in modelNodes) {
        n.setY(n.y + offset)
      }
      field = topPadding
    }

  var leftPadding = 5.0
    set(leftPadding) {
      val offset = leftPadding - this.leftPadding
      for (n in modelNodes) {
        n.setX(n.x + offset)
      }
      field = leftPadding
    }

  var bottomPadding = 5.0

  var rightPadding = 5.0

  abstract val isInvalid:Boolean

  override abstract fun builder(): RootDrawableProcessModel.Builder

  open fun setNodes(nodes: Collection<DrawableProcessNode>) {
    super.setModelNodes(nodes)
    invalidate()
  }


  abstract fun invalidate()

  @Deprecated("Use the version taking an identifier", ReplaceWith("getNode(Identifier(nodeId))", "nl.adaptivity.process.util.Identifier"))
  override fun getNode(nodeId: String) = getNode(Identifier(nodeId))

  fun setOwner(owner: String) {
    this.owner = SimplePrincipal(owner)
  }

  val startNodes: Collection<DrawableStartNode>
    get() = modelNodes.filterIsInstance<DrawableStartNode>()

  override fun addNode(node: DrawableProcessNode): Boolean {
    if (super.addNode(node)) {
      node.setOwnerModel(asM())
      // Make sure that children can know of the change.
      notifyNodeChanged(node)
      return true
    }
    return false
  }

  @Deprecated("Unsafe")
  override fun setNode(pos: Int, newValue: DrawableProcessNode): DrawableProcessNode {
    val oldValue = setNode(pos, newValue)

    newValue.setOwnerModel(asM())
    oldValue.setSuccessors(emptySet<Identified>())
    oldValue.setPredecessors(emptySet<Identified>())
    // TODO this is fundamentally unsafe, but we should get rid of [ClientProcessModel] anyway
    oldValue.setOwnerModel(null)

    for (pred in newValue.predecessors) {
      getNode(pred)!!.addSuccessor(newValue.identifier!!)
    }
    for (suc in newValue.successors) {
      getNode(suc)!!.addPredecessor(newValue.identifier!!)
    }

    return oldValue
  }

  override fun removeNode(nodePos: Int): DrawableProcessNode {
    val node = super.removeNode(nodePos)
    disconnectNode(node)
    return node
  }

  override fun removeNode(node: DrawableProcessNode): Boolean {
    if (super.removeNode(node)) {
      disconnectNode(node)
      return true
    }
    return false
  }

  private fun disconnectNode(node: DrawableProcessNode) {
    node.setPredecessors(emptyList<Identified>())
    node.setSuccessors(emptyList<Identified>())
    notifyNodeChanged(node)
  }

  open fun layout() {
    val diagramNodes = toDiagramNodes(modelNodes)
    if (layoutAlgorithm.layout(diagramNodes)) {
      var maxX = java.lang.Double.MIN_VALUE
      var maxY = java.lang.Double.MIN_VALUE
      for (n in diagramNodes) {
        n.target.setX(n.x + leftPadding)
        n.target.setY(n.y + topPadding)
        maxX = Math.max(n.right, maxX)
        maxY = Math.max(n.bottom, maxY)
      }
    }
  }

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
