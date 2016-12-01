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
import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identifiable
import org.w3c.dom.Node
import java.security.Principal
import java.sql.SQLException
import java.util.*


class JoinInstance : ProcessNodeInstance {

  interface Builder : ProcessNodeInstance.Builder<ExecutableJoin> {
    override fun build(): JoinInstance
  }

  class ExtBuilder(instance:JoinInstance) : ProcessNodeInstance.ExtBuilderBase<ExecutableJoin>(instance), Builder {
    override var node: ExecutableJoin by overlay { instance.node }
    override fun build() = JoinInstance(this)
  }

  class BaseBuilder(
        node: ExecutableJoin,
        predecessors: Iterable<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
        hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
        owner: Principal,
        handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
        state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableJoin>(node, predecessors, hProcessInstance, owner, handle, state), Builder {
    override fun build() = JoinInstance(this)
  }

  override val node: ExecutableJoin
    get() = super.node as ExecutableJoin

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<out SecureObject<JoinInstance>>
        = super.getHandle() as ComparableHandle<out SecureObject<JoinInstance>>

  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  constructor(node: ExecutableJoin,
              predecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
              hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
              owner: Principal,
              handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList()) :
        super(node, predecessors, hProcessInstance, owner, handle, state, results) {
  }

  constructor(builder:Builder): this(builder.node, builder.predecessors, builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results)

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param node
   * *
   * @param processInstance
   */
  @Throws(SQLException::class)
  internal constructor(transaction: ProcessTransaction, node: ExecutableJoin, processInstance: ProcessInstance, state: NodeInstanceState)
        : super(transaction, node, processInstance, state) {
  }

  fun updateJoin(transaction: ProcessTransaction, body: Builder.() -> Unit):JoinInstance {
    val origHandle = handle
    return JoinInstance(ExtBuilder(this).apply { body() }).apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  override fun builder(): Builder = ExtBuilder(this)

  @Deprecated("Use updateJoin when using this function directly.", ReplaceWith("updateJoin(transaction, body)"))
  override fun update(transaction: ProcessTransaction,
                      body: ProcessNodeInstance.Builder<out ExecutableProcessNode>.() -> Unit): ProcessNodeInstance {
    return super.update(transaction, body)
  }

  @Throws(SQLException::class)
  fun addPredecessor(transaction: ProcessTransaction, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>): JoinInstance? {

    if (canAddNode(transaction) && predecessor !in directPredecessors) {
      return updateJoin(transaction) {
        predecessors.add(predecessor)
      }
    }
    return null
  }

  @Throws(SQLException::class)
  override fun <V> startTask(transaction: ProcessTransaction, messageService: IMessageService<V, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    if (node.startTask(messageService, this)) {
      return updateTaskState(transaction)
    }
    return this
  }

  override fun finishTask(transaction: ProcessTransaction, resultPayload: Node?)
        = super.finishTask(transaction, resultPayload) as JoinInstance

  override fun <U> takeTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>)
        = super.takeTask(transaction, messageService) as JoinInstance

  override fun cancelTask(transaction: ProcessTransaction)
        = super.cancelTask(transaction) as JoinInstance

  override fun tryCancelTask(transaction: ProcessTransaction)
        = super.tryCancelTask(transaction) as JoinInstance

  override fun failTask(transaction: ProcessTransaction, cause: Throwable)
        = super.failTask(transaction, cause) as JoinInstance

  override fun failTaskCreation(transaction: ProcessTransaction, cause: Throwable)
        = super.failTaskCreation(transaction, cause) as JoinInstance

  /**
   * Update the state of the task, based on the predecessors
   * @param transaction The transaction to use for the operations.
   * *
   * @return `true` if the task is complete, `false` if not.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  private fun updateTaskState(transaction: ProcessTransaction): JoinInstance {

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
      val processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
      if (complete >= join.max || processInstance.getActivePredecessorsFor(transaction, join).isEmpty()) {
        return next()
      }
    }
    return this
  }

  @Throws(SQLException::class)
  private fun cancelNoncompletedPredecessors(transaction: ProcessTransaction) {
    val processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    val preds = processInstance.getActivePredecessorsFor(transaction, node)
    for (pred in preds) {
      pred.tryCancelTask(transaction)
    }
  }

  @Throws(SQLException::class)
  override fun <V> provideTask(transaction: ProcessTransaction, messageService: IMessageService<V, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    if (!isFinished) {
      val shouldProgress = node.provideTask(transaction, messageService, this)
      if (shouldProgress) {
        val processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
        val directSuccessors = processInstance.getDirectSuccessors(transaction, this)
        val canAdd = directSuccessors
              .asSequence()
              .map { transaction.readableEngineData.nodeInstance(it).withPermission() }
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
  override fun tickle(transaction: ProcessTransaction,
                      messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    super.tickle(transaction, messageService)
    val missingIdentifiers = TreeSet<Identifiable>(node.predecessors)
    val data = transaction.readableEngineData

    directPredecessors
          .forEach { missingIdentifiers
                .remove(data.nodeInstance(it).withPermission().node) }

    return updateJoin(transaction) {
      val processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
      missingIdentifiers.asSequence()
            .mapNotNull { processInstance.getNodeInstance(transaction, it) }
            .forEach { predecessors.add(it.handle) }
    }.let { updated ->
      updateTaskState(transaction)
    }.let { updated ->
      if (updated.state==NodeInstanceState.Started) {
        updated.finishTask(transaction)
      } else {
        updated
      }
    }
  }

  @Throws(SQLException::class)
  private fun canAddNode(transaction: ProcessTransaction): Boolean {
    if (!isFinished) {
      return true
    }
    val processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    val directSuccessors = processInstance.getDirectSuccessors(transaction, this)

    return directSuccessors.asSequence()
          .map { transaction.readableEngineData.nodeInstance(it).withPermission() }
          .none { it.state == NodeInstanceState.Started || it.state == NodeInstanceState.Complete }

  }

  companion object {
    fun <T:ProcessTransaction> build(joinImpl: ExecutableJoin,
                                     predecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                                     hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
                                     owner: Principal,
                                     handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, hProcessInstance, owner, handle, state).apply(body))
    }

    fun <T:ProcessTransaction> build(joinImpl: ExecutableJoin,
                                     predecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                                     processInstance: ProcessInstance,
                                     handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, processInstance.handle, processInstance.owner, handle, state).apply(body))
    }
  }

}
