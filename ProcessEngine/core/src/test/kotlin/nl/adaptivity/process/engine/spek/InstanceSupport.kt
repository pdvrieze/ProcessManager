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

import net.devrieze.util.security.SecureObject
import net.devrieze.util.writer
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.Gettable
import nl.adaptivity.util.Getter
import nl.adaptivity.xml.XmlStreaming
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Created by pdvrieze on 15/01/17.
 */
interface InstanceSupport {
  val transaction: StubProcessTransaction

  fun ProcessInstance.allChildren(): Sequence<ProcessNodeInstance<*>> {
    return allChildren(this@InstanceSupport.transaction)
  }

  val ProcessInstance.nodeInstances: Gettable<Identified, ProcessNodeInstanceDelegate> get() = object: Gettable<Identified,ProcessNodeInstanceDelegate> {
    operator override fun get(key: Identified): ProcessNodeInstanceDelegate {
      return ProcessNodeInstanceDelegate(this@InstanceSupport, this@nodeInstances.getHandle(), key)
    }
  }


  fun ProcessInstance.trace(filter: (ProcessNodeInstance<*>)->Boolean) = trace(transaction, filter)

  val ProcessInstance.trace: Trace get() = trace(transaction)

  fun ProcessInstance.toDebugString():String {
    return toDebugString(this@InstanceSupport.transaction)
  }

