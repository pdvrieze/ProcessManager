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

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import net.devrieze.util.collection.ArrayAccess
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.serialutil.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import kotlin.jvm.JvmStatic

/**
 * Created by pdvrieze on 02/01/17.
 */
@Serializable
abstract class ProcessModelBase<NodeT : ProcessNode> :
    ProcessModel<NodeT>, XmlSerializable {

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
        @Serializable(IdentifiableSetSerializer::class)
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


    abstract class BaseSerializer<T : ProcessModelBase<*>> : KSerializer<T> {

        private val importsIdx by lazy { descriptor.getElementIndex("import") }
        private val exportsIdx by lazy { descriptor.getElementIndex("export") }
        private val nodesIdx by lazy { descriptor.getElementIndex("nodes") }

        override fun serialize(encoder: Encoder, obj: T) {
            encoder.writeStructure(descriptor) {
                writeValues(this, obj)
            }
        }

        open fun writeValues(encoder: CompositeEncoder, obj: T) {
            val desc = descriptor
            encoder.encodeSerializableElement(desc, importsIdx, XmlResultType.list, obj.imports.map(::XmlResultType))
            encoder.encodeSerializableElement(desc, exportsIdx, XmlDefineType.list, obj.exports.map(::XmlDefineType))
            encoder.encodeSerializableElement(desc, nodesIdx, ModelNodeSerializer.list, obj.modelNodes.toList())
        }
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
    abstract class Builder : ProcessModel.Builder, SimpleXmlDeserializable {

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
            arrayOf(
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
                   )
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

        @Throws(XmlException::class)
        override fun deserializeChild(reader: XmlReader): Boolean {
            if (ProcessConsts.Engine.NAMESPACE == reader.namespaceURI) {
                val newNode = when (reader.localName) {
                    EndNode.ELEMENTLOCALNAME   -> endNodeBuilder().deserializeHelper(reader)
                    Activity.ELEMENTLOCALNAME  -> ActivityBase.DeserializationBuilder().deserializeHelper(reader)
                    StartNode.ELEMENTLOCALNAME -> startNodeBuilder().deserializeHelper(reader)
                    Join.ELEMENTLOCALNAME      -> joinBuilder().deserializeHelper(reader)
                    Split.ELEMENTLOCALNAME     -> splitBuilder().deserializeHelper(reader)
                    else                       -> return false
                }
                nodes.add(newNode)
                return true
            }
            return false
        }

        override fun toString(): String {
            return "${this::class.simpleName}(nodes=$nodes, imports=$imports, exports=$exports)"
        }

        abstract class BaseSerializer<T : Builder> : KSerializer<T> {

            abstract fun builder(): T

            override fun deserialize(decoder: Decoder): T {
                @Suppress("NAME_SHADOWING")
                decoder.decodeStructure(descriptor) {
                    val input = this
                    val result = builder()
                    decodeElements(input) { index ->
                        readElement(result, input, index)
                    }
                    return result
                }

            }

            fun readElement(result: T, input: KInput, index: Int) {
                readElement(result, input, index, descriptor.getElementName(index))
            }

            open fun readElement(result: T, input: KInput, index: Int, name: String) = when (name) {
                "import" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newImports = input.updateSerializableElement(
                        descriptor, index,
                        XmlResultType.list,
                        result.imports as List<XmlResultType>
                                                                    )
                    result.imports.replaceBy(newImports)
                }
                "export" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newExports = input.updateSerializableElement(
                        descriptor, index, XmlDefineType.list,
                        result.exports as List<XmlDefineType>
                                                                    )
                    result.exports.replaceBy(newExports)
                }
                "nodes"  -> {
                    val newNodes: Iterable<Any> = input.updateSerializableElement(
                        descriptor, index,
                        ModelNodeBuilderSerializer.list,
                        result.nodes
                                                                                 )
                    // Generics is utterly broken here
                    @Suppress("UNCHECKED_CAST")
                    (result.nodes as MutableList<Any>).replaceBy(iterable = newNodes)
                }
                else     -> throw SerializationException("Could not resolve field $name with index $index")
            }

        }

        companion object {

            @Throws(XmlException::class)
            @JvmStatic
            @Deprecated("Poor approach")
            fun <B : ProcessModelBase.Builder> deserialize(builder: B, reader: XmlReader): B {

                reader.skipPreamble()
                val elementName = RootProcessModelBase.ELEMENTNAME
                assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
                for (i in reader.attributeCount - 1 downTo 0) {
                    builder.deserializeAttribute(
                        reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i)
                                                )
                }

                var event: EventType? = null
                while (reader.hasNext() && event !== EventType.END_ELEMENT) {
                    event = reader.next()
                    if (!(event == EventType.START_ELEMENT && builder.deserializeChild(reader))) {
                        throw XmlException("Expected child tag, found other content")
                    }
                }

                for (node in builder.nodes) {
                    for (pred in node.predecessors) {
                        builder.nodes.firstOrNull { it.id == pred.id }?.addSuccessor(Identifier(node.id!!))
                    }
                }
                return builder
            }

        }


    }

    @Serializer(forClass = ProcessModelBase::class)
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
    override val kind: SerialKind get() = UnionKind.POLYMORPHIC
    override val name: String get() = "nodes"

    override fun getElementIndex(name: String) = when (name) {
        "klass" -> 0
        "value" -> 1
        else    -> CompositeDecoder.UNKNOWN_NAME
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return when (index) {
            0    -> StringSerializer.descriptor
            1    -> simpleSerialClassDesc<XmlProcessNode>()
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
        val saver = serializerByValue(obj, encoder.context) as SerializationStrategy<ProcessNode>
        encoder.writeStructure(descriptor) {
            encodeStringElement(descriptor, 0, saver.descriptor.name)
            encodeSerializableElement(descriptor, 1, saver, obj)
        }
    }

    @JvmStatic
    private fun serializerByValue(obj: ProcessNode, context: SerialModule?): KSerializer<out ProcessNode> {
        // If the context has a serializer use that
        context?.getContextual(obj::class)?.let { return it }
        // Otherwise fall back to "known" serializers
        when (obj) {
            is XmlStartNode -> return XmlStartNode.serializer()
            is XmlActivity  -> return XmlActivity.serializer()
            is XmlSplit     -> return XmlSplit.serializer()
            is XmlJoin      -> return XmlJoin.serializer()
            is XmlEndNode   -> return XmlEndNode.serializer()
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
            mainLoop@ while (true) {
                when (input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_ALL  -> {
                        klassName = input.decodeStringElement(descriptor, 0)
                        val loader = serializerBySerialDescClassname(klassName, input.context)
                        value = input.decodeSerializableElement(descriptor, 1, loader)
                        break@mainLoop
                    }
                    CompositeDecoder.READ_DONE -> {
                        break@mainLoop
                    }
                    0                          -> {
                        klassName = input.decodeStringElement(descriptor, 0)
                    }
                    1                          -> {
                        klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                        val loader = serializerBySerialDescClassname(klassName, input.context)
                        value = input.decodeSerializableElement(descriptor, 1, loader)
                    }
                    else                       -> throw SerializationException("Invalid index")
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
        context: SerialModule = EmptyModule
                                               ): KSerializer<out ProcessNode.Builder> {
        if (klassName.startsWith(NODE_PACKAGE)) {
            serializerBySimpleName(klassName.substring(NODE_PACKAGE.length + 1))?.let { return it }
        } else if (klassName == "nl.adaptivity.xmlutil.serialization.canary.CanaryInput\$Dummy") {
            return ActivityBase.DeserializationBuilder.serializer()
        }
        throw IllegalArgumentException("No serializer found for class $klassName")
    }

    @JvmStatic
    private fun serializerBySimpleName(
        simpleName: String,
        context: SerialModule = EmptyModule
                                      ): KSerializer<out ProcessNode.Builder>? = when (simpleName) {
        "XmlStartNode",
        "XmlStartNode\$Builder" -> StartNodeBase.Builder.serializer()
        "XmlActivity",
        "XmlActivity\$Builder"  -> ActivityBase.DeserializationBuilder.serializer()
        "XmlSplit",
        "XmlSplit\$Builder"     -> SplitBase.Builder.serializer()
        "XmlJoin",
        "XmlJoin\$Builder"      -> JoinBase.Builder.serializer()
        "XmlEndNode",
        "XmlEndNode\$Builder"   -> EndNodeBase.Builder.serializer()
        else                    -> null
    }

}
