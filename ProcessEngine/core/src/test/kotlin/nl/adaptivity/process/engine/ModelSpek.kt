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

package nl.adaptivity.process.engine

import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.Complete
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.spek.*
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.Getter
import nl.adaptivity.util.getter
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.lifecycle.CachingMode
import org.jetbrains.spek.api.lifecycle.LifecycleAware
import org.jetbrains.spek.api.lifecycle.LifecycleListener
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.dsl.SubjectDsl
import org.jetbrains.spek.subject.dsl.SubjectProviderDsl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Node
import java.security.Principal
import java.util.*

data class ModelData(val engineData: ()->EngineTestData,
                     val model: ExecutableProcessModel,
                     val valid: List<Trace>,
                     val invalid: List<Trace>) {
  internal constructor(model:Model, valid:List<Trace>, invalid:List<Trace>):this({ EngineTestData.defaultEngine() }, model.rootModel, valid, invalid)
}

class ModelSpekSubjectContext(private val subjectProviderDsl: SubjectProviderDsl<ModelData>) {
  internal fun ModelData(model:Model, valid:List<Trace>, invalid:List<Trace>):ModelData {
    val engineData = { EngineTestData.defaultEngine() }
    return ModelData(engineData, model.rootModel, valid, invalid)
  }

}

val SubjectDsl<EngineTestData>.engine get() = subject.engine

private var subjectCreated = false

abstract class ModelSpek(modelData: ModelData, custom:(CustomDsl.()->Unit)?=null) : SubjectSpek<EngineTestData>(
  {
    val model = modelData.model
    val valid = modelData.valid
    val invalid = modelData.invalid
    val principal by getter { model.owner }

    subject(CachingMode.GROUP, {
      if (subjectCreated) {
        System.err.println("Recreating the subject")
      } else {
        subjectCreated = true
      }
      modelData.engineData()
    })

    describe("the model") {
      this.it("should be valid") {
        model.builder().validate()
      }

      if (custom!=null) {
        this.group("Custom checks") {
          CustomDsl(subject(), model, valid, invalid, this).custom()
        }
      }

      for (validTrace in valid) {
        this.group("For valid trace [${validTrace.joinToString()}]") {

          val transaction = lazy { subject.engine.startTransaction() }
          val hinstance = startProcess(transaction, model, principal, "${model.name} instance for [${validTrace.joinToString()}]")
          val processInstanceF = getter { transaction().readableEngineData.instance(hinstance()).withPermission() }

          testTraceStarting(processInstanceF)

          val queue = StateQueue()
          for (pos in validTrace.indices) {
            val traceElement = validTrace[pos]
            val previous = queue.solidify()
            // TODO we want to properly support the trace
            val nodeInstanceF = getter {
              processInstanceF().let { processInstance: ProcessInstance ->
                processInstance.allChildren(transaction()).lastOrNull { traceElement.fits(it) }
                ?: throw NoSuchElementException("No node instance for $traceElement found in ${processInstance.toDebugString(transaction())}}")
              }
            }

            queue.add { transaction().finishNodeInstance(hinstance(), traceElement) }

            given("trace element #$pos -> ${traceElement}") {
              beforeGroup { previous();  }
              val node = model.findNode(traceElement) ?: throw AssertionError("No node could be find for trace element $traceElement")
              when (node) {
                is StartNode<*, *> -> testStartNode(nodeInstanceF, traceElement)
                is EndNode<*, *>   -> testEndNode(transaction, nodeInstanceF, traceElement)
                is Join<*, *>      -> testJoin(transaction, nodeInstanceF, traceElement)
                is Split<*, *>     -> testSplit(transaction, nodeInstanceF, traceElement)
                is Activity<*, *>  -> when {
                  node.childModel == null -> testActivity(transaction, nodeInstanceF, processInstanceF, traceElement)
                  else                    -> testComposite(transaction, nodeInstanceF, traceElement)
                }
                else               -> test("$traceElement should not be in a final state") {
                  val nodeInstance = nodeInstanceF()
                  Assertions.assertFalse(
                    nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node.javaClass.simpleName} is in final state ${nodeInstance.state}" }
                }
              } // when

            } // trace element group


          } // test everything
          testTraceCompletion(model, queue.solidify(), transaction, processInstanceF, validTrace)



        } // valid group

      } // for valid traces
      for(invalidTrace in invalid) {
        testInvalidTrace(model, principal, this, invalidTrace)
      }

    }

  }) {
}

