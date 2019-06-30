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
import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.*
import nl.adaptivity.util.PrincipalSerializer
import nl.adaptivity.util.UUIDSerializer
import nl.adaptivity.util.multiplatform.*
import nl.adaptivity.util.security.Principal
import nl.adaptivity.serialutil.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import kotlin.jvm.JvmStatic

@Serializable
abstract class RootProcessModelBase<NodeT : ProcessNode> :
    ProcessModelBase<@ContextualSerialization NodeT>,
    RootProcessModel<NodeT>,
    MutableHandleAware<RootProcessModel<NodeT>>,
    XmlSerializable {

    /**
     * The name of the model.
     */
    override val name: String?

    @Transient
    private var _handle = -1L

    @SerialName("handle")
    @XmlDefault("-1")
    val handleValue: Long
        get() = _handle

    /**
     * The owner of a model
     */
    @Serializable(PrincipalSerializer::class)
    final override var owner: Principal = SYSTEMPRINCIPAL

    @SerialName("roles")
    override val roles: Set<String>

    @Serializable(UUIDSerializer::class)
    final override val uuid: UUID?

    @SerialName("childModel")
    override val childModels: Collection<ChildProcessModelBase<NodeT>>
        get() = _childModels

    @Transient
    private val _childModels: IdentifyableSet<ChildProcessModelBase<NodeT>>

    @SerialName("nodes")
    @Serializable(IdentifiableSetSerializer::class)
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
    private val _processNodes: MutableIdentifyableSet<NodeT>

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
       */
    @Transient
    override val ref: IProcessModelRef<NodeT, RootProcessModel<NodeT>>
        get() {
            return ProcessModelRef(name, this.getHandle(), uuid)
        }

    /**
     * Get an array of all process nodes in the model. Used by XmlProcessModel
     *
     * @return An array of all nodes.
     */
    @Transient
    override val modelNodes: IdentifyableSet<NodeT>
        get() = _processNodes.readOnly()

    constructor(builder: RootProcessModel.Builder,
                nodeFactory: NodeFactory<NodeT, NodeT, ChildProcessModelBase<NodeT>>,
                pedantic: Boolean = builder.defaultPedantic) : super(builder, pedantic) {
        @Suppress("LeakingThis")
        val childModelProvider = ChildModelProvider<NodeT, ProcessModel<NodeT>, RootProcessModel<NodeT>, ChildProcessModelBase<NodeT>>(
            builder.childModels, nodeFactory, pedantic, this)
        _processNodes = buildNodes(builder, childModelProvider)
        this._childModels = IdentifyableSet.processNodeSet(childModelProvider)

        this.name = builder.name
        this._handle = builder.handle
        this.owner = builder.owner
        this.roles = builder.roles.toArraySet()
        this.uuid = builder.uuid
    }

    /**
     * Constructor only provided to allow for serialization
     */
    @Suppress("unused")
    protected constructor() : super(emptyList(), emptyList()) {
        this._childModels = IdentifyableSet.processNodeSet()
        this._processNodes = IdentifyableSet.processNodeSet()
        this.name = null
        this._handle = -1L
        this.owner = SYSTEMPRINCIPAL
        this.roles = ArraySet()
        this.uuid = null
    }

    abstract override fun copy(imports: Collection<IXmlResultType>,
                               exports: Collection<IXmlDefineType>,
                               nodes: Collection<ProcessNode>,
                               name: String?,
                               uuid: UUID?,
                               roles: Set<String>,
                               owner: Principal,
                               childModels: Collection<ChildProcessModel<NodeT>>): RootProcessModelBase<NodeT>

    abstract override fun builder(): Builder

    abstract fun update(body: (Builder) -> Unit): RootProcessModelBase<NodeT>/* {
        return builder().apply(body).build()
    }*/

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {

        if (modelNodes.any { it.id == null }) {
            builder().build().serialize(out)
        } else {
            out.smartStartTag(ELEMENTNAME) {
                writeAttribute("name", name)
                writeAttribute("owner", owner.getName())

                if (roles.isNotEmpty()) {
                    writeAttribute(ATTR_ROLES, roles.joinToString(","))
                }
                if (uuid != null) {
                    writeAttribute("uuid", uuid.toString())
                }
                ignorableWhitespace("\n  ")
                writeChildren(imports)
                writeChildren(exports)
                writeChildren(_childModels as Iterable<XmlSerializable>)
                writeChildren(modelNodes)
                ignorableWhitespace("\n")
            }
        }
    }

    fun hasUnpositioned() = modelNodes.any { !it.hasPos() }

    override fun getChildModel(childId: Identifiable) = _childModels[childId]

    /**
     * Get the handle recorded for this model.
     */
    override fun getHandle(): Handle<RootProcessModelBase<NodeT>> {
        return handle(handle = _handle)
    }

    /**
     * Set the handle for this model.
     */
    override fun setHandleValue(handleValue: Long) {
        _handle = handleValue
    }

    /**
     * Initiate the notification that a node has changed. Actual implementations can override this.
     * @param node The node that has changed.
     */
    @Deprecated("Use the builder to update models")
    open fun notifyNodeChanged(node: NodeT) {
        // no implementation here
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as RootProcessModelBase<*>

        if (name != other.name) return false
        if (_handle != other._handle) return false
        if (owner != other.owner) return false
        if (roles != other.roles) return false
        if (uuid != other.uuid) return false
        if (_childModels != other._childModels) return false
        if (_processNodes != other._processNodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + _handle.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + roles.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + _childModels.hashCode()
        result = 31 * result + _processNodes.hashCode()
        return result
    }

    override fun toString(): String {
        return "RootProcessModelBase(name=$name, handle=$_handle, owner=$owner, roles=$roles, uuid=$uuid, childModels=$_childModels, processNodes=$_processNodes) ${super.toString()}"
    }


    abstract class BaseSerializer<T : RootProcessModelBase<*>> : ProcessModelBase.BaseSerializer<T>() {
        private val nameIdx by lazy { descriptor.getElementIndexOrThrow("name") }
        private val ownerIdx by lazy { descriptor.getElementIndexOrThrow("owner") }
        private val rolesIdx by lazy { descriptor.getElementIndexOrThrow("roles") }
        private val uuidIdx by lazy { descriptor.getElementIndexOrThrow("uuid") }
        private val handleIdx by lazy { descriptor.getElementIndexOrThrow("handle") }
        private val childModelIdx by lazy { descriptor.getElementIndexOrThrow("childModel") }

        protected abstract val childModelSerializer: KSerializer<ChildProcessModel<*>>

        override fun serialize(encoder: Encoder, obj: T) {
            // For serialization node ids are required. If they are somehow missing, rebuild the model with ids.
            if (obj.modelNodes.any { it.id == null }) {
                val rebuilt = obj.builder().build()
                @Suppress("UNCHECKED_CAST")
                super.serialize(encoder, rebuilt as T)
            } else {
                super.serialize(encoder, obj)
            }
        }

        override fun writeValues(output: KOutput, obj: T) {
            val desc = descriptor
            output.encodeNullableStringElement(desc, nameIdx, obj.name)
            output.encodeStringElement(desc, ownerIdx, obj.owner.getName())
            if (obj.handleValue >= 0) output.encodeLongElement(desc, handleIdx, obj.handleValue)

            val rolesString = if (obj.roles.isEmpty()) null else obj.roles.joinToString(",")
            output.encodeNullableStringElement(desc, rolesIdx, rolesString)
            output.encodeNullableStringElement(desc, uuidIdx, obj.uuid?.toString())
            output.encodeSerializableElement(desc, childModelIdx, childModelSerializer.list, obj._childModels)

            super.writeValues(output, obj)
        }
    }

    companion object {

        const val ELEMENTLOCALNAME = "processModel"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
        const val ATTR_ROLES = "roles"

        @Throws(XmlException::class)
        @kotlin.jvm.JvmStatic
        @Deprecated("Remove convenience building", ReplaceWith("Builder.deserialize(builder, reader).build().asM()"))
        fun deserialize(builder: Builder, reader: XmlReader): RootProcessModelBase<out ProcessNode> {
            return builder.deserialize(reader).build()
        }
    }


    @ProcessModelDSL
    @Serializable
    abstract class Builder : ProcessModelBase.Builder, RootProcessModel.Builder {

        constructor(nodes: Collection<ProcessNode.IBuilder> = emptyList(),
                    childModels: Collection<ChildProcessModel.Builder> = emptyList(),
                    name: String? = null,
                    handle: Long = -1L,
                    owner: Principal = SYSTEMPRINCIPAL,
                    roles: Collection<String> = emptyList(),
                    uuid: UUID? = null,
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList())
            : super(nodes, imports, exports) {
            this.childModels = childModels.toMutableList()
            this.name = name
            this.handle = handle
            this.owner = owner
            this.roles = roles.toMutableSet()
            this.uuid = uuid
        }

        override var name: String? = null

        @SerialName("handle")
        @XmlDefault("-1")
        override var handle: Long = -1L

        @Serializable(PrincipalSerializer::class)
        override var owner: Principal = SYSTEMPRINCIPAL

        @Serializable(UUIDSerializer::class)
        override var uuid: UUID? = null

        override val roles: MutableSet<String>

        @SerialName("childModel")
        final override val childModels: MutableList<ChildProcessModel.Builder>

        constructor() : this(nodes = emptyList())

        constructor(base: RootProcessModel<*>) :
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

            base.modelNodes.mapTo(nodes) {
                it.visit(object : ProcessNode.Visitor<ProcessNode.IBuilder> {
                    override fun visitStartNode(startNode: StartNode) = startNodeBuilder(startNode)
                    override fun visitActivity(activity: Activity) = activityBuilder(activity)
                    override fun visitSplit(split: Split) = splitBuilder(split)
                    override fun visitJoin(join: Join) = joinBuilder(join)
                    override fun visitEndNode(endNode: EndNode) = endNodeBuilder(endNode)
                })
            }
        }

        abstract override fun build(pedantic: Boolean): RootProcessModelBase<out ProcessNode>

        @Transient
        override val elementName: QName
            get() = ELEMENTNAME

        abstract fun childModelBuilder(): ChildProcessModelBase.Builder

        abstract fun childModelBuilder(base: ChildProcessModel<*>): ChildProcessModelBase.Builder

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            when (attributeLocalName) {
                "name"  -> name = attributeValue
                "owner" -> owner = SimplePrincipal(attributeValue)
                "roles" -> roles.replaceBy(attributeValue.split(" *, *".toRegex()).filter(String::isEmpty))
                "uuid"  -> uuid = attributeValue.toUUID()
                else    -> return false
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

        abstract class BaseSerializer<T : RootProcessModelBase.Builder> : ProcessModelBase.Builder.BaseSerializer<T>() {

            override fun readElement(result: T, input: KInput, index: Int, name: String) {
                when (name) {
                    "name"                             -> result.name = input.readNullableString(descriptor, index)
                    "handle"                           -> result.handle = input.decodeLongElement(descriptor, index)
                    "owner"                            -> input.readNullableString(descriptor, index)?.let {
                        result.owner = SimplePrincipal(it)
                    }
                    "roles"                            -> input.readNullableString(descriptor, index)?.let { value ->
                        result.roles.replaceBy(value.split(" *, *".toRegex()).filter(String::isEmpty))
                    }
                    "uuid"                             -> result.uuid = input.readNullableString(descriptor,
                                                                                                 index)?.toUUID()
                    ChildProcessModel.ELEMENTLOCALNAME -> {
                        @Suppress("UNCHECKED_CAST")
                        val newList = input.updateSerializableElement(descriptor,
                                                                      index,
                                                                      XmlChildModel.Builder.serializer().list,
                                                                      result.childModels as List<XmlActivity.ChildModelBuilder>)
                        @Suppress("UNCHECKED_CAST")
                        result.childModels.replaceBy(newList)
                    }
                    else                               -> super.readElement(result, input, index, name)
                }
            }

            override fun serialize(encoder: Encoder, obj: T) {
                XmlProcessModel.serializer().serialize(encoder, XmlProcessModel(obj))
            }
        }

        companion object {

            @JvmStatic
            fun <B : Builder> deserialize(builder: B,
                                          reader: XmlReader): B {

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
                        throw XmlException("Expected child tag, found other content")
                    }
                }

                val addedSplits = mutableListOf<ProcessNode.IBuilder>()
                for (node in builder.nodes) {
                    node.id?.let { nodeId ->
                        for (pred in node.predecessors) {
                            val predNode = builder.nodes.firstOrNull { it.id == pred.id }
                            if (predNode != null && predNode.successors.isNotEmpty() && predNode !is Split.Builder) {
                                // A special case for the compatibility option where splits are introduced.
                                // non-split builders now no longer accept multiple successors
                                val predSuccessor = predNode.successors.single()
                                val oldSuccessor = builder.nodes.firstOrNull { it.id == predSuccessor.id }
                                if (oldSuccessor != null) {
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

    private class ChildModelProvider<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>, RootT : RootProcessModel<NodeT>, ChildT : ChildProcessModel<NodeT>> : ProcessModel.BuildHelper<NodeT, ModelT, RootT, ChildT>, Sequence<ChildT> {

        private val nodeFactory: NodeFactory<NodeT, NodeT, ChildT>
        override val pedantic: Boolean
        override val newOwner: ModelT

        private val data: LinkedHashMap<String, Node>

        constructor(childModelBuilders: List<ChildProcessModel.Builder>,
                    nodeFactory: NodeFactory<NodeT, NodeT, ChildT>,
                    pedantic: Boolean,
                    newOwner: ModelT) {
            this.nodeFactory = nodeFactory
            this.pedantic = pedantic
            this.newOwner = newOwner
            this.data = childModelBuilders.associateByTo(LinkedHashMap(childModelBuilders.size),
                                                         { it.childId!! }, this::Node)
        }

        constructor(orig: ChildModelProvider<NodeT, *, RootT, ChildT>, newOwner: ModelT) {
            nodeFactory = orig.nodeFactory
            pedantic = orig.pedantic
            data = LinkedHashMap(orig.data as LinkedHashMap<String, Node>)
            this.newOwner = newOwner
        }

        override fun <M : ProcessModel<NodeT>> withOwner(newOwner: M): ProcessModel.BuildHelper<NodeT, M, RootT, ChildT> {
            return ChildModelProvider<NodeT, M, RootT, ChildT>(this, newOwner)
        }

        @Deprecated("Use the childModel method", ReplaceWith("childModel(id)"))
        operator fun get(id: String) = childModel(id)

        override fun childModel(childId: String): ChildT {
            return data[childId]?.invoke()
                   ?: throw IllegalProcessModelException("No child model with id $childId exists")
        }

        fun newId(idBase: String): String {
            return generateSequence(1) { it + 1 }
                .map { "$idBase$it" }
                .first { candidateId -> newOwner.modelNodes.none { it.id == candidateId } && candidateId !in data }
        }

        override fun childModel(builder: ChildProcessModel.Builder): ChildT {
            val childId: String = builder.childId ?: (newId(builder.childIdBase).also { builder.childId = it })
            val newNode = Node(builder)
            data[childId] = newNode
            return newNode()
        }

        override fun node(builder: ProcessNode.IBuilder): NodeT = nodeFactory.invoke(builder, this)

        override fun condition(text: String): Condition = nodeFactory.condition(text)

        override fun iterator() = data.values.asSequence().map { it.invoke() }.iterator()

        private inner class Node(builder: ChildProcessModel.Builder) {

            var builder: ChildProcessModel.Builder? = builder
            var model: ChildT? = null

            operator fun invoke(): ChildT {
                model?.let { return it }
                val b = builder ?: throw IllegalProcessModelException(
                    "The process model has cyclic/recursive child models. This is not allowed")
                builder = null
                return nodeFactory(baseChildBuilder = b, buildHelper = this@ChildModelProvider).also { model = it }
            }
        }
    }

}


fun <B : RootProcessModelBase.Builder> B.deserialize(reader: XmlReader): B {
    return this.deserializeHelper(reader)
}

