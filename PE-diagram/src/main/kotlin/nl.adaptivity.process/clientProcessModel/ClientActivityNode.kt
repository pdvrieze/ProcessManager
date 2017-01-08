/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.*


abstract class ClientActivityNode : ActivityBase<DrawableProcessNode, DrawableProcessModel?>, ClientProcessNode {

    abstract class Builder : ActivityBase.Builder<DrawableProcessNode, DrawableProcessModel?>, ClientProcessNode.Builder {

        constructor() {}

        constructor(compat: Boolean) {
            this.isCompat = compat
        }

        constructor(id: String?, predecessor: Identified?, successor: Identified?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, message: XmlMessage?, condition: String?, name: String?, compat: Boolean) : super(id, predecessor, successor, label, defines, results, message, condition, name, x, y) {
            this.isCompat = compat
        }

        constructor(node: Activity<*, *>) : super(node) {
            if (node is ClientProcessNode) {
                isCompat = node.isCompat
            } else {
                isCompat = false
            }
        }

        override var isCompat = false
    }

    override val isCompat: Boolean
    override var condition: String? = null

    constructor(owner: ModelT, compat: Boolean) : super(owner) {
        isCompat = compat
    }


    constructor(owner: ModelT, id: String, compat: Boolean) : super(owner) {
        setId(id)
        isCompat = compat
    }

    protected constructor(orig: Activity<*, *>, newOwnerModel: ModelT, compat: Boolean) : super(builder = orig.builder(), newOwnerModel = newOwnerModel) {
        isCompat = compat
    }

    constructor(builder: Activity.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel) {
        if (builder is Builder) {
            isCompat = builder.isCompat
        } else {
            isCompat = false
        }
    }

    override fun setId(id: String) = super.setId(id)

    override fun setLabel(label: String?) = super.setLabel(label)

    @Throws(XmlException::class)
    override fun serializeCondition(out: XmlWriter) {
        if (! condition.isNullOrEmpty()) {
            out.writeSimpleElement(Condition.ELEMENTNAME, condition)
        }
    }

    override val maxSuccessorCount: Int
        get() = if (isCompat) Integer.MAX_VALUE else 1

    override fun setOwnerModel(newOwnerModel: ModelT) {
        super.setOwnerModel(newOwnerModel)
    }

    override fun setPredecessors(predecessors: Collection<Identifiable>) {
        super.setPredecessors(predecessors)
    }

    override fun removePredecessor(predecessorId: Identified) {
        super.removePredecessor(predecessorId)
    }

    override fun addPredecessor(predecessorId: Identified) {
        super.addPredecessor(predecessorId)
    }

    override fun addSuccessor(successorId: Identified) {
        super.addSuccessor(successorId)
    }

    override fun removeSuccessor(successorId: Identified) {
        super.removeSuccessor(successorId)
    }

    override fun setSuccessors(successors: Collection<Identified>) {
        super.setSuccessors(successors)
    }

}
