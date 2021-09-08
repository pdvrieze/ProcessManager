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

    /*
        @SerialName("nodes")
        @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.ActivityBase\$DeserializationBuilder=pe:activity",
                                 "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
                                 "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
                                 "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
                                 "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
                                 "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
                                 "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
                                 "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
                                 "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
                                 "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"))
        @foo.FakeSerializable(IdentifiableSetSerializer::class)
    */
    @Transient
    abstract override val modelNodes: IdentifyableSet<NodeT>

    @SerialName("import")
    @XmlSerialName(value = "import", namespace = ProcessConsts.Engine.NAMESPACE, prefix = ProcessConsts.Engine.NSPREFIX)
    @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.XmlResultType=import"))
    @Serializable(IXmlResultTypeListSerializer::class)
    private var _imports: List<IXmlResultType>

    @SerialName("export")
    @XmlSerialName(value = "export", namespace = ProcessConsts.Engine.NAMESPACE, prefix = ProcessConsts.Engine.NSPREFIX)
    @Serializable(IXmlDefineTypeListSerializer::class)
    @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.XmlDefineType=export"))
    private var _exports: List<IXmlDefineType>

    @Transient
    final override val imports: Collection<IXmlResultType>
        get() = _imports

    @Transient
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

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
       */
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

    @Serializable
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

        @SerialName("nodes")
        @XmlPolyChildren(
            [
                "nl.adaptivity.process.processModel.ActivityBase\$DeserializationBuilder=pe:activity",
                "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
                "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
                "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
                "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
                "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
                "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
                "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
                "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
                "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"
            ]
        )
        final override val nodes: MutableList<ProcessNode.Builder>

        @SerialName("import")
        @XmlSerialName(
            value = "import", namespace = ProcessConsts.Engine.NAMESPACE,
            prefix = ProcessConsts.Engine.NSPREFIX
        )
        @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.XmlResultType=import"))
        @Serializable(IXmlResultTypeListSerializer::class)
        final override val imports: MutableList<IXmlResultType>

        @SerialName("export")
        @XmlSerialName(
            value = "export", namespace = ProcessConsts.Engine.NAMESPACE,
            prefix = ProcessConsts.Engine.NSPREFIX
        )
        @Serializable(IXmlDefineTypeListSerializer::class)
        @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.XmlDefineType=export"))
        final override val exports: MutableList<IXmlDefineType>

        @Transient
        val node: ArrayAccess<String, ProcessNode.Builder> = object : ArrayAccess<String, ProcessNode.Builder> {
            private val outer = this@Builder
            override operator fun get(key: String) = outer.nodes.firstOrNull { it.id == key }
        }

        constructor(base: ProcessModel<*>) :
            this(
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

        abstract class BaseSerializer<T : Builder> : KSerializer<T> {

            abstract fun builder(): T

            override fun deserialize(decoder: Decoder): T {
                TODO()
/*
                @Suppress("NAME_SHADOWING")
                decoder.decodeStructure(descriptor) {
                    val input = this
                    val result = builder()
                    decodeElements(input) { index ->
                        readElement(result, input, index)
                    }
                    return result
                }
*/

            }

            fun readElement(result: T, input: CompositeDecoder, index: Int) {
                readElement(result, input, index, descriptor.getElementName(index))
            }

            open fun readElement(result: T, input: CompositeDecoder, index: Int, name: String) = when (name) {
                "import" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newImports = input.decodeSerializableElement(
                        descriptor, index,
                        serializer<List<XmlResultType>>(),
                        result.imports as List<XmlResultType>
                    )
                    result.imports.replaceBy(newImports)
                }
                "export" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newExports = input.decodeSerializableElement(
                        descriptor, index, serializer<List<XmlDefineType>>(),
                        result.exports as List<XmlDefineType>
                    )
                    result.exports.replaceBy(newExports)
                }
                "nodes"  -> {
                    val newNodes: Iterable<Any> = input.decodeSerializableElement(
                        descriptor, index,
                        ListSerializer(ModelNodeBuilderSerializer),
                        result.nodes
                    )
                    // Generics is utterly broken here
                    @Suppress("UNCHECKED_CAST")
                    (result.nodes as MutableList<Any>).replaceBy(iterable = newNodes)
                }
                else     -> throw SerializationException("Could not resolve field $name with index $index")
            }

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

private object ModelNodeClassDesc : SerialDescriptor {
    override val kind: SerialKind get() = PolymorphicKind.OPEN

    @ExperimentalSerializationApi
    override val serialName: String
        get() = "nodes"

    @ExperimentalSerializationApi
    override fun getElementAnnotations(index: Int): List<Annotation> {
        return emptyList()
    }

    override fun getElementIndex(name: String) = when (name) {
        "klass" -> 0
        "value" -> 1
        else    -> CompositeDecoder.UNKNOWN_NAME
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return when (index) {
            0    -> String.serializer().descriptor
            1    -> TODO()//simpleSerialClassDesc<XmlProcessNode>()
            else -> throw IndexOutOfBoundsException("$index")
        }
    }

