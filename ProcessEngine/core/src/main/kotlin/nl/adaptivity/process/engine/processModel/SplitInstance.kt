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
import net.devrieze.util.collection.replaceByNotNull
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.engine.ConditionResult
import nl.adaptivity.process.processModel.engine.ExecutableSplit
import org.w3c.dom.Node
import java.security.Principal
import java.util.logging.Level

/**
 * Specialisation of process node instance for splits
 */
class SplitInstance : ProcessNodeInstance<SplitInstance> {

  interface Builder : ProcessNodeInstance.Builder<ExecutableSplit,SplitInstance> {
    override fun build(): SplitInstance
    var predecessor: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>?
      get() = predecessors.firstOrNull()
      set(value) = predecessors.replaceByNotNull(value)

    override fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.provideTask(engineData, this)
    }

    override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.takeTask(this)
    }

    override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
      state = NodeInstanceState.Started
      return updateState(engineData)
    }

    override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      super.doFinishTask(engineData, resultPayload)
    }
  }

  class ExtBuilder(private val instance:SplitInstance, processInstanceBuilder: ProcessInstance.Builder) : ProcessNodeInstance.ExtBuilder<ExecutableSplit, SplitInstance>(instance, processInstanceBuilder), Builder {
    override var node: ExecutableSplit by overlay { instance.node }
    override fun build() = if (changed) SplitInstance(this) else base
  }

  class BaseBuilder(
    node: ExecutableSplit,
    predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
    processInstanceBuilder: ProcessInstance.Builder,
    owner: Principal,
    entryNo: Int,
    handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
    state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableSplit, SplitInstance>(node, listOf(predecessor), processInstanceBuilder, owner, entryNo,
                                                              handle, state), Builder {
    override fun build() = SplitInstance(this)
  }

  override val node: ExecutableSplit
    get() = super.node as ExecutableSplit


  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<SecureObject<SplitInstance>>
      = super.getHandle() as ComparableHandle<SecureObject<SplitInstance>>

  constructor(node: ExecutableSplit,
              predecessor: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
              hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
              owner: Principal,
              handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList(),
              entryNo: Int) :
      super(node, listOf(predecessor), hProcessInstance, owner, entryNo, handle, state, results) {
  }

  constructor(builder: SplitInstance.Builder): this(builder.node, builder.predecessor?: throw NullPointerException("Missing predecessor node instance"), builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results, builder.entryNo)

  override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder {
    return ExtBuilder(this, processInstanceBuilder)
  }
//
//  override fun update(writableEngineData: MutableProcessEngineDataAccess,
//                      body: ProcessNodeInstance.Builder<*, SplitInstance>.() -> Unit): ProcessInstance.PNIPair<SplitInstance> {
//    val instance = writableEngineData.instance(hProcessInstance).withPermission()
//    val instanceBuilder = instance.builder()
//    val origHandle = getHandle()
//    val builder = builder(instanceBuilder).apply(body)
//    if (builder.changed) {
//      if (origHandle.valid && getHandle().valid) {
//        val nodeFuture = instanceBuilder.storeChild(builder)
//        return ProcessInstance.PNIPair(instanceBuilder.build(writableEngineData), nodeFuture.get() as SplitInstance)
//      }
//    }
//    return ProcessInstance.PNIPair(instance, this)
//  }

//  @JvmName("updateSplit")
  fun update(writableEngineData: MutableProcessEngineDataAccess, instance: ProcessInstance, body: Builder.() -> Unit): PNIPair<SplitInstance> {
    val instanceBuilder = instance.builder()
    val origHandle = getHandle()
    val builder = builder(instanceBuilder).apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        val nodeFuture = instanceBuilder.storeChild(builder)
        return PNIPair(instanceBuilder.build(writableEngineData), nodeFuture.get() as SplitInstance)
      }
    }
    return PNIPair(instance, this)
  }

  private fun successorInstances(engineData: ProcessEngineDataAccess): Sequence<ProcessNodeInstance<*>> {
    val instance = engineData.instance(hProcessInstance).withPermission()
    return node.successors
        .asSequence()
        .mapNotNull { instance.getChild(it.id, entryNo)?.withPermission() }
        .filter { it.entryNo == entryNo }
  }

  @Deprecated("Use builder")
  override fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<SplitInstance> {
    return update(engineData){ state= NodeInstanceState.Started }.let {
      it.node.updateState(engineData, it.instance)
    }
  }

  override fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node?): PNIPair<SplitInstance> {
    val committedSuccessors = successorInstances(engineData).filter { it.state.isCommitted }
    if (committedSuccessors.count()<node.min) {
      throw ProcessException("A split can only be finished once the minimum amount of children is committed")
    }
    return super.finishTask(engineData, processInstance, resultPayload)
  }

  internal fun updateState(engineData: MutableProcessEngineDataAccess, _processInstance: ProcessInstance): PNIPair<SplitInstance> {

    fun canStartMore() = successorInstances(engineData).filter { isActiveOrCompleted(it) }.count() < node.max

    if (state.isFinal) { return PNIPair(_processInstance, this) }
    // XXX really needs fixing
    var processInstance = _processInstance
    val successorNodes = node.successors.map { node.ownerModel.getNode(it).mustExist(it) }
    var viableNodes: Int = 0

    for (successorNode in successorNodes) {
      if (canStartMore()) {
        if (successorNode is Join<*, *>) {
          throw IllegalStateException("Splits cannot be immediately followed by joins")
        }

        var conditionResult = ConditionResult.MAYBE
        val pniPair = processInstance.updateWithNode(engineData) {
          val successorBuilder = successorNode.createOrReuseInstance(engineData, this, this@SplitInstance, entryNo)
          if (successorBuilder.state == NodeInstanceState.Pending) {
            // temporaryly build a node to evaluate the condition against, but don't register it.
            conditionResult = successorBuilder.build().run { condition(engineData) }
          }
          successorBuilder
        }.let { pniPair ->
          when (conditionResult) {
            ConditionResult.TRUE  -> { // only if it can be executed, otherwise just drop it.
              pniPair.node.provideTask(engineData, pniPair.instance)
            }
            ConditionResult.NEVER -> {
              pniPair.instance.skipTask(engineData, pniPair.node)
            }
            else                  -> pniPair
          }
        }

        processInstance = pniPair.instance

      }
      // in any case
      if (state == NodeInstanceState.Complete || ! state.isFinal) {
        viableNodes+=1
      }

    }
    if (viableNodes<node.min) { // No way to succeed, try to cancel anything that is not in a final state
      processInstance = successorInstances(engineData)
          .filter { ! it.state.isFinal }
          .fold(processInstance) { processInstance, it -> it.tryCancelTask(engineData, processInstance).instance }

      return update(engineData) { state = NodeInstanceState.Failed }
    }

    if (! canStartMore()) {
      // We have a maximum amount of successors
      processInstance = successorInstances(engineData)
          .filter { !isActiveOrCompleted(it) }
          .fold(processInstance) { processInstance, successor -> successor.cancelAndSkip(engineData, processInstance).instance }

      return update(engineData) { state = NodeInstanceState.Complete }
    }

    return PNIPair(processInstance, this) // the state is whatever it should be
  }

  companion object {

    private fun isActiveOrCompleted(it: IProcessNodeInstance): Boolean {
      return when (it.state) {
        NodeInstanceState.Started,
        NodeInstanceState.Complete,
        NodeInstanceState.Skipped,
        NodeInstanceState.Failed,
        NodeInstanceState.Acknowledged -> true
        else                           -> false
      }
    }

    /**
     * Update the state of the split.
     *
     * @return Whether or not the split is complete.
     */
    private fun SplitInstance.Builder.updateState(engineData: MutableProcessEngineDataAccess):Boolean {

      fun canStartMore() = processInstanceBuilder.allChildren { isActiveOrCompleted(it) && handle in it.predecessors }.count() < node.max

      if (state.isFinal) return true

      // XXX really needs fixing
      val successorNodes = node.successors.map { node.ownerModel.getNode(it).mustExist(it) }
      var viableNodes: Int = 0

      for (successorNode in successorNodes) {
        if (canStartMore()) {
          if (successorNode is Join<*, *>) {
            throw IllegalStateException("Splits cannot be immediately followed by joins")
          }

          val successorBuilder = successorNode.createOrReuseInstance(engineData, processInstanceBuilder, this, entryNo)
          processInstanceBuilder.storeChild(successorBuilder)
          if (successorBuilder.state == NodeInstanceState.Pending) {
            // temporaryly build a node to evaluate the condition against, but don't register it.
            val conditionResult = successorBuilder.build().run { condition(engineData) }
            when (conditionResult) {
              ConditionResult.TRUE  -> { // only if it can be executed, otherwise just drop it.
                successorBuilder.provideTask(engineData)
              }
              ConditionResult.NEVER -> {
                successorBuilder.skipTask(engineData, NodeInstanceState.Skipped)
              }
            }
          }

          // in any case
          if (successorBuilder.state == NodeInstanceState.Complete || ! successorBuilder.state.isFinal) {
            viableNodes+=1
          }
        }


      }
      if (viableNodes<node.min) { // No way to succeed, try to cancel anything that is not in a final state
        for(successor in processInstanceBuilder.allChildren { handle in it.predecessors && ! it.state.isFinal }) {
          try {
            successor.builder(processInstanceBuilder).cancel(engineData)
          } catch (e: IllegalArgumentException) { DefaultProcessNodeInstance.logger.log(Level.WARNING, "Task could not be cancelled", e) } // mainly ignore
        }
        state = NodeInstanceState.Failed
        return true // complete, but invalid
      }

      if (! canStartMore()) {
        processInstanceBuilder
          .allChildren { !isActiveOrCompleted(it) && handle in it.predecessors }
          .forEach { it.builder(processInstanceBuilder).cancelAndSkip(engineData) }
        state = NodeInstanceState.Complete
        return true
      }
      return false
    }

  }
}