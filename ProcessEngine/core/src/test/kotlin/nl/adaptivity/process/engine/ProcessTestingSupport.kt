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

package nl.adaptivity.process.engine

import net.devrieze.util.ComparableHandle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.Gettable
import org.jetbrains.spek.api.dsl.Dsl
import org.jetbrains.spek.api.dsl.Pending
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.w3c.dom.Node
import java.net.URI
import java.security.Principal
import javax.xml.namespace.QName
import kotlin.reflect.KProperty

@Retention(AnnotationRetention.SOURCE)
@DslMarker
annotation class ProcessTestingDslMarker


@ProcessTestingDslMarker
class EngineTestingDsl(val delegate: Dsl) {
  val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
  val stubMessageService = StubMessageService(localEndpoint)
  val stubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
      return StubProcessTransaction(engineData)
    }
  }
  val principal = SimplePrincipal("pdvrieze")

  val processEngine = ProcessEngine.newTestInstance(
      stubMessageService,
      stubTransactionFactory,
      TestProcessEngine.cacheModels<Any>(MemProcessModelMap(), 3),
      TestProcessEngine.cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
      TestProcessEngine.cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>(TestProcessEngine.PNI_SET_HANDLE), 2), true)

  fun afterEachTest(callback: () -> Unit) = delegate.afterEachTest(callback)

  fun beforeEachTest(callback: () -> Unit) = delegate.beforeEachTest(callback)

  fun test(description: String, pending: Pending = Pending.No, body: () -> Unit)
      = delegate.test(description, pending, body)

  inline fun group(description: String, pending: Pending = Pending.No, lazy: Boolean = false, crossinline body: EngineTestingDsl.() -> Unit) {
    delegate.group(description, pending, lazy) { EngineTestingDsl(this).body() }
  }


  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun describe(description: String, noinline body: EngineTestingDsl.() -> Unit) {
    group("describe $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun context(description: String, noinline body: EngineTestingDsl.() -> Unit) {
    group("context $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun given(description: String, noinline body: EngineTestingDsl.() -> Unit) {
    group("given $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun on(description: String, noinline body: EngineTestingDsl.() -> Unit) {
    group("on $description", lazy = true, body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xdescribe(description: String, reason: String? = null, noinline body: EngineTestingDsl.() -> Unit) {
    group("describe $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xcontext(description: String, reason: String? = null, noinline body: EngineTestingDsl.() -> Unit) {
    group("context $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xgiven(description: String, reason: String? = null, noinline body: EngineTestingDsl.() -> Unit) {
    group("given $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a pending [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xon(description: String, reason: String? = null, noinline body: EngineTestingDsl.() -> Unit = {}) {
    group("on $description", Pending.Yes(reason), lazy = true, body = body)
  }

  fun it(description: String, body: () -> Unit) {
    test("it $description", body = body)
  }

}

fun Dsl.givenEngine(body: EngineTestingDsl.()->Unit) {
  EngineTestingDsl(this).body()
}

/**
 * An extended dsl for testing processes without having to carry around large amounts of local variables.
 */
@ProcessTestingDslMarker
class ProcessTestingDsl(val delegate:Dsl, val transaction:StubProcessTransaction, val instanceHandle: HProcessInstance) {

  @ProcessTestingDslMarker
  inline fun <R> group(description: String, pending: Pending = Pending.No, lazy: Boolean = false, noinline body: ProcessTestingDsl.() -> R):R {
    var result: R? = null
    delegate.group(description, pending, lazy) { result = ProcessTestingDsl(this, transaction, instanceHandle).body() }
    return result!!
  }

  inline fun test(description: String, pending: Pending = Pending.No, noinline body: () -> Unit) {
    delegate.test(description, pending, body)
  }

  inline fun it(description: String, noinline body: () -> Unit) {
    test("it $description", body = body)
  }

  inline fun beforeEachTest(noinline callback: () -> Unit) {
    delegate.beforeEachTest(callback)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun describe(description: String, noinline body: ProcessTestingDsl.() -> Unit) {
    group("describe $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun context(description: String, noinline body: ProcessTestingDsl.() -> Unit) {
    group("context $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun given(description: String, noinline body: ProcessTestingDsl.() -> Unit) {
    group("given $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun on(description: String, noinline body: ProcessTestingDsl.() -> Unit) {
    group("on $description", lazy = true, body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xdescribe(description: String, reason: String? = null, noinline body: ProcessTestingDsl.() -> Unit) {
    group("describe $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xcontext(description: String, reason: String? = null, noinline body: ProcessTestingDsl.() -> Unit) {
    group("context $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xgiven(description: String, reason: String? = null, noinline body: ProcessTestingDsl.() -> Unit) {
    group("given $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a pending [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  inline fun xon(description: String, reason: String? = null, noinline body: ProcessTestingDsl.() -> Unit = {}) {
    group("on $description", Pending.Yes(reason), lazy = true, body = body)
  }


  val instance: ProcessInstance get() {
    return transaction.readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission()
  }

  fun ProcessNodeInstance.take(): ProcessNodeInstance {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    return this.update(transaction.writableEngineData, instance) { state= NodeInstanceState.Taken }.node
  }

  fun ProcessNodeInstance.start(): ProcessNodeInstance {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    return startTask(transaction.writableEngineData, instance).node
  }

  fun ProcessNodeInstance.finish(payload: Node? = null): ProcessNodeInstance {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    return instance.finishTask(transaction.writableEngineData, this, payload).node
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
        if (nodeInstance.state != NodeInstanceState.Skipped) nodeInstance.node.id else null
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
        if (nodeInstance.state == NodeInstanceState.Skipped) null else nodeInstance.node.id
      }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), complete, { "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${complete.joinToString()}], ${instance.toDebugString()})" })
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

  fun ProcessInstance.allChildren(): Sequence<ProcessNodeInstance> {
    return childNodes.asSequence().flatMap { val child = it.withPermission()
      when (child) {
        is CompositeInstance -> sequenceOf(child) +
                                transaction.readableEngineData.instance(child.hChildInstance).withPermission().allChildren()
        else                 -> sequenceOf(child)
      }
    }
  }

  internal fun ProcessInstance.toDebugString():String {
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
      append("])")
    }
  }

  inner class ProcessNodeInstanceDelegate(val instanceHandle: ComparableHandle<out SecureObject<ProcessInstance>>, val nodeId: Identifier) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ProcessNodeInstance {
      val idString = nodeId.id
      return instance.allChildren().firstOrNull { it.node.id == idString }
          ?: kfail("The process node instance for node id $nodeId could not be found. Instance is: ${instance.toDebugString()}")
    }
  }

  val ProcessInstance.nodeInstance get() = object: Gettable<String, ProcessNodeInstanceDelegate> {
    operator override fun get(key:String): ProcessNodeInstanceDelegate {
      return ProcessNodeInstanceDelegate(getHandle(), Identifier(key))
    }
  }

  fun tracePossible(trace:Trace): Boolean {
    val childIds = instance.childNodes.asSequence().map { it.withPermission() }.filter { it.state.isFinal }.map { it.node.id }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in childIds }
    val lastPos = seen.lastIndexOf(true)
    return seen.slice(0 .. lastPos).all { it }
  }

  fun assertTracePossible(trace: Trace) {
    val childIds = instance.allChildren().map { it.withPermission() }.filter { it.state.isFinal }.map { it.node.id }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in childIds }
    val lastPos = seen.lastIndexOf(true)
    assertTrue(seen.slice(0 .. lastPos).all { it }) { "All trace elements should be in the trace: [${trace.mapIndexed { i, s -> "$s=${seen[i]}" }.joinToString()}]"}
    assertTrue(childIds.all { instance.getChild(it)?.withPermission()?.state == NodeInstanceState.Skipped || it in trace }) { "All child nodes should be in the full trace or skipped (child nodes: [${childIds.joinToString()}])" }
  }


}

fun trace(vararg trace:String):Trace {
  return trace
}

fun findNode(model: ExecutableProcessModel, nodeId: String): ExecutableProcessNode? {
  return model.getModelNodes().firstOrNull { it.id==nodeId }?:
    model.childModels.asSequence().flatMap { it.getModelNodes().asSequence() }.firstOrNull{ it.id==nodeId }
}

@ProcessTestingDslMarker
fun EngineTestingDsl.testTraces(engine:ProcessEngine<StubProcessTransaction>, model:ExecutableProcessModel, owner: Principal, valid: List<Trace>, invalid:List<Trace>) {

  fun addStartedNodeContext(dsl: ProcessTestingDsl, trace: nl.adaptivity.process.engine.Trace, i: kotlin.Int):ProcessTestingDsl {
    val nodeId = trace[i].splitToSequence(':').first()
    val nodeInstance by with(dsl) { instance.nodeInstance[nodeId] }
    val node = findNode(model, nodeId) ?: throw AssertionError("No node with id ${nodeId} was defined in the tested model")
    when(node) {
      is StartNode<*,*> -> {
        dsl.test("$nodeId should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
      }
      is EndNode<*,*> -> {
        dsl.test("$nodeId should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
        dsl.test("$nodeId should be part of the completion nodes") {
          val parentInstance = dsl.transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
          assertTrue(parentInstance.completedNodeInstances.any { it.withPermission().node.id==nodeId }) { "Instance is: ${with(dsl) { parentInstance.toDebugString()} }" }
        }
      }
      is Join<*, *> -> dsl.test("Join $nodeId should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "There are still active predecessors: ${dsl.instance.getActivePredecessorsFor(dsl.transaction.readableEngineData, nodeInstance as JoinInstance)}" }
      }
      is Split<*,*> -> dsl.test("Split $nodeId should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "Node $nodeId should be finished. The current nodes are: ${with(dsl) {instance.toDebugString()}}"}
      }
      is Activity<*,*> -> {
        if (node.childModel==null) {
          dsl.test("$nodeId should not be in a final state") {
            assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node?.javaClass?.simpleName} is in final state ${nodeInstance.state}" }
          }
        }
      }
      else -> {
        dsl.test("$nodeId should not be in a final state") {
          assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node?.javaClass?.simpleName} is in final state ${nodeInstance.state}" }
        }
      }
    }
    if (node is Activity<*,*> && node.childModel!=null) {
      dsl.test("A child instance should have been created") {
        assertTrue((nodeInstance as CompositeInstance).hChildInstance.valid) {"No child instance was recorded"}
      }
      dsl.test("The child instance was finished") {
        val childInstance = dsl.transaction.readableEngineData.instance((nodeInstance as CompositeInstance).hChildInstance).withPermission()
        assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
      }
    }
    dsl.test("The trace should still be possible") {
      dsl.assertTracePossible(trace)
    }
    return when(node) {
      is EndNode<*,*>,
      is StartNode<*,*>,
      is Join<*,*>,
      is Split<*,*> -> if (i+1 <trace.size) {
        addStartedNodeContext(dsl, trace, i + 1)
      } else {
        dsl
      }
      else -> {
        dsl.test("$nodeId should be committed after starting") {
          with(dsl) { nodeInstance.start() }
          assertTrue(nodeInstance.state.isCommitted) { "The instance state was ${with(dsl){ instance.toDebugString()}}" }
          assertEquals(NodeInstanceState.Started, nodeInstance.state)
        }
        dsl.group("After Finishing ${nodeId}") {
          beforeEachTest {
            if (nodeInstance.state!=NodeInstanceState.Complete) {
              nodeInstance.finish()
            }
          }
          if (i + 1 < trace.size) {
            addStartedNodeContext(this, trace, i + 1)
          } else {
            this
          }
        }
      }
    }
  }

  for(validTrace in valid) {
    givenProcess(model, description = validTrace.joinToString(prefix = "For trace: [", postfix = "]")) {
      test("Only start nodes should be finished") {
        assertTrue(instance.finishedNodes.all { it.state==NodeInstanceState.Skipped || it.node is StartNode<*, *> }) { "Nodes [${instance.finishedNodes.filter { it.state!=NodeInstanceState.Skipped && it.node !is StartNode<*,*> }}] are not startnodes, but already finished" }
      }

      val startPos = 0
      addStartedNodeContext(this, validTrace, startPos).apply {
        group("The trace should be finished correctly") {
          test("The trace should be valid") {
            assertTracePossible(validTrace)
          }
          test("All non-endnodes are finished") {
            val expectedFinishedNodes = validTrace.asSequence()
              .map { findNode(model, it)!! }
              .filterNot { it is EndNode<*, *> }.map { it.id }.toList().toTypedArray()
            instance.assertFinished(*expectedFinishedNodes)
          }
          test("No nodes are active") {
            instance.assertActive()
          }
          test("All endNodes in the trace are complete, skipped, cancelled or failed") {
            val expectedCompletedNodes = validTrace.asSequence()
                .map { findNode(model,it)!! }
                .filterIsInstance(EndNode::class.java)
                .filter { endNode ->
                  val nodeInstance = instance.allChildren().firstOrNull { it.node.id== endNode.id} ?: kfail("Nodeinstance ${endNode.identifier} does not exist, the instance is ${instance.toDebugString()}")
                  nodeInstance.state !in listOf(NodeInstanceState.Skipped, NodeInstanceState.SkippedCancel, NodeInstanceState.SkippedFail)
                }
                .map { it.id!! }
                .toList().toTypedArray()
            instance.assertComplete(*expectedCompletedNodes)
          }
        }
      }

    }
  }
  for(trace in invalid) {
    givenProcess(model, description = "For invalid trace: ${trace.joinToString(prefix = "[", postfix = "]")}") {
      test("Executing the trace should fail") {
        var success = false
        try {
          assertTracePossible(trace)
          for (nodeId in trace) {
            val nodeInstance by instance.nodeInstance[nodeId]
            assertNotNull(nodeInstance, "The node instance should exist")
            if (nodeInstance.state != NodeInstanceState.Complete) {
              if (! (nodeInstance.node is Join<*,*> || nodeInstance.node is Split<*,*>)) {
                instance.finishTask(transaction.writableEngineData, nodeInstance, null)
              }
            }
            assertEquals(NodeInstanceState.Complete, nodeInstance.state)
          }
        } catch (e: ProcessException) {
          success = true
        } catch (e: AssertionError) {
          success = true
        }
        if (! success) kfail("The invalid trace ${trace.joinToString(prefix = "[", postfix = "]")} could be executed")
      }
    }
  }
}

val ProcessInstance.trace:Trace get(){
  return childNodes
      .sortedBy { it.withPermission().getHandle().handleValue }
      .map { it.withPermission().node.id }
      .toTypedArray()
}

typealias Trace = Array<out String>

const val ANYINSTANCE = -1
const val SINGLEINSTANCE = -2
const val LASTINSTANCE = -3

private fun String.toTraceNo(): Int {
  return when (this) {
    "*" -> ANYINSTANCE
    "#" -> LASTINSTANCE
    else -> this.toInt()
  }
}

class TraceElement(val nodeId: String, val intanceNo:Int, val outputs:List<ProcessData> = emptyList()) {
  private constructor(stringdescrs: Iterator<String>) : this(stringdescrs.next(), if (stringdescrs.hasNext()) stringdescrs.next().toInt() else SINGLEINSTANCE)
  constructor(stringdescr: String): this(stringdescr.splitToSequence(':').iterator())
}

@ProcessTestingDslMarker
fun EngineTestingDsl.givenProcess(processModel: ExecutableProcessModel, description: String="Given a process instance", principal: Principal = this.principal, payload: Node? = null, body: ProcessTestingDsl.() -> Unit) {
  val transaction = processEngine.startTransaction()
  val instance = with(transaction) {
    processEngine.testProcess(processModel, principal, payload)
  }

  group(description, body = { ProcessTestingDsl(this.delegate, transaction, instance.instanceHandle).body() })

}
/*

@ProcessTestingDslMarker
fun Dsl.givenProcess(engine: ProcessEngine<StubProcessTransaction>, processModel: ExecutableProcessModel, principal: Principal, payload: Node? = null, description: String="Given a process instance", body: ProcessTestingDsl.() -> Unit) {
  val transaction = engine.startTransaction()
  val instance = with(transaction) {
    engine.testProcess(processModel, principal, payload)
  }

  group(description, body = { ProcessTestingDsl(this, transaction, instance.instanceHandle).body() })
}
*/

fun kfail(message:String):Nothing {
  fail(message)
  throw UnsupportedOperationException("This code should not be reachable")
}