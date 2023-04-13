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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.nameCompat
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XmlDefault


/**
 * A base class for process nodes. Works like [RootProcessModelBase]
 * Created by pdvrieze on 23/11/15.
 */
abstract class ProcessNodeBase : ProcessNode {

    private var _ownerModel: ProcessModel<ProcessNode>? = null

    final override val isMultiInstance: Boolean

    private var _predecessors: MutableIdentifyableSet<Identified> = IdentifyableSet.processNodeSet()

    final override val predecessors: IdentifyableSet<Identified>
        get() = _predecessors

    private var _successors: MutableIdentifyableSet<Identified> = IdentifyableSet.processNodeSet()

    override val successors: IdentifyableSet<Identified>
        get() = _successors

    final override val x: Double

    final override val y: Double

    override val defines: List<IXmlDefineType>

    override val results: List<IXmlResultType>

    private var _hashCode = 0

    override val ownerModel: ProcessModel<ProcessNode>?
        get() = _ownerModel

    override val idBase: String
        get() = "id"

    private val _id: String?

    override val id: String? get() = _id

    final override val label: String?

    /** The maximum amount of successors this *type* of node can have */
    override val maxSuccessorCount: Int
        get() = 1

    /** The maximum amount of predecessors this *type* of node can have */
    override val maxPredecessorCount: Int
        get() = 1

    constructor(
        _ownerModel: ProcessModel<ProcessNode>?,
        predecessors: Collection<Identified>, /*= emptyList()*/
        successors: Collection<Identified>, /*= emptyList()*/
        id: String?,
        label: String?, /*= null*/
        x: Double, /*= Double.NaN*/
        y: Double, /*= Double.NaN*/
        defines: Collection<IXmlDefineType>, /*= emptyList()*/
        results: Collection<IXmlResultType>, /*= emptyList()*/
        isMultiInstance: Boolean, /*= false*/
    ) {
        this._ownerModel = _ownerModel
        this.isMultiInstance = isMultiInstance
        this._predecessors = toIdentifiers(Int.MAX_VALUE, predecessors)
        this._successors = toIdentifiers(Int.MAX_VALUE, successors)
        this.x = x
        this.y = y
        @Suppress("LeakingThis")
        this.defines = defines.toList()
        @Suppress("LeakingThis")
        this.results = results.toList()
        this.label = label
        this._id = id
    }

