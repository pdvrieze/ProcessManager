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
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.spek.InstanceSupport
import nl.adaptivity.process.engine.spek.ProcessNodeActions
import nl.adaptivity.process.engine.spek.SafeNodeActions
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
  inner class InstanceTestBody(delegate: EngineTestBody): DelegateTestBody(delegate), InstanceSupport, SafeNodeActions {
    override val transaction get() = this@ProcessTestingDsl.transaction
    val instance: ProcessInstance get() = this@ProcessTestingDsl.instance
  }

  inner class InstanceFixtureBody: ProcessNodeActions {
    override val transaction get() = this@ProcessTestingDsl.transaction

  }

  val instance: ProcessInstance get() {
    return transaction.readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission()
  }

}

fun ExecutableProcessModel.findNode(nodeIdentified: Identified): ExecutableProcessNode? {
  val nodeId = nodeIdentified.id
  return getModelNodes().firstOrNull { it.id==nodeId } ?:
         childModels.asSequence().flatMap { it.getModelNodes().asSequence() }.firstOrNull{ it.id==nodeId }
}

@Suppress("NOTHING_TO_INLINE")
@ProcessTestingDslMarker
inline fun EngineSpecBody.testTraces(model:RootProcessModel<*,*>, valid: List<Trace>, invalid:List<Trace>) {
  return testTraces(ExecutableProcessModel.from(model.rootModel), valid, invalid)
}

