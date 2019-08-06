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


class ExecutableSplit(builder: Split.Builder, newOwner: ProcessModel<ExecutableProcessNode>) :
    SplitBase(builder, newOwner), ExecutableProcessNode {

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon


    class Builder : SplitBase.Builder, ExecutableProcessNode.Builder {
        constructor(id: String? = null,
                    predecessor: Identified? = null,
                    successors: Collection<Identified> = emptyList(), label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    min: Int = -1,
                    max: Int = -1,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    multiInstance: Boolean = false) : super(id, predecessor, successors, label, defines, results, x, y,
                                                            min, max, multiInstance)

        constructor(node: Split) : super(node)
    }

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override fun createOrReuseInstance(data: MutableProcessEngineDataAccess,
                                       processInstanceBuilder: ProcessInstance.Builder,
                                       predecessor: IProcessNodeInstance,
                                       entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        // TODO handle reentry
        return processInstanceBuilder.getChild(this, entryNo) ?: SplitInstance.BaseBuilder(this, predecessor.handle(),
                                                                                           processInstanceBuilder,
                                                                                           processInstanceBuilder.owner,
                                                                                           entryNo)
    }

    override fun startTask(instance: ProcessNodeInstance.Builder<*, *>) = false
}
