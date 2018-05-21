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
import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.*
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.multiplatform.*
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlDefault
import nl.adaptivity.xml.serialization.readNullableString
import nl.adaptivity.xml.serialization.writeNullableStringElementValue

/**
 * Created by pdvrieze on 21/11/15.
 */
typealias ProcessModelHandle<M> = ComparableHandle<M>

@Serializable
abstract class RootProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModelBase<NodeT, ModelT>, RootProcessModel<NodeT, ModelT>, MutableHandleAware<RootProcessModel<out NodeT, out ModelT>>, XmlSerializable {

    @ProcessModelDSL
    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    constructor(nodes: Collection<ProcessNode.IBuilder<NodeT, ModelT>> = emptyList(),
                childModels: Collection<ChildProcessModel.Builder<NodeT, ModelT>> = emptyList(),
                override var name: String? = null,
                override var handle: Long = -1L,
                override var owner: Principal = SYSTEMPRINCIPAL,
                roles: Collection<String> = emptyList(),
                override var uuid: UUID? = null,
                imports: Collection<IXmlResultType> = emptyList(),
                exports: Collection<IXmlDefineType> = emptyList()) :
        ProcessModelBase.Builder<NodeT, ModelT>(nodes, imports, exports), RootProcessModel.Builder<NodeT, ModelT> {

        override val roles: MutableSet<String> = roles.toMutableSet()

        @SerialName("childModel")
        override val childModels: MutableList<ChildProcessModel.Builder<NodeT, ModelT>> = childModels.toMutableList()

        constructor() : this(nodes = emptyList())

        constructor(base: RootProcessModel<*, *>) :
            this(nodes = emptyList(),
                 childModels = emptyList(),
                 name = base.name,
                 handle = (base as? ReadableHandleAware<*>)?.getHandle()?.handleValue ?: -1L,
                 owner = base.owner,
                 roles = base.roles.toMutableList(),
                 uuid = base.uuid,
                 imports = base.imports.toMutableList(),
                 exports = base.exports.toMutableList()) {

            base.childModels.mapTo(childModels) { childProcessModel -> childModelBuilder(childProcessModel) }

            base.getModelNodes().mapTo(nodes) {
                it.visit(object : ProcessNode.Visitor<ProcessNode.IBuilder<NodeT, ModelT>> {
                    override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
                    override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
                    override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
                    override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
                    override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
                })
            }
        }

        override abstract fun build(pedantic: Boolean): RootProcessModelBase<NodeT, ModelT>

        @Transient
        override val elementName: QName
            get() = ELEMENTNAME

        abstract fun childModelBuilder(): ChildProcessModelBase.Builder<NodeT, ModelT>

        abstract fun childModelBuilder(base: ChildProcessModel<*, *>): ChildProcessModelBase.Builder<NodeT, ModelT>

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            val value = attributeValue.toString()
            when (attributeLocalName.toString()) {
                "name"                      -> name = value
                "owner"                     -> owner = SimplePrincipal(value)
                RootProcessModel.ATTR_ROLES -> roles.replaceBy(value.split(" *, *".toRegex()).filter(String::isEmpty))
                "uuid"                      -> uuid = value.toUUID()
                else                        -> return false
            }
            return true
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            return when {
                reader.isElement(ProcessConsts.Engine.NAMESPACE,
                                 ChildProcessModel.ELEMENTLOCALNAME) -> {
                    childModels.add(childModelBuilder().deserializeHelper(reader)); true
                }
                else                                                 -> super.deserializeChild(reader)
            }
        }

        override fun toString(): String {
            return "${this::class.simpleName}(nodes=$nodes, name=$name, handle=$handle, owner=$owner, roles=$roles, uuid=$uuid, imports=$imports, exports=$exports)"
        }

