/*
 * Copyright (c) 2016.
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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.SplitInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableSplit(
    builder: Split.Builder,
    newOwner: ProcessModel<ExecutableProcessNode>,
    otherNodes: Iterable<ProcessNode.Builder>
) : SplitBase(builder, newOwner, otherNodes), ExecutableProcessNode {

    init {
        checkPredSuccCounts(succRange = 1..Int.MAX_VALUE)
    }

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess<*>,
        processInstanceBuilder: ProcessInstance.Builder<*>,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*, *>, *> {
        // TODO handle reentry
        return processInstanceBuilder.getChildNodeInstance(this, entryNo)
            ?: SplitInstance.BaseBuilder(
                this, predecessor.handle,
                processInstanceBuilder,
                processInstanceBuilder.owner,
                entryNo
            )
    }

    override fun canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *, *>): Boolean = false

    class Builder : SplitBase.Builder, ExecutableProcessNode.Builder {
        constructor(
            id: String? = null,
            predecessor: Identified? = null,
            successors: Collection<Identified> = emptyList(), label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            min: Int = -1,
            max: Int = -1,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
        ) : super(id, predecessor, successors, label, defines, results, x, y, min, max, multiInstance)

        constructor(node: Split) : super(node)

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): ExecutableSplit {
            return ExecutableSplit(this, buildHelper.newOwner, otherNodes)
        }
    }
}
