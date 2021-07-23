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
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.xmlserializable.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.*


/**
 * Created by pdvrieze on 24/11/15.
 */
@FakeSerializable
abstract class EndNodeBase : ProcessNodeBase, EndNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: EndNode.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes)

    @Suppress("DEPRECATION")
    @FakeSerializable(with = Identifiable.Companion::class)
    override val predecessor: Identified? = predecessors.singleOrNull()

    @Transient
    override val maxSuccessorCount: Int
        get() = 0

    @Transient
    override val successors: IdentifyableSet<Identified>
        get() = IdentifyableSet.empty<Identified>()


    override fun builder(): EndNode.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitEndNode(this)
    }

    @FakeSerializable
    open class Builder :
        ProcessNodeBase.Builder,
        EndNode.Builder {

        @Transient
        override val idBase: String
            get() = "end"

        @FakeSerializable(with = Identifiable.Companion::class)
        final override var predecessor: Identifiable? = null

        constructor() : this(id = null)

        constructor(
            id: String? = null,
            predecessor: Identified? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false
                   ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
        }


        constructor(node: EndNode) : super(node) {
            this.predecessor = node.predecessor
        }

    }
}