  fun ProcessInstance.tracePossible(trace:Trace): Boolean {
    val currentTrace = this.trace { it.state.isFinal }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in currentTrace }
    val lastPos = seen.lastIndexOf(true)
    return seen.slice(0 .. lastPos).all { it }
  }


  fun ProcessInstance.assertTracePossible(trace: Trace) {
    assertTracePossible(transaction, trace)
  }


  fun ProcessInstance.assertFinished(vararg nodeInstances: DefaultProcessNodeInstance) {
    val transaction = transaction
    assertFinished(transaction, *Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertFinished(vararg nodeIds: String) = assertFinished(transaction, *nodeIds)

  fun ProcessInstance.assertComplete() {
    Assertions.assertTrue(this.completedEndnodes.isEmpty(), { "The list of completed nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertComplete(vararg nodeInstances: DefaultProcessNodeInstance) {
    assertComplete(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertComplete(vararg nodeIds: String) {
    val complete = allChildren()
      .filter { it.state.isFinal && it.node is EndNode<*, *> }
      .mapNotNull { nodeInstance ->
        Assertions.assertTrue(nodeInstance.state.isFinal,
                              { "The node instance state should be final (but is ${nodeInstance.state})" })
        Assertions.assertTrue(nodeInstance.node is EndNode<*, *>, "Completion nodes should be EndNodes")
        if (nodeInstance.state.isSkipped) null else nodeInstance.node.id
      }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), complete, { "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${complete.joinToString()}], ${this.toDebugString()})" })
  }

  fun ProcessInstance.assertActive() {
    Assertions.assertTrue(this.active.isEmpty(), { "The list of active nodes is not empty (Expected: [], found: [${finished.joinToString {transaction.readableEngineData.nodeInstance(it).withPermission().toString()}}])" })
  }

  fun ProcessInstance.assertActive(vararg nodeInstances: DefaultProcessNodeInstance) {
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


fun ProcessInstance.allChildren(transaction: StubProcessTransaction): Sequence<ProcessNodeInstance<*>> {
  return childNodes.asSequence().flatMap {
    val child = it.withPermission()
    when (child) {
      is CompositeInstance -> sequenceOf(child) +
                              transaction.readableEngineData.instance(
                                child.hChildInstance).withPermission().allChildren(transaction)
      else                 -> sequenceOf(child)
    }
  }.filter { it.state != NodeInstanceState.SkippedInvalidated }
}

fun ProcessInstance.toDebugString(transaction: Getter<StubProcessTransaction>) = toDebugString(transaction())

fun ProcessInstance.toDebugString(transaction: StubProcessTransaction): String {
  return buildString {
    append("process(")
    append(processModel.rootModel.getName())
    name?.let {
      append(", instance: ")
      append(it)
    }
    append('[').append(getHandle()).append(']')
    append(", allnodes: [")
    this@toDebugString.allChildren(transaction).joinTo(this) {
      val inst = it.withPermission()
      "${inst.node.id}[${inst.entryNo}]:${inst.state}"
    }
    appendln("])\n\nModel:")
    XmlStreaming.newWriter(this.writer()).use { processModel.rootModel.serialize(it) }
    appendln("\n")
  }
}

fun ProcessInstance.findChild(transaction: StubProcessTransaction, id: String) = allChildren(transaction).firstOrNull { it.node.id==id }
fun ProcessInstance.findChild(transaction: StubProcessTransaction, id: Identified) = findChild(transaction, id.id)

fun ProcessInstance.trace(transaction: StubProcessTransaction,
                          filter: (ProcessNodeInstance<*>) -> Boolean): Sequence<TraceElement> {
  return allChildren(transaction)
    .map { it.withPermission() }
    .filter(filter)
    .sortedBy { getHandle().handleValue }
    .map { TraceElement(it.node.id, it.entryNo) }
}

fun ProcessInstance.trace(transaction: StubProcessTransaction): Array<TraceElement> {
  return trace(transaction) { true }
    .toList()
    .toTypedArray<TraceElement>()
}

fun ProcessInstance.assertTracePossible(transaction: StubProcessTransaction,
                                        trace: Trace) {
  val nonSeenChildNodes = this.childNodes.asSequence()
    .map(SecureObject<ProcessNodeInstance<*>>::withPermission)
    .filter { it.state.isFinal && ! it.state.isSkipped }
    .toMutableSet()

  var seenNonFinal = false
  for(traceElementPos in trace.indices) {
    val traceElement = trace[traceElementPos]
    val nodeInstance = traceElement.getNodeInstance(transaction, this)?.takeIf { it.state.isFinal }
    if (nodeInstance != null) {
      if(seenNonFinal) {
        kfail("Found gap in the trace ${trace}#$traceElementPos before node: $nodeInstance")
      }
      nonSeenChildNodes.remove(nodeInstance)
    } else {
      seenNonFinal = true
    }
  }
  if (nonSeenChildNodes.isNotEmpty()) {
    kfail("All actual child nodes should be in the full trace or skipped. Nodes that were not seen: [${nonSeenChildNodes.joinToString()}]" )
  }
}

fun ProcessInstance.assertFinished() {
  Assertions.assertTrue(this.finished.isEmpty(), { "The list of finished nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
}

fun ProcessInstance.assertFinished(transaction: StubProcessTransaction, vararg nodeInstances: DefaultProcessNodeInstance) {
  assertFinished(transaction, *Array(nodeInstances.size, { nodeInstances[it].node.id }))
}

fun ProcessInstance.assertFinished(transaction: StubProcessTransaction, vararg nodeIds: String) {
  val finished = allChildren(transaction)
    .filter { it.state.isFinal && it.node !is EndNode<*, *> }
    .mapNotNull { nodeInstance ->
      assertTrue(nodeInstance.state.isFinal,
                 { "The node instance state should be final (but is ${nodeInstance.state})" })
      assertTrue(nodeInstance.node !is EndNode<*, *>, { "Completed nodes should not be endnodes" })
      if (nodeInstance.state.isSkipped) null else nodeInstance.node.id
    }.sorted().toList()
  Assertions.assertEquals(nodeIds.sorted(), finished,
                          { "The list of finished nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${finished.joinToString()}])" })
}


fun ProcessInstance.assertComplete() {
  Assertions.assertTrue(this.completedEndnodes.isEmpty(), { "The list of completed nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
}

fun ProcessInstance.assertComplete(transaction: StubProcessTransaction, vararg nodeInstances: DefaultProcessNodeInstance) {
  assertComplete(transaction, *Array(nodeInstances.size, { nodeInstances[it].node.id }))
}

fun  ProcessInstance.assertComplete(transaction: StubProcessTransaction, vararg nodeIds: String) {
  val complete = allChildren(transaction)
    .filter { it.state.isFinal && it.node is EndNode<*, *> }
    .mapNotNull { nodeInstance ->
      Assertions.assertTrue(nodeInstance.state.isFinal,
                            { "The node instance state should be final (but is ${nodeInstance.state})" })
      Assertions.assertTrue(nodeInstance.node is EndNode<*, *>, "Completion nodes should be EndNodes")
      if (nodeInstance.state.isSkipped) null else nodeInstance.node.id
    }.sorted().toList()
  Assertions.assertEquals(nodeIds.sorted(), complete, { "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${complete.joinToString()}], ${this.toDebugString(transaction)})" })
}

fun ProcessInstance.assertActive(transaction: StubProcessTransaction) {
  Assertions.assertTrue(this.active.isEmpty(), { "The list of active nodes is not empty (Expected: [], found: [${active.joinToString {transaction.readableEngineData.nodeInstance(it).withPermission().toString()}}])" })
}

fun ProcessInstance.assertActive(transaction: StubProcessTransaction, vararg nodeInstances: DefaultProcessNodeInstance) {
  assertActive(transaction, *Array(nodeInstances.size, { nodeInstances[it].node.id }))
}

fun  ProcessInstance.assertActive(transaction: StubProcessTransaction, vararg nodeIds: String) {
  val active = allChildren(transaction)
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
