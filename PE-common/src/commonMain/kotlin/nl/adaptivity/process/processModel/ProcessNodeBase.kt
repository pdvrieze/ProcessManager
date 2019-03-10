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
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault


/**
 * A base class for process nodes. Works like [RootProcessModelBase]
 * Created by pdvrieze on 23/11/15.
 */
@Serializable
abstract class ProcessNodeBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    : ProcessNode<NodeT, ModelT> {

    @Transient
    private var _ownerModel: ModelT

//    @Optional
    @XmlDefault("false")
    override val isMultiInstance: Boolean

    @Transient
    private var _predecessors: MutableIdentifyableSet<Identified>

    @Transient
    override val predecessors: IdentifyableSet<Identified>
        get() = _predecessors


    @Transient
    private var _successors: MutableIdentifyableSet<Identified>

    @Transient
    override val successors: IdentifyableSet<Identified>
        get() = _successors

//    @Optional
    @SerialName("x")
    @XmlDefault("NaN")
    private var _x: Double

    @Transient
    override val x: Double get() = _x

//    @Optional
    @SerialName("y")
    @XmlDefault("NaN")
    private var _y: Double

    @Transient
    override val y: Double get() = _y

    @SerialName("define")
    private var _defines: MutableList<XmlDefineType>
    @Transient
    override val defines: List<XmlDefineType>
        get() = _defines

    @SerialName("result")
    private var _results: MutableList<XmlResultType>

    @Transient
    override val results: List<XmlResultType>
        get() = _results

    @Transient
    private var _hashCode = 0

    @Transient
    override val ownerModel: ModelT
        get() = _ownerModel

    @Transient
    override val idBase: String
        get() = "id"

    @Transient
    private var mId: String?
        set(value) {
            field = value
            _hashCode = 0
            notifyChange()
        }

    override val id: String? get() = mId

    override val label: String?


    @Transient
    override val maxSuccessorCount: Int
        get() = Int.MAX_VALUE

    @Transient
    override val maxPredecessorCount: Int
        get() = 1

    constructor(_ownerModel: ModelT,
                predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String?,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
                results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
                isMultiInstance: Boolean = false) {
        this._ownerModel = _ownerModel
        this.isMultiInstance = isMultiInstance
        this._predecessors = toIdentifiers(Int.MAX_VALUE, predecessors)
        this._successors = toIdentifiers(Int.MAX_VALUE, successors)
        this._x = x
        this._y = y
        this._defines = toExportableDefines(defines)
        this._results = toExportableResults(results)
        this.label = label
        this.mId = id
    }

    @Deprecated("Don't use this if it can be avoided")
    constructor(ownerModel: ModelT) : this(ownerModel, id = null)

    constructor(builder: ProcessNode.IBuilder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) :
        this(buildHelper.newOwner, builder.predecessors, builder.successors, builder.id, builder.label, builder.x,
             builder.y, builder.defines, builder.results, builder.isMultiInstance)

    abstract override fun builder(): Builder<NodeT, ModelT>

    fun offset(offsetX: Int, offsetY: Int) {
        _x += offsetX
        _y += offsetY
        notifyChange()
    }

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

    override fun asT(): NodeT {
        @Suppress("UNCHECKED_CAST")
        return this as NodeT
    }

    override fun compareTo(other: Identifiable): Int {
        return id?.let { other.id?.let { otherId -> it.compareTo(otherId) } ?: 1 } ?: other.id?.let { 1 } ?: 0
    }

    override fun hasPos(): Boolean {
        return x.isFinite() && y.isFinite()
    }

    override fun isPredecessorOf(node: ProcessNode<*, *>): Boolean {
        return node.predecessors.any { pred ->
            this === pred ||
            id == pred.id ||
            (pred is ProcessNode<*, *> && isPredecessorOf(pred)) ||
            _ownerModel?.getNode(pred)?.let { node -> isPredecessorOf(node) } ?: false
        }
    }

    override fun getDefine(name: String) = _defines.firstOrNull { it.name == name }

    override fun getResult(name: String): XmlResultType? {
        return _results.firstOrNull { it.getName() == name }
    }

    @Deprecated("Don't use")
    open fun onBeforeDeserializeChildren(reader: XmlReader) {
        // do nothing
    }

    @Deprecated("Use builders instead of mutable process models")
    protected open fun addPredecessor(predecessorId: Identified) {
        _hashCode = 0
        if (predecessorId === this || predecessorId.id == id) {
            throw IllegalArgumentException()
        }
        if (true) {
            if (_predecessors.containsKey(predecessorId.id)) {
                return
            }
            if (_predecessors.size + 1 > maxPredecessorCount) {
                throw IllegalProcessModelException("Can not add more predecessors")
            }
        }

        if (_predecessors.add(predecessorId)) {
            val ownerModel = _ownerModel

            val mutableNode = predecessorId as? MutableProcessNode<*, *> ?: ownerModel?.getNode(
                predecessorId) as? MutableProcessNode<*, *>
            identifier?.let { mutableNode?.addSuccessor(it) }
        }

    }

    @Deprecated("Use builders instead of mutable process models")
    protected open fun removePredecessor(predecessorId: Identified) {
        _hashCode = 0
        if (_predecessors.remove(predecessorId)) {
            val owner = _ownerModel
            val predecessor: NodeT? = owner?.getNode(predecessorId)
            if (predecessor != null) {
                identifier?.let {
                    (predecessor as MutableProcessNode<*, *>).removeSuccessor(it)
                }
            }
        }
    }