    override fun getElementName(index: Int): String {
        return when (index) {
            0    -> "klass"
            1    -> "value"
            else -> throw IndexOutOfBoundsException("$index")
        }
    }

    override fun isElementOptional(index: Int): Boolean = false

    override val elementsCount: Int get() = 2
}

object ModelNodeSerializer : KSerializer<ProcessNode> {
    override val descriptor: SerialDescriptor get() = ModelNodeClassDesc

    override fun deserialize(decoder: Decoder): ProcessNode {
        throw UnsupportedOperationException("No valid model loading outside of process model context")
    }


    override fun serialize(encoder: Encoder, obj: ProcessNode) {
        val saver = serializerByValue(obj, encoder.serializersModule) as SerializationStrategy<ProcessNode>
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, saver.descriptor.serialName)
            encodeSerializableElement(descriptor, 1, saver, obj)
        }
    }

    @JvmStatic
    private fun serializerByValue(obj: ProcessNode, context: SerializersModule?): KSerializer<out ProcessNode> {
        // If the context has a serializer use that
        context?.getContextual(obj::class)?.let { return it }
        // Otherwise fall back to "known" serializers
        when (obj) {
            is XmlStartNode -> return serializer<XmlStartNode>() // TODO use concrete serializer instance
            is XmlActivity  -> return serializer<XmlActivity>() // TODO use concrete serializer instance
            is XmlSplit     -> return serializer<XmlSplit>() // TODO use concrete serializer instance
            is XmlJoin      -> return serializer<XmlJoin>() // TODO use concrete serializer instance
            is XmlEndNode   -> return serializer<XmlEndNode>() // TODO use concrete serializer instance
        }
        throw SerializationException("No serializer known for ${obj::class} with value '${obj.toString()}'")
    }

}

object ModelNodeBuilderSerializer : KSerializer<ProcessNode.Builder> {
    override val descriptor: SerialDescriptor get() = ModelNodeClassDesc

    override fun deserialize(decoder: Decoder): ProcessNode.Builder {
        decoder.decodeStructure(descriptor) {
            val input = this
            var klassName: String? = null
            var value: ProcessNode.Builder? = null
            if (input.decodeSequentially()) {
                klassName = input.decodeStringElement(descriptor, 0)
                val loader = serializerBySerialDescClassname(klassName, input.serializersModule)
                value = input.decodeSerializableElement(descriptor, 1, loader)
                return value

            }
            mainLoop@ while (true) {
                when (input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> {
                        break@mainLoop
                    }
                    0                            -> {
                        klassName = input.decodeStringElement(descriptor, 0)
                    }
                    1                            -> {
                        klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                        val loader = serializerBySerialDescClassname(klassName, input.serializersModule)
                        value = input.decodeSerializableElement(descriptor, 1, loader)
                    }
                    else                         -> throw SerializationException("Invalid index")
                }
            }

            return requireNotNull(value) { "Polymorphic value have not been read" }
        }

    }

    override fun serialize(encoder: Encoder, obj: ProcessNode.Builder) {
        throw UnsupportedOperationException("Only final process nodes can be serialized for now")
    }

    private const val NODE_PACKAGE = "nl.adaptivity.process.processModel.engine"

    @JvmStatic
    private fun serializerBySerialDescClassname(
        klassName: String,
        context: SerializersModule = EmptySerializersModule
    ): KSerializer<out ProcessNode.Builder> {
        if (klassName.startsWith(NODE_PACKAGE)) {
            serializerBySimpleName(klassName.substring(NODE_PACKAGE.length + 1))?.let { return it }
        } else if (klassName == "nl.adaptivity.xmlutil.serialization.canary.CanaryInput\$Dummy") {
            return serializer<ActivityBase.DeserializationBuilder>() // TODO use concrete serializer instance
        }
        throw IllegalArgumentException("No serializer found for class $klassName")
    }

    @JvmStatic
    private fun serializerBySimpleName(
        simpleName: String,
        context: SerializersModule = EmptySerializersModule
    ): KSerializer<out ProcessNode.Builder>? = when (simpleName) {
        "XmlStartNode",
        "XmlStartNode\$Builder" -> serializer<StartNodeBase.Builder>() // TODO use concrete serializer instance
        "XmlActivity",
        "XmlActivity\$Builder"  -> serializer<ActivityBase.DeserializationBuilder>() // TODO use concrete serializer instance
        "XmlSplit",
        "XmlSplit\$Builder"     -> serializer<SplitBase.Builder>() // TODO use concrete serializer instance
        "XmlJoin",
        "XmlJoin\$Builder"      -> serializer<JoinBase.Builder>() // TODO use concrete serializer instance
        "XmlEndNode",
        "XmlEndNode\$Builder"   -> serializer<EndNodeBase.Builder>() // TODO use concrete serializer instance
        else                    -> null
    }

}
