/*
 * Copyright (c) 2018.
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

import net.devrieze.util.collection.ArrayAccess
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2

/**
 * Created by pdvrieze on 02/01/17.
 */
abstract class ProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT, ModelT>, XmlSerializable {

  interface NodeFactory<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
    operator fun invoke(baseNodeBuilder: ProcessNode.IBuilder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): NodeT

    operator fun invoke(baseChildBuilder: ChildProcessModel.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ChildProcessModelBase<NodeT, ModelT>
  }

  @ProcessModelDSL
  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>(
      nodes: Collection<ProcessNode.IBuilder<NodeT, ModelT>> = emptyList(),
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) : ProcessModel.Builder<NodeT, ModelT>, SimpleXmlDeserializable {

    override val nodes: MutableList<ProcessNode.IBuilder<NodeT, ModelT>> = nodes.toMutableList()
    override val imports: MutableList<IXmlResultType> = imports.toMutableList()
    override val exports: MutableList<IXmlDefineType> = exports.toMutableList()

    val node = object: ArrayAccess<String, ProcessNode.IBuilder<NodeT, ModelT>> {
      override operator fun get(key:String) = this@Builder.nodes.firstOrNull { it.id==key }
    }

    constructor(base: ProcessModel<*,*>) :
        this(emptyList(),
            base.imports.toMutableList(),
            base.exports.toMutableList()) {

      base.getModelNodes().mapTo(nodes) { it.visit(object : ProcessNode.Visitor<ProcessNode.IBuilder<NodeT, ModelT>> {
        override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
        override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
        override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
        override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
        override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
      }) }

    }

    @Throws(XmlException::class)
    override fun deserializeChild(reader: XmlReader): Boolean {
      if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
        val newNode = when (reader.localName.toString()) {
          EndNode.ELEMENTLOCALNAME -> endNodeBuilder().deserializeHelper(reader)
          Activity.ELEMENTLOCALNAME -> activityBuilder().deserializeHelper(reader)
          StartNode.ELEMENTLOCALNAME -> startNodeBuilder().deserializeHelper(reader)
          Join.ELEMENTLOCALNAME -> joinBuilder().deserializeHelper(reader)
          Split.ELEMENTLOCALNAME -> splitBuilder().deserializeHelper(reader)
          else -> return false
        }
        nodes.add(newNode)
        return true
      }
      return false
    }

    override fun toString(): String {
      return "${this::class.simpleName}(nodes=$nodes, imports=$imports, exports=$exports)"
    }

    companion object {

      @Throws(XmlException::class)
      @JvmStatic
      @Deprecated("Poor approach")
      fun <B: ProcessModelBase.Builder<*, *>> deserialize(builder: B, reader: XmlReader): B {

        reader.skipPreamble()
        val elementName = RootProcessModelBase.ELEMENTNAME
        assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
        for (i in reader.attributeCount - 1 downTo 0) {
          builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        var event: EventType? = null
        while (reader.hasNext() && event !== EventType.END_ELEMENT) {
          event = reader.next()
          if (!(event== EventType.START_ELEMENT && builder.deserializeChild(reader))) {
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

  protected abstract val _processNodes: IdentifyableSet<NodeT>

  private var _imports: List<IXmlResultType>
  private var _exports: List<IXmlDefineType>

  override fun getModelNodes(): List<NodeT> = _processNodes

  final override fun getImports(): Collection<IXmlResultType> = _imports
  protected fun setImports(value: Iterable<IXmlResultType>) {_imports = value.toList() }

  final override fun getExports(): Collection<IXmlDefineType> = _exports
  protected fun setExports(value: Iterable<IXmlDefineType>) { _exports = value.toList() }

  constructor(builder: ProcessModel.Builder<*, *>, pedantic: Boolean) {
    builder.normalize(pedantic)

    this._imports = builder.imports.map { XmlResultType(it) }
    this._exports = builder.exports.map { XmlDefineType.get(it) }
  }

  constructor(processNodes: Iterable<ProcessNode<*,*>>, imports: Collection<IXmlResultType>, exports: Collection<IXmlDefineType>, nodeFactory: NodeFactory<NodeT, ModelT>) {
    val newOwner = this
    _imports = imports.toList()
    _exports = exports.toList()
  }

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

  open fun getNode(nodeId: String) = _processNodes.get(nodeId)

  fun getNode(pos: Int): NodeT {
    return _processNodes[pos]
  }


  companion object {

    @JvmStatic
    protected fun <NodeT : ProcessNode<NodeT, ModelT>,
      ModelT : ProcessModel<NodeT, ModelT>?> buildNodes(builder: ProcessModel.Builder<*, *>,
                                                        buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): MutableIdentifyableSet<NodeT> {
      val newNodes = builder.nodes.map {
        buildHelper.node(it)
      }.let { IdentifyableSet.processNodeSet(Int.MAX_VALUE, it) }
      return newNodes
    }
  }
}