        abstract class BaseSerializer<T : RootProcessModelBase.Builder<*, *>> : ProcessModelBase.Builder.BaseSerializer<T>() {
            val nameIdx by lazy { serialClassDesc.getElementIndex("name") }
            val ownerIdx by lazy { serialClassDesc.getElementIndex("owner") }
            val rolesIdx by lazy { serialClassDesc.getElementIndex("roles") }
            val uuidIdx by lazy { serialClassDesc.getElementIndex("uuid") }
            val handleIdx by lazy { serialClassDesc.getElementIndex("handle") }
            val childModelsIdx by lazy { serialClassDesc.getElementIndex("childModels") }

            override fun readElement(result: T, input: KInput, index: Int) {
                when (index) {
                    nameIdx  -> result.name = input.readNullableString(serialClassDesc, index)
                    handleIdx -> result.handle = input.readLongElementValue(serialClassDesc, index)
                    ownerIdx -> input.readNullableString(serialClassDesc, index)?.let {
                        result.owner = SimplePrincipal(it)
                    }
                    rolesIdx -> input.readNullableString(serialClassDesc, index)?.let { value ->
                        result.roles.replaceBy(value.split(" *, *".toRegex()).filter(String::isEmpty))
                    }
                    uuidIdx  -> result.uuid = input.readNullableString(serialClassDesc, index)?.toUUID()
                    childModelsIdx -> {
                        val newList = input.updateSerializableElementValue(serialClassDesc,
                                                                                                                index,
                                                                                                                XmlActivity.ChildModelBuilder.list,
                                                                                                                result.childModels as List<XmlActivity.ChildModelBuilder>)
                        (result.childModels as MutableList<ChildProcessModel.Builder<XmlProcessNode, XmlModelCommon>>).replaceBy(newList)
                    }
                    else     -> super.readElement(result, input, index)
                }
            }

            override fun save(output: KOutput, obj: T) {
                output.context.klassSerializer(XmlProcessModel::class).save(output, XmlProcessModel(obj))
            }
        }