internal fun SubjectProviderDsl<EngineTestData>.testInvalidTrace(
  model: ExecutableProcessModel,
  principal: Principal,
  specBody: SpecBody,
  invalidTrace: Trace) {
  val transaction = lazy { subject.engine.startTransaction() }
  val hinstance = startProcess(transaction, model, principal,
                               "${model.name} instance for [${invalidTrace.joinToString()}]}")
  val processInstanceF = getter { transaction().readableEngineData.instance(hinstance()).withPermission() }
  specBody.given("invalid trace ${invalidTrace.joinToString(prefix = "[", postfix = "]")}") {
    test("Executing the trace should fail") {
      var success = false
      try {
        val instanceSupport = object : InstanceSupport {
          override val transaction: StubProcessTransaction get() = transaction()
        }
        instanceSupport.testTraceExceptionThrowing(processInstanceF(), invalidTrace)
      } catch (e: ProcessTestingException) {
        success = true
      }
      if (!success) kfail(
        "The invalid trace ${invalidTrace.joinToString(prefix = "[", postfix = "]")} could be executed")
    }
  }
}

private fun SubjectProviderDsl<EngineTestData>.startProcess(transaction: Lazy<StubProcessTransaction>,
                                                            model: ExecutableProcessModel,
                                                            owner: Principal,
                                                            name: String,
                                                            payload: Node? = null): Lazy<HProcessInstance> {
  return lazy {
    val transaction = transaction()
    val hmodel = subject.engine.addProcessModel(transaction, model, owner)
    engine.startProcess(transaction, owner, hmodel, name, UUID.randomUUID(), payload)
  }
}

private fun SpecBody.testTraceStarting(processInstanceF: Getter<ProcessInstance>) {
  group("After starting") {
    test("Only start nodes should be finished") {
      val processInstance = processInstanceF()
      val onlyStartNodesCompleted = processInstance.finishedNodes.all { it.state == IProcessNodeInstance.NodeInstanceState.Skipped || it.node is StartNode<*, *> }
      Assertions.assertTrue(onlyStartNodesCompleted) {
        processInstance.finishedNodes
          .filter { it.state != IProcessNodeInstance.NodeInstanceState.Skipped && it.node !is StartNode<*, *> }
          .joinToString(prefix = "Nodes [",
                        postfix = "] are not startnodes, but already finished.")
      }
    }
  }
}

private fun SpecBody.testTraceCompletion(model: ExecutableProcessModel,
                                         queue: StateQueue.SolidQueue,
                                         transaction: Lazy<StubProcessTransaction>,
                                         processInstanceF: Getter<ProcessInstance>,
                                         validTrace: Trace) {
  group("The trace should be finished correctly") {
    beforeGroup { queue.invoke() }
    test("The trace should be valid") {
      processInstanceF().assertTracePossible(transaction(), validTrace)
    }
    test("All non-endnodes are finished") {
      val expectedFinishedNodes = validTrace.asSequence()
        .map { model.findNode(it)!! }
        .filterNot { it is EndNode<*, *> }.map { it.id }.toList().toTypedArray()
      processInstanceF().assertFinished(transaction(), *expectedFinishedNodes)
    }
    test("No nodes are active") {
      processInstanceF().assertActive(transaction())
    }
    test("All endNodes in the trace are complete, skipped, cancelled or failed") {
      val transaction = transaction()
      val processInstance = processInstanceF()
      val expectedCompletedNodes = validTrace.asSequence()
        .map { model.findNode(it)!! }
        .filterIsInstance(EndNode::class.java)
        .filter { endNode ->
          val nodeInstance = processInstance.allChildren(transaction).firstOrNull { it.node.id == endNode.id } ?: kfail(
            "Nodeinstance ${endNode.identifier} does not exist, the instance is ${processInstance.toDebugString(
              transaction)}")
          nodeInstance.state !in listOf(IProcessNodeInstance.NodeInstanceState.Skipped,
                                        IProcessNodeInstance.NodeInstanceState.SkippedCancel,
                                        IProcessNodeInstance.NodeInstanceState.SkippedFail)
        }
        .map { it.id!! }
        .toList().toTypedArray()
      processInstanceF().assertComplete(transaction(), *expectedCompletedNodes)
    }
  }
}

private fun SpecBody.testStartNode(nodeInstanceF: Getter<ProcessNodeInstance>, traceElement: TraceElement) {
  test("Start node $traceElement should be finished") {
    testAssertNodeFinished(nodeInstanceF, traceElement)
  }
}

private fun SpecBody.testComposite(transaction: Lazy<StubProcessTransaction>,
                                   nodeInstanceF: Getter<ProcessNodeInstance>,
                                   traceElement: TraceElement) {
  test("A child instance should have been created for $traceElement") {
    Assertions.assertTrue((nodeInstanceF() as CompositeInstance).hChildInstance.valid) { "No child instance was recorded" }
  }
  test("The child instance was finished for $traceElement") {
    val childInstance = transaction().readableEngineData.instance(
      (nodeInstanceF() as CompositeInstance).hChildInstance).withPermission()
    Assertions.assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
  }
}

