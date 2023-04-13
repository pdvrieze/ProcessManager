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

import nl.adaptivity.process.util.Identified


/**
 * Created by pdvrieze on 25/11/15.
 */
abstract class JoinSplitBase : ProcessNodeBase, JoinSplit {

    constructor(
        ownerModel: ProcessModel<ProcessNode>,
        predecessors: Collection<Identified>,
        successors: Collection<Identified>,
        id: String?,
        label: String?,
        x: Double,
        y: Double,
        defines: Collection<IXmlDefineType>,
        results: Collection<IXmlResultType>,
        min: Int,
        max: Int,
        isMultiInstance: Boolean,
    ) : super(ownerModel, predecessors, successors, id, label, x, y, defines, results, isMultiInstance) {
        this.min = min
        this.max = max
    }


    final override var min: Int
    final override var max: Int

    constructor(builder: JoinSplit.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes) {
        this.min = builder.min
        this.max = builder.max
    }

    override abstract fun builder(): JoinSplit.Builder

    @Deprecated("Don't use")
    open fun deserializeChildText(elementText: CharSequence): Boolean {
        return false
    }

    abstract class Builder :
        ProcessNodeBase.Builder,
        JoinSplit.Builder {

        final override var min: Int
        final override var max: Int

        constructor(
            id: String? = null,
            label: String? = null,
            defines: Iterable<IXmlDefineType>? = emptyList(),
            results: Iterable<IXmlResultType>? = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            min: Int = -1,
            max: Int = -1,
            multiInstance: Boolean = false
        ) : super(id, label, defines, results, x, y, multiInstance) {
            this.min = min
            this.max = max
        }

        constructor(node: JoinSplit) : super(node) {
            min = node.min
            max = node.max
        }

        override fun toString(): String {
            return "${super.toString().dropLast(1)}, min=$min, max=$max)"
        }

    }

}
