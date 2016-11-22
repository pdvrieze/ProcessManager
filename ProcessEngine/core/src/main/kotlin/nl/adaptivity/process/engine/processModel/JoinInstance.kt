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

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.JoinImpl
import nl.adaptivity.process.util.Identifiable
import org.w3c.dom.Node
import java.sql.SQLException
import java.util.*


class JoinInstance<T : ProcessTransaction<T>> : ProcessNodeInstance<T> {

  interface Builder<T: ProcessTransaction<T>> : ProcessNodeInstance.Builder<T, JoinImpl> {
    override fun build(): JoinInstance<T>
  }

  class ExtBuilder<T : ProcessTransaction<T>>(instance:JoinInstance<T>) : ProcessNodeInstance.ExtBuilderBase<T, JoinImpl>(instance), Builder<T> {
    override var node: JoinImpl by overlay { instance.node }
    override fun build() = JoinInstance(this)
  }

  class BaseBuilder<T : ProcessTransaction<T>>(
        node: JoinImpl,
        predecessors: Iterable<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
        processInstance: ProcessInstance<T>,
        handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
        state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<T, JoinImpl>(node, predecessors, processInstance, handle, state), Builder<T> {
    override fun build() = JoinInstance(this)
  }

  override val node: JoinImpl
    get() = super.node as JoinImpl

  @Suppress("UNCHECKED_CAST")
  override val handle: ComparableHandle<out SecureObject<JoinInstance<T>>>
    get() = super.handle as ComparableHandle<out SecureObject<JoinInstance<T>>>


  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  constructor(node: JoinImpl,
              predecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
              processInstance: ProcessInstance<T>,
              handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList()) :
        super(node, predecessors, processInstance, handle, state, results) {
  }

  constructor(builder:Builder<T>): this(builder.node, builder.predecessors, builder.processInstance, builder.handle, builder.state, builder.results)

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param node
   * *
   * @param processInstance
   */
  @Throws(SQLException::class)
  internal constructor(transaction: T, node: JoinImpl, processInstance: ProcessInstance<T>, state: NodeInstanceState)
        : super(transaction, node, processInstance, state) {
  }

  fun updateJoin(transaction: T, body: Builder<T>.() -> Unit):JoinInstance<T> {
    val origHandle = handle
    return JoinInstance(ExtBuilder(this).apply { body() }).apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  @Deprecated("Use updateJoin when using this function directly.", ReplaceWith("updateJoin(transaction, body)"))
  override fun update(transaction: T,
                      body: ProcessNodeInstance.Builder<T, out ExecutableProcessNode>.() -> Unit): ProcessNodeInstance<T> {
    return updateJoin(transaction) { body() }
  }

  @Throws(SQLException::class)
  fun addPredecessor(transaction: T, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>): JoinInstance<T>? {

    if (canAddNode(transaction) && predecessor !in directPredecessors) {
      return updateJoin(transaction) {
        predecessors.add(predecessor)
      }
    }
    return null
  }

  @Throws(SQLException::class)
  override fun <V> startTask(transaction: T, messageService: IMessageService<V, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    if (node.startTask(messageService, this)) {
      return updateTaskState(transaction)
    }
    return this
  }

  override fun finishTask(transaction: T, resultPayload: Node?)
        = super.finishTask(transaction, resultPayload) as JoinInstance<T>

  override fun <U> takeTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>)
        = super.takeTask(transaction, messageService) as JoinInstance<T>

  override fun cancelTask(transaction: T)
        = super.cancelTask(transaction) as JoinInstance<T>

  override fun tryCancelTask(transaction: T)
        = super.tryCancelTask(transaction) as JoinInstance<T>

  override fun failTask(transaction: T, cause: Throwable)
        = super.failTask(transaction, cause) as JoinInstance<T>

  override fun failTaskCreation(transaction: T, cause: Throwable)
        = super.failTaskCreation(transaction, cause) as JoinInstance<T>

  /**
   * Update the state of the task, based on the predecessors
   * @param transaction The transaction to use for the operations.
   * *
   * @return `true` if the task is complete, `false` if not.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  private fun updateTaskState(transaction: T): JoinInstance<T> {

    fun next() = updateJoin(transaction) { state = NodeInstanceState.Started }.finishTask(transaction, null)

    if (state == NodeInstanceState.Complete) return this // Don't update if we're already complete

    val join = node
    val totalPossiblePredecessors = join.predecessors.size
    val realizedPredecessors = directPredecessors.size

    if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
      return next()
    }

    var complete = 0
    var skipped = 0
    for (predecessor in resolvePredecessors(transaction)) {
      when (predecessor.state) {
        NodeInstanceState.Complete                            -> complete += 1
        NodeInstanceState.Cancelled, NodeInstanceState.Failed -> skipped += 1
      }// do nothing
    }
    if (totalPossiblePredecessors - skipped < join.min) {
      cancelNoncompletedPredecessors(transaction)
      return failTask(transaction, ProcessException("Too many predecessors have failed"))
    }

    if (complete >= join.min) {
      if (complete >= join.max || processInstance.getActivePredecessorsFor(transaction, join).isEmpty()) {
        return next()
      }
    }
    return this
  }

  @Throws(SQLException::class)
  private fun cancelNoncompletedPredecessors(transaction: T) {
    val preds = processInstance.getActivePredecessorsFor(transaction, node)
    for (pred in preds) {
      pred.tryCancelTask(transaction)
    }
  }

  @Throws(SQLException::class)
  override fun <V> provideTask(transaction: T, messageService: IMessageService<V, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    if (!isFinished) {
      val shouldProgress = node.provideTask(transaction, messageService, this)
      if (shouldProgress) {
        val directSuccessors = processInstance.getDirectSuccessors(transaction, this)
        val canAdd = directSuccessors
              .asSequence()
              .map { transaction.readableEngineData.nodeInstances[it].mustExist(it).withPermission() }
              .none { it.state == NodeInstanceState.Started || it.state == NodeInstanceState.Complete }
        if (canAdd) {
          return updateJoin(transaction) { state = NodeInstanceState.Sent }.takeTask(transaction, messageService)
        }
        return this // no need to update as the initial state is already pending.
      }

    }
    return this
  }

  @Throws(SQLException::class)
  override fun tickle(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>): JoinInstance<T> {
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
        addPredecessor(transaction, candidate.handle)
      }
    }
    val updatedInstance = updateTaskState(transaction)
    if (updatedInstance.state==NodeInstanceState.Started) {
      return updatedInstance.finishTask(transaction)
    } else {
      return updatedInstance
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
      val directSuccessor:ProcessNodeInstance<T> = processInstance.engine.getNodeInstance(transaction,
                                                                   hDirectSuccessor,
                                                                   SecurityProvider.SYSTEMPRINCIPAL) ?: throw NullPointerException("Successor not resolved")
      if (directSuccessor.state == NodeInstanceState.Started || directSuccessor.state == NodeInstanceState.Complete) {
        canAdd = false
        break
      }
      canAdd = true
    }
    return canAdd
  }

  companion object {
    fun <T:ProcessTransaction<T>> build(joinImpl: JoinImpl,
                                        predecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
                                        processInstance: ProcessInstance<T>,
                                        handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
                                        state: NodeInstanceState = NodeInstanceState.Pending,
                                        body: Builder<T>.() -> Unit):JoinInstance<T> {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, processInstance, handle, state).apply(body))
    }
  }

}
