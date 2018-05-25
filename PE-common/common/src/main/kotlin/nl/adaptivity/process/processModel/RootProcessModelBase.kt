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
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.util.addField
import nl.adaptivity.util.addFields
import nl.adaptivity.util.multiplatform.*
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlDefault
import nl.adaptivity.xml.serialization.readNullableString
import nl.adaptivity.xml.serialization.writeNullableStringElementValue

abstract class RootProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
    ProcessModelBase<NodeT, ModelT>,
    RootProcessModel<NodeT, ModelT>,
    MutableHandleAware<RootProcessModel<out NodeT, out ModelT>>,
    XmlSerializable {

    @SerialName("name")
    private var _name: String? = null

    @SerialName("handle")
    @XmlDefault("-1")
    private var _handle = -1L

    @Transient
    val handleValue: Long
        get() = _handle

    /**
     * The owner of a model
     */
    final override var owner: Principal = SYSTEMPRINCIPAL

    @SerialName("roles")
    private var _roles: MutableSet<String> = ArraySet()


    override val roles: Set<String>
        get() = _roles


    final override val uuid: UUID?

    @SerialName("childModel")
    override val childModels: Collection<ChildProcessModelBase<NodeT, ModelT>> get() = _childModels

    @Transient
    private val _childModels: IdentifyableSet<ChildProcessModelBase<NodeT, ModelT>>

    @Transient
    private val _processNodes: MutableIdentifyableSet<NodeT>

    /**
     * Get an array of all process nodes in the model. Used by XmlProcessModel
     *
     * @return An array of all nodes.
     */
    override val modelNodes: IdentifyableSet<NodeT>
        get() = _processNodes.readOnly()

    constructor(builder: RootProcessModel.Builder<*, *>,
                nodeFactory: NodeFactory<NodeT, ModelT>,
                pedantic: Boolean = builder.defaultPedantic) : super(builder, pedantic) {
        @Suppress("LeakingThis")
        val childModelProvider = ChildModelProvider(builder.childModels, nodeFactory, pedantic, this)
        _processNodes = buildNodes(builder, childModelProvider)
        this._childModels = IdentifyableSet.processNodeSet(childModelProvider)

        this._name = builder.name
        this._handle = builder.handle
        this.owner = builder.owner
        this._roles = builder.roles.toMutableArraySet()
        this.uuid = builder.uuid
    }

    /**
     * Constructor only provided to allow for serialization
     */
    @Suppress("unused")
    protected constructor() : super(emptyList(), emptyList()) {
        this._childModels = IdentifyableSet.processNodeSet()
        this._processNodes = IdentifyableSet.processNodeSet()
        this._name = null
        this._handle = -1L
        this.owner = SYSTEMPRINCIPAL
        this._roles = ArraySet()
        this.uuid = null
    }

    abstract override fun copy(imports: Collection<IXmlResultType>,
                               exports: Collection<IXmlDefineType>,
                               nodes: Collection<NodeT>,
                               name: String?,
                               uuid: UUID?,
                               roles: Set<String>,
                               owner: Principal,
                               childModels: Collection<ChildProcessModel<NodeT, ModelT>>): RootProcessModelBase<NodeT, ModelT>

    abstract override fun builder(): Builder<NodeT, ModelT>

    open fun update(body: (Builder<NodeT, ModelT>) -> Unit): ModelT {
        return builder().apply(body).build().asM
    }

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {

        if (modelNodes.any { it.id == null }) {
            builder().build().serialize(out)
        } else {
            out.smartStartTag(ELEMENTNAME) {
                writeAttribute("name", name)
                writeAttribute("owner", owner.getName())

                if (_roles.isNotEmpty()) {
                    writeAttribute(ATTR_ROLES, roles.joinToString(","))
                }
                if (uuid != null) {
                    writeAttribute("uuid", uuid.toString())
                }
                ignorableWhitespace("\n  ")
                writeChildren(imports)
                writeChildren(exports)
                writeChildren(_childModels)
                writeChildren(modelNodes)
                ignorableWhitespace("\n")
            }
        }
    }

    fun hasUnpositioned() = modelNodes.any { !it.hasPos() }

    override fun getChildModel(childId: Identifiable) = _childModels[childId]

    /**
     * The name of the model.
     */
    override val name: String? get() = _name

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
    override fun getHandle(): Handle<RootProcessModelBase<NodeT, ModelT>> {
        return handle(handle = _handle)
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
    override val ref: IProcessModelRef<NodeT, ModelT, RootProcessModel<NodeT, ModelT>>
        get() {
            return ProcessModelRef(name, this.getHandle(), uuid)
        }
    fun setRoles(roles: Collection<String>) {
        _roles = HashSet(roles)
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

        other as RootProcessModelBase<*, *>

        if (_name != other._name) return false
        if (_handle != other._handle) return false
        if (owner != other.owner) return false
        if (_roles != other._roles) return false
        if (uuid != other.uuid) return false
        if (_childModels != other._childModels) return false
        if (_processNodes != other._processNodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (_name?.hashCode() ?: 0)
        result = 31 * result + _handle.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + _roles.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + _childModels.hashCode()
        result = 31 * result + _processNodes.hashCode()
        return result
    }

    override fun toString(): String {
        return "RootProcessModelBase(name=$_name, handle=$_handle, owner=$owner, roles=$_roles, uuid=$uuid, childModels=$_childModels, processNodes=$_processNodes) ${super.toString()}"
    }


    abstract class BaseSerializer<T : RootProcessModelBase<*, *>> : ProcessModelBase.BaseSerializer<T>() {
        private val nameIdx by lazy { serialClassDesc.getElementIndexOrThrow("name") }
        private val ownerIdx by lazy { serialClassDesc.getElementIndexOrThrow("owner") }
        private val rolesIdx by lazy { serialClassDesc.getElementIndexOrThrow("roles") }
        private val uuidIdx by lazy { serialClassDesc.getElementIndexOrThrow("uuid") }
        private val handleIdx  by lazy { serialClassDesc.getElementIndexOrThrow("handle") }
        private val childModelIdx  by lazy { serialClassDesc.getElementIndexOrThrow("childModel") }

        protected abstract val childModelSerializer: KSerializer<ChildProcessModel<*,*>>

        override fun save(output: KOutput, obj: T) {
            if (obj.modelNodes.any { it.id == null }) {
                val builder = obj.builder()
                val rebuilt = builder.build()
                @Suppress("UNCHECKED_CAST")
                super.save(output, rebuilt as T)
            } else {
                super.save(output, obj)
            }
        }

        override fun writeValues(output: KOutput, obj: T) {
            val desc = serialClassDesc
            output.writeNullableStringElementValue(desc, nameIdx, obj.name)
            output.writeStringElementValue(desc, ownerIdx, obj.owner.getName())
            if (obj.handleValue>=0) output.writeLongElementValue(desc, handleIdx, obj.handleValue)

            val rolesString = if (obj.roles.isEmpty()) null else obj.roles.joinToString(",")
            output.writeNullableStringElementValue(desc, rolesIdx, rolesString)
            output.writeNullableStringElementValue(desc, uuidIdx, obj.uuid?.toString())
            output.writeSerializableElementValue(desc, childModelIdx, childModelSerializer.list, obj._childModels)

            super.writeValues(output, obj)
        }
    }

    companion object {

        fun serialClassDesc(name: String): SerialClassDescImpl {
            return SerialClassDescImpl(name).apply {
                addField(RootProcessModelBase<*, *>::_name)
                addField(RootProcessModelBase<*, *>::_handle)
                addField(RootProcessModelBase<*,*>::owner)
                addField(RootProcessModelBase<*,*>::roles)
                addField(RootProcessModelBase<*,*>::uuid)
                addField(RootProcessModelBase<*,*>::childModels)
                addFields(ProcessModelBase.serialClassDesc)
            }
        }

        const val ELEMENTLOCALNAME = "processModel"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
        const val ATTR_ROLES = "roles"

        @Throws(XmlException::class)
        @JvmStatic
        @Deprecated("Remove convenience building", ReplaceWith("Builder.deserialize(builder, reader).build().asM()"))
        fun <T : ProcessNode<T, M>, M : RootProcessModelBase<T, M>> deserialize(builder: Builder<T, M>,
                                                                                reader: XmlReader): M {
            return builder.deserialize(reader).build().asM
        }
    }


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
        final override val childModels: MutableList<ChildProcessModel.Builder<NodeT, ModelT>> = childModels.toMutableList()

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

        abstract override fun build(pedantic: Boolean): RootProcessModelBase<NodeT, ModelT>

        @Transient
        override val elementName: QName
            get() = ELEMENTNAME

        abstract fun childModelBuilder(): ChildProcessModelBase.Builder<NodeT, ModelT>

        abstract fun childModelBuilder(base: ChildProcessModel<*, *>): ChildProcessModelBase.Builder<NodeT, ModelT>

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

        abstract class BaseSerializer<T : RootProcessModelBase.Builder<*, *>> : ProcessModelBase.Builder.BaseSerializer<T>() {
            private val nameIdx by lazy { serialClassDesc.getElementIndexOrThrow("name") }
            private val ownerIdx by lazy { serialClassDesc.getElementIndexOrThrow("owner") }
            private val rolesIdx by lazy { serialClassDesc.getElementIndexOrThrow("roles") }
            private val uuidIdx by lazy { serialClassDesc.getElementIndexOrThrow("uuid") }
            private val handleIdx by lazy { serialClassDesc.getElementIndexOrThrow("handle") }
            private val childModelsIdx by lazy { serialClassDesc.getElementIndexOrThrow(ChildProcessModel.ELEMENTLOCALNAME) }

            override fun readElement(result: T, input: KInput, index: Int) {
                when (index) {
                    nameIdx        -> result.name = input.readNullableString(serialClassDesc, index)
                    handleIdx      -> result.handle = input.readLongElementValue(serialClassDesc, index)
                    ownerIdx       -> input.readNullableString(serialClassDesc, index)?.let {
                        result.owner = SimplePrincipal(it)
                    }
                    rolesIdx       -> input.readNullableString(serialClassDesc, index)?.let { value ->
                        result.roles.replaceBy(value.split(" *, *".toRegex()).filter(String::isEmpty))
                    }
                    uuidIdx        -> result.uuid = input.readNullableString(serialClassDesc, index)?.toUUID()
                    childModelsIdx -> {
                        @Suppress("UNCHECKED_CAST")
                        val newList = input.updateSerializableElementValue(serialClassDesc,
                                                                           index,
                                                                           XmlChildModel.Builder.serializer().list,
                                                                           result.childModels as List<XmlActivity.ChildModelBuilder>)
                        @Suppress("UNCHECKED_CAST")
                        (result.childModels as MutableList<ChildProcessModel.Builder<XmlProcessNode, XmlModelCommon>>).replaceBy(newList)
                    }
                    else           -> super.readElement(result, input, index)
                }
            }

            override fun save(output: KOutput, obj: T) {
                output.context.klassSerializer(XmlProcessModel::class).save(output, XmlProcessModel(obj))
            }
        }

        companion object {

            @JvmStatic
            fun <B : Builder<T, M>, T : ProcessNode<T, M>, M : ProcessModel<T, M>?> deserialize(builder: B,
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
                        reader.unhandledEvent()
                    }
                }

                val addedSplits = mutableListOf<ProcessNode.IBuilder<T, M>>()
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

    private class ChildModelProvider<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.BuildHelper<NodeT, ModelT>, Sequence<ChildProcessModelBase<NodeT, ModelT>> {

        private val nodeFactory: NodeFactory<NodeT, ModelT>
        override val pedantic: Boolean
        override val newOwner: ModelT

        private val data: LinkedHashMap<String?, Node>

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
            return ChildModelProvider(this, newOwner)
        }

        @Deprecated("Use the childModel method", ReplaceWith("childModel(id)"))
        operator fun get(id: String) = childModel(id)

        override fun childModel(childId: String): ChildProcessModel<NodeT, ModelT> {
            return data[childId]?.invoke() ?: throw IllegalProcessModelException(
                "No child model with id $childId exists")
        }

        override fun node(builder: ProcessNode.IBuilder<*, *>): NodeT = nodeFactory.invoke(builder, this)

        override fun condition(text: String): Condition = nodeFactory.condition(text)

        override fun iterator() = data.values.asSequence().map { it.invoke() }.iterator()

        private inner class Node(builder: ChildProcessModel.Builder<*, *>) {

            var builder: ChildProcessModel.Builder<*, *>? = builder
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

}


fun <B : RootProcessModelBase.Builder<*, *>> B.deserialize(reader: XmlReader): B {
    return this.deserializeHelper(reader)
}