        companion object {

            @JvmStatic
            fun <B : Builder<T, M>, T : ProcessNode<T, M>, M : ProcessModel<T, M>?> deserialize(builder: B, reader: XmlReader): B {

                reader.skipPreamble()
                val elementName = ELEMENTNAME
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

                val addedSplits = mutableListOf<ProcessNode.IBuilder<T,M>>()
                for (node in builder.nodes) {
                    node.id?.let { nodeId ->
                        for (pred in node.predecessors) {
                            val predNode =builder.nodes.firstOrNull { it.id == pred.id }
                            if (predNode !=null && predNode.successors.isNotEmpty() && predNode !is Split.Builder) {
                                // A special case for the compatibility option where splits are introduced.
                                // non-split builders now no longer accept multiple successors
                                val predSuccessor = predNode.successors.single()
                                val oldSuccessor = builder.nodes.firstOrNull{ it.id == predSuccessor.id }
                                if (oldSuccessor!=null) {
                                    val splitName = builder.newId("split")
                                    val split = builder.splitBuilder().apply {
                                        id = splitName
                                        predecessor = pred
                                        successors.replaceBy(predSuccessor.identifier, Identifier(nodeId))
                                    }
                                    addedSplits.add(split)
                                    val splitIdentifier = Identifier(splitName)
                                    predNode.removeSuccessor(predSuccessor)
                                    predNode.addSuccessor(splitIdentifier)
                                    oldSuccessor.removePredecessor(pred)
                                    oldSuccessor.addPredecessor(splitIdentifier)
                                    node.removePredecessor(pred)
                                    node.addPredecessor(splitIdentifier)
                                }
                            } else {
                                predNode?.addSuccessor(Identifier(nodeId))
                            }
                        }
                    }
                }
                builder.nodes.addAll(addedSplits)

                return builder
            }

        }


    }

    private class ChildModelProvider<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.BuildHelper<NodeT, ModelT>, Sequence<ChildProcessModelBase<NodeT, ModelT>> {

        private val nodeFactory: NodeFactory<NodeT, ModelT>
        override val pedantic: Boolean
        override val newOwner: ModelT

        private val data: LinkedHashMap<String?, Node>

        private constructor(nodeFactory: NodeFactory<NodeT, ModelT>,
                            pedantic: Boolean,
                            newOwner: ModelT,
                            data: LinkedHashMap<String?, ChildModelProvider<NodeT, ModelT>.Node>) {
            this.nodeFactory = nodeFactory
            this.pedantic = pedantic
            this.newOwner = newOwner
            this.data = data
        }

        constructor(childModelBuilders: List<ChildProcessModel.Builder<*, *>>,
                    nodeFactory: NodeFactory<NodeT, ModelT>,
                    pedantic: Boolean,
                    newOwner: RootProcessModel<NodeT, ModelT>) {
            this.nodeFactory = nodeFactory
            this.pedantic = pedantic
            this.newOwner = newOwner.asM
            this.data = childModelBuilders.associateByTo(LinkedHashMap(childModelBuilders.size),
                                                         ChildProcessModel.Builder<*, *>::childId, this::Node)
        }

        constructor(orig: ChildModelProvider<NodeT, ModelT>, newOwner: ModelT) {
            nodeFactory = orig.nodeFactory
            pedantic = orig.pedantic
            data = orig.data
            this.newOwner = newOwner
        }

        override fun withOwner(newOwner: ModelT): ProcessModel.BuildHelper<NodeT, ModelT> {
            return ChildModelProvider<NodeT, ModelT>(this, newOwner)
        }

        @Deprecated("Use the childModel method", ReplaceWith("childModel(id)"))
        operator fun get(id: String) = childModel(id)

        override fun childModel(childId: String): ChildProcessModel<NodeT, ModelT> {
            return data[childId]?.invoke() ?: throw IllegalProcessModelException(
                "No child model with id ${childId} exists")
        }

        override fun node(builder: ProcessNode.IBuilder<*, *>): NodeT = nodeFactory.invoke(builder, this)

        override fun iterator() = data.values.asSequence().map { it.invoke() }.iterator()

        private inner class Node {
            constructor(builder: ChildProcessModel.Builder<*, *>) {
                this.builder = builder
            }

            constructor(model: ChildProcessModelBase<NodeT, ModelT>) {
                builder = null
                this.model = model
            }

            var builder: ChildProcessModel.Builder<*, *>?
            var model: ChildProcessModelBase<NodeT, ModelT>? = null

            operator fun invoke(): ChildProcessModelBase<NodeT, ModelT> {
                model?.let { return it }
                val b = builder ?: throw IllegalProcessModelException(
                    "The process model has cyclic/recursive child models. This is not allowed")
                builder = null
                return nodeFactory(baseChildBuilder = b, buildHelper = this@ChildModelProvider).apply { model = this }
            }
        }
    }

    @SerialName("name")
    private var _name: String? = null

    @SerialName("handle")
    @XmlDefault("-1")
    private var _handle = -1L

    @Transient
    val handleValue: Long get() = _handle

    /**
     * Set the owner of a model
     * @param owner
     */
    @Transient
    private var _owner: Principal = SYSTEMPRINCIPAL

    override var owner: Principal
        get() = _owner
        set(value) {
            _owner = value
        }

    @SerialName("roles")
    private var _roles: MutableSet<String> = ArraySet()
    private var uuid: UUID?

    override val childModels: Collection<ChildProcessModelBase<NodeT, ModelT>> get() = _childModels
    @Transient
    private val _childModels: IdentifyableSet<out ChildProcessModelBase<NodeT, ModelT>>

    @Transient
    override final val _processNodes: MutableIdentifyableSet<NodeT>

    constructor(builder: RootProcessModel.Builder<*, *>,
                nodeFactory: NodeFactory<NodeT, ModelT>,
                pedantic: Boolean = builder.defaultPedantic) : super(builder, pedantic) {
        @Suppress("LeakingThis")
        val childModelProvider = ChildModelProvider(builder.childModels, nodeFactory, pedantic, this)
        _processNodes = buildNodes(builder, childModelProvider)
        this._childModels = IdentifyableSet.processNodeSet(childModelProvider)

        this._name = builder.name
        this._handle = builder.handle
        this._owner = builder.owner
        this._roles = builder.roles.toMutableArraySet()
        this.uuid = builder.uuid
    }

    abstract fun builder(): Builder<NodeT, ModelT>

    open fun update(body: (Builder<NodeT, ModelT>) -> Unit): ModelT {
        return builder().apply(body).build().asM
    }

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {

        if (getModelNodes().any { it.id == null }) {
            builder().build().serialize(out)
        } else {
            out.smartStartTag(ELEMENTNAME) {
                writeAttribute("name", name)
                writeAttribute("owner", _owner.getName())

                if (_roles.isNotEmpty()) {
                    writeAttribute(ATTR_ROLES, roles.joinToString(","))
                }
                if (uuid != null) {
                    writeAttribute("uuid", uuid!!.toString())
                }
                ignorableWhitespace("\n  ")
                writeChildren(imports)
                writeChildren(exports)
                writeChildren(_childModels)
                writeChildren(getModelNodes())
                ignorableWhitespace("\n")
            }
        }
    }

    fun hasUnpositioned() = modelNodes.any { !it.hasPos() }

    override fun getChildModel(childId: Identifiable) = _childModels[childId]

    override fun getUuid(): UUID? {
        return uuid
    }

    fun setUuid(uuid: UUID) {
        this.uuid = uuid
    }

    /**
     * Get the name of the model.

     * @return
     */
    override fun getName(): String? {
        return _name
    }

    /**
     * Set the name of the model.

     * @param name The name
     */
    fun setName(name: String?) {
        _name = name
    }

    /**
     * Get the handle recorded for this model.
     */
    override fun getHandle(): Handle<out RootProcessModelBase<NodeT, ModelT>> {
        return Handles.handle(_handle)
    }

    /**
     * Set the handle for this model.
     */
    override fun setHandleValue(handleValue: Long) {
        _handle = handleValue
    }

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
       */
    override fun getRef(): IProcessModelRef<NodeT, ModelT, RootProcessModel<NodeT, ModelT>> {
        return ProcessModelRef(name, this.getHandle(), uuid)
    }

    override fun getRoles(): Set<String> {
        return _roles ?: HashSet<String>().apply { _roles = this }
    }

    fun setRoles(roles: Collection<String>) {
        _roles = HashSet(roles)
    }

    /**
     * Get an array of all process nodes in the model. Used by XmlProcessModel

     * @return An array of all nodes.
     */
    override fun getModelNodes(): List<NodeT> {
        return super.getModelNodes().toUnmodifyableList()
    }

    /**
     * Initiate the notification that a node has changed. Actual implementations can override this.
     * @param node The node that has changed.
     */
    @Deprecated("Use the builder to update models")
    open fun notifyNodeChanged(node: NodeT) {
        // no implementation here
    }

    abstract class BaseSerializer<T : RootProcessModelBase<*, *>> : ProcessModelBase.BaseSerializer<T>() {
        val nameIdx by lazy { serialClassDesc.getElementIndex("name") }
        val ownerIdx by lazy { serialClassDesc.getElementIndex("owner") }
        val rolesIdx by lazy { serialClassDesc.getElementIndex("roles") }
        val uuidIdx by lazy { serialClassDesc.getElementIndex("uuid") }

        override fun save(output: KOutput, obj: T) {
            if (obj.getModelNodes().any { it.id == null }) {
                val builder = obj.builder()
                val rebuilt = builder.build().asM
                super.save(output, rebuilt as T)
            } else {
                super.save(output, obj)
            }
        }

        override fun writeValues(output: KOutput, obj: T) {
            output.writeNullableStringElementValue(serialClassDesc, nameIdx, obj.name)
            output.writeStringElementValue(serialClassDesc, ownerIdx, obj.owner.getName())

            val rolesString = if (obj.roles.isEmpty()) null else obj.roles.joinToString(",")
            output.writeNullableStringElementValue(serialClassDesc, rolesIdx, rolesString)
            output.writeNullableStringElementValue(serialClassDesc, uuidIdx, obj.uuid?.toString())

            super.writeValues(output, obj)
        }
    }

    @Serializer(forClass = RootProcessModelBase::class)
    companion object: KSerializer<RootProcessModelBase<*,*>> {

        fun serialClassDesc(name: String): SerialClassDescImpl {
            return SerialClassDescImpl(serialClassDesc, name)
        }

        override fun load(input: KInput): RootProcessModelBase<*, *> {
            throw UnsupportedOperationException("Deserialization of the abstract class is not supported")
        }

        override fun save(output: KOutput, obj: RootProcessModelBase<*, *>) {
            throw UnsupportedOperationException("Serialization of the abstract class is not supported")
        }

        const val ELEMENTLOCALNAME = "processModel"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
        const val ATTR_ROLES = "roles"
        const val ATTR_NAME = "name"

        private fun getOrDefault(map: Map<String, Int>, key: String, defaultValue: Int): Int {
            return map[key] ?: defaultValue
        }

        @Throws(XmlException::class)
        @JvmStatic
        @Deprecated("Remove convenience building", ReplaceWith("Builder.deserialize(builder, reader).build().asM()"))
        fun <T : ProcessNode<T, M>, M : RootProcessModelBase<T, M>> deserialize(builder: Builder<T, M>,
                                                                                reader: XmlReader): M {
            return builder.deserialize(reader).build().asM
        }
    }
}


