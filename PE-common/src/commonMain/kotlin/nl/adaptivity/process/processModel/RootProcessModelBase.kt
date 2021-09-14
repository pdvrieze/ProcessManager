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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import net.devrieze.util.*
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.security.name
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.IdentifiableSetSerializer
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.util.PrincipalSerializer
import nl.adaptivity.util.UUIDSerializer
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName

abstract class RootProcessModelBase<NodeT : ProcessNode> :
    ProcessModelBase<NodeT>,
    RootProcessModel<NodeT>,
    MutableHandleAware<RootProcessModel<NodeT>> {

    /**
     * The name of the model.
     */
    override val name: String?

    private var _handle = -1L

    val handleValue: Long
        get() = _handle

    /**
     * The owner of a model
     */
    final override var owner: Principal = SYSTEMPRINCIPAL

    override val roles: Set<String>

    final override val uuid: UUID?

    override val childModels: Collection<ChildProcessModelBase<NodeT>>
        get() = _childModels

    private var _childModels: IdentifyableSet<ChildProcessModelBase<NodeT>> = IdentifyableSet.processNodeSet()

    private val _processNodes: MutableIdentifyableSet<NodeT>

    /**
     * Get an array of all process nodes in the model. Used by XmlProcessModel
     *
     * @return An array of all nodes.
     */
    override val modelNodes: IdentifyableSet<NodeT>
        get() = _processNodes.readOnly()

    override val ref: IProcessModelRef<NodeT, RootProcessModel<NodeT>>
        get() {
            return ProcessModelRef(name, handle, uuid)
        }

    constructor(
        builder: RootProcessModel.Builder,
        nodeFactory: NodeFactory<NodeT, NodeT, ChildProcessModelBase<NodeT>>,
        pedantic: Boolean
    ) : super(builder, pedantic) {
        @Suppress("LeakingThis")
        val childModelProvider =
            ChildModelProvider<NodeT, ProcessModel<NodeT>, RootProcessModel<NodeT>, ChildProcessModelBase<NodeT>>(
                builder.childModels, nodeFactory, pedantic, this
            )
        _processNodes = buildNodes(builder, childModelProvider)
        this._childModels = IdentifyableSet.processNodeSet(childModelProvider)

        this.name = builder.name
        this._handle = builder.handle
        this.owner = builder.owner
        this.roles = builder.roles.toArraySet()
        this.uuid = builder.uuid
    }

    protected constructor(
        serialDelegate: SerialDelegate,
        nodeFactory: NodeFactory<NodeT, NodeT, ChildProcessModelBase<NodeT>>,
        pedantic: Boolean
    ) : this(Builder(serialDelegate), nodeFactory, pedantic)

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

    abstract override fun builder(): RootProcessModel.Builder

    abstract fun update(body: (RootProcessModel.Builder) -> Unit): RootProcessModelBase<NodeT>/* {
        return builder().apply(body).build()
    }*/

    fun hasUnpositioned() = modelNodes.any { !it.hasPos() }

    override fun getChildModel(childId: Identifiable) = _childModels[childId]

    /**
     * Get the handle recorded for this model.
     */
    override val handle: Handle<RootProcessModelBase<NodeT>>
        get() = handle(handle = _handle)

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

    companion object {

        const val ELEMENTLOCALNAME = "processModel"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

    }


    @ProcessModelDSL
    @Serializable
    @XmlSerialName(ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
    public class SerialDelegate : ProcessModelBase.SerialDelegate {
        var name: String? = null
            private set

        var handle: Long? = null
            private set

        var owner: String? = null
            private set

        @Serializable(UUIDSerializer::class)
        var uuid: UUID? = null
            private set

        val roles: List<String>

        @SerialName("childModel")
        val childModels: List<ChildProcessModelBase.SerialDelegate>

        constructor(
            imports: List<XmlResultType>,
            exports: List<XmlDefineType>,
            nodes: List<ProcessNodeBase.SerialDelegate>,
            name: String?,
            handle: Long?,
            owner: String?,
            uuid: UUID?,
            roles: List<String>,
            childModels: List<ChildProcessModelBase.SerialDelegate>,
        ) : super(imports, exports, nodes) {
            this.name = name
            this.handle = handle.takeIf { it!=-1L }
            this.owner = owner
            this.uuid = uuid
            this.roles = roles
            this.childModels = childModels
        }

        constructor(model: RootProcessModel<*>) : this(
            model.imports.map { XmlResultType(it) },
            model.exports.map { XmlDefineType(it) },
            model.modelNodes.map { ProcessNodeBase.SerialDelegate(it) },
            model.name,
            (model as? ReadableHandleAware<*>)?.handle?.handleValue,
            model.owner.name,
            model.uuid,
            model.roles.toList(),
            model.childModels.map { ChildProcessModelBase.SerialDelegate(it) }
        )

        constructor(model: RootProcessModel.Builder) : this(
            model.imports.map { XmlResultType(it) },
            model.exports.map { XmlDefineType(it) },
            model.nodes.map { ProcessNodeBase.SerialDelegate(it) },
            model.name,
            (model as? ReadableHandleAware<*>)?.handle?.handleValue,
            model.owner.name,
            model.uuid,
            model.roles.toList(),
            model.childModels.map { ChildProcessModelBase.SerialDelegate(it) }
        )
    }

    @ProcessModelDSL
    open class Builder : ProcessModelBase.Builder, RootProcessModel.Builder {

        constructor(
            nodes: Collection<ProcessNode.Builder> = emptyList(),
            childModels: Collection<ChildProcessModel.Builder> = emptyList(),
            name: String? = null,
            handle: Long = -1L,
            owner: Principal = SYSTEMPRINCIPAL,
            roles: Collection<String> = emptyList(),
            uuid: UUID? = null,
            imports: Collection<IXmlResultType> = emptyList(),
            exports: Collection<IXmlDefineType> = emptyList()
        ) : super(nodes, imports, exports) {
            this.childModels = childModels.toMutableList()
            this.name = name
            this.handle = handle
            this.owner = owner
            this.roles = roles.toMutableSet()
            this.uuid = uuid
        }

        public constructor(serialDelegate: SerialDelegate) : this(
            serialDelegate.nodes.map { ProcessNodeBase.Builder(it) },
            // Use a dummy builder, to allow correct construction
            serialDelegate.childModels.map { ChildProcessModelBase.ModelBuilder(Builder(), it) },
            serialDelegate.name,
            serialDelegate.handle ?: -1L,
            serialDelegate.owner?.let { SimplePrincipal(it) } ?: SYSTEMPRINCIPAL,
            serialDelegate.roles,
            serialDelegate.uuid,
            serialDelegate.imports,
            serialDelegate.exports
        )

        final override var name: String? = null

        final override var handle: Long = -1L

        final override var owner: Principal = SYSTEMPRINCIPAL

        final override var uuid: UUID? = null

        final override val roles: MutableSet<String>

        final override val childModels: MutableList<ChildProcessModel.Builder>

        constructor() : this(nodes = emptyList())

        constructor(base: RootProcessModel<*>) : this(
            nodes = emptyList(),
            childModels = emptyList(),
            name = base.name,
            handle = (base as? ReadableHandleAware<*>)?.handle?.handleValue ?: -1L,
            owner = base.owner,
            roles = base.roles.toMutableList(),
            uuid = base.uuid,
            imports = base.imports.toMutableList(),
            exports = base.exports.toMutableList()
        ) {

            base.childModels.mapTo(childModels) { childProcessModel -> childModelBuilder(childProcessModel) }

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

        // These are open to allow drawable builders to be directly drawable
        open fun childModelBuilder(): ChildProcessModel.Builder =
            ChildProcessModelBase.ModelBuilder(rootBuilder)

        open fun childModelBuilder(base: ChildProcessModel<*>): ChildProcessModel.Builder =
            ChildProcessModelBase.ModelBuilder(rootBuilder, base)

        override fun toString(): String {
            return "${this::class.simpleName}(nodes=$nodes, name=$name, handle=$handle, owner=$owner, roles=$roles, uuid=$uuid, imports=$imports, exports=$exports)"
        }

    }

    private class ChildModelProvider<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>, RootT : RootProcessModel<NodeT>, ChildT : ChildProcessModel<NodeT>> :
        ProcessModel.BuildHelper<NodeT, ModelT, RootT, ChildT>, Sequence<ChildT> {

        private val nodeFactory: NodeFactory<NodeT, NodeT, ChildT>
        override val pedantic: Boolean
        override val newOwner: ModelT

        private val data: LinkedHashMap<String, Node>

        constructor(
            childModelBuilders: List<ChildProcessModel.Builder>,
            nodeFactory: NodeFactory<NodeT, NodeT, ChildT>,
            pedantic: Boolean,
            newOwner: ModelT
        ) {
            this.nodeFactory = nodeFactory
            this.pedantic = pedantic
            this.newOwner = newOwner
            this.data = childModelBuilders.associateByTo(
                LinkedHashMap(childModelBuilders.size),
                { it.childId!! }, this::Node
            )
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

        override fun node(
            builder: ProcessNode.Builder,
            otherNodes: Iterable<ProcessNode.Builder>
        ): NodeT = nodeFactory.invoke(builder, this, otherNodes)

        override fun condition(condition: Condition): Condition = nodeFactory.condition(condition)

        override fun iterator() = data.values.asSequence().map { it.invoke() }.iterator()

        private inner class Node(builder: ChildProcessModel.Builder) {

            var builder: ChildProcessModel.Builder? = builder
            var model: ChildT? = null

            operator fun invoke(): ChildT {
                model?.let { return it }
                val b = builder
                    ?: throw IllegalProcessModelException("The process model has cyclic/recursive child models. This is not allowed")
                builder = null
                return nodeFactory(baseChildBuilder = b, buildHelper = this@ChildModelProvider).also { model = it }
            }
        }
    }

}

internal fun SerialDescriptor.getElementIndexOrThrow(name: String): Int = when (val idx = getElementIndex(name)) {
    CompositeDecoder.UNKNOWN_NAME -> throw IllegalArgumentException("No element with name $name available")
    else                          -> idx
}