private fun SpecBody.testActivity(transaction: Lazy<StubProcessTransaction>,
                                  nodeInstanceF: Getter<ProcessNodeInstance>,
                                  processInstanceF: Getter<ProcessInstance>,
                                  traceElement: TraceElement) {
  test("$traceElement should not be in a final state") {
    val nodeInstance = nodeInstanceF()
    Assertions.assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in final state ${nodeInstance.state}" }
  }
  test("node instance ${traceElement} should be committed after starting") {
    val processInstance = processInstanceF()
    nodeInstanceF().startTask(transaction().writableEngineData, processInstance)
    val nodeInstance = nodeInstanceF()
    Assertions.assertTrue(nodeInstance.state.isCommitted) {
      "The instance state was ${processInstance.toDebugString(transaction)}"
    }
    Assertions.assertEquals(IProcessNodeInstance.NodeInstanceState.Started, nodeInstance.state)
  }
  test("the node instance ${traceElement} should be final after finishing") {
    run {
      val pi = processInstanceF()
      val ni = nodeInstanceF()
      assertEquals(pi.getHandle(), ni.hProcessInstance) { "Node, owner mismatch. Node owner handle: ${ni.hProcessInstance}, instance(${pi.getHandle()}): ${pi.toDebugString(transaction)}" }
    }
    processInstanceF().finishTask(transaction().writableEngineData, nodeInstanceF(), traceElement.resultPayload)
    assertEquals(IProcessNodeInstance.NodeInstanceState.Complete, nodeInstanceF().state)
  }
}

private fun SpecBody.testSplit(transaction: Lazy<StubProcessTransaction>, nodeInstanceF: Getter<ProcessNodeInstance>, traceElement: TraceElement) {
  test("Split $traceElement should already be finished") {
    val nodeInstance = nodeInstanceF()
    Assertions.assertEquals(Complete, nodeInstance.state) {
      val processInstance = transaction().readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
      "Node $traceElement should be finished. The current nodes are: ${processInstance.toDebugString(transaction)}"
    }
  }
}

private fun SpecBody.testJoin(transaction: Lazy<StubProcessTransaction>, nodeInstanceF: Getter<ProcessNodeInstance>,
                              traceElement: TraceElement) {
  test("Join $traceElement should already be finished") {
    val nodeInstance = nodeInstanceF()
    Assertions.assertEquals(Complete, nodeInstance.state) {
      val processInstance = transaction().readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
      "There are still active predecessors: ${processInstance.getActivePredecessorsFor(
        transaction().readableEngineData, nodeInstanceF() as JoinInstance)}"
    }
  }
}

private fun SpecBody.testEndNode(transaction: Lazy<StubProcessTransaction>,
                                 nodeInstanceF: Getter<ProcessNodeInstance>,
                                 traceElement: TraceElement) {
  testAssertNodeFinished(nodeInstanceF, traceElement)
  test("$traceElement should be part of the completion nodes") {
    val nodeInstance = nodeInstanceF()
    val parentInstance = transaction().readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
    Assertions.assertTrue(
      parentInstance.completedNodeInstances.any { it.withPermission().node.id == traceElement.id }) {
      "Instance is: ${parentInstance.toDebugString(transaction)}"
    }
  }
}

private fun SpecBody.testAssertNodeFinished(nodeInstanceF: Getter<ProcessNodeInstance>, traceElement: TraceElement) {
  test("$traceElement should be finished") {
    Assertions.assertEquals(Complete, nodeInstanceF().state)
  }
}

fun StubProcessTransaction.finishNodeInstance(hProcessInstance: HProcessInstance, traceElement: TraceElement) {
  val instance = readableEngineData.instance(hProcessInstance).withPermission()
  val nodeInstance = instance.getNodeInstance(traceElement) ?: throw ProcessTestingException("No node instance for the trace elemnt $traceElement could be found in instance: ${instance.toDebugString(this)}")
  if (nodeInstance.state != Complete) {
    System.err.println("Re-finishing node ${nodeInstance.node.id} $nodeInstance for instance $instance")
    instance.finishTask(writableEngineData, nodeInstance, traceElement.resultPayload)
  }
  assert(nodeInstance.state == Complete)
}

private class StateQueue {
  private val operations = mutableListOf<()->Unit>()
  private val operationState = mutableListOf<Boolean>()

  fun add(operation:()->Unit) {
    operations.add(operation)
    operationState.add(false)
  }

  fun solidify() = SolidQueue(operations.size-1)


  inner class SolidQueue(val position:Int) {
    operator fun invoke() = (0 until position).map { idx ->
      if (! operationState[idx]) {
        operations[idx]()
        operationState[idx]=true
      }
    }
  }
}

class CustomDsl(private val _subject: LifecycleAware<EngineTestData>,
                         val model: ExecutableProcessModel,
                         val valid:List<Trace>,
                         val invalid:List<Trace>,
                         val specBody: SpecBody) : SpecBody by specBody, SubjectDsl<EngineTestData> {
  override fun registerListener(listener: LifecycleListener) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override val subject: EngineTestData
    get() = _subject()

  override fun subject(): LifecycleAware<EngineTestData> {
    return _subject
  }
}

private inline operator fun <T> Lazy<T>.invoke() = this.value