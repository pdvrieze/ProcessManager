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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.ComparableHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import java.sql.SQLException


/**
 * Created by pdvrieze on 23/11/15.
 */
interface ExecutableProcessNode : ProcessNode<ExecutableProcessNode, ExecutableProcessModel>, Identified {

  override val ownerModel: ExecutableProcessModel

  interface Builder : ProcessNode.Builder<ExecutableProcessNode, ExecutableProcessModel> {
    override fun build(newOwner: ExecutableProcessModel?): ProcessNode<ExecutableProcessNode, ExecutableProcessModel>

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
  fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ProcessNodeInstance.HandleT): ProcessNodeInstance

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
  fun condition(engineData: ProcessEngineDataAccess, instance: IExecutableProcessNodeInstance<*>): Boolean

  /**
   * Take action to make task available
   *
   * @param transaction
   * *
   * @param messageService The message service to use for the communication.
   * *
   * @param instance The processnode instance involved.
   * *
   * @return `true` if the task can/must be automatically taken
   */
  @Throws(SQLException::class)
  fun <V, U : IExecutableProcessNodeInstance<U>> provideTask(engineData: MutableProcessEngineDataAccess,
                                                                                     messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                     processInstance: ProcessInstance, instance: U): Boolean

  /**
   * Take action to accept the task (but not start it yet)

   * @param messageService The message service to use for the communication.
   * *
   * @param instance The processnode instance involved.
   * *
   * @return `true` if the task can/must be automatically started
   */
  fun <V, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                  instance: U): Boolean

  fun <V, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                   instance: U): Boolean


  override val results: List<XmlResultType>

  override val defines: List<XmlDefineType>
}

