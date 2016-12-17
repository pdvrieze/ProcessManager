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
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.util.Identified
import org.w3c.dom.Node
import java.security.Principal
import java.sql.SQLException
import java.util.*


class JoinInstance : ProcessNodeInstance {

  typealias SecureT = SecureObject<JoinInstance>
  typealias HandleT = ComparableHandle<out SecureT>

  interface Builder : ProcessNodeInstance.Builder<ExecutableJoin> {
    override fun build(): JoinInstance
  }

  class ExtBuilder(instance:JoinInstance) : ProcessNodeInstance.ExtBuilderBase<ExecutableJoin>(instance), Builder {
    override var node: ExecutableJoin by overlay { instance.node }
    override fun build() = JoinInstance(this)
  }

  class BaseBuilder(
        node: ExecutableJoin,
        predecessors: Iterable<ProcessNodeInstance.HandleT>,
        hProcessInstance: ProcessInstance.HandleT,
        owner: Principal,
        handle: ProcessNodeInstance.HandleT = Handles.getInvalid(),
        state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableJoin>(node, predecessors, hProcessInstance, owner, handle, state), Builder {
    override fun build() = JoinInstance(this)
  }

  override val node: ExecutableJoin
    get() = super.node as ExecutableJoin

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): HandleT
        = super.getHandle() as HandleT

  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  constructor(node: ExecutableJoin,
              predecessors: Collection<ProcessNodeInstance.HandleT>,
              hProcessInstance: ProcessInstance.HandleT,
              owner: Principal,
              handle: ProcessNodeInstance.HandleT = Handles.getInvalid(),
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

  @JvmName("updateJoin")
  fun updateJoin(writableEngineData: MutableProcessEngineDataAccess, instance: ProcessInstance, body: Builder.() -> Unit): PNIPair<JoinInstance> {
    val origHandle = getHandle()
    val builder = builder().apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        return instance.updateNode(writableEngineData, builder.build())
      } else {
        return PNIPair(instance, this)
      }
    } else {
      return PNIPair(instance, this)
    }
  }

  override fun builder() = ExtBuilder(this)

  @Deprecated("Use updateJoin when using this function directly.", ReplaceWith("updateJoin(transaction, body)"))

  override fun update(writableEngineData: MutableProcessEngineDataAccess, instance: ProcessInstance, body: ProcessNodeInstance.Builder<*>.() -> Unit): PNIPair<JoinInstance> {
    val origHandle = getHandle()
    val builder = builder().apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        return instance.updateNode(writableEngineData, builder.build())
      } else {
        return PNIPair(instance, this)
      }
    } else {
      return PNIPair(instance, this)
    }
  }

  @Throws(SQLException::class)
  fun addPredecessor(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ProcessNodeInstance.HandleT): PNIPair<JoinInstance>? {

    if (canAddNode(engineData) && predecessor !in directPredecessors) {
      return updateJoin(engineData, processInstance) {
        predecessors.add(predecessor)
      }
    }
    return null
  }

  @Throws(SQLException::class)
  override fun <V> startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, messageService: IMessageService<V, MutableProcessEngineDataAccess, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    if (node.startTask(messageService, this)) {
      return updateTaskState(engineData, processInstance)
    }
    return PNIPair(processInstance, this)
  }

  override fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node?)
        = super.finishTask(engineData, processInstance, resultPayload) as PNIPair<JoinInstance>

  override fun <U> takeTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, messageService: IMessageService<U, MutableProcessEngineDataAccess, in ProcessNodeInstance>)
        = super.takeTask(engineData, processInstance, messageService) as PNIPair<JoinInstance>

  override fun cancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance)
        = super.cancelTask(engineData, processInstance) as PNIPair<JoinInstance>

  override fun tryCancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance)
        = super.tryCancelTask(engineData, processInstance) as PNIPair<JoinInstance>

  override fun failTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable)
        = super.failTask(engineData, processInstance, cause) as PNIPair<JoinInstance>

  override fun failTaskCreation(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable)
        = super.failTaskCreation(engineData, processInstance, cause) as PNIPair<JoinInstance>

  /**
   * Update the state of the task, based on the predecessors
   * @param transaction The transaction to use for the operations.
   * *
   * @return `true` if the task is complete, `false` if not.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  private fun updateTaskState(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<JoinInstance> {

    fun next() = updateJoin(engineData, processInstance) { state = NodeInstanceState.Started }.let {
      it.node.finishTask(engineData, it.instance, null)
    }

    if (state == NodeInstanceState.Complete) return PNIPair(processInstance, this) // Don't update if we're already complete

    val join = node
    val totalPossiblePredecessors = join.predecessors.size
    val realizedPredecessors = directPredecessors.size

    if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
      return next()
    }

    var complete = 0
    var skipped = 0
    for (predecessor in resolvePredecessors(engineData)) {
      when (predecessor.state) {
        NodeInstanceState.Complete                            -> complete += 1
        NodeInstanceState.Cancelled, NodeInstanceState.Failed -> skipped += 1
      }// do nothing
    }
    if (totalPossiblePredecessors - skipped < join.min) {
      cancelNoncompletedPredecessors(engineData, processInstance).let { processInstance ->
        failTask(engineData, processInstance, ProcessException("Too many predecessors have failed"))
      }
    }

    if (complete >= join.min) {
      if (complete >= join.max || processInstance.getActivePredecessorsFor(engineData, join).isEmpty()) {
        return next()
      }
    }
    return PNIPair(processInstance, this)
  }

  @Throws(SQLException::class)
  private fun cancelNoncompletedPredecessors(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): ProcessInstance {
    val preds = processInstance.getActivePredecessorsFor(engineData, node)
    return preds.fold(processInstance) { processInstance, pred -> pred.tryCancelTask(engineData, processInstance).instance }
  }


  @Throws(SQLException::class)
  override fun <V> provideTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, messageService: IMessageService<V, MutableProcessEngineDataAccess, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    if (!isFinished) {
      val shouldProgress = node.provideTask(engineData, messageService, processInstance, this)
      if (shouldProgress) {
        val processInstance = engineData.instance(hProcessInstance).withPermission()
        val directSuccessors = processInstance.getDirectSuccessors(engineData, this)
        val canAdd = directSuccessors
              .asSequence()
              .map { engineData.nodeInstance(it).withPermission() }
              .none { it.state == NodeInstanceState.Started || it.state == NodeInstanceState.Complete }
        if (canAdd) {
          return updateJoin(engineData, processInstance) { state = NodeInstanceState.Sent }.let { pair -> pair.node.takeTask(engineData, pair.instance, messageService) }
        }
        return PNIPair(processInstance, this) // no need to update as the initial state is already pending.
      }

    }
    return PNIPair(processInstance, this)
  }

  @Throws(SQLException::class)
  override fun tickle(engineData: MutableProcessEngineDataAccess,
                      instance: ProcessInstance, messageService: IMessageService<*, MutableProcessEngineDataAccess, in ProcessNodeInstance>): PNIPair<JoinInstance> {
    val (processInstance, self) = super.tickle(engineData, instance, messageService) as PNIPair<JoinInstance>
    val missingIdentifiers = TreeSet<Identified>(node.predecessors)
    val data = engineData

    directPredecessors
          .forEach { missingIdentifiers
                .remove(data.nodeInstance(it).withPermission().node) }

    return self.updateJoin(engineData, processInstance) {
      val processInstance = engineData.instance(hProcessInstance).withPermission()
      missingIdentifiers.asSequence()
            .mapNotNull { processInstance.getNodeInstance(it) }
            .forEach { predecessors.add(it.getHandle()) }
    }.let { updatedPair ->
      updatedPair.node.updateTaskState(engineData, updatedPair.instance)
    }.let { updatedPair ->
      if (updatedPair.node.state==NodeInstanceState.Started) {
        updatedPair.node.finishTask(engineData = engineData, processInstance = updatedPair.instance)
      } else {
        updatedPair
      }
    }
  }

  @Throws(SQLException::class)
  private fun canAddNode(engineData: ProcessEngineDataAccess): Boolean {
    if (!isFinished) {
      return true
    }
    val processInstance = engineData.instance(hProcessInstance).withPermission()
    val directSuccessors = processInstance.getDirectSuccessors(engineData, this)

    return directSuccessors.asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .none { it.state == NodeInstanceState.Started || it.state == NodeInstanceState.Complete }

  }

  companion object {
    fun <T:ProcessTransaction> build(joinImpl: ExecutableJoin,
                                     predecessors: Set<ProcessNodeInstance.HandleT>,
                                     hProcessInstance: ProcessInstance.HandleT,
                                     owner: Principal,
                                     handle: ProcessNodeInstance.HandleT = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, hProcessInstance, owner, handle, state).apply(body))
    }

    fun <T:ProcessTransaction> build(joinImpl: ExecutableJoin,
                                     predecessors: Set<ProcessNodeInstance.HandleT>,
                                     processInstance: ProcessInstance,
                                     handle: ProcessNodeInstance.HandleT = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, processInstance.getHandle(), processInstance.owner, handle, state).apply(body))
    }
  }

}
