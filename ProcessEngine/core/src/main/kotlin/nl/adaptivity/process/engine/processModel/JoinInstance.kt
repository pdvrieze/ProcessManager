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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handles
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.util.Identified
import org.w3c.dom.Node
import java.security.Principal
import java.sql.SQLException
import java.util.*


class JoinInstance : ProcessNodeInstance<JoinInstance> {

  interface Builder : ProcessNodeInstance.Builder<ExecutableJoin, JoinInstance> {

    val isFinished: Boolean
      get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

    override fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean {
      if (!isFinished) {
        val shouldProgress = node.provideTask(engineData, this)
        if (shouldProgress) {
          val directSuccessors = processInstanceBuilder.getDirectSuccessorsFor(this.handle)

          val canAdd = directSuccessors
            .asSequence()
            .none { it.state.isCommitted || it.state.isFinal }
          if (canAdd) {
            state = NodeInstanceState.Acknowledged
            return true
          }
        }
        return shouldProgress

      }
      return false
    }

    override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.takeTask(this)
    }

    override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.startTask(this)
    }

    override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      var committedPredecessorCount = 0
      var completedPredecessorCount = 0
      for(predecessorHandle in predecessors) {
        val predecessor = processInstanceBuilder.getChild(predecessorHandle)
        if (predecessor.state.isCommitted) {
          if (! predecessor.state.isFinal) {
            throw ProcessException("Predecessor ${predecessor} is committed but not final, cannot finish join without cancelling the predecessor")
          } else {
            committedPredecessorCount++
            if (predecessor.state== NodeInstanceState.Complete) {
              completedPredecessorCount++
            }
          }
        }
      }
      val cancelablePredecessors = mutableListOf<IProcessNodeInstance>()
      if (!node.isMultiMerge) {
        processInstanceBuilder
          .allChildren { ! it.state.isFinal && it.entryNo == entryNo && it.node preceeds node }
          .mapTo(cancelablePredecessors) { it }
      }

      if (committedPredecessorCount<node.min) {
        throw ProcessException("Finishing the join is not possible as the minimum amount of predecessors ${node.min} was not reached ${committedPredecessorCount}")
      }
      for(instanceToCancel in cancelablePredecessors) {
        processInstanceBuilder.updateChild(instanceToCancel) {
          cancelAndSkip(engineData)
        }
      }
      super.doFinishTask(engineData, resultPayload)
    }
  }

  class ExtBuilder(instance:JoinInstance, processInstanceBuilder: ProcessInstance.Builder) : ProcessNodeInstance.ExtBuilder<ExecutableJoin, JoinInstance>(instance, processInstanceBuilder), Builder {
    override var node: ExecutableJoin by overlay { instance.node }
    override fun build() = if (changed) JoinInstance(this) else base
  }

  class BaseBuilder(
    node: ExecutableJoin,
    predecessors: Iterable<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
    processInstanceBuilder: ProcessInstance.Builder,
    owner: Principal,
    entryNo: Int,
    handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
    state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableJoin, JoinInstance>(node, predecessors, processInstanceBuilder, owner, entryNo, handle, state), Builder {
    override fun build() = JoinInstance(this)
  }

  override val node: ExecutableJoin
    get() = super.node as ExecutableJoin

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<SecureObject<JoinInstance>>
        = super.getHandle() as ComparableHandle<SecureObject<JoinInstance>>

  /** Is this join completed or can other entries be added? */
  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  constructor(node: ExecutableJoin,
              predecessors: Collection<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
              owner: Principal,
              entryNo: Int,
              handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList()) :
        super(node, predecessors, hProcessInstance, owner, entryNo, handle, state, results) {
  }

  constructor(builder:Builder): this(builder.node, builder.predecessors, builder.hProcessInstance, builder.owner, builder.entryNo, builder.handle, builder.state, builder.results)

  @JvmName("updateJoin")
  fun updateJoin(writableEngineData: MutableProcessEngineDataAccess, instance: ProcessInstance, body: Builder.() -> Unit): PNIPair<JoinInstance> {
    val processInstanceBuilder = instance.builder()
    val origHandle = getHandle()
    val builder = builder(processInstanceBuilder).apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        val nodeFuture = processInstanceBuilder.storeChild(builder)
        return PNIPair(processInstanceBuilder.build(writableEngineData), nodeFuture.get() as JoinInstance)
      }
    }
    return PNIPair(instance, this)
  }

  override fun builder(processInstanceBuilder: ProcessInstance.Builder) = ExtBuilder(this, processInstanceBuilder)

  @Deprecated("Use updateJoin when using this function directly.", ReplaceWith("updateJoin(transaction, body)"))

  @Throws(SQLException::class)
  fun addPredecessor(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>): PNIPair<JoinInstance>? {

    if (canAddNode(engineData) && predecessor !in predecessors) {
      return updateJoin(engineData, processInstance) {
        predecessors.add(predecessor)
      }
    }
    return null
  }

  @Throws(SQLException::class)
  override fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<JoinInstance> {
    if (node.startTask(this)) {
      return updateTaskState(engineData, processInstance)
    }
    return PNIPair(processInstance, this)
  }

  @Deprecated("Use the builder directly")
  @Suppress("UNCHECKED_CAST")
  override fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node?): PNIPair<JoinInstance> {
    return update(engineData) {
      finishTask(engineData, resultPayload)
    }
/*

    var committedPredecessorCount = 0
    var completedPredecessorCount = 0
    val cancelablePredecessors = mutableListOf<ProcessNodeInstance<*>>()
    for(predecessorId in node.predecessors) {
      val predecessor = processInstance.getChild(predecessorId.id, entryNo)?.withPermission()
      if (predecessor==null) {
        val splitInstance = precedingClosure(engineData).filterIsInstance(SplitInstance::class.java).lastOrNull()
        if (splitInstance != null) {
          if (splitInstance.state.isFinal) {
            throw ProcessException(
              "Missing predecessor $predecessorId for join ${node.id}, split ${splitInstance.node.id} is already final")
          } else {
            // Finish the split and try again
            return processInstance.finishTask(engineData, splitInstance, null)
              .instance.finishTask(engineData, this, resultPayload)
          }
        } // else if we don't have a preceding split, it doesn't need to be "finished"
      } else {
        if (predecessor.state.isCommitted) {
          if (! predecessor.state.isFinal) {
            throw ProcessException("Predecessor ${predecessorId} is committed but not final, cannot finish join without cancelling the predecessor")
          } else {
            committedPredecessorCount++
            if (predecessor.state== NodeInstanceState.Complete) {
              completedPredecessorCount++
            }
          }
        } else {
          val predPred = predecessor.predecessors.map { engineData.nodeInstance(it).withPermission() }
          val splitCandidate = predPred.firstOrNull()
          cancelablePredecessors.add(predecessor)
//          if (splitCandidate is SplitInstance) {
//          } else {
//            throw ProcessException("Predecessor $predecessorId cannot be cancelled as it has non-split predecessor(s) ${predPred.joinToString { "${it.node.id}:${it.state}" }}")
//          }
        }
      }
    }
    if (committedPredecessorCount<node.min) {
      throw ProcessException("Finishing the join is not possible as the minimum amount of predecessors ${node.min} was not reached ${committedPredecessorCount}")
    }
    val processInstance = if(node.isMultiMerge) processInstance else cancelablePredecessors.fold(processInstance) { processInstance, instanceToCancel ->
      instanceToCancel.cancelAndSkip(engineData, processInstance).instance
    }
    return super.finishTask(engineData, processInstance, resultPayload) as PNIPair<JoinInstance>
*/
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
  private fun updateTaskState(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<JoinInstance> {

    fun next() = updateJoin(engineData, processInstance) { state = NodeInstanceState.Started }.let {
      it.instance.finishTask(engineData, it.node, null)
    }

    if (state == NodeInstanceState.Complete) return PNIPair(processInstance, this) // Don't update if we're already complete

    val join = node
    val totalPossiblePredecessors = join.predecessors.size
    val realizedPredecessors = predecessors.size

    if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
      return next()
    }

    var complete = 0
    var skipped = 0
    for (predecessor in resolvePredecessors(engineData)) {
      when (predecessor.state) {
        NodeInstanceState.Complete -> complete += 1

        NodeInstanceState.Skipped,
        NodeInstanceState.SkippedCancel,
        NodeInstanceState.Cancelled,
        NodeInstanceState.SkippedFail,
        NodeInstanceState.Failed   -> skipped += 1
        else                                                                 -> Unit // do nothing
      }
    }
    if (totalPossiblePredecessors - skipped < join.min) {
      cancelNoncompletedPredecessors(engineData, processInstance).let { processInstance ->
        failTask(engineData, processInstance, ProcessException("Too many predecessors have failed"))
      }
    }

    if (complete >= join.min) {
      if (complete >= join.max || processInstance.getActivePredecessorsFor(engineData, this).isEmpty()) {
        return next()
      }
    }
    return PNIPair(processInstance, this)
  }

  @Throws(SQLException::class)
  private fun cancelNoncompletedPredecessors(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): ProcessInstance {
    val preds = processInstance.getActivePredecessorsFor(engineData, this)
    return preds.fold(processInstance) { processInstance, pred -> pred.tryCancelTask(engineData, processInstance).instance }
  }


  @Throws(SQLException::class)
  override fun tickle(engineData: MutableProcessEngineDataAccess,
                      instance: ProcessInstance, messageService: IMessageService<*>): PNIPair<JoinInstance> {
    val (processInstance, self) = super.tickle(engineData, instance, messageService) as PNIPair<JoinInstance>
    val missingIdentifiers = TreeSet<Identified>(node.predecessors)
    val data = engineData

    predecessors
          .forEach { missingIdentifiers
                .remove(data.nodeInstance(it).withPermission().node) }

    return self.updateJoin(engineData, processInstance) {
      val processInstance = engineData.instance(hProcessInstance).withPermission()
      missingIdentifiers.asSequence()
            .flatMap { processInstance.getNodeInstances(it) }
            .forEach { predecessors.add(it.getHandle()) }
    }.let { updatedPair ->
      updatedPair.node.updateTaskState(engineData, updatedPair.instance)
    }.let { updatedPair ->
      if (updatedPair.node.state== NodeInstanceState.Started) {
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
    fun build(joinImpl: ExecutableJoin,
              predecessors: Set<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstanceBuilder: ProcessInstance.Builder,
              entryNo: Int,
              handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, processInstanceBuilder, processInstanceBuilder.owner, entryNo, handle, state).apply(body))
    }

    fun build(joinImpl: ExecutableJoin,
              predecessors: Set<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstance: ProcessInstance,
              entryNo: Int,
              handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              body: Builder.() -> Unit):JoinInstance {
      return build(joinImpl, predecessors, processInstance.builder(), entryNo, handle, state, body)
    }

    /**
     * Update the state of the task. Returns true if the task should now be finished by the caller.
     * @return `true` if the caller should finish the task, `false` if not
     */
    @JvmStatic
    private fun Builder.updateTaskState(engineData: MutableProcessEngineDataAccess): Boolean {

      if (state == NodeInstanceState.Complete) return false // Don't update if we're already complete

      val join = node
      val totalPossiblePredecessors = join.predecessors.size
      val realizedPredecessors = predecessors.size

      if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
        state = NodeInstanceState.Started
        return true
      }

      var complete = 0
      var skipped = 0
      for (predecessor in activePredecessors() ) {
        when (predecessor.state) {
          NodeInstanceState.Complete -> complete += 1

          NodeInstanceState.Skipped,
          NodeInstanceState.SkippedCancel,
          NodeInstanceState.Cancelled,
          NodeInstanceState.SkippedFail,
          NodeInstanceState.Failed   -> skipped += 1
          else                                                                 -> Unit // do nothing
        }
      }
      if (totalPossiblePredecessors - skipped < join.min) {
        // XXX this needs to be done in the caller
        // cancelNoncompletedPredecessors(engineData)
        failTask(engineData, ProcessException("Too many predecessors have failed"))
      }

      if (complete >= join.min) {
        if (complete >= join.max || activePredecessors().none()) {
          return true
        }
      }
      return false
    }

    private fun Builder.activePredecessors(): Sequence<IProcessNodeInstance> {
      return processInstanceBuilder.allChildren { it.handle() in predecessors  }
    }

  }

}
