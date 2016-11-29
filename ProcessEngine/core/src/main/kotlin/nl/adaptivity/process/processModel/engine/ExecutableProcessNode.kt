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

import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.processModel.MutableProcessNode
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import java.sql.SQLException


/**
 * Created by pdvrieze on 23/11/15.
 */
interface ExecutableProcessNode : ProcessNode<ExecutableProcessNode, ExecutableProcessModel> {

  /**
   * Should this node be able to be provided?


   * @param transaction
   * *
   * @param instance The instance against which the condition should be evaluated.
   * *
   * @return `true` if the node can be started, `false` if
   * *         not.
   */
  fun <T : ProcessTransaction> condition(transaction: T, instance: IExecutableProcessNodeInstance<*>): Boolean

  /**
   * Take action to make task available


   * @param transaction
   * *
   * @param messageService The message service to use for the communication.
   * *
   * @param instance The processnode instance involved.
   * *
   * @return `true` if the task can/must be automatically taken
   */
  @Throws(SQLException::class)
  fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> provideTask(transaction: T,
                                                                                     messageService: IMessageService<V, T, in U>,
                                                                                     instance: U): Boolean

  /**
   * Take action to accept the task (but not start it yet)

   * @param messageService The message service to use for the communication.
   * *
   * @param instance The processnode instance involved.
   * *
   * @return `true` if the task can/must be automatically started
   */
  fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, T, in U>,
                                                                                  instance: U): Boolean

  fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, T, in U>,
                                                                                   instance: U): Boolean


  override val results: List<XmlResultType>

  override val defines: List<XmlDefineType>
}

