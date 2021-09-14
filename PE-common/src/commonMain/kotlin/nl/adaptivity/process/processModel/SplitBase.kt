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
import net.devrieze.util.ArraySet
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class SplitBase : JoinSplitBase, Split {

    override val maxSuccessorCount: Int
        get() = Int.MAX_VALUE

    final override val predecessor: Identifiable? = predecessors.singleOrNull()

    constructor(
        ownerModel: ProcessModel<ProcessNode>,
        predecessor: Identified? /*= null*/,
        successors: Collection<Identified> /*= emptyList()*/,
        id: String?,
        label: String? /*= null*/,
        x: Double /*= Double.NaN*/,
        y: Double /*= Double.NaN*/,
        defines: Collection<IXmlDefineType> /*= emptyList()*/,
        results: Collection<IXmlResultType> /*= emptyList()*/,
        min: Int /*= -1*/,
        max: Int /*= -1*/,
        isMultiInstance: Boolean,
    ) : super(
        ownerModel,
        predecessor?.let { listOf(it) } ?: emptyList(),
        successors,
        id,
        label,
        x,
        y,
        defines,
        results,
        min,
        max,
        isMultiInstance
    )

    constructor(builder: Split.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes)

    override fun builder(): Split.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitSplit(this)
    }

    @Serializable
    @SerialName(Split.ELEMENTLOCALNAME)
    @XmlSerialName(Split.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        val predecessor: Identifier?

        val min: Int

        val max: Int

        constructor(
            id: String?,
            label: String?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
            predecessor: Identifier?,
            min: Int,
            max: Int,
        ) :
            super(id, label, x = x, y = y, isMultiInstance = isMultiInstance) {
            this.predecessor = predecessor
            this.min = min
            this.max = max
        }

        constructor(base: Split) : this(
            base.id,
            base.label,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.min,
            base.max
        )

        constructor(base: Split.Builder) : this(
            base.id,
            base.label,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.min,
            base.max
        )
    }

    open class Builder :
        JoinSplitBase.Builder,
        Split.Builder {

        override val idBase: String
            get() = "split"

        final override var successors: MutableSet<Identified> = ArraySet()
            set(value) {
                field.replaceBy(value)
            }

        final override var predecessor: Identifiable? = null

        constructor() : this(null, null, emptyList(), null, emptyList(), emptyList(), Double.NaN, Double.NaN, -1, -1, false)

        @Deprecated("use the constructor that takes a single predecessor")
        constructor(
            id: String? /*= null*/,
            predecessors: Collection<Identified>,
            successors: Collection<Identified> /*= emptyList()*/,
            label: String? /*= null*/,
            defines: Collection<IXmlDefineType> /*= emptyList()*/,
            results: Collection<IXmlResultType> /*= emptyList()*/,
            x: Double /*= Double.NaN*/,
            y: Double /*= Double.NaN*/,
            min: Int /*= -1*/,
            max: Int /*= -1*/,
            isMultiInstance: Boolean /*= false*/
        ) : this(
            id,
            predecessors.singleOrNull(),
            successors,
            label,
            defines,
            results,
            x,
            y,
            min,
            max,
            isMultiInstance
        )


        constructor(
            id: String? /*= null*/,
            predecessor: Identifiable? /*= null*/,
            successors: Collection<Identified> /*= emptyList()*/,
            label: String? /*= null*/,
            defines: Iterable<IXmlDefineType>? /*= emptyList()*/,
            results: Iterable<IXmlResultType>? /*= emptyList()*/,
            x: Double /*= Double.NaN*/,
            y: Double /*= Double.NaN*/,
            min: Int /*= -1*/,
            max: Int /*= -1*/,
            multiInstance: Boolean /*= false*/
        ) : super(
            id, label, defines,
            results, x,
            y, min, max, multiInstance
        ) {
            this.predecessor = predecessor
            this.successors.addAll(successors)

        }

        constructor(node: Split) : super(node) {
            this.predecessor = node.predecessor
            this.successors.addAll(node.successors)
        }

        constructor(serialDelegate: SerialDelegate) : this(
            serialDelegate.id,
            serialDelegate.predecessor,
            emptyList(),
            serialDelegate.label,
            serialDelegate.defines,
            serialDelegate.results,
            serialDelegate.x,
            serialDelegate.y,
            serialDelegate.min,
            serialDelegate.max,
            serialDelegate.isMultiInstance
        )
    }

}
