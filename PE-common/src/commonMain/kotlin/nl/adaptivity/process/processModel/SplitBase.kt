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

import foo.FakeSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.devrieze.util.ArraySet
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Created by pdvrieze on 26/11/15.
 */
@FakeSerializable
@XmlSerialName("split", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
abstract class SplitBase : JoinSplitBase, Split {

    @Transient
    override val maxSuccessorCount: Int
        get() = Int.MAX_VALUE

    @FakeSerializable(with = Identifiable.Companion::class)
    final override val predecessor: Identifiable? = predecessors.singleOrNull()

    constructor(
        ownerModel: ProcessModel<ProcessNode>,
        predecessor: Identified? = null,
        successors: Collection<Identified> = emptyList(),
        id: String?,
        label: String? = null,
        x: Double = Double.NaN,
        y: Double = Double.NaN,
        defines: Collection<IXmlDefineType> = ArrayList(),
        results: Collection<IXmlResultType> = ArrayList(),
        min: Int = -1,
        max: Int = -1
               ) : super(ownerModel, predecessor?.let { listOf(it) } ?: emptyList(), successors, id,
                         label, x, y, defines, results, min, max)

    constructor(builder: Split.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes)

    override fun builder(): Split.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitSplit(this)
    }

    @FakeSerializable
    @XmlSerialName("split", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    open class Builder :
        JoinSplitBase.Builder,
        Split.Builder {

        @Transient
        override val idBase: String
            get() = "split"

        @Transient
        final override var successors: MutableSet<Identified> = ArraySet()
            set(value) {
                field.replaceBy(value)
            }

        @FakeSerializable(with = Identifiable.Companion::class)
        final override var predecessor: Identifiable? = null

        constructor() : this(id = null)

        @Deprecated("use the constructor that takes a single predecessor")
        constructor(
            id: String? = null,
            predecessors: Collection<Identified>,
            successors: Collection<Identified> = emptyList(),
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            min: Int = -1,
            max: Int = -1,
            multiInstance: Boolean = false
                   ) : this(
            id, predecessors.singleOrNull(), successors, label, defines,
            results, x, y,
            min, max, multiInstance
                           )


        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successors: Collection<Identified> = emptyList(),
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            min: Int = -1,
            max: Int = -1,
            multiInstance: Boolean = false
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
    }

}
