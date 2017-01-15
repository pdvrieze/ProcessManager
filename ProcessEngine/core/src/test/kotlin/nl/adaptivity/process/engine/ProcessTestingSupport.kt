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

import nl.adaptivity.process.engine.EngineTesting.*
import nl.adaptivity.process.engine.ProcessTestingDsl.InstanceSpecBody
import nl.adaptivity.process.engine.ProcessTestingDsl.InstanceTestBody
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.spek.InstanceSupport
import nl.adaptivity.process.engine.spek.ProcessNodeActions
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.spek.*
import nl.adaptivity.xml.XmlStreaming
import org.jetbrains.spek.api.dsl.ActionBody
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.TestBody
import org.junit.jupiter.api.Assertions.*
import org.w3c.dom.Node
import java.security.Principal

@Retention(AnnotationRetention.SOURCE)
@DslMarker
annotation class ProcessTestingDslMarker


class EngineTesting {

  @ProcessTestingDslMarker
  inner class EngineSpecBody(delegate:SpecBody): DelegateSpecBody<EngineSpecBody, EngineActionBody, EngineTestBody, Any>(delegate) {
    val stubMessageService = this@EngineTesting.testData.messageService
    val processEngine = this@EngineTesting.testData.engine

    override fun actionBody(base: ActionBody) = EngineActionBody(base)

    override fun specBody(base: SpecBody) = EngineSpecBody(base)

    override fun testBody(base: TestBody) = EngineTestBody(base)

    override fun otherBody() = Any()


    fun givenProcess(processModel: ExecutableProcessModel, description: String="Given a process instance", principal: Principal = EngineTestData.principal, payload: Node? = null, body: InstanceSpecBody.() -> Unit) {
      val transaction = processEngine.startTransaction()
      val instance = with(transaction) {
        processEngine.testProcess(processModel, principal, payload)
      }
      val ptd :ProcessTestingDsl = ProcessTestingDsl(this@EngineTesting, transaction, instance.instanceHandle)

      group(description, extbody = { ptd.InstanceSpecBody(this).body() })

    }

  }

  @ProcessTestingDslMarker
  inner class EngineActionBody(delegate:ActionBody): DelegateActionBody<EngineTestBody>(delegate) {
    override fun testBody(base: TestBody) = EngineTestBody(base)
  }

  @ProcessTestingDslMarker
  inner class EngineTestBody(delegate:TestBody): DelegateTestBody(delegate) {

  }

  val testData = EngineTestData.defaultEngine()

  @Deprecated("Use testData", ReplaceWith("testData.engine"))
  val processEngine get() = testData.engine

//
//  val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
//  val stubMessageService = StubMessageService(localEndpoint)
//  val stubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
//    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
//      return StubProcessTransaction(engineData)
//    }
//  }

}

inline fun SpecBody.givenEngine(body: EngineSpecBody.()->Unit) {
  EngineTesting().EngineSpecBody(this).body()
}


/**
 * An extended dsl for testing processes without having to carry around large amounts of local variables.
 */
class ProcessTestingDsl(val engineTesting:EngineTesting, val transaction:StubProcessTransaction, val instanceHandle: HProcessInstance) {

  @ProcessTestingDslMarker
  inner class InstanceSpecBody(delegate:EngineSpecBody): DelegateSpecBody<InstanceSpecBody, InstanceActionBody, InstanceTestBody, InstanceFixtureBody>(delegate.delegate), InstanceSupport {
    override val transaction get() = this@ProcessTestingDsl.transaction

    val instance: ProcessInstance get() = this@ProcessTestingDsl.instance

    override fun testBody(base: TestBody) = InstanceTestBody(base as? EngineTestBody ?: engineTesting.EngineTestBody(base))

    override fun actionBody(base: ActionBody) = InstanceActionBody(base as? EngineActionBody ?: engineTesting.EngineActionBody(base))

    override fun specBody(base: SpecBody) = InstanceSpecBody(base as? EngineSpecBody ?: engineTesting.EngineSpecBody(base))

    override fun otherBody() = InstanceFixtureBody()
  }

  @ProcessTestingDslMarker
  inner class InstanceActionBody(delegate: EngineActionBody): DelegateActionBody<InstanceTestBody>(delegate.delegate), ProcessNodeActions {
    override fun testBody(base: TestBody) = InstanceTestBody(base as? EngineTestBody ?: engineTesting.EngineTestBody(base))
    override val transaction get() = this@ProcessTestingDsl.transaction
  }

