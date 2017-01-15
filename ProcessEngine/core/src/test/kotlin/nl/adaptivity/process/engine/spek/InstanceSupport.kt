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

package nl.adaptivity.process.engine.spek

import net.devrieze.util.writer
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.Gettable
import nl.adaptivity.xml.XmlStreaming
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Created by pdvrieze on 15/01/17.
 */
interface InstanceSupport {
  val transaction: StubProcessTransaction

  fun ProcessInstance.allChildren(): Sequence<ProcessNodeInstance> {
    return childNodes.asSequence().flatMap { val child = it.withPermission()
      when (child) {
        is CompositeInstance -> sequenceOf(child) +
                                transaction.readableEngineData.instance(child.hChildInstance).withPermission().allChildren()
        else                 -> sequenceOf(child)
      }
    }
  }

  // TODO rename to findChildNode
  fun ProcessInstance.findChild(id: String) = allChildren().firstOrNull { it.node.id==id }
  fun ProcessInstance.findChild(id: Identified) = findChild(id.id)

  val ProcessInstance.nodeInstances: Gettable<Identified, ProcessNodeInstanceDelegate> get() = object: Gettable<Identified,ProcessNodeInstanceDelegate> {
    operator override fun get(key: Identified): ProcessNodeInstanceDelegate {
      return ProcessNodeInstanceDelegate(this@InstanceSupport, this@nodeInstances.getHandle(), key)
    }
  }


  fun ProcessInstance.trace(filter: (ProcessNodeInstance)->Boolean): Sequence<TraceElement> {
    return allChildren()
      .map { it.withPermission() }
      .filter(filter)
      .sortedBy { getHandle().handleValue }
      .map { TraceElement(it.node.id, SINGLEINSTANCE) }
  }

  val ProcessInstance.trace: Trace get(){
    return trace {true}
      .toList()
      .toTypedArray<TraceElement>()
  }

  fun ProcessInstance.toDebugString():String {
    return buildString {
      append("process(")
      append(processModel.rootModel.getName())
      name?.let {
        append(", instance: ")
        append(it)
      }
      append(", allnodes: [")
      this@toDebugString.allChildren().joinTo(this) { val inst = it.withPermission()
        "${inst.node.id}:${inst.state}"
      }
      appendln("])\n\nModel:")
      XmlStreaming.newWriter(this.writer()).use { processModel.rootModel.serialize(it) }
      appendln("\n")
    }
  }

  fun ProcessInstance.tracePossible(trace:Trace): Boolean {
    val currentTrace = this.trace { it.state.isFinal }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in currentTrace }
    val lastPos = seen.lastIndexOf(true)
    return seen.slice(0 .. lastPos).all { it }
  }


  fun ProcessInstance.assertTracePossible(trace: Trace) {
    val childIds = this.trace { it.state.isFinal }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in childIds }
    val lastPos = seen.lastIndexOf(true)
    Assertions.assertTrue(seen.slice(
      0..lastPos).all { it }) { "All trace elements should be in the trace: [${trace.mapIndexed { i, s -> "$s=${seen[i]}" }.joinToString()}]" }
    assertTrue(childIds.all { this.findChild(it)?.state == IProcessNodeInstance.NodeInstanceState.Skipped || it in trace }) { "All child nodes should be in the full trace or skipped (child nodes: [${childIds.joinToString()}])" }
  }


  fun ProcessInstance.assertFinished() {
    Assertions.assertTrue(this.finished.isEmpty(), { "The list of finished nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertFinished(vararg nodeInstances: ProcessNodeInstance) {
    assertFinished(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertFinished(vararg nodeIds: String) {
    val finished = allChildren()
      .filter { it.state.isFinal && it.node !is EndNode<*, *> }
      .mapNotNull { nodeInstance ->
        Assertions.assertTrue(nodeInstance.state.isFinal,
                              { "The node instance state should be final (but is ${nodeInstance.state})" })
        Assertions.assertTrue(nodeInstance.node !is EndNode<*, *>, { "Completed nodes should not be endnodes" })
        if (nodeInstance.state != IProcessNodeInstance.NodeInstanceState.Skipped) nodeInstance.node.id else null
      }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), finished, { "The list of finished nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertComplete() {
    Assertions.assertTrue(this.completedEndnodes.isEmpty(), { "The list of completed nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertComplete(vararg nodeInstances: ProcessNodeInstance) {
    assertComplete(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertComplete(vararg nodeIds: String) {
    val complete = allChildren()
      .filter { it.state.isFinal && it.node is EndNode<*, *> }
      .mapNotNull { nodeInstance ->
        Assertions.assertTrue(nodeInstance.state.isFinal,
                              { "The node instance state should be final (but is ${nodeInstance.state})" })
        Assertions.assertTrue(nodeInstance.node is EndNode<*, *>, "Completion nodes should be EndNodes")
        if (nodeInstance.state == IProcessNodeInstance.NodeInstanceState.Skipped) null else nodeInstance.node.id
      }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), complete, { "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${complete.joinToString()}], ${this.toDebugString()})" })
  }

  fun ProcessInstance.assertActive() {
    Assertions.assertTrue(this.active.isEmpty(), { "The list of active nodes is not empty (Expected: [], found: [${finished.joinToString {transaction.readableEngineData.nodeInstance(it).withPermission().toString()}}])" })
  }

  fun ProcessInstance.assertActive(vararg nodeInstances: ProcessNodeInstance) {
    assertActive(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertActive(vararg nodeIds: String) {
    val active = allChildren()
      .filter { !it.state.isFinal }
      .mapNotNull { nodeInstance ->
        Assertions.assertTrue(nodeInstance.state.isActive,
                              { "The node instance state should be active (but is ${nodeInstance.state})" })
        Assertions.assertFalse(nodeInstance.state.isFinal,
                               { "The node instance state should not be final (but is ${nodeInstance.state})" })
        nodeInstance.node.id
      }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), active, { "The list of active nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${active.joinToString()}])" })
  }


}

