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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Transaction
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.engine.JoinImpl
import nl.adaptivity.process.util.Identifiable
import java.sql.SQLException
import java.util.*


class JoinInstance<T : Transaction> : ProcessNodeInstance<T> {

  constructor(node: JoinImpl, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState) : super(
        node,
        predecessors,
        processInstance,
        state) {
  }

  @Throws(SQLException::class)
  constructor(transaction: T, node: JoinImpl, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>) : super(
        node,
        predecessors,
        processInstance) {
  }

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param node
   * *
   * @param processInstance
   */
  @Throws(SQLException::class)
  internal constructor(transaction: T, node: JoinImpl, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState) : super(
        transaction,
        node,
        processInstance,
        state) {
  }

  override val node: JoinImpl
    get() = super.node as JoinImpl

  @Suppress("UNCHECKED_CAST")
  override fun getHandle() = super.getHandle() as ComparableHandle<JoinInstance<T>>

  @Throws(SQLException::class)
  fun addPredecessor(transaction: T, predecessor: ComparableHandle<out ProcessNodeInstance<T>>): Boolean {
    if (canAddNode(transaction) && directPredecessors.add(predecessor)) {
      processInstance.engine.updateStorage(transaction, this)
      return true
    }
    return false
  }

  val isFinished: Boolean
    get() = state == IProcessNodeInstance.NodeInstanceState.Complete || state == IProcessNodeInstance.NodeInstanceState.Failed

  @Throws(SQLException::class)
  override fun <V> startTask(transaction: T, messageService: IMessageService<V, T, ProcessNodeInstance<T>>): Boolean {
    if (node.startTask(messageService, this)) {
      return updateTaskState(transaction)
    }
    return false
  }

  /**
   * Update the state of the task, based on the predecessors
   * @param transaction The transaction to use for the operations.
   * *
   * @return `true` if the task is complete, `false` if not.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  private fun updateTaskState(transaction: T): Boolean {
    if (state == IProcessNodeInstance.NodeInstanceState.Complete) return false // Don't update if we're already complete

    val join = node
    val totalPossiblePredecessors = join.predecessors!!.size
    val realizedPredecessors = directPredecessors.size

    if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
      return true
    }

    var complete = 0
    var skipped = 0
    for (predecessor in resolvePredecessors(transaction)) {
      when (predecessor.state) {
        IProcessNodeInstance.NodeInstanceState.Complete                                                 -> complete += 1
        IProcessNodeInstance.NodeInstanceState.Cancelled, IProcessNodeInstance.NodeInstanceState.Failed -> skipped += 1
      }// do nothing
    }
    if (totalPossiblePredecessors - skipped < join.min) {
      failTask(transaction, ProcessException("Too many predecessors have failed"))
      cancelNoncompletedPredecessors(transaction)
      return false
    }

    if (complete >= join.min) {
      if (complete >= join.max) {
        return true
      }
      // XXX todo if we skipped/failed too many predecessors to ever be able to finish,
      return processInstance.getActivePredecessorsFor(transaction, join).size == 0
    }
    return false
  }

  @Throws(SQLException::class)
  private fun cancelNoncompletedPredecessors(transaction: T) {
    val preds = processInstance.getActivePredecessorsFor(transaction, node)
    for (pred in preds) {
      pred.tryCancelTask(transaction)
    }
  }

  @Throws(SQLException::class)
  override fun <V> provideTask(transaction: T, messageService: IMessageService<V, T, ProcessNodeInstance<T>>): Boolean {
    if (!isFinished) {
      return node.provideTask(transaction, messageService, this)
    }
    val directSuccessors = processInstance.getDirectSuccessors(transaction, this)
    var canAdd = false
    for (hDirectSuccessor in directSuccessors) {
      val directSuccessor = processInstance.engine.getNodeInstance(transaction,
                                                                   hDirectSuccessor,
                                                                   SecurityProvider.SYSTEMPRINCIPAL)
      if (directSuccessor!!.state == IProcessNodeInstance.NodeInstanceState.Started || directSuccessor.state == IProcessNodeInstance.NodeInstanceState.Complete) {
        canAdd = false
        break
      }
      canAdd = true
    }
    return canAdd
  }

  @Throws(SQLException::class)
  override fun tickle(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>) {
    super.tickle(transaction, messageService)
    val missingIdentifiers = TreeSet<Identifiable>()
    missingIdentifiers.addAll(node.predecessors!!)
    for (predDef in directPredecessors) {
      val pred: ProcessNodeInstance<T> = processInstance.engine.getNodeInstance(transaction, predDef, SecurityProvider.SYSTEMPRINCIPAL)
          ?: throw NullPointerException("No predecessor instance could be found")
      missingIdentifiers.remove(pred.node)
    }
    for (missingIdentifier in missingIdentifiers) {
      val candidate: ProcessNodeInstance<T>? = processInstance.getNodeInstance(transaction, missingIdentifier)
      if (candidate != null) {
        addPredecessor(transaction, candidate.getHandle())
      }
    }
    if (updateTaskState(transaction) && state != IProcessNodeInstance.NodeInstanceState.Complete) {
      processInstance.finishTask(transaction, messageService, this, null)
    }
  }

  @Throws(SQLException::class)
  private fun canAddNode(transaction: T): Boolean {
    if (!isFinished) {
      return true
    }
    val directSuccessors = processInstance.getDirectSuccessors(transaction, this)
    var canAdd = false
    for (hDirectSuccessor in directSuccessors) {
      val directSuccessor = processInstance.engine.getNodeInstance(transaction,
                                                                   hDirectSuccessor,
                                                                   SecurityProvider.SYSTEMPRINCIPAL)
      if (directSuccessor!!.state == IProcessNodeInstance.NodeInstanceState.Started || directSuccessor.state == IProcessNodeInstance.NodeInstanceState.Complete) {
        canAdd = false
        break
      }
      canAdd = true
    }
    return canAdd
  }

}