  @ProcessTestingDslMarker
  inner class InstanceTestBody(delegate: EngineTestBody): DelegateTestBody(delegate), InstanceSupport {
    override val transaction get() = this@ProcessTestingDsl.transaction
    val instance: ProcessInstance get() = this@ProcessTestingDsl.instance
  }

  inner class InstanceFixtureBody: ProcessNodeActions {
    override val transaction get() = this@ProcessTestingDsl.transaction

  }

  val instance: ProcessInstance get() {
    return transaction.readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission()
  }

/*

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

  fun ProcessInstance.findChild(id: Identified) = findChild(id.id)
  fun ProcessInstance.findChild(id: String) = allChildren().firstOrNull { it.node.id==id }

  fun ProcessInstance.allChildren(): Sequence<ProcessNodeInstance> {
    return childNodes.asSequence().flatMap { val child = it.withPermission()
      when (child) {
        is CompositeInstance -> sequenceOf(child) +
                                transaction.readableEngineData.instance(child.hChildInstance).withPermission().allChildren()
        else                 -> sequenceOf(child)
      }
    }
  }

  fun ProcessInstance.trace(filter: (ProcessNodeInstance)->Boolean): Sequence<TraceElement> {
    return allChildren()
      .map { it.withPermission() }
      .filter(filter)
      .sortedBy { getHandle().handleValue }
      .map { TraceElement(it.node.id, SINGLEINSTANCE) }
  }

  val ProcessInstance.trace:Trace get(){
    return trace {true}
      .toList()
      .toTypedArray<TraceElement>()
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
      appendln("])\n\nModel:")
      XmlStreaming.newWriter(this.writer()).use { processModel.rootModel.serialize(it) }
      appendln("\n")
    }
  }

  inner class ProcessNodeInstanceDelegate(val instanceHandle: ComparableHandle<out SecureObject<ProcessInstance>>, val nodeId: Identified) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ProcessNodeInstance {
      val idString = nodeId.id
      return instance.allChildren().firstOrNull { it.node.id == idString }
          ?: kfail("The process node instance for node id $nodeId could not be found. Instance is: ${instance.toDebugString()}")
    }
  }

  val ProcessInstance.nodeInstance get() = object: Gettable<Identified, ProcessNodeInstanceDelegate> {
    operator override fun get(key: Identified): ProcessNodeInstanceDelegate {
      return ProcessNodeInstanceDelegate(getHandle(), key)
    }
  }

  fun tracePossible(trace:Trace): Boolean {
    val currentTrace = instance.trace { it.state.isFinal }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in currentTrace }
    val lastPos = seen.lastIndexOf(true)
    return seen.slice(0 .. lastPos).all { it }
  }

  fun assertTracePossible(trace: Trace) {
    val childIds = instance.trace { it.state.isFinal }.toSet()
    val seen = Array<Boolean>(trace.size) { idx -> trace[idx] in childIds }
    val lastPos = seen.lastIndexOf(true)
    assertTrue(seen.slice(0 .. lastPos).all { it }) { "All trace elements should be in the trace: [${trace.mapIndexed { i, s -> "$s=${seen[i]}" }.joinToString()}]"}
    assertTrue(childIds.all { instance.findChild(it)?.state == NodeInstanceState.Skipped || it in trace }) { "All child nodes should be in the full trace or skipped (child nodes: [${childIds.joinToString()}])" }
  }

*/

}

fun findNode(model: ExecutableProcessModel, nodeIdentified: Identified): ExecutableProcessNode? {
  val nodeId = nodeIdentified.id
  return model.getModelNodes().firstOrNull { it.id==nodeId }?:
    model.childModels.asSequence().flatMap { it.getModelNodes().asSequence() }.firstOrNull{ it.id==nodeId }
}

@Suppress("NOTHING_TO_INLINE")
@ProcessTestingDslMarker
inline fun EngineSpecBody.testTraces(engine:ProcessEngine<StubProcessTransaction>, model:RootProcessModel<*,*>, owner: Principal = EngineTestData.principal, valid: List<Trace>, invalid:List<Trace>) {
  return testTraces(engine, ExecutableProcessModel.from(model.rootModel), owner, valid, invalid)
}

