/*
 * Copyright (c) 2017.
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
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.dom.Node
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.security.Principal
import kotlin.jvm.JvmStatic

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
      if(node.startTask(this)) {
        return updateTaskState(engineData)
      } else {
        return false
      }
    }

    override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      var committedPredecessorCount = 0
      var completedPredecessorCount = 0
      predecessors
        .map { processInstanceBuilder.getChild(it) }
        .filter { it.state.isCommitted }
        .forEach {
          if (! it.state.isFinal) {
            throw ProcessException("Predecessor $it is committed but not final, cannot finish join without cancelling the predecessor")
          } else {
            committedPredecessorCount++
            if (it.state== NodeInstanceState.Complete) {
              completedPredecessorCount++
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
        throw ProcessException("Finishing the join is not possible as the minimum amount of predecessors ${node.min} was not reached (predecessor count: $committedPredecessorCount)")
      }
      for(instanceToCancel in cancelablePredecessors) {
        processInstanceBuilder.updateChild(instanceToCancel) {
          if(this is Builder && this.updateTaskState(engineData)) {
            this.state = NodeInstanceState.Complete
          } else if (! state.isFinal) this.cancelAndSkip(engineData)
        }
      }
      super.doFinishTask(engineData, resultPayload)
    }
  }

  class ExtBuilder(instance:JoinInstance, processInstanceBuilder: ProcessInstance.Builder) : ProcessNodeInstance.ExtBuilder<ExecutableJoin, JoinInstance>(instance, processInstanceBuilder), Builder {
    override var node: ExecutableJoin by overlay { instance.node }
    override fun build() = if (changed) JoinInstance(this) else base
    override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) = skipTaskImpl(engineData, newState)

  }

  class BaseBuilder(
      node: ExecutableJoin,
      predecessors: Iterable<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
      processInstanceBuilder: ProcessInstance.Builder,
      owner: Principal,
      entryNo: Int,
      handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
      state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableJoin, JoinInstance>(node, predecessors, processInstanceBuilder, owner, entryNo, handle, state), Builder {
    override fun build() = JoinInstance(this)
    override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) = skipTaskImpl(engineData, newState)

  }

  override val node: ExecutableJoin
    get() = super.node as ExecutableJoin

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<SecureObject<JoinInstance>>
        = super.getHandle() as ComparableHandle<SecureObject<JoinInstance>>

  fun canFinish() = predecessors.size>=node.min

  constructor(node: ExecutableJoin,
              predecessors: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstanceBuilder: ProcessInstance.Builder,
              hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
              owner: Principal,
              entryNo: Int,
              handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList()) :
        super(node, predecessors, processInstanceBuilder, hProcessInstance, owner, entryNo, handle, state, results) {
    assert(predecessors.none { !it.isValid }, {"When creating joins all handles should be valid $predecessors"})
  }

  constructor(builder:Builder): this(builder.node, builder.predecessors, builder.processInstanceBuilder, builder.hProcessInstance, builder.owner, builder.entryNo, builder.handle, builder.state, builder.results)

  override fun builder(processInstanceBuilder: ProcessInstance.Builder) = ExtBuilder(this, processInstanceBuilder)

  companion object {
    fun build(joinImpl: ExecutableJoin,
              predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstanceBuilder: ProcessInstance.Builder,
              entryNo: Int,
              handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              body: Builder.() -> Unit):JoinInstance {
      return JoinInstance(BaseBuilder(joinImpl, predecessors, processInstanceBuilder, processInstanceBuilder.owner, entryNo, handle, state).apply(body))
    }

    fun build(joinImpl: ExecutableJoin,
              predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstance: ProcessInstance,
              entryNo: Int,
              handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
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

      val predecessorsToAdd = mutableListOf<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>()
      // register existing predecessors
      val instantiatedPredecessors = processInstanceBuilder.allChildren { pred ->
        join in pred.node.successors &&
        ( pred.handle() in predecessors ||
          node.getExistingInstance(engineData, processInstanceBuilder, pred, pred.entryNo).first?.let { predecessorsToAdd.add(pred.handle()); it.handle() } == handle() )
      }.toList()
      predecessors.addAll(predecessorsToAdd)

      if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
        state = NodeInstanceState.Started
        return true
      }

      var complete = 0
      var skipped = 0
      for (predecessor in instantiatedPredecessors ) {
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
        if (totalPossiblePredecessors-complete-skipped ==0) return true
        if (complete >= join.max || instantiatedPredecessors.none()) {
          return true
        }
      }
      return false
    }

    private fun Builder.skipTaskImpl(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
      // Skipping a join merely triggers a recalculation
      assert(newState == NodeInstanceState.Skipped || newState == NodeInstanceState.SkippedCancel || newState == NodeInstanceState.SkippedFail)
      updateTaskState(engineData)
      store(engineData)
      processInstanceBuilder.storeChild(this)
    }


  }

}