    @Deprecated("BuildHelper is not needed here")
    internal constructor(builder: ProcessNode.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        this(
            buildHelper.newOwner, builder.predecessors, builder.successors, builder.id, builder.label, builder.x,
            builder.y, builder.defines, builder.results, builder.isMultiInstance
        )

    internal constructor(
        builder: ProcessNode.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : this(
        _ownerModel = newOwner,
        predecessors = builder.predecessors,
        successors = builder.successors,
        id = builder.id,
        label = builder.label,
        x = builder.x,
        y = builder.y,
        defines = builder.defines.resolveNodes(otherNodes),
        results = builder.results,
        isMultiInstance = builder.isMultiInstance
    )

    abstract override fun builder(): ProcessNode.Builder

    override fun asT(): ProcessNodeBase {
        @Suppress("UNCHECKED_CAST")
        return this
    }

    override fun compareTo(other: Identifiable): Int {
        return id?.let { other.id?.let { otherId -> it.compareTo(otherId) } ?: 1 } ?: other.id?.let { 1 } ?: 0
    }

    override fun hasPos(): Boolean {
        return x.isFinite() && y.isFinite()
    }

    override fun isPredecessorOf(node: ProcessNode): Boolean {
        return node.predecessors.any { pred ->
            this === pred ||
                id == pred.id ||
                (pred is ProcessNode && isPredecessorOf(pred)) ||
                _ownerModel?.getNode(pred)?.let { node -> isPredecessorOf(node) } ?: false
        }
    }

    override fun getDefine(name: String): IXmlDefineType? = defines.firstOrNull { it.name == name }

    override fun getResult(name: String): IXmlResultType? {
        return results.firstOrNull { it.getName() == name }
    }

    @Deprecated("Don't use")
    open fun onBeforeDeserializeChildren(reader: XmlReader) {
        // do nothing
    }


    override fun toString(): String {
        var name: String = this::class.simpleName ?: "<anonymous type>"
        if (name.endsWith("Impl")) {
            name = name.substring(0, name.length - 4)
        }
        if (name.endsWith("Node")) {
            name = name.substring(0, name.length - 4)
        }
        run {
            for (i in name.length - 1 downTo 0) {
                if (name[i].isUpperCase() && (i == 0 || !name[i - 1].isUpperCase())) {
                    name = name.substring(i)
                    break
                }
            }
        }
        return buildString {
            append(name).append('(')
            id?.let { id -> append(" id='$id'") }

            if (_predecessors.size > 0) {
                _predecessors.joinTo(this, ", ", " pred='", "'") { it.id }
            }

            (_ownerModel as? RootProcessModel<*>)?.name?.let { name ->
                if (!name.isEmpty()) append(" owner='$name'")
            }
            append(" )")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessNodeBase) return false

        if (isMultiInstance != other.isMultiInstance) return false
        if (_predecessors != other._predecessors) return false
        if (_successors != other._successors) return false
        if (x.isNaN()) {
            if (!other.x.isNaN()) return false
        } else if (x != other.x) return false
        if (y.isNaN()) {
            if (!other.y.isNaN()) return false
        } else if (y != other.y) return false
        if (defines != other.defines) return false
        if (results != other.results) return false
        if (_hashCode != other._hashCode) return false
        if (label != other.label) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isMultiInstance.hashCode()
        result = 31 * result + _predecessors.hashCode()
        result = 31 * result + _successors.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + defines.hashCode()
        result = 31 * result + results.hashCode()
        result = 31 * result + _hashCode
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }


    companion object {

        const val ATTR_PREDECESSOR = "predecessor"

        val serialModule = SerializersModule {
            polymorphic(
                IXmlResultType::class,
                XmlResultType::class,
                serializer<XmlResultType>()
            ) // TODO use concrete versions again
            polymorphic(
                IXmlDefineType::class,
                XmlDefineType::class,
                serializer<XmlDefineType>()
            ) // TODO use concrete versions again
        }

        private fun toIdentifiers(
            maxSize: Int,
            identifiables: Iterable<Identified>
        ): MutableIdentifyableSet<Identified> =
            IdentifyableSet.processNodeSet(maxSize, identifiables.map { it as? Identifier ?: Identifier(it.id) })

    }

    @Serializable
    @SerialName("node")
    sealed class SerialDelegate(
        val id: String?,
        val label: String?,
        val defines: List<XmlDefineType>? = null,
        val results: List<XmlResultType>? = null,
        @XmlDefault("NaN")
        val x: Double = Double.NaN,
        @XmlDefault("NaN")
        val y: Double = Double.NaN,
        @XmlDefault("false")
        val isMultiInstance: Boolean = false,
    ) {

        constructor(
            id: String?,
            label: String?,
            defines: Iterable<IXmlDefineType>?,
            results: Iterable<IXmlResultType>?,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false
        ) : this(
            id,
            label,
            defines?.map { XmlDefineType(it) },
            results?.map { XmlResultType(it) },
            x,
            y,
            isMultiInstance
        )

        companion object {
            operator fun invoke(source: ProcessNode): SerialDelegate {
                return source.visit(object : ProcessNode.Visitor<SerialDelegate> {
                    override fun visitStartNode(startNode: StartNode): SerialDelegate {
                        return StartNodeBase.SerialDelegate(startNode)
                    }

                    override fun visitActivity(messageActivity: MessageActivity): SerialDelegate {
                        return ActivityBase.SerialDelegate(messageActivity)
                    }

                    override fun visitCompositeActivity(compositeActivity: CompositeActivity): SerialDelegate {
                        return ActivityBase.SerialDelegate(compositeActivity)
                    }

                    override fun visitSplit(split: Split): SerialDelegate {
                        return SplitBase.SerialDelegate(split)
                    }

                    override fun visitJoin(join: Join): SerialDelegate {
                        return JoinBase.SerialDelegate(join)
                    }

                    override fun visitEndNode(endNode: EndNode): SerialDelegate {
                        return EndNodeBase.SerialDelegate(endNode)
                    }
                })
            }

            operator fun invoke(source: ProcessNode.Builder): SerialDelegate {
                return source.visit(object : ProcessNode.BuilderVisitor<SerialDelegate> {
                    override fun visitStartNode(startNode: StartNode.Builder): SerialDelegate {
                        return StartNodeBase.SerialDelegate(startNode)
                    }

                    override fun visitEventNode(eventNode: EventNode.Builder): SerialDelegate {
                        return EventNodeBase.SerialDelegate(eventNode)
                    }

                    override fun visitActivity(activity: MessageActivity.Builder): SerialDelegate {
                        return ActivityBase.SerialDelegate(activity)
                    }

                    override fun visitActivity(activity: CompositeActivity.ModelBuilder): SerialDelegate {
                        return ActivityBase.SerialDelegate(activity)
                    }

                    override fun visitActivity(activity: CompositeActivity.ReferenceBuilder): SerialDelegate {
                        return ActivityBase.SerialDelegate(activity)
                    }

                    override fun visitSplit(split: Split.Builder): SerialDelegate {
                        return SplitBase.SerialDelegate(split)
                    }

                    override fun visitJoin(join: Join.Builder): SerialDelegate {
                        return JoinBase.SerialDelegate(join)
                    }

                    override fun visitEndNode(endNode: EndNode.Builder): SerialDelegate {
                        return EndNodeBase.SerialDelegate(endNode)
                    }
                })
            }
        }
    }

    abstract class Builder : ProcessNode.Builder {

        override var id: String?
        override var label: String?

        override var x: Double = Double.NaN

        override var y: Double = Double.NaN

        override var isMultiInstance: Boolean = false

        @Suppress("LeakingThis")
        constructor(
            id: String?,
            label: String?,
            defines: Iterable<IXmlDefineType>?,
            results: Iterable<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean
        ) {
            this.id = id
            this.label = label
            this.x = x
            this.y = y
            this.isMultiInstance = isMultiInstance
            this.defines = defines?.toMutableList() ?: mutableListOf()
            this.results = results?.toMutableList() ?: mutableListOf()
        }

        override val defines: MutableCollection<IXmlDefineType>

        override val results: MutableCollection<IXmlResultType>

        constructor(node: ProcessNode) : this(
            node.id, node.label,
            node.defines, node.results, node.x, node.y, node.isMultiInstance
        )

        override fun toString(): String {
            val className = this::class.nameCompat
            val pkgPos = className.lastIndexOf('.', className.lastIndexOf('.') - 1)
            return "${className.substring(pkgPos + 1)}(id=$id, label=$label, x=$x, y=$y, predecessors=$predecessors, successors=$successors, defines=$defines, results=$results)"
        }

        internal companion object {
            operator fun invoke(serialDelegate: SerialDelegate): Builder = when (serialDelegate) {
                is StartNodeBase.SerialDelegate -> StartNodeBase.Builder(serialDelegate)
                is EventNodeBase.SerialDelegate -> EventNodeBase.Builder(serialDelegate)
                is JoinBase.SerialDelegate      -> JoinBase.Builder(serialDelegate)
                is ActivityBase.SerialDelegate  -> when (serialDelegate.childId) {
                    null -> MessageActivityBase.Builder(serialDelegate)
                    else -> CompositeActivityBase.ReferenceBuilder(serialDelegate)
                }
                is SplitBase.SerialDelegate     -> SplitBase.Builder(serialDelegate)
                is EndNodeBase.SerialDelegate   -> EndNodeBase.Builder(serialDelegate)
            }
        }
    }

}

private fun <E : IXmlDefineType> Collection<E>.resolveNodes(nodeBuilders: Iterable<ProcessNode.Builder>): Collection<IXmlDefineType> =
    map { define ->
        val refNode = define.getRefNode()
        val d: IXmlDefineType = when {
            refNode != null && define.getRefName().isNullOrBlank() -> {
                val pred = nodeBuilders.first { it.id == refNode }
                val result = pred.results.singleOrNull()
                    ?: throw IllegalArgumentException("Cannot resolve missing result in ${refNode} when there is no single result (${pred.results.count()})")
                define.copy(refName = result.getName())
            }
            else                                                   -> define
        }
        d// as E
    }

private fun Char.isUpperCase() = uppercaseChar() == this

fun <R : ProcessNode.Builder> R.ensureExportable(): R = apply {
    defines.replaceBy(defines.map { XmlDefineType(it) })
    results.replaceBy(results.map { XmlResultType(it) })
}
