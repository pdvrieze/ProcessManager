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

import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableEventNode(
    builder: EventNode.Builder,
    buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>
) : EventNodeBase<ExecutableProcessNode, ExecutableModelCommon>(
    builder, buildHelper
), ExecutableProcessNode {

    init {
        checkPredSuccCounts()
    }

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon


    class Builder : EventNodeBase.Builder, ExecutableProcessNode.Builder {
        constructor(
            id: String? = null,
            predecessor: Identified? = null,
            successor: Identified? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false,
            isThrowing: Boolean = false,
            eventType: IEventNode.Type = IEventNode.Type.MESSAGE,
        ) : super(
            id = id,
            successor = successor,
            label = label,
            defines = defines,
            results = results,
            x = x,
            y = y,
            isMultiInstance = multiInstance,
            isThrowing = isThrowing,
            eventType = eventType
        )

        constructor(node: EventNode) : super(node)

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): ExecutableEventNode {
            return ExecutableEventNode(this, buildHelper)
        }
    }

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    fun createOrReuseInstance(
        processInstanceBuilder: ProcessInstance.Builder,
        entryNo: Int
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*>> =
        processInstanceBuilder.getChildNodeInstance(this, entryNo)
            ?: DefaultProcessNodeInstance.BaseBuilder(
                this, emptyList(),
                processInstanceBuilder,
                processInstanceBuilder.owner,
                entryNo
            )

}
