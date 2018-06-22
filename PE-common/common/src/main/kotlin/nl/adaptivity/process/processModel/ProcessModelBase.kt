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
import kotlinx.serialization.internal.SerialClassDescImpl
import net.devrieze.util.collection.ArrayAccess
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlPolyChildren
import nl.adaptivity.xml.serialization.writeBegin

/**
 * Created by pdvrieze on 02/01/17.
 */
@Serializable
abstract class ProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
    ProcessModel<NodeT, ModelT>, XmlSerializable {

    @SerialName("nodes")
    @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.engine.XmlActivity\$Builder=pe:activity",
                             "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
                             "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
                             "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
                             "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
                             "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
                             "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
                             "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
                             "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
                             "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"))
    abstract override val modelNodes: IdentifyableSet<NodeT>

    @SerialName("import")
    @XmlPolyChildren(arrayOf("import=XmlResultType"))
    private var _imports: List<IXmlResultType>

    @SerialName("export")
    @XmlPolyChildren(arrayOf("export=XmlDefineType"))
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

    constructor(builder: ProcessModel.Builder<*, *>, pedantic: Boolean) {
        builder.normalize(pedantic)

        this._imports = builder.imports.map { XmlResultType(it) }
        this._exports = builder.exports.map { XmlDefineType(it) }
    }

    constructor(imports: Collection<IXmlResultType>,
                exports: Collection<IXmlDefineType>) {
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
        return modelNodes[nodeId]
    }

    open fun getNode(nodeId: String) = modelNodes[nodeId]

    fun getNode(pos: Int): NodeT {
        return modelNodes[pos]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessModelBase<*, *>

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


    abstract class BaseSerializer<T : ProcessModelBase<*, *>> : KSerializer<T> {

        private val importsIdx by lazy { serialClassDesc.getElementIndex("import") }
        private val exportsIdx by lazy { serialClassDesc.getElementIndex("export") }
        private val nodesIdx by lazy { serialClassDesc.getElementIndex("nodes") }

        override fun save(output: KOutput, obj: T) {
            output.writeBegin(serialClassDesc) {
                writeValues(this, obj)
            }
        }

        open fun writeValues(output: KOutput, obj: T) {
            val desc = serialClassDesc
            output.writeSerializableElementValue(desc, importsIdx, XmlResultType.list, obj.imports.map(::XmlResultType))
            output.writeSerializableElementValue(desc, exportsIdx, XmlDefineType.list, obj.exports.map(::XmlDefineType))
            output.writeSerializableElementValue(desc, nodesIdx, ModelNodeSerializer.list, obj.modelNodes.toList())
        }
    }

    interface NodeFactory<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
        operator fun invoke(baseNodeBuilder: ProcessNode.IBuilder<*, *>,
                            buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): NodeT

        operator fun invoke(baseChildBuilder: ChildProcessModel.Builder<*, *>,
                            buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ChildProcessModelBase<NodeT, ModelT>

        fun condition(text: String): Condition
    }

    @ProcessModelDSL
    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    constructor(nodes: Collection<ProcessNode.IBuilder<NodeT, ModelT>> = emptyList(),
                imports: Collection<IXmlResultType> = emptyList(),
                exports: Collection<IXmlDefineType> = emptyList()) :
        ProcessModel.Builder<NodeT, ModelT>, SimpleXmlDeserializable {

        constructor() : this(nodes = emptyList())

        final override val nodes: MutableList<ProcessNode.IBuilder<NodeT, ModelT>> = nodes.toMutableList()
        @SerialName("import")
        final override val imports: MutableList<IXmlResultType> = imports.toMutableList()
        @SerialName("export")
        final override val exports: MutableList<IXmlDefineType> = exports.toMutableList()

        @Transient
        val node = object : ArrayAccess<String, ProcessNode.IBuilder<NodeT, ModelT>> {
            override operator fun get(key: String) = this@Builder.nodes.firstOrNull { it.id == key }
        }

        constructor(base: ProcessModel<*, *>) :
            this(emptyList(),
                 base.imports.toMutableList(),
                 base.exports.toMutableList()) {

            base.modelNodes.mapTo(nodes) {
                it.visit(object : ProcessNode.Visitor<ProcessNode.IBuilder<NodeT, ModelT>> {
                    override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
                    override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
                    override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
                    override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
                    override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
                })
            }

        }

        @Throws(XmlException::class)
        override fun deserializeChild(reader: XmlReader): Boolean {
            if (ProcessConsts.Engine.NAMESPACE == reader.namespaceURI) {
                val newNode = when (reader.localName) {
                    EndNode.ELEMENTLOCALNAME   -> endNodeBuilder().deserializeHelper(reader)
                    Activity.ELEMENTLOCALNAME  -> activityBuilder().deserializeHelper(reader)
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

        abstract class BaseSerializer<T : Builder<*, *>> : KSerializer<T> {

            abstract fun builder(): T

            override fun load(input: KInput): T {
                @Suppress("NAME_SHADOWING")
                val input = input.readBegin(serialClassDesc)
                val result = builder()

                mainLoop@ while (true) {
                    val index = input.readElement(serialClassDesc)
                    when (index) {
                        KInput.READ_ALL  -> {
                            for (i in 0 until serialClassDesc.associatedFieldsCount) {
                                readElement(result, input, i)
                            }
                            break@mainLoop
                        }
                        KInput.READ_DONE -> break@mainLoop
                        else             -> readElement(result, input, index)
                    }
                }
                input.readEnd(serialClassDesc)
                return result

            }

            fun readElement(result: T, input: KInput, index: Int) {
                readElement(result, input, index, serialClassDesc.getElementName(index))
            }

            open fun readElement(result: T, input: KInput, index:Int, name: String) = when (name) {
                "import" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newImports = input.updateSerializableElementValue(serialClassDesc, index,
                                                                          XmlResultType.list,
                                                                          result.imports as List<XmlResultType>)
                    result.imports.replaceBy(newImports)
                }
                "export" -> {
                    @Suppress("UNCHECKED_CAST")
                    val newExports = input.updateSerializableElementValue(serialClassDesc, index, XmlDefineType.list,
                                                                          result.exports as List<XmlDefineType>)
                    result.exports.replaceBy(newExports)
                }
                "nodes"   -> {
                    val newNodes: Iterable<Any> = input.updateSerializableElementValue(serialClassDesc, index,
                                                                                       ModelNodeBuilderSerializer.list, result.nodes)
                    // Generics is utterly broken here
                    @Suppress("UNCHECKED_CAST")
                    (result.nodes as MutableList<Any>).replaceBy(iterable=newNodes)
                }
                else       -> throw SerializationException("Could not resolve field $name with index $index")
            }

        }

        companion object {

            @Throws(XmlException::class)
            @JvmStatic
            @Deprecated("Poor approach")
            fun <B : ProcessModelBase.Builder<*, *>> deserialize(builder: B, reader: XmlReader): B {

                reader.skipPreamble()
                val elementName = RootProcessModelBase.ELEMENTNAME
                assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
                for (i in reader.attributeCount - 1 downTo 0) {
                    builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                                                 reader.getAttributeValue(i))
                }

                var event: EventType? = null
                while (reader.hasNext() && event !== EventType.END_ELEMENT) {
                    event = reader.next()
                    if (!(event == EventType.START_ELEMENT && builder.deserializeChild(reader))) {
                        reader.unhandledEvent()
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

//        fun serialClassDesc(name:String): SerialClassDescImpl = SerialClassDescImpl(serialClassDesc, name)

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

private object ModelNodeClassDesc : KSerialClassDesc {
    override val kind: KSerialClassKind get() = KSerialClassKind.POLYMORPHIC
    override val name: String get() = "nodes"

    override fun getElementIndex(name: String) = when (name) {
        "klass" -> 0
        "value" -> 1
        else    -> KInput.UNKNOWN_NAME
    }

    override fun getElementName(index: Int): String {
        return when (index) {
            0    -> "klass"
            1    -> "value"
            else -> throw IndexOutOfBoundsException("$index")
        }
    }

    override val associatedFieldsCount: Int get() = 2
}

object ModelNodeSerializer : KSerializer<ProcessNode<*, *>> {
    override val serialClassDesc: KSerialClassDesc get() = ModelNodeClassDesc

    override fun load(input: KInput): ProcessNode<*, *> {
        throw UnsupportedOperationException("No valid model loading outside of process model context")
    }


    override fun save(output: KOutput, obj: ProcessNode<*, *>) {
        val saver = serializerByValue(obj, output.context)
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc)
        output.writeStringElementValue(serialClassDesc, 0, saver.serialClassDesc.name)
        @Suppress("UNCHECKED_CAST")
        output.writeSerializableElementValue(serialClassDesc, 1, saver as KSerialSaver<ProcessNode<*, *>>, obj)
        output.writeEnd(serialClassDesc)
    }


    @JvmStatic
    private fun serializerByValue(obj: ProcessNode<*, *>, context: SerialContext?): KSerializer<out ProcessNode<*, *>> {
        // If the context has a serializer use that
        context?.getSerializerByClass(obj::class)?.let { return it }
        // Otherwise fall back to "known" serializers
        when (obj) {
            is XmlStartNode -> return XmlStartNode::class.serializer()
            is XmlActivity  -> return XmlActivity::class.serializer()
            is XmlSplit     -> return XmlSplit::class.serializer()
            is XmlJoin      -> return XmlJoin::class.serializer()
            is XmlEndNode   -> return XmlEndNode::class.serializer()
        }
        return context.valueSerializer(obj)
    }

}

object ModelNodeBuilderSerializer : KSerializer<ProcessNode.IBuilder<*, *>> {
    override val serialClassDesc: KSerialClassDesc get() = ModelNodeClassDesc

    override fun load(input: KInput): ProcessNode.IBuilder<*, *> {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc)
        var klassName: String? = null
        var value: ProcessNode.IBuilder<*, *>? = null
        mainLoop@ while (true) {
            when (input.readElement(serialClassDesc)) {
                KInput.READ_ALL  -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                    val loader = serializerBySerialDescClassname(klassName, input.context)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                    break@mainLoop
                }
                KInput.READ_DONE -> {
                    break@mainLoop
                }
                0                -> {
                    klassName = input.readStringElementValue(serialClassDesc, 0)
                }
                1                -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = serializerBySerialDescClassname(klassName, input.context)
                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
                }
                else             -> throw SerializationException("Invalid index")
            }
        }

        input.readEnd(serialClassDesc)
        return requireNotNull(value) { "Polymorphic value have not been read" }

    }

    override fun save(output: KOutput, obj: ProcessNode.IBuilder<*, *>) {
        throw UnsupportedOperationException("Only final process nodes can be serialized for now")
    }

    private const val NODE_PACKAGE = "nl.adaptivity.process.processModel.engine"

    @JvmStatic
    private fun serializerBySerialDescClassname(klassName: String,
                                                context: SerialContext?): KSerializer<out ProcessNode.IBuilder<*, *>> {
        if (klassName.startsWith(NODE_PACKAGE)) {
            serializerBySimpleName(klassName.substring(NODE_PACKAGE.length + 1))?.let { return it }
        } else if (klassName == "nl.adaptivity.xml.serialization.canary.CanaryInput\$Dummy") {
            return context.klassSerializer(XmlActivity.Builder::class)
        }
        throw IllegalArgumentException("No serializer found for class $klassName")
    }

    @JvmStatic
    private fun serializerBySimpleName(simpleName: String,
                                       context: SerialContext? = null): KSerializer<out ProcessNode.IBuilder<*, *>>? = when (simpleName) {
        "XmlStartNode",
        "XmlStartNode\$Builder" -> context.klassSerializer(XmlStartNode.Builder::class)
        "XmlActivity",
        "XmlActivity\$Builder"  -> context.klassSerializer(XmlActivity.Builder::class)
        "XmlSplit",
        "XmlSplit\$Builder"     -> context.klassSerializer(XmlSplit.Builder::class)
        "XmlJoin",
        "XmlJoin\$Builder"      -> context.klassSerializer(XmlJoin.Builder::class)
        "XmlEndNode",
        "XmlEndNode\$Builder"   -> context.klassSerializer(XmlEndNode.Builder::class)
        else                    -> null
    }

}