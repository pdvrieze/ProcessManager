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
import nl.adaptivity.process.diagram.LayoutAlgorithm
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import java.security.Principal
import java.util.*

interface ClientProcessModel<NodeT: @JvmWildcard ClientProcessNode<NodeT,ModelT>, ModelT: @kotlin.jvm.JvmWildcard ClientProcessModel<NodeT,ModelT>?> : ProcessModel<NodeT, ModelT> {
  interface Builder<NodeT: @JvmWildcard ClientProcessNode<NodeT,ModelT>, ModelT: @JvmWildcard ClientProcessModel<NodeT,ModelT>?> : ProcessModel.Builder<NodeT, ModelT> {

  }

  val layoutAlgorithm: LayoutAlgorithm<NodeT>
}

abstract class RootClientProcessModel<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> :
    RootProcessModelBase<NodeT, ModelT>, MutableRootProcessModel<NodeT, ModelT> {

  var layoutAlgorithm: LayoutAlgorithm<NodeT>

  @JvmOverloads constructor(uuid: UUID? = null, name: String? = null, nodes: Collection<NodeT> = emptyList(), layoutAlgorithm: LayoutAlgorithm<NodeT> = LayoutAlgorithm<NodeT>(), nodeFactory: (ProcessModel<NodeT,ModelT>, ProcessNode<*, *>) -> NodeT) :
    super(nodes, uuid = uuid ?: UUID.randomUUID(), name = name, nodeFactory = nodeFactory) {
    this.layoutAlgorithm = layoutAlgorithm
  }

  @JvmOverloads
  constructor(builder: RootProcessModelBase.Builder<NodeT, ModelT>, pedantic: Boolean = false) : super(builder, pedantic) {
    this.layoutAlgorithm = (builder as? Builder)?.layoutAlgorithm ?: LayoutAlgorithm()
  }

  abstract class Builder<T : ClientProcessNode<T, M>, M : ClientProcessModel<T, M>?> : RootProcessModelBase.Builder<T,M>, ClientProcessModel.Builder<T,M> {
    var  layoutAlgorithm: LayoutAlgorithm<T>

    constructor(): this(nodes= mutableSetOf())

    constructor(
        nodes: MutableSet<ProcessNode.Builder<T, M>> = mutableSetOf(),
        name: String? = null,
        handle: Long = -1L,
        owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
        roles: MutableList<String> = mutableListOf<String>(),
        uuid: UUID? = null,
        imports: MutableList<IXmlResultType> = mutableListOf<IXmlResultType>(),
        exports: MutableList<IXmlDefineType> = mutableListOf<IXmlDefineType>(),
        layoutAlgorithm: LayoutAlgorithm<T> = LayoutAlgorithm()) : super(nodes, name, handle, owner, roles, uuid, imports, exports) {
      this.layoutAlgorithm = layoutAlgorithm
    }

    constructor(base: RootProcessModel<*,*>) : super(base) {
      this.layoutAlgorithm = (base as? ClientProcessModel<T,M>)?.layoutAlgorithm ?: LayoutAlgorithm<T>()
    }

    abstract override fun build(pedantic: Boolean): RootProcessModelBase<T, M>
  }

  var topPadding = 5.0
    set(topPadding) {
      val offset = topPadding - this.topPadding
      for (n in modelNodes) {
        n.y = n.y + offset
      }
      field = topPadding
    }

  var leftPadding = 5.0
    set(leftPadding) {
      val offset = leftPadding - this.leftPadding
      for (n in modelNodes) {
        n.x = n.x + offset
      }
      field = leftPadding
    }

  var bottomPadding = 5.0

  var rightPadding = 5.0

  var isInvalid = false
    private set

  abstract fun asNode(id: Identifiable): NodeT?

  override abstract fun builder(): Builder<NodeT, ModelT>

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  fun normalize(): ModelT {
    return builder().apply { normalize(false) }.build().asM
  }

  open fun setNodes(nodes: Collection<NodeT>) {
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
      n.x = java.lang.Double.NaN
      n.y = java.lang.Double.NaN
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

  override fun getRef(): IProcessModelRef<NodeT, ModelT, out @JvmWildcard RootClientProcessModel<NodeT,ModelT>> {
    throw UnsupportedOperationException("Not implemented")
  }

  override fun getHandle(): Handle<out @JvmWildcard RootClientProcessModel<NodeT, ModelT>> {
    return Handles.handle(handleValue)
  }

  fun getNode(nodeId: String): NodeT? {
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

  val startNodes: Collection<ClientStartNode<out NodeT, ModelT>>
    get() {
      val result = ArrayList<ClientStartNode<out NodeT, ModelT>>()
      for (n in modelNodes) {
        if (n is ClientStartNode<*, *>) {
          result.add(n as ClientStartNode<out NodeT, ModelT>)
        }
      }
      return result
    }

  override fun addNode(node: NodeT): Boolean {
    if (super.addNode(node)) {
      node.setOwnerModel(asM())
      // Make sure that children can know of the change.
      notifyNodeChanged(node)
      return true
    }
    return false
  }

  @Deprecated("Unsafe")
  override fun setNode(pos: Int, newValue: NodeT): NodeT {
    val oldValue = setNode(pos, newValue)

    newValue.setOwnerModel(asM())
    oldValue.setSuccessors(emptySet<Identified>())
    oldValue.setPredecessors(emptySet<Identified>())
    oldValue.setOwnerModel(null)

    for (pred in newValue.predecessors) {
      getNode(pred)!!.addSuccessor(newValue.identifier!!)
    }
    for (suc in newValue.successors) {
      getNode(suc)!!.addPredecessor(newValue.identifier!!)
    }

    return oldValue
  }

  override fun removeNode(nodePos: Int): NodeT {
    val node = super.removeNode(nodePos)
    disconnectNode(node)
    return node
  }

  override fun removeNode(node: NodeT): Boolean {
    if (node == null) {
      return false
    }
    if (super.removeNode(node)) {
      disconnectNode(node)
      return true
    }
    return false
  }

  private fun disconnectNode(node: NodeT) {
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
        n.target.x = n.x + leftPadding
        n.target.y = n.y + topPadding
        maxX = Math.max(n.right, maxX)
        maxY = Math.max(n.bottom, maxY)
      }
    }
  }

  private fun toDiagramNodes(modelNodes: Collection<NodeT>): List<DiagramNode<NodeT>> {
    val map = HashMap<Identified, DiagramNode<NodeT>>()
    val result = ArrayList<DiagramNode<NodeT>>()
    for (node in modelNodes) {
      val leftExtend: Double
      val rightExtend: Double
      val topExtend: Double
      val bottomExtend: Double
      if (node is Bounded) {
        val tempCoords = java.lang.Double.isNaN(node.x) || java.lang.Double.isNaN(node.y)
        var tmpX = 0.0
        var tmpY = 0.0
        if (tempCoords) {
          tmpX = node.x
          node.x = 0.0
          tmpY = node.y
          node.y = 0.0
          isInvalid = true // we need layout as we have undefined coordinates.
        }
        val bounds = node.bounds
        leftExtend = node.x - bounds.left
        rightExtend = bounds.right() - node.x
        topExtend = node.y - bounds.top
        bottomExtend = bounds.bottom() - node.y
        if (tempCoords) {
          node.x = tmpX
          node.y = tmpY
        }
      } else {
        rightExtend = layoutAlgorithm.defaultNodeWidth / 2
        leftExtend = rightExtend
        bottomExtend = layoutAlgorithm.defaultNodeHeight / 2
        topExtend = bottomExtend
      }
      val dn = DiagramNode(node, leftExtend, rightExtend, topExtend, bottomExtend)
      node.id?.let(::Identifier)?.let { map.put(it, dn) }
      result.add(dn)
    }

    for (diagramNode in result) {
      val modelNode = diagramNode.target
      modelNode.successors.asSequence()
          .map { map[it] }
          .filterNotNullTo(diagramNode.rightNodes)

      modelNode.predecessors.asSequence()
          .map { map[it] }
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
