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

import net.devrieze.util.Handles
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import java.sql.SQLException


class ExecutableStartNode(builder: StartNode.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) : StartNodeBase<ExecutableProcessNode, ExecutableModelCommon>(
  builder, buildHelper), ExecutableProcessNode {

  class Builder : StartNodeBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {
    constructor(id: String? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                multiInstance: Boolean = false) : super(id, successor, label, defines, results, x, y, multiInstance)
    constructor(node: StartNode<*, *>) : super(node)


    override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>): ProcessNode<ExecutableProcessNode, ExecutableModelCommon> {
      return ExecutableStartNode(this, buildHelper)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(node=this)

  fun createOrReuseInstance(processInstance: ProcessInstance, entryNo: Int)
      = processInstance.getNodeInstance(this, entryNo) ?: ProcessNodeInstance(this, Handles.getInvalid(), processInstance, entryNo)

/*
  fun createOrReuseInstance(processInstanceBuilder: ProcessInstance.ExtBuilder, entryNo: Int)
      = processInstanceBuilder.getNodeInstance(this, entryNo) ?:
        ProcessNodeInstance.BaseBuilder<ExecutableProcessNode>(this, emptyList(),
                                                               processInstanceBuilder.handle,
                                                               processInstanceBuilder.owner, entryNo)
*/

  override fun condition(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance) = ConditionResult.TRUE

  @Throws(SQLException::class)
  override fun provideTask(engineData: ProcessEngineDataAccess,
                           processInstance: ProcessInstance, instance: ProcessNodeInstance): Boolean {
    return true
  }

  override fun takeTask(instance: ProcessNodeInstance) = true

  override fun startTask(instance: ProcessNodeInstance) = true

}