@ProcessTestingDslMarker
fun EngineSpecBody.testTraces(engine:ProcessEngine<StubProcessTransaction>, model:ExecutableProcessModel, owner: Principal = EngineTestData.principal, valid: List<Trace>, invalid:List<Trace>) {

  fun addStartedNodeContext(dsl: InstanceSpecBody, trace: Trace, i: kotlin.Int):InstanceSpecBody {
    val traceElement = trace[i]
    val nodeInstance by with(dsl) { instance.nodeInstances[traceElement] }
    val node = findNode(model, traceElement) ?: throw AssertionError("No node with id $traceElement was defined in the tested model\n\n${XmlStreaming.toString(model).prependIndent(">  ")}\n")
    when(node) {
      is StartNode<*,*> -> {
        dsl.test("$traceElement should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
      }
      is EndNode<*,*> -> {
        dsl.test("$traceElement should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
        dsl.test("$traceElement should be part of the completion nodes") {
          val parentInstance = dsl.transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
          assertTrue(parentInstance.completedNodeInstances.any { it.withPermission().node.id==traceElement.id }) { "Instance is: ${with(dsl) { parentInstance.toDebugString()} }" }
        }
      }
      is Join<*, *> -> dsl.test("Join $traceElement should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "There are still active predecessors: ${dsl.instance.getActivePredecessorsFor(dsl.transaction.readableEngineData, nodeInstance as JoinInstance)}" }
      }
      is Split<*,*> -> dsl.test("Split $traceElement should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "Node $traceElement should be finished. The current nodes are: ${with(dsl) {instance.toDebugString()}}"}
      }
      is Activity<*,*> -> {
        if (node.childModel==null) {
          dsl.test("$traceElement should not be in a final state") {
            assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node?.javaClass?.simpleName} is in final state ${nodeInstance.state}" }
          }
        }
      }
      else -> {
        dsl.test("$traceElement should not be in a final state") {
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
      instance.assertTracePossible(trace)
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
        dsl.on("starting $traceElement") {
          nodeInstance.start()
        }
        dsl.it("should be committed") {
//          with(dsl) { nodeInstance.start() }
          assertTrue(nodeInstance.state.isCommitted) { "The instance state was ${with(dsl){ instance.toDebugString()}}" }
          assertEquals(NodeInstanceState.Started, nodeInstance.state)
        }
        dsl.rgroup("After Finishing ${traceElement}") {
          beforeGroup {
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
            instance.assertTracePossible(validTrace)
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
  mutableSetOf<List<TraceElement>>().let { seen ->
    valid.flatMap { trace ->
      (1 until trace.size)
        .map { trace.slice(0..it) }
        .filter { it !in seen }
    }.forEach {
      val trace: Trace = it.toTypedArray()
      givenProcess(model, "Given subtrace [${trace.joinToString()}]") {
        it("should not throw an exception") {
          try {
            testTraceExceptionThrowing(trace)
          } catch (e: Exception) {
            throw AssertionError("the subtrace checking failed with model \n\n${XmlStreaming.toString(model).prependIndent("> ")}", e)
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
          testTraceExceptionThrowing(trace)
        } catch (e: ProcessTestingException) {
          success = true
        }
        if (! success) kfail("The invalid trace ${trace.joinToString(prefix = "[", postfix = "]")} could be executed")
      }
    }
  }
}

private fun InstanceTestBody.testTraceExceptionThrowing(trace: Trace) {
  try {
    instance.assertTracePossible(trace)
  } catch (e: AssertionError) {
    throw ProcessTestingException(e)
  }
  for (nodeId in trace) {
    run {
      val nodeInstance = instance.findChild(nodeId) ?: throw ProcessTestingException("The node instance should exist")

      if (nodeInstance.state != NodeInstanceState.Complete) {
        if (!(nodeInstance.node is Join<*, *> || nodeInstance.node is Split<*, *>)) {
          if (nodeInstance.state.isFinal && nodeInstance.state != NodeInstanceState.Complete) {
            try {
              instance.finishTask(transaction.writableEngineData, nodeInstance, null)
            } catch (e: ProcessException) {
              assertNotNull(e.message)
              assertTrue(e.message!!.startsWith("instance ${nodeInstance.node.id}") &&
                         e.message!!.endsWith(" cannot be finished as it is already in a final state."))
            }
            throw ProcessTestingException("The node is final but not complete (failed, skipped)")
          }
          val i = transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
          i.finishTask(transaction.writableEngineData, nodeInstance, null)
        }
      }
    }
    run {
      val nodeInstance = instance.findChild(nodeId) ?: throw ProcessTestingException("The node instance should exist")
      if (nodeInstance.state != NodeInstanceState.Complete) throw ProcessTestingException("State of node ${nodeInstance} not complete but ${nodeInstance.state}")
    }
  }
}

private class ProcessTestingException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
  constructor(cause: Throwable): this(cause.message, cause)
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