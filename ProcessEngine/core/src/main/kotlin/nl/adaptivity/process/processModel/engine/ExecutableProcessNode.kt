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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import java.sql.SQLException


/**
 * Base type for any process node that can be executed
 */
interface ExecutableProcessNode : ProcessNode<ExecutableProcessNode, ExecutableProcessModel>, Identified {

  override val ownerModel: ExecutableProcessModel

  interface Builder : ProcessNode.Builder<ExecutableProcessNode, ExecutableProcessModel> {
    override fun build(newOwner: ExecutableProcessModel): ProcessNode<ExecutableProcessNode, ExecutableProcessModel>

    override fun predecessors(vararg values: Identifiable) {
      values.forEach {
        predecessors.add(it.identifier ?: throw NullPointerException("Missing identifier for predecessor ${it}"))
      }
    }

    override fun result(builder: XmlResultType.Builder.() -> Unit) {
      results.add(XmlResultType.Builder().apply(builder).build())
    }

  }

  override fun builder(): ExecutableProcessNode.Builder

  /**
   * Get an instance for this node within the process instance. This may return an existing instance if that is valid for
   * the type (joins)
   */
  fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>): ProcessNodeInstance =
      processInstance.getNodeInstance(this) ?: ProcessNodeInstance(this, predecessor, processInstance)

  override val identifier: Identifier?
    get() = Identifier(id)

  /**
   * Should this node be able to be provided?
   * @param transaction
   *
   * @param instance The instance against which the condition should be evaluated.
   *
   * @return `true` if the node can be started, `false` if
   *          not.
   */
  fun condition(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance): ConditionResult = ConditionResult.TRUE

  /**
   * Take action to make task available
   *
   * @param transaction
   *
   * @param instance The processnode instance involved.
   *
   * @return `true` if the task can/must be automatically taken
   */
  @Throws(SQLException::class)
  fun provideTask(engineData: MutableProcessEngineDataAccess,
                  processInstance: ProcessInstance, instance: ProcessNodeInstance): Boolean = true

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param instance The processnode instance involved.
   *
   * @return `true` if the task can/must be automatically started
   */
  fun takeTask(instance: ProcessNodeInstance): Boolean = true

  fun startTask(instance: ProcessNodeInstance): Boolean = true

  override val results: List<XmlResultType>

  override val defines: List<XmlDefineType>
}

