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

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.SplitInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableSplit(builder: Split.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>)
  : SplitBase<ExecutableProcessNode, ExecutableModelCommon>(builder, buildHelper), ExecutableProcessNode {

  class Builder : SplitBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {
    constructor(id: String? = null,
                predecessor: Identified? = null,
                successors: Collection<Identified> = emptyList(), label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                multiInstance: Boolean = false) : super(id, predecessor, successors, label, defines, results, x, y, min, max, multiInstance)
    constructor(node: Split<*, *>) : super(node)

    override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>): ProcessNode<ExecutableProcessNode, ExecutableModelCommon> {
      return ExecutableSplit(this, buildHelper)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(this)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     predecessor: ProcessNodeInstance<*>,
                                     entryNo: Int)
      = processInstance.getNodeInstance(this, entryNo) as SplitInstance?
        ?: SplitInstance(this, predecessor.getHandle(), processInstance.getHandle(), processInstance.owner, entryNo = entryNo)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess,
                                     processInstanceBuilder: ProcessInstance.ExtBuilder,
                                     predecessor: ProcessNodeInstance<*>,
                                     entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
    return processInstanceBuilder.getChild(this, entryNo) ?: SplitInstance.BaseBuilder(this, predecessor.getHandle(),
                                                                                       processInstanceBuilder,
                                                                                       processInstanceBuilder.owner,
                                                                                       entryNo)
  }

  override fun startTask(instance: ProcessNodeInstance<*>) = false

}
