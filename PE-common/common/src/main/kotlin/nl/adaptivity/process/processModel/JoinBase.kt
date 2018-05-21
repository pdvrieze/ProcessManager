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

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.devrieze.util.ArraySet
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.JvmDefault
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlDefault
import nl.adaptivity.xml.serialization.XmlElement


/**
 * Created by pdvrieze on 26/11/15.
 */
@Serializable
abstract class JoinBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> :
    JoinSplitBase<NodeT, ModelT>,
    Join<NodeT, ModelT> {

    override val maxPredecessorCount: Int
        get() = Int.MAX_VALUE

    override val idBase: String
        get() = IDBASE

    final override val isMultiMerge: Boolean

    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    @SerialName("predecessor")
    @XmlElement(true)
    override val predecessors: IdentifyableSet<Identified>
        get() = super.predecessors

    @Suppress("DEPRECATION")
    @Deprecated("Use builders")
    constructor(ownerModel: ModelT) : super(ownerModel) {
        isMultiMerge = false
    }

    constructor(builder: Join.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder,
                                                                                                           buildHelper) {
        isMultiMerge = builder.isMultiMerge
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

        @Optional
        @XmlDefault("false")
        final override var isMultiMerge: Boolean = false

        @SerialName("predecessor")
        final override var predecessors: MutableSet<Identified> = ArraySet()
            set(value) {
                field.replaceBy(value)
            }

        @Transient
        final override var successor: Identifiable? = null

        @Transient
        override val elementName: QName
            get() = Join.ELEMENTNAME

        constructor() : this(predecessors = emptyList(), isMultiMerge = false, isMultiInstance = false)

        constructor(id: String? = null,
                    predecessors: Collection<Identified> = emptyList(),
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
            this.predecessors.addAll(predecessors)
            this.successor = successor
            this.isMultiMerge = isMultiMerge
        }

        constructor(node: Join<*, *>) : super(node) {
            this.isMultiMerge = node.isMultiMerge
            this.predecessors.addAll(node.predecessors)
            this.successor = node.successor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (reader.isElement(Join.PREDELEMNAME)) {
                val id = reader.readSimpleElement()
                predecessors.add(Identifier(id))
                return true
            }
            return super.deserializeChild(reader)
        }

    }

    companion object {

        const val IDBASE = "join"
    }
}
