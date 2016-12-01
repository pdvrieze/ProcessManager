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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.clientProcessModel

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.diagram.Bounded
import nl.adaptivity.process.diagram.DiagramNode
import nl.adaptivity.process.diagram.LayoutAlgorithm
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.ProcessModelBase
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.IdentifyableSet
import java.util.*


abstract class ClientProcessModel<T : ClientProcessNode<T, M>, M : ClientProcessModel<T, M>> @JvmOverloads constructor(uuid: UUID? = null, name: String? = null, nodes: Collection<T> = emptyList(), var layoutAlgorithm: LayoutAlgorithm<T> = LayoutAlgorithm<T>(), nodeFactory: (M, ProcessNode<*, *>) -> T) :
    ProcessModelBase<T, M>(nodes, uuid = uuid ?: UUID.randomUUID(), name = name, nodeFactory = nodeFactory) {

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

  init {
    invalidate()
  }

  abstract fun asNode(id: Identifiable): T

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  fun normalize(splitFactory: ProcessModelBase.SplitFactory<out T, M>): M {
    ensureIds()
    // Make all nodes directly refer to other nodes.
    for (childNode in modelNodes) {
      childNode.resolveRefs()
    }
    for (childNode in modelNodes) {
      // Create a copy as we are actually going to remove all successors, but need to keep the list
      val successors = childNode.successors.map { getNode(it)?: throw NullPointerException("Missing node ${it}") }
      if (successors.size > 1 && childNode !is Split<*, *>) {
        for (suc2 in successors) { // Remove the current node as predecessor.
          suc2.removePredecessor(childNode)
          childNode.removeSuccessor(suc2) // remove the predecessor from the current node
        }
        // create a new join, this should
        val newSplit = splitFactory.createSplit(asM(), successors)
        childNode.addSuccessor(newSplit)
      }
    }
    return this.asM()
  }

  open fun setNodes(nodes: Collection<T>) {
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

  override fun getRef(): IProcessModelRef<T, M> {
    throw UnsupportedOperationException("Not implemented")
  }

  fun getNode(nodeId: String): T? {
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

  val startNodes: Collection<ClientStartNode<out T, M>>
    get() {
      val result = ArrayList<ClientStartNode<out T, M>>()
      for (n in modelNodes) {
        if (n is ClientStartNode<*, *>) {
          result.add(n as ClientStartNode<out T, M>)
        }
      }
      return result
    }

  override fun addNode(node: T): Boolean {
    if (super.addNode(node)) {
      node.setOwnerModel(asM())
      // Make sure that children can know of the change.
      notifyNodeChanged(node)
      return true
    }
    return false
  }

  override fun setNode(pos: Int, newValue: T): T {
    val oldValue = setNode(pos, newValue) as T

    newValue.setOwnerModel(asM())
    oldValue.setSuccessors(emptySet<Identifiable>())
    oldValue.setPredecessors(emptySet<Identifiable>())
    oldValue.setOwnerModel(null)
    newValue.resolveRefs()
    for (pred in newValue.predecessors) {
      getNode(pred)!!.addSuccessor(newValue)
    }
    for (suc in newValue.successors) {
      getNode(suc)!!.addPredecessor(newValue)
    }

    return oldValue
  }

  override fun removeNode(nodePos: Int): T {
    val node = super.removeNode(nodePos)
    disconnectNode(node)
    return node
  }

  override fun removeNode(node: T): Boolean {
    if (node == null) {
      return false
    }
    if (super.removeNode(node)) {
      disconnectNode(node)
      return true
    }
    return false
  }

  private fun disconnectNode(node: T) {
    node.setPredecessors(emptyList<Identifiable>())
    node.setSuccessors(emptyList<Identifiable>())
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

  private fun toDiagramNodes(modelNodes: Collection<T>): List<DiagramNode<T>> {
    val map = HashMap<T, DiagramNode<T>>()
    val result = ArrayList<DiagramNode<T>>()
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
      if (node.id != null) {
        map.put(node, dn)
      }
      result.add(dn)
    }

    for (dn in result) {
      val mn = dn.target
      for (successor in mn.successors) {
        val rightdn = map[successor]
        if (rightdn != null) {
          dn.rightNodes.add(rightdn)
        }
      }
      for (predecessorId in mn.predecessors) {
        val predecessor = getNode(predecessorId)
        val leftdn = map[predecessor]
        if (leftdn != null) {
          dn.leftNodes.add(leftdn)
        }
      }
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
