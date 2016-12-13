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
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.collection.replaceByNotNull
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.mustExist
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableSplit
import java.security.Principal

/**
 * Specialisation of process node instance for splits
 */
class SplitInstance : ProcessNodeInstance {

  interface Builder : ProcessNodeInstance.Builder<ExecutableSplit> {
    override fun build(): SplitInstance
    var predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>?
      get() = predecessors.firstOrNull()
      set(value) = predecessors.replaceByNotNull(value)
  }

  class ExtBuilder(instance:SplitInstance) : ProcessNodeInstance.ExtBuilderBase<ExecutableSplit>(instance), Builder {
    override var node: ExecutableSplit by overlay { instance.node }
    override fun build() = SplitInstance(this)
  }

  class BaseBuilder(
      node: ExecutableSplit,
      predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>,
      hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
      owner: Principal,
      handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
      state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableSplit>(node, listOf(predecessor), hProcessInstance, owner, handle, state), Builder {
    override fun build() = SplitInstance(this)
  }

  override val node: ExecutableSplit
    get() = super.node as ExecutableSplit


  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<out SecureObject<SplitInstance>>
      = super.getHandle() as ComparableHandle<out SecureObject<SplitInstance>>

  constructor(node: ExecutableSplit,
              predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>,
              hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
              owner: Principal,
              handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList()) :
      super(node, listOf(predecessor), hProcessInstance, owner, handle, state, results) {
  }

  constructor(builder: SplitInstance.Builder): this(builder.node, builder.predecessor?: throw NullPointerException("Missing predecessor node instance"), builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results)

  override fun update(transaction: ProcessTransaction, body: ProcessNodeInstance.Builder<out ExecutableProcessNode>.() -> Unit): SplitInstance {
    val origHandle = handle
    return ExtBuilder(this).apply(body).build().apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  override fun builder(): ProcessNodeInstance.Builder<out ExecutableProcessNode> {
    return ExtBuilder(this)
  }

  @JvmName("updateSplit")
  fun update(transaction: ProcessTransaction, body: SplitInstance.Builder.() -> Unit): ProcessNodeInstance {
    val origHandle = handle
    return ExtBuilder(this).apply(body).build().apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  private fun successorInstances(transaction: ProcessTransaction): Sequence<ProcessNodeInstance> {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    return node.successors
        .asSequence()
        .mapNotNull { instance.getChild(transaction, it.id)?.withPermission() }
  }

  private fun isActiveOrCompleted(it: ProcessNodeInstance): Boolean {
    return when (it.state) {
      NodeInstanceState.Started,
      NodeInstanceState.Acknowledged,
      NodeInstanceState.Complete -> true
      else -> false
    }
  }

  override fun <U> startTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): SplitInstance {
    return update(transaction){ state=NodeInstanceState.Started }.updateState(transaction, messageService)
  }

  internal fun <U> updateState(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): SplitInstance {
    var processInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    val successorNodes = node.successors.map { node.ownerModel.getNode(it).mustExist(it) }
    var viableNodes: Int = 0

    var canStartMore = successorInstances(transaction).filter { isActiveOrCompleted(it) }.count() < node.max

    for (successor in successorNodes) {
      if (canStartMore) {
        if (successor is Join<*, *>) {
          throw IllegalStateException("Splits cannot be immediately followed by joins")
        }

        var successorInstance = successor.createOrReuseInstance(transaction, processInstance, this.handle)
        if (successorInstance.state==NodeInstanceState.Pending && successorInstance.condition(transaction)) { // only if it can be executed, otherwise just drop it.
          val nodeInstanceHandle = transaction.writableEngineData.nodeInstances.put(successorInstance)
          // Load the updated version with updated handle
          successorInstance = transaction.readableEngineData.nodeInstance(nodeInstanceHandle).withPermission()

          processInstance = processInstance.update(transaction) {
            children.add(Handles.handle(nodeInstanceHandle))
          }
          transaction.commit()

          successorInstance = successorInstance.provideTask(transaction, messageService)

          canStartMore = successorInstances(transaction).filter { isActiveOrCompleted(it) }.count() < node.max
        }
      }
      // in any case
      if (state == NodeInstanceState.Complete || ! state.isFinal) {
        viableNodes+=1
      }

    }
    if (viableNodes<node.min) { // No way to succeed, try to cancel anything that is not in a final state
      successorInstances(transaction)
          .filter { ! it.state.isFinal }
          .forEach { it.tryCancelTask(transaction) }

      return update(transaction) { state = NodeInstanceState.Failed }
    }

    if (successorInstances(transaction).filter { isActiveOrCompleted(it) }.count()>=node.max) {
      // We have a maximum amount of successors
      successorInstances(transaction)
          .filter { !isActiveOrCompleted(it) }
          .forEach { it.cancelAndSkip(transaction) }

      return update(transaction) { state = NodeInstanceState.Complete }
    }

    return this // the state is whatever it should be
  }
}