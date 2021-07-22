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
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.collections.set


/**
 * Created by pdvrieze on 26/11/15.
 */
@Serializable
abstract class JoinBase<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>?> :
    JoinSplitBase,
    Join {

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

    @Transient
    final override var conditions: Map<Identifier, Condition?> = emptyMap()
        private set

    @Required
//    @Serializable(with = ConditionStringSerializer::class)
    @XmlSerialName("predecessor", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @SerialName("predecessors")
    @XmlElement(true)
    internal val conditionStringsForSerialization: List<PredecessorInfo>


    constructor(
        builder: Join.Builder,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
               )
        : super(builder, buildHelper.newOwner, otherNodes) {
        isMultiMerge = builder.isMultiMerge
        val predecessors = (this.predecessors as MutableIdentifyableSet<Identified>)
        val conditions = mutableMapOf<Identifier, Condition?>()
        val serialConditions = mutableListOf<PredecessorInfo>()
        builder.conditions.forEach { entry ->
            predecessors.add(entry.key)
            conditions[entry.key] = entry.value?.let { buildHelper.condition(it) }
            serialConditions.add(PredecessorInfo(entry.key.id, entry.value))
        }
        this.conditions = conditions
        conditionStringsForSerialization = serialConditions
    }

    override fun builder(): Join.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitJoin(this)
    }

    @Serializable
    open class Builder : JoinSplitBase.Builder, Join.Builder {

        @Transient
        override val idBase: String
            get() = "join"

        @XmlDefault("false")
        final override var isMultiMerge: Boolean = false

        @Transient
        final override var predecessors: MutableSet<Identified> = PredecessorSet()
            set(value) {
                field.retainAll(value)
                field.addAll(value)
            }

        @XmlSerialName("predecessor", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
        @SerialName("predecessors")
        override var conditions: MutableMap<Identifier, Condition?> = mutableMapOf()

        @Transient
        final override var successor: Identifiable? = null

        constructor() : this(predecessors = emptyList<PredecessorInfo>(), isMultiMerge = false, isMultiInstance = false)

        constructor(
            id: String? = null,
            predecessors: Collection<PredecessorInfo> = emptyList(),
            successor: Identified? = null, label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            min: Int = -1,
            max: Int = -1,
            isMultiMerge: Boolean = false,
            isMultiInstance: Boolean = false
                   ) : super(
            id, label,
            defines, results, x,
            y, min, max, isMultiInstance
                            ) {
            predecessors.forEach { conditions[Identifier(it.id)] = it.condition }
            this.successor = successor
            this.isMultiMerge = isMultiMerge
        }

        constructor(
            id: String? = null,
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
            @Suppress("UNUSED_PARAMETER") dummy: Boolean = false
                   ) :
            this(
                id,
                predecessors.map { PredecessorInfo(it.id, null) },
                successor,
                label,
                defines,
                results, x, y, min, max, isMultiMerge, isMultiInstance
                )

        constructor(node: Join) : super(node) {
            this.isMultiMerge = node.isMultiMerge
            node.predecessors.associateByTo(this.conditions, Identified::identifier) {
                node.conditions[it.identifier]
            }
            this.successor = node.successor
        }

        private inner class PredecessorSet : AbstractMutableSet<Identified>() {
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



    @Serializable()
    private class ConditionPairs(val map: Map<Identifier, String?>)

    @Serializer(forClass = ConditionPairs::class)
    private class ConditionSerializer {
        override fun deserialize(decoder: Decoder): ConditionPairs {
            TODO("not implemented")
        }

        override fun serialize(encoder: Encoder, value: ConditionPairs) {
            TODO("not implemented")
        }
    }
}