fun <B : RootProcessModelBase.Builder<*, *>> B.deserialize(reader: XmlReader): B {
    return this.deserializeHelper(reader)
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
    fun serializerByValue(obj: ProcessNode<*, *>, context: SerialContext?): KSerializer<out ProcessNode<*, *>> {
        // If the context has a serializer use that
        context?.getSerializerByClass(obj::class)?.let { return it }
        // Otherwise fall back to "known" serializers
        when (obj) {
            is StartNode -> return XmlStartNode::class.serializer()
            is Activity  -> return XmlActivity::class.serializer()
            is Split     -> return XmlSplit::class.serializer()
            is Join      -> return XmlJoin::class.serializer()
            is EndNode   -> return XmlEndNode::class.serializer()
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
    fun serializerBySerialDescClassname(klassName: String,
                                        context: SerialContext?): KSerializer<out ProcessNode.IBuilder<*, *>> {
        if (klassName.startsWith(NODE_PACKAGE)) {
            serializerBySimpleName(klassName.substring(NODE_PACKAGE.length+1))?.let { return it }
        } else if (klassName=="nl.adaptivity.xml.serialization.canary.CanaryInput\$Dummy")  {
            return context.klassSerializer(XmlActivity.Builder::class)
        }
        throw IllegalArgumentException("No serializer found for class $klassName")
    }

    @JvmStatic
    fun serializerBySimpleName(simpleName: String,
                               context: SerialContext? = null): KSerializer<out ProcessNode.IBuilder<*, *>>? = when (simpleName) {
        "XmlStartNode",
        "XmlStartNode\$Builder" -> context.klassSerializer(XmlStartNode.Builder::class)
        "XmlActivity",
        "XmlActivity\$Builder"  -> context.klassSerializer(XmlActivity.Builder::class)
        "XmlSplit",
        "XmlSplit\$Builder"      -> context.klassSerializer(XmlSplit.Builder::class)
        "XmlJoin",
        "XmlJoin\$Builder"       -> context.klassSerializer(XmlJoin.Builder::class)
        "XmlEndNode",
        "XmlEndNode\$Builder"    -> context.klassSerializer(XmlEndNode.Builder::class)
        else                  -> null
    }

}