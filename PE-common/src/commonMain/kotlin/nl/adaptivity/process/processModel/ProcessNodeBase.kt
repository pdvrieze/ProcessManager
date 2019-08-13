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
import kotlinx.serialization.Transient
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xml.XMLConstants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault


/**
 * A base class for process nodes. Works like [RootProcessModelBase]
 * Created by pdvrieze on 23/11/15.
 */
@Serializable
abstract class ProcessNodeBase : ProcessNode {

    @Transient
    private val _ownerModel: ProcessModel<ProcessNode>?

    //    @Optional
    @XmlDefault("false")
    final override val isMultiInstance: Boolean

    @Transient
    private val _predecessors: MutableIdentifyableSet<Identified>

    @Transient
    override val predecessors: IdentifyableSet<Identified>
        get() = _predecessors


    @Transient
    private val _successors: MutableIdentifyableSet<Identified>

    @Transient
    override val successors: IdentifyableSet<Identified>
        get() = _successors

    //    @Optional
    @SerialName("x")
    @XmlDefault("NaN")
    internal val _x: Double

    @Transient
    override val x: Double
        get() = _x

    //    @Optional
    @SerialName("y")
    @XmlDefault("NaN")
    internal val _y: Double

    @Transient
    override val y: Double
        get() = _y

    @SerialName("define")
    override val defines: List<IXmlDefineType>

    @SerialName("result")
    final override val results: List<IXmlResultType>

    @Transient
    private var _hashCode = 0

    @Transient
    override val ownerModel: ProcessModel<ProcessNode>?
        get() = _ownerModel

    @Transient
    override val idBase: String
        get() = "id"

    override val id: String?

    final override val label: String?


    @Transient
    override val maxSuccessorCount: Int
        get() = Int.MAX_VALUE

    @Transient
    override val maxPredecessorCount: Int
        get() = 1

    constructor(
        _ownerModel: ProcessModel<ProcessNode>?,
        predecessors: Collection<Identified> = emptyList(),
        successors: Collection<Identified> = emptyList(),
        id: String?,
        label: String? = null,
        x: Double = Double.NaN,
        y: Double = Double.NaN,
        defines: Collection<IXmlDefineType> = emptyList(),
        results: Collection<IXmlResultType> = emptyList(),
        isMultiInstance: Boolean = false
               ) {
        this._ownerModel = _ownerModel
        this.isMultiInstance = isMultiInstance
        this._predecessors = toIdentifiers(Int.MAX_VALUE, predecessors)
        this._successors = toIdentifiers(Int.MAX_VALUE, successors)
        this._x = x
        this._y = y
        this.defines = defines.toList()
        this.results = results.toList()
        this.label = label
        this.id = id
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
                        ) :
        this(
            newOwner, builder.predecessors, builder.successors, builder.id, builder.label, builder.x,
            builder.y, builder.defines.resolveNodes(otherNodes), builder.results, builder.isMultiInstance
            )

    abstract override fun builder(): ProcessNode.Builder

    @Throws(XmlException::class)
    protected open fun serializeAttributes(out: XmlWriter) {
        out.writeAttribute("id", id)
        out.writeAttribute("label", label)
        out.writeAttribute("x", x)
        out.writeAttribute("y", y)
    }

    @Throws(XmlException::class)
    protected open fun serializeChildren(out: XmlWriter) {
        out.writeChildren(results)
        out.writeChildren(defines)
    }

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
        var name = this::class.name.substringAfterLast('.')
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
        if (_x.isNaN()) {
            if (!other._x.isNaN()) return false
        } else if (_x != other._x) return false
        if (_y.isNaN()) {
            if (!other._y.isNaN()) return false
        } else if (_y != other._y) return false
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
        result = 31 * result + _x.hashCode()
        result = 31 * result + _y.hashCode()
        result = 31 * result + defines.hashCode()
        result = 31 * result + results.hashCode()
        result = 31 * result + _hashCode
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }


    companion object {

        const val ATTR_PREDECESSOR = "predecessor"

        private fun toIdentifiers(
            maxSize: Int,
            identifiables: Iterable<Identified>
                                 ): MutableIdentifyableSet<Identified> =
            IdentifyableSet.processNodeSet(maxSize, identifiables.map { it as? Identifier ?: Identifier(it.id) })

    }

    @Serializable
    abstract class Builder : ProcessNode.Builder, XmlDeserializable {

        override var id: String?
        override var label: String?

        //        @Optional
        @XmlDefault("NaN")
        override var x: Double = Double.NaN

        //        @Optional
        @XmlDefault("NaN")
        override var y: Double = Double.NaN

        //        @Optional
        @XmlDefault("false")
        override var isMultiInstance: Boolean = false

        constructor() : this(id = null)

        @Suppress("LeakingThis")
        constructor(
            id: String? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false
                   ) {
            this.id = id
            this.label = label
            this.x = x
            this.y = y
            this.isMultiInstance = isMultiInstance
            this.defines = ArrayList(defines)
            this.results = ArrayList(results)
        }

        @Serializable(with = IXmlDefineTypeListSerializer::class)
        @SerialName("define")
        override val defines: MutableCollection<IXmlDefineType>

        @Serializable(with = IXmlResultTypeListSerializer::class)
        @SerialName("result")
        override val results: MutableCollection<IXmlResultType>

        constructor(node: ProcessNode) : this(
            node.id, node.label,
            node.defines, node.results, node.x, node.y, node.isMultiInstance
                                             )

        override final fun <T : ProcessNode> build(
            buildHelper: ProcessModel.BuildHelper<T, *, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
                                                  ): T {
            return buildHelper.node(this, otherNodes)
        }

        override fun onBeforeDeserializeChildren(reader: XmlReader) {
            // By default do nothing
        }

        override fun deserializeAttribute(
            attributeNamespace: String?,
            attributeLocalName: String,
            attributeValue: String
                                         ): Boolean {
            if (XMLConstants.NULL_NS_URI == attributeNamespace) {
                val value = attributeValue
                when (attributeLocalName) {
                    "id"    -> id = value
                    "label" -> label = value
                    "x"     -> x = value.toDouble()
                    "y"     -> y = value.toDouble()
                    else    -> return false
                }
                return true
            }
            return false
        }

        override fun toString(): String {
            val className = this::class.name
            val pkgPos = className.lastIndexOf('.', className.lastIndexOf('.') - 1)
            return "${className.substring(pkgPos + 1)}(id=$id, label=$label, x=$x, y=$y, predecessors=$predecessors, successors=$successors, defines=$defines, results=$results)"
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

private fun Char.isUpperCase() = toUpperCase() == this

fun <R : ProcessNode.Builder> R.ensureExportable(): R = apply {
    defines.replaceBy(defines.map { XmlDefineType(it) })
    results.replaceBy(results.map { XmlResultType(it) })
}
