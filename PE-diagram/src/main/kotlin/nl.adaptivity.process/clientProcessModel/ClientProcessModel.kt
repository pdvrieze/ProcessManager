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

package nl.adaptivity.process.clientProcessModel

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.diagram.Bounded
import nl.adaptivity.process.diagram.DiagramNode
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.LayoutAlgorithm
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import java.security.Principal
import java.util.*

interface ClientProcessModel : ProcessModel<DrawableProcessNode, DrawableProcessModel?> {
  interface Builder : ProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?> {

  }

  override val rootModel: RootClientProcessModel?

  var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

  fun layout()

}

abstract class RootClientProcessModel : RootProcessModelBase<DrawableProcessNode, DrawableProcessModel?>, MutableRootProcessModel<DrawableProcessNode, DrawableProcessModel?> {

  abstract class Builder : RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>, ClientProcessModel.Builder {
    var  layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

    constructor(): this(nodes= mutableSetOf())

    constructor(
      nodes: MutableSet<ProcessNode.Builder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
      childModels: MutableSet<ChildProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?>> = mutableSetOf(),
      name: String? = null,
      handle: Long = -1L,
      owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: MutableList<String> = mutableListOf<String>(),
      uuid: UUID? = null,
      imports: MutableList<IXmlResultType> = mutableListOf<IXmlResultType>(),
      exports: MutableList<IXmlDefineType> = mutableListOf<IXmlDefineType>(),
      layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode> = LayoutAlgorithm()) : super(nodes, childModels, name, handle, owner, roles, uuid, imports, exports) {
      this.layoutAlgorithm = layoutAlgorithm
    }

    constructor(base: RootProcessModel<*,*>) : super(base) {
      this.layoutAlgorithm = (base as? ClientProcessModel)?.layoutAlgorithm ?: LayoutAlgorithm<DrawableProcessNode>()
    }

    abstract override fun build(pedantic: Boolean): RootProcessModelBase<DrawableProcessNode, DrawableProcessModel?>
  }

  override val rootModel: RootClientProcessModel get() = this

  var layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode>

  @JvmOverloads constructor(uuid: UUID? = null, name: String? = null, nodes: Collection<DrawableProcessNode> = emptyList(), layoutAlgorithm: LayoutAlgorithm<DrawableProcessNode> = LayoutAlgorithm<DrawableProcessNode>(), nodeFactory: NodeFactory<DrawableProcessNode, DrawableProcessModel?>) :
    super(nodes, uuid = uuid ?: UUID.randomUUID(), name = name, nodeFactory = nodeFactory) {
    this.layoutAlgorithm = layoutAlgorithm
  }

  @JvmOverloads
  constructor(builder: RootProcessModelBase.Builder<DrawableProcessNode, DrawableProcessModel?>, nodeFactory: NodeFactory<DrawableProcessNode, DrawableProcessModel?>, pedantic: Boolean = builder.defaultPedantic) : super(builder, nodeFactory, pedantic) {
    this.layoutAlgorithm = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()
  }

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

  var isInvalid = false
    private set

  abstract fun asNode(id: Identifiable): DrawableProcessNode?

  override abstract fun builder(): Builder

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  fun normalize(): DrawableProcessModel? {
    return builder().apply { normalize(false) }.build().asM
  }

  open fun setNodes(nodes: Collection<DrawableProcessNode>) {
    super.setModelNodes(IdentifyableSet.processNodeSet(nodes))
    invalidate()
  }

  var vertSeparation: Double
    get() = layoutAlgorithm.vertSeparation
    set(vertSeparation) {
      if (layoutAlgorithm.vertSeparation != vertSeparation) {
        invalidate()
      }
      layoutAlgorithm.vertSeparation = vertSeparation
    }


  var horizSeparation: Double
    get() = layoutAlgorithm.horizSeparation
    set(horizSeparation) {
      if (layoutAlgorithm.horizSeparation != horizSeparation) {
        invalidate()
      }
      layoutAlgorithm.horizSeparation = horizSeparation
    }

  var defaultNodeWidth: Double
    get() = layoutAlgorithm.defaultNodeWidth
    set(defaultNodeWidth) {
      if (layoutAlgorithm.defaultNodeWidth != defaultNodeWidth) {
        invalidate()
      }
      layoutAlgorithm.defaultNodeWidth = defaultNodeWidth
    }


  var defaultNodeHeight: Double
    get() = layoutAlgorithm.defaultNodeHeight
    set(defaultNodeHeight) {
      if (layoutAlgorithm.defaultNodeHeight != defaultNodeHeight) {
        invalidate()
      }
      layoutAlgorithm.defaultNodeHeight = defaultNodeHeight
    }


  open fun invalidate() {
    isInvalid = true
  }

  fun resetLayout() {
    for (n in modelNodes) {
      n.setX(Double.NaN)
      n.setY(Double.NaN)
    }
    invalidate()
  }

  val endNodeCount: Int
    get() {
      var i = 0
      for (node in modelNodes) {
        if (node is EndNode<*, *>) {
          ++i
        }
      }
      return i
    }

  override fun getRef(): IProcessModelRef<DrawableProcessNode, DrawableProcessModel?, out @JvmWildcard RootClientProcessModel> {
    throw UnsupportedOperationException("Not implemented")
  }

  override fun getHandle(): Handle<out @JvmWildcard RootClientProcessModel> {
    return Handles.handle(handleValue)
  }

  fun getNode(nodeId: String): DrawableProcessNode? {
    for (n in modelNodes) {
      if (nodeId == n.id) {
        return n
      }
    }
    return null
  }

  fun setOwner(owner: String) {
    this.owner = SimplePrincipal(owner)
  }

  val startNodes: Collection<ClientStartNode>
    get() {
      val result = ArrayList<ClientStartNode>()
      for (n in modelNodes) {
        if (n is ClientStartNode) {
          result.add(n as ClientStartNode)
        }
      }
      return result
    }

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
    (oldValue as ClientProcessNode).setOwnerModel(null)

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
    if (node == null) {
      return false
    }
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

    const val NS_UMH = "http://adaptivity.nl/userMessageHandler"

    const val NS_PM = "http://adaptivity.nl/ProcessEngine/"

    internal const val PROCESSMODEL_NS = NS_PM
  }

}
