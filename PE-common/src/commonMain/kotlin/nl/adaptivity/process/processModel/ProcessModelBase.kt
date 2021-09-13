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

import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import net.devrieze.util.collection.ArrayAccess
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.serialutil.simpleSerialClassDesc
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.jvm.JvmStatic

/**
 * Created by pdvrieze on 02/01/17.
 */
abstract class ProcessModelBase<NodeT : ProcessNode> : ProcessModel<NodeT> {

    abstract override val modelNodes: IdentifyableSet<NodeT>

    private var _imports: List<IXmlResultType>

    private var _exports: List<IXmlDefineType>

    final override val imports: Collection<IXmlResultType>
        get() = _imports

    final override val exports: Collection<IXmlDefineType>
        get() = _exports

    protected fun setImports(value: Iterable<IXmlResultType>) {
        _imports = value.toList()
    }

    protected fun setExports(value: Iterable<IXmlDefineType>) {
        _exports = value.toList()
    }

    /** For serialization only */
    protected constructor() {
        _imports = emptyList()
        _exports = emptyList()
    }

    constructor(builder: ProcessModel.Builder, pedantic: Boolean) {
        builder.normalize(pedantic)

        this._imports = builder.imports.map { XmlResultType(it) }
        this._exports = builder.exports.map { XmlDefineType(it) }
    }

    constructor(
        imports: Collection<IXmlResultType>,
        exports: Collection<IXmlDefineType>
    ) {
        _imports = imports.toList()
        _exports = exports.toList()
    }

    override fun getNode(nodeId: Identifiable): NodeT? {
        if (nodeId is ProcessNode) {
            @Suppress("UNCHECKED_CAST")
            return nodeId as NodeT
        }
        return modelNodes[nodeId]
    }

    open fun getNode(nodeId: String) = modelNodes[nodeId]

    fun getNode(pos: Int): NodeT {
        return modelNodes[pos]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessModelBase<*>

        if (_imports != other._imports) return false
        if (_exports != other._exports) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _imports.hashCode()
        result = 31 * result + _exports.hashCode()
        return result
    }

    override fun toString(): String {
        return "ProcessModelBase(modelNodes=$modelNodes, imports=$_imports, exports=$_exports)"
    }


    @Serializable
    internal abstract class SerialDelegate(
        val imports: List<XmlResultType>,
        val exports: List<XmlDefineType>,
        val nodes: List<ProcessNodeBase.SerialDelegate>
    ) {
        constructor(source: ProcessModelBase<*>):this(
            imports = source.imports.map { XmlResultType(it) },
            exports = source.exports.map { XmlDefineType(it) },
            nodes = source.modelNodes.map { ProcessNodeBase.SerialDelegate(it) }
        )

        constructor(source: Builder):this(
            imports = source.imports.map { XmlResultType(it) },
            exports = source.exports.map { XmlDefineType(it) },
            nodes = source.nodes.map { ProcessNodeBase.SerialDelegate(it) }
        )
    }


    interface NodeFactory<NodeT : ChildNodeT, out ChildNodeT : ProcessNode, out ChildT : ChildProcessModel<ChildNodeT>> {
        operator fun invoke(
            baseNodeBuilder: ProcessNode.Builder,
            buildHelper: ProcessModel.BuildHelper<NodeT, *, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): NodeT

        operator fun invoke(
            baseChildBuilder: ChildProcessModel.Builder,
            buildHelper: ProcessModel.BuildHelper<NodeT, *, *, *>
        ): ChildT

        fun condition(condition: Condition): Condition = condition
    }

    @ProcessModelDSL
    abstract class Builder : ProcessModel.Builder {

        constructor(
            nodes: Collection<ProcessNode.Builder> = emptyList(),
            imports: Collection<IXmlResultType> = emptyList(),
            exports: Collection<IXmlDefineType> = emptyList()
        ) {
            this.nodes = nodes.toMutableList()
            this.imports = imports.toMutableList()
            this.exports = exports.toMutableList()
        }

        constructor() : this(nodes = emptyList())

        final override val nodes: MutableList<ProcessNode.Builder>

        final override val imports: MutableList<IXmlResultType>

        final override val exports: MutableList<IXmlDefineType>

        val node: ArrayAccess<String, ProcessNode.Builder> = object : ArrayAccess<String, ProcessNode.Builder> {
            private val outer = this@Builder
            override operator fun get(key: String) = outer.nodes.firstOrNull { it.id == key }
        }

        constructor(base: ProcessModel<*>) : this(
            emptyList(),
            base.imports.toMutableList(),
            base.exports.toMutableList()
        ) {

            base.modelNodes.mapTo(nodes) {
                it.visit(object : ProcessNode.Visitor<ProcessNode.Builder> {
                    override fun visitStartNode(startNode: StartNode) = startNodeBuilder(startNode)
                    override fun visitActivity(messageActivity: MessageActivity) = activityBuilder(messageActivity)
                    override fun visitActivity(compositeActivity: CompositeActivity) =
                        activityBuilder(compositeActivity)

                    override fun visitSplit(split: Split) = splitBuilder(split)
                    override fun visitJoin(join: Join) = joinBuilder(join)
                    override fun visitEndNode(endNode: EndNode) = endNodeBuilder(endNode)
                })
            }

        }

        override fun toString(): String {
            return "${this::class.simpleName}(nodes=$nodes, imports=$imports, exports=$exports)"
        }

    }

    companion object {

//        fun descriptor(name:String): SerialClassDescImpl = SerialClassDescImpl(descriptor, name)

        @JvmStatic
        protected fun <NodeT : ProcessNode> buildNodes(
            builder: ProcessModel.Builder,
            buildHelper: ProcessModel.BuildHelper<NodeT, *, *, *>
        ): MutableIdentifyableSet<NodeT> {
            val newNodes = builder.nodes.map {
                buildHelper.node(it, builder.nodes)
            }.let { IdentifyableSet.processNodeSet(Int.MAX_VALUE, it) }
            return newNodes
        }
    }
}