@ProcessTestingDslMarker
fun EngineSpecBody.testTraces(model:ExecutableProcessModel, valid: List<Trace>, invalid:List<Trace>) {

  fun InstanceSpecBody.addStartedNodeContext(trace: Trace,
                                                                                            i: Int):InstanceSpecBody {
    val traceElement = trace[i]
    val nodeInstance by with(this) { instance.nodeInstances[traceElement] }
    val node = model.findNode(
      traceElement) ?: throw AssertionError("No node with id $traceElement was defined in the tested model\n\n${XmlStreaming.toString(model).prependIndent(">  ")}\n")
    when(node) {
      is StartNode<*,*> -> {
        test("$traceElement should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
      }
      is EndNode<*,*> -> {
        test("$traceElement should be finished") {
          assertEquals(NodeInstanceState.Complete, nodeInstance.state)
        }
        test("$traceElement should be part of the completion nodes") {
          val parentInstance = transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
          assertTrue(parentInstance.completedNodeInstances.any { it.withPermission().node.id==traceElement.id }) { "Instance is: ${parentInstance.toDebugString()}" }
        }
      }
      is Join<*, *> -> test("Join $traceElement should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "There are still active predecessors: ${instance.getActivePredecessorsFor(
          transaction.readableEngineData, nodeInstance as JoinInstance)}" }
      }
      is Split<*,*> -> test("Split $traceElement should already be finished") {
        assertEquals(NodeInstanceState.Complete, nodeInstance.state) { "Node $traceElement should be finished. The current nodes are: ${instance.toDebugString()}"}
      }
      is Activity<*,*> -> {
        if (node.childModel==null) {
          test("$traceElement should not be in a final state") {
            assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node?.javaClass?.simpleName} is in final state ${nodeInstance.state}" }
          }
        }
      }
      else -> {
        test("$traceElement should not be in a final state") {
          assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node?.javaClass?.simpleName} is in final state ${nodeInstance.state}" }
        }
      }
    }
    if (node is Activity<*,*> && node.childModel!=null) {
      test("A child instance should have been created for $traceElement") {
        assertTrue((nodeInstance as CompositeInstance).hChildInstance.valid) {"No child instance was recorded"}
      }
      test("The child instance was finished for $traceElement") {
        val childInstance = transaction.readableEngineData.instance((nodeInstance as CompositeInstance).hChildInstance).withPermission()
        assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
      }
    }
    test("The trace should still be possible") {
      instance.assertTracePossible(trace)
    }
    return when(node) {
      is EndNode<*,*>,
      is StartNode<*,*>,
      is Join<*,*>,
      is Split<*,*> -> if (i+1 <trace.size) {
        addStartedNodeContext(trace, i + 1)
      } else {
        this
      }
      else -> {
        test("node instance ${traceElement} should be committed after starting") {
          nodeInstance.start()
          assertTrue(nodeInstance.state.isCommitted) { "The instance state was ${instance.toDebugString()}" }
          assertEquals(NodeInstanceState.Started, nodeInstance.state)
        }
        rgroup("After finishing ${traceElement}") {
          beforeEachTest {
            if (nodeInstance.state != NodeInstanceState.Complete) {
              nodeInstance.finish()
            }
          }
          if (i + 1 < trace.size) {
            addStartedNodeContext(trace, i + 1)
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
        assertTrue(instance.finishedNodes.all { it.state== NodeInstanceState.Skipped || it.node is StartNode<*, *> }) { "Nodes [${instance.finishedNodes.filter { it.state!= NodeInstanceState.Skipped && it.node !is StartNode<*,*> }}] are not startnodes, but already finished" }
      }

      val startPos = 0
      this.addStartedNodeContext(validTrace, startPos).apply {
        group("The trace should be finished correctly") {
          test("The trace should be valid") {
            instance.assertTracePossible(validTrace)
          }
          test("All non-endnodes are finished") {
            val expectedFinishedNodes = validTrace.asSequence()
              .map { model.findNode(it)!! }
              .filterNot { it is EndNode<*, *> }.map { it.id }.toList().toTypedArray()
            instance.assertFinished(*expectedFinishedNodes)
          }
          test("No nodes are active") {
            instance.assertActive()
          }
          test("All endNodes in the trace are complete, skipped, cancelled or failed") {
            val expectedCompletedNodes = validTrace.asSequence()
                .map { model.findNode(it)!! }
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
  testTraceExceptionThrowing(instance, trace)
}

fun InstanceSupport.testTraceExceptionThrowing(_instance: ProcessInstance,
                                                        trace: Trace) {
  try {
    _instance.assertTracePossible(trace)
  } catch (e: AssertionError) {
    throw ProcessTestingException(e)
  }
  var instance = _instance
  for (traceElement in trace) {

    run {
      val nodeInstance = traceElement.getNodeInstance(instance) ?: throw ProcessTestingException("The node instance (${traceElement}) should exist")

      if (nodeInstance.state != NodeInstanceState.Complete) {
        if (!(nodeInstance.node is Join<*, *> || nodeInstance.node is Split<*, *>)) {
          if (nodeInstance is CompositeInstance) {
            val childInstance = transaction.readableEngineData.instance(nodeInstance.hChildInstance).withPermission()
            if (childInstance.state != ProcessInstance.State.FINISHED && nodeInstance.state != NodeInstanceState.Complete) {
              try {
                transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
                  .finishTask(transaction.writableEngineData, nodeInstance, null)
              } catch (e: ProcessException) {
                if (e.message?.startsWith(
                  "A Composite task cannot be finished until its child process is. The child state is:") ?: false) {
                  throw ProcessTestingException("The composite instance cannot be finished yet")
                } else throw e
              }
            }
          } else if (nodeInstance.state.isFinal && nodeInstance.state != NodeInstanceState.Complete) {
            try {
              transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
                .finishTask(transaction.writableEngineData, nodeInstance, null)
            } catch (e: ProcessException) {
              assertNotNull(e.message)
              assertTrue(e.message!!.startsWith("instance ${nodeInstance.node.id}") &&
                         e.message!!.endsWith(" cannot be finished as it is already in a final state."))
            }
            throw ProcessTestingException("The node is final but not complete (failed, skipped)")
          }
          instance = instance.finishTask(transaction.writableEngineData, nodeInstance, null).instance
        }
      }
    }
    run {
      val nodeInstance = traceElement.getNodeInstance(instance) ?: throw ProcessTestingException("The node instance should exist")
      if (nodeInstance.state != NodeInstanceState.Complete) throw ProcessTestingException(
        "State of node ${nodeInstance} not complete but ${nodeInstance.state}")
    }
  }
}

internal class ProcessTestingException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
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