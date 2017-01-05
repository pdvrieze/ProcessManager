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

package nl.adaptivity.process.processModel

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.*

/**
 * Created by pdvrieze on 02/01/17.
 */
abstract class ProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT, ModelT>, XmlSerializable {

  @ProcessModelDSL
  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>(
      nodes: Collection<ProcessNode.Builder<NodeT, ModelT>> = emptyList(),
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) : ProcessModel.Builder<NodeT, ModelT> {

    override val nodes: MutableSet<ProcessNode.Builder<NodeT, ModelT>> = nodes.toMutableSet()
    override val imports: MutableList<IXmlResultType> = imports.toMutableList()
    override val exports: MutableList<IXmlDefineType> = exports.toMutableList()

    constructor(base: ProcessModel<*,*>) :
        this(emptyList(),
            base.imports.toMutableList(),
            base.exports.toMutableList()) {

      base.getModelNodes().mapTo(nodes) { it.visit(object : ProcessNode.Visitor<ProcessNode.Builder<NodeT, ModelT>> {
        override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
        override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
        override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
        override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
        override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
      }) }

    }

    override fun toString(): String {
      return "${this.javaClass.name.split('.').last()}(nodes=$nodes, imports=$imports, exports=$exports)"
    }

    companion object {

      @Throws(XmlException::class)
      @JvmStatic
      fun <B: ProcessModel.Builder<*, *>> deserialize(builder: B, reader: XmlReader): B {

        reader.skipPreamble()
        val elementName = RootProcessModelBase.ELEMENTNAME
        assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
        for (i in reader.attributeCount - 1 downTo 0) {
          builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        var event: XmlStreaming.EventType? = null
        while (reader.hasNext() && event !== XmlStreaming.EventType.END_ELEMENT) {
          event = reader.next()
          if (!(event== XmlStreaming.EventType.START_ELEMENT && builder.deserializeChild(reader))) {
            reader.unhandledEvent()
          }
        }

        for (node in builder.nodes) {
          for (pred in node.predecessors) {
            builder.nodes.firstOrNull { it.id == pred.id }?.successors?.add(Identifier(node.id!!))
          }
        }
        return builder
      }

    }


  }

  private val _processNodes: IdentifyableSet<NodeT>
  private var _imports: List<IXmlResultType>
  private var _exports: List<IXmlDefineType>

  override fun getModelNodes(): Collection<NodeT> = _processNodes

  final override fun getImports(): Collection<IXmlResultType> = _imports
  protected fun setImports(value: Iterable<IXmlResultType>) {_imports = value.toList() }

  final override fun getExports(): Collection<IXmlDefineType> = _exports
  protected fun setExports(value: Iterable<IXmlDefineType>) { _exports = value.toList() }

  constructor(builder: ProcessModel.Builder<NodeT, ModelT>, pedantic: Boolean) {
    val newOwner = this.asM
    val newNodes = builder.apply { normalize(pedantic) }.nodes.map { it.build(newOwner).asT() }
    this._processNodes = IdentifyableSet.processNodeSet(Int.MAX_VALUE, newNodes)
    this._imports = builder.imports.map { XmlResultType.get(it) }
    this._exports = builder.exports.map { XmlDefineType.get(it) }
  }

  constructor(processNodes: Iterable<ProcessNode<*,*>>, imports: Collection<IXmlResultType>, exports: Collection<IXmlDefineType>, nodeFactory: (ProcessModel<NodeT,ModelT>, ProcessNode<*,*>)->NodeT) {
    val newOwner = this
    _processNodes = IdentifyableSet.processNodeSet(processNodes.asSequence().map { nodeFactory(newOwner, it) })
    _imports = imports.toList()
    _exports = exports.toList()
  }

  abstract fun builder(): Builder<NodeT, ModelT>

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
     */
  override fun getNode(nodeId: Identifiable): NodeT? {
    if (nodeId is ProcessNode<*, *>) {
      @Suppress("UNCHECKED_CAST")
      return nodeId as NodeT
    }
    return _processNodes.get(nodeId)
  }

  fun getNode(pos: Int): NodeT {
    return _processNodes[pos]
  }

  /**
   * Set the process nodes for the model. This will actually just retrieve the
   * [XmlEndNode]s and sets the model accordingly. This does mean that only
   * passing [XmlEndNode]s will have the same result, and the other nodes
   * will be pulled in.

   * @param processNodes The process nodes to base the model on.
   */
  protected open fun setModelNodes(processNodes: Collection<NodeT>) {
    (processNodes as IdentifyableSet).replaceBy(processNodes)
  }

}