    @Deprecated("Use builders instead of mutable process models")
    protected open fun addSuccessor(successorId: Identified) {
        _hashCode = 0

        if (successorId in _successors) return

        if (_successors.size + 1 > maxSuccessorCount) throw IllegalProcessModelException("Can not add more successors")

        _successors.add(successorId)

        val mutableNode = successorId as? MutableProcessNode<*, *> ?: _ownerModel?.getNode(
            successorId) as? MutableProcessNode<*, *>
        identifier?.let {
            mutableNode?.addPredecessor(it)
        }
    }

    @Deprecated("Use builders instead of mutable process models")
    protected open fun removeSuccessor(successorId: Identified) {
        if (_successors.remove(successorId)) {
            _hashCode = 0
            val successorNode = successorId as? MutableProcessNode<*, *>
                                ?: if (_ownerModel == null) null else _ownerModel!!.getNode(
                                    successorId) as MutableProcessNode<*, *>
            identifier?.let { successorNode?.removePredecessor(it) }
        }
    }

    @Deprecated("Use builders instead of mutable process models")
    protected fun notifyChange() {
        (_ownerModel as? MutableRootProcessModel<NodeT, ModelT>)?.notifyNodeChanged(this.asT())
    }


    @Deprecated("Use builders instead of mutable process models")
    fun setDefine(define: IXmlDefineType): XmlDefineType? {
        val targetName = define.name
        val idx = _defines.indexOfFirst { it.name == targetName }
        if (idx >= 0) {
            return _defines.set(idx, XmlDefineType.get(define))
        } else {
            _defines.add(XmlDefineType.get(define))
            return null
        }
    }


    @Deprecated("Use builders instead of mutable process models")
    protected open fun setResults(results: Collection<IXmlResultType>) {
        _hashCode = 0
        _results = results.let { toExportableResults(results) }
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
            mId?.let { id -> append(" id='$id'") }

            if (_predecessors.size > 0) {
                _predecessors.joinTo(this, ", ", " pred='", "'") { it.id }
            }

            (_ownerModel as? RootProcessModel<*, *>)?.name?.let { name ->
                if (!name.isEmpty()) append(" owner='$name'")
            }
            append(" )")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessNodeBase<*, *>) return false

        if (isMultiInstance != other.isMultiInstance) return false
        if (_predecessors != other._predecessors) return false
        if (_successors != other._successors) return false
        if (_x.isNaN()) { if(!other._x.isNaN()) return false }
        else if (_x != other._x) return false
        if (_y.isNaN()) { if(!other._y.isNaN()) return false }
        else if (_y != other._y) return false
        if (_defines != other._defines) return false
        if (_results != other._results) return false
        if (_hashCode != other._hashCode) return false
        if (label != other.label) return false
        if (mId != other.mId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isMultiInstance.hashCode()
        result = 31 * result + _predecessors.hashCode()
        result = 31 * result + _successors.hashCode()
        result = 31 * result + _x.hashCode()
        result = 31 * result + _y.hashCode()
        result = 31 * result + _defines.hashCode()
        result = 31 * result + _results.hashCode()
        result = 31 * result + _hashCode
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (mId?.hashCode() ?: 0)
        return result
    }


    companion object {

        const val ATTR_PREDECESSOR = "predecessor"

        private fun toIdentifiers(maxSize: Int,
                                  identifiables: Iterable<Identified>): MutableIdentifyableSet<Identified> =
            IdentifyableSet.processNodeSet(maxSize, identifiables.map { it as? Identifier ?: Identifier(it.id) })

        fun toExportableDefines(exports: Collection<IXmlDefineType>) = exports.asSequence().map {
            XmlDefineType(it)
        }.toMutableList()

        fun toExportableResults(imports: Collection<IXmlResultType>) = imports.asSequence().map {
            XmlResultType(it)
        }.toMutableList()

        /**
         * Method to only use the specific ids of predecessors / successors for the hash code. Otherwise there may be an infinite loop.
         * @param c The collection of ids
         *
         * @return The hashcode.
         */
        private fun getHashCode(c: Collection<Identifiable>): Int {
            return c.fold(1) { result, elem -> result * 31 + (elem.id?.hashCode() ?: 1) }
        }
    }

    @Serializable
    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode.IBuilder<NodeT, ModelT>, XmlDeserializable {

        override var id: String?
        override var label: String?

        @XmlDefault("NaN")
        override var x: Double = Double.NaN

        @XmlDefault("NaN")
        override var y: Double = Double.NaN

        @XmlDefault("false")
        override var isMultiInstance: Boolean = false

        constructor(): this(id=null)

        @Suppress("LeakingThis")
        constructor(id: String? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    isMultiInstance: Boolean = false) {
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

        constructor(node: ProcessNode<*, *>) : this(node.id, node.label,
                                                    node.defines, node.results, node.x, node.y, node.isMultiInstance)

        override abstract fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

        override fun onBeforeDeserializeChildren(reader: XmlReader) {
            // By default do nothing
        }

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
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
            val pkgPos = className.lastIndexOf('.', className.lastIndexOf('.')-1)
            return "${className.substring(pkgPos+1)}(id=$id, label=$label, x=$x, y=$y, predecessors=$predecessors, successors=$successors, defines=$defines, results=$results)"
        }

        @Serializer(forClass = Builder::class)
        companion object {

        }
    }

}

private fun Char.isUpperCase() = toUpperCase() == this