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
import nl.adaptivity.process.processModel.serialization.ConditionSerializer
import nl.adaptivity.process.processModel.serialization.ConditionStringSerializer
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlDefault
import nl.adaptivity.xml.serialization.XmlElement
import kotlin.collections.AbstractMutableSet
import kotlin.collections.Collection
import kotlin.collections.Map
import kotlin.collections.MutableIterator
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.all
import kotlin.collections.associateByTo
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.fold
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.singleOrNull


/**
 * Created by pdvrieze on 26/11/15.
 */
@Serializable
abstract class JoinBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
    JoinSplitBase<NodeT, ModelT>,
    Join<NodeT, ModelT> {

    @Transient
    override val maxPredecessorCount: Int
        get() = Int.MAX_VALUE

    @Transient
    override val idBase: String
        get() = IDBASE

    final override val isMultiMerge: Boolean

    @Transient
    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    @Transient
    override val predecessors: IdentifyableSet<Identified>
        get() = super.predecessors

    @Serializable(with = ConditionSerializer::class)
    @SerialName("predecessor")
    @XmlElement(true)
    final override val conditions: Map<Identifier, Condition?>

    @Suppress("DEPRECATION")
    @Deprecated("Use builders")
    constructor(ownerModel: ModelT) : super(ownerModel) {
        isMultiMerge = false
        conditions= emptyMap()
    }

    constructor(builder: Join.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder,
                                                                                                           buildHelper) {
        isMultiMerge = builder.isMultiMerge
        val predecessors = (this.predecessors as MutableIdentifyableSet<Identified>)
        val conditions = mutableMapOf<Identifier, Condition?>()
        builder.conditions.forEach { entry ->
            predecessors.add(entry.key)
            conditions[entry.key] = entry.value?.let { buildHelper.condition(it)}
        }
        this.conditions = conditions
    }

    abstract override fun builder(): Builder<NodeT, ModelT>

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
        out.smartStartTag(Join.ELEMENTNAME)
        serializeAttributes(out)
        serializeChildren(out)
        out.endTag(Join.ELEMENTNAME)
    }

    @Throws(XmlException::class)
    override fun serializeChildren(out: XmlWriter) {
        super.serializeChildren(out)
        for (pred in predecessors) {
            out.smartStartTag(Join.PREDELEMNAME)
            val c = conditions[pred.identifier]
            out.writeAttribute("condition", c?.condition)

            out.text(pred.id)
            out.endTag(Join.PREDELEMNAME)
        }
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitJoin(this)
    }

    @Serializable
    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
        JoinSplitBase.Builder<NodeT, ModelT>,
        Join.Builder<NodeT, ModelT> {

        @Transient
        override val idBase: String
            get() = "join"

        @XmlDefault("false")
        final override var isMultiMerge: Boolean = false

        @Transient
        final override var predecessors: MutableSet<Identified> = PrececessorSet()
            set(value) {
                field.retainAll(value)
                field.addAll(value)
            }

        @Serializable(ConditionStringSerializer::class)
        @SerialName("predecessor")
        override var conditions: MutableMap<Identifier, String?> = mutableMapOf()

        @Transient
        final override var successor: Identifiable? = null

        @Transient
        override val elementName: QName
            get() = Join.ELEMENTNAME

        constructor() : this(predecessors = emptyList<PredecessorInfo>(), isMultiMerge = false, isMultiInstance = false)

        constructor(id: String? = null,
                    predecessors: Collection<PredecessorInfo> = emptyList(),
                    successor: Identified? = null, label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    min: Int = -1,
                    max: Int = -1,
                    isMultiMerge: Boolean = false,
                    isMultiInstance: Boolean = false) : super(id, label,
                                                              defines, results, x,
                                                              y, min, max, isMultiInstance) {
            predecessors.forEach { conditions[Identifier(it.id)] = it.condition }
            this.successor = successor
            this.isMultiMerge = isMultiMerge
        }

        constructor(id: String? = null,
                    predecessors: Collection<Identified>,
                    successor: Identified? = null, label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    min: Int = -1,
                    max: Int = -1,
                    isMultiMerge: Boolean = false,
                    isMultiInstance: Boolean = false,
                    dummy: Boolean = false):
            this(id,
                 predecessors.map { PredecessorInfo(it.id, null) },
                 successor,
                 label,
                 defines,
                 results, x, y, min, max, isMultiMerge, isMultiInstance)

        constructor(node: Join<*, *>) : super(node) {
            this.isMultiMerge = node.isMultiMerge
            node.predecessors.associateByTo(this.conditions, Identified::identifier) {
                node.conditions[it.identifier]?.condition
            }
            this.successor = node.successor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (reader.isElement(Join.PREDELEMNAME)) {
                val condition = reader.getAttributeValue(null, "condition")
                val id = reader.readSimpleElement()
                val identifier = Identifier(id)
                if (condition!=null) {
                    conditions[identifier] = condition
                }
                predecessors.add(identifier)
                return true
            }
            return super.deserializeChild(reader)
        }

        private inner class PrececessorSet: AbstractMutableSet<Identified>() {
            override val size: Int get() = conditions.size

            override fun add(element: Identified): Boolean {
                val identifier = element.identifier
                return if (conditions.containsKey(identifier)) {
                    false
                } else {
                    conditions[identifier] = null
                    true
                }
            }

            override fun addAll(elements: Collection<Identified>): Boolean {
                return elements.fold(false) { prev, elem -> prev or add(elem) }
            }

            override fun clear() {
                conditions.clear()
            }

            override fun contains(element: Identified): Boolean {
                return conditions.contains(element.identifier)
            }

            override fun containsAll(elements: Collection<Identified>): Boolean {
                return elements.all { contains(it.identifier) }
            }

            override fun isEmpty(): Boolean = conditions.isEmpty()

            override fun iterator(): MutableIterator<Identified> {
                return conditions.keys.iterator()
            }

            override fun remove(element: Identified): Boolean {
                val identifier = element.identifier
                return conditions.containsKey(identifier).also { conditions.remove(identifier) }
            }

            override fun removeAll(elements: Collection<Identified>): Boolean {
                return conditions.keys.removeAll(elements.map(Identified::identifier))
            }

            override fun retainAll(elements: Collection<Identified>): Boolean {
                return conditions.keys.retainAll(elements.map(Identified::identifier))
            }
        }


    }



    companion object {

        const val IDBASE = "join"
    }
}

