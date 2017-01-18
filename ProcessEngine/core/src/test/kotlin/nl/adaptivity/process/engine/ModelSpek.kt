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
import nl.adaptivity.process.engine.spek.allChildren
import nl.adaptivity.process.engine.spek.toDebugString
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.Getter
import nl.adaptivity.util.getter
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.lifecycle.CachingMode
import org.jetbrains.spek.api.lifecycle.LifecycleAware
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.dsl.SubjectDsl
import org.jetbrains.spek.subject.dsl.SubjectProviderDsl
import org.junit.jupiter.api.Assertions

data class ModelData(val engineData: LifecycleAware<EngineTestData>,
                     val model: ExecutableProcessModel,
                     val valid: List<Trace>,
                     val invalid: List<Trace>)

class ModelSpekSubjectContext(private val subjectProviderDsl: SubjectProviderDsl<ModelData>) {
  internal fun ModelData(model:Model, valid:List<Trace>, invalid:List<Trace>):ModelData {
    val engineData = subjectProviderDsl.memoized(CachingMode.GROUP) { EngineTestData.defaultEngine() }
    return ModelData(engineData, model.rootModel, valid, invalid)
  }

}

abstract class ModelSpek(subjectFactory: ModelSpekSubjectContext.() -> ModelData, custom:(SubjectDsl<ModelData>.()->Unit)?=null) : SubjectSpek<ModelData>(
  {
    subject(CachingMode.GROUP, {ModelSpekSubjectContext(this).subjectFactory()})

    describe("The model") {
      if (custom!=null) {
        group("Custom checks") { custom() }
      }

      it("Should be valid") {
        model.builder().validate()
      }
      for (validTrace in subject.valid) {
        group("For valid trace [${validTrace.joinToString()}]") {
          val hinstance = this.memoized(CachingMode.GROUP) {
            val transaction = transaction
            val hmodel = engine.addProcessModel(transaction, model, principal)
            val payload = null
            engine.startProcess(transaction, principal, hmodel,
                                "${model.name} instance for [${validTrace.joinToString()}]}",
                                java.util.UUID.randomUUID(), payload)
          }
          val processInstanceF = getter { transaction.readableEngineData.instance(hinstance()).withPermission() }
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
          val queue = StateQueue()
          for (pos in validTrace.indices) {
            val traceElement = validTrace[pos]
            val previous = queue.solidify()
            // TODO we want to properly support the trace
            val nodeInstanceF = getter {
              processInstanceF().let { processInstance: ProcessInstance ->
                processInstance.allChildren(transaction).lastOrNull { traceElement.fits(it) }
                ?: throw NoSuchElementException("No node instance for $traceElement found in ${processInstance.toDebugString(transaction)}}")
              }
            }


            queue.add { transaction.finishNodeInstance(hinstance(), traceElement) }

            group("For trace element #$pos -> ${traceElement}") {
              beforeGroup { previous();  }
              val node = model.findNode(traceElement) ?: throw AssertionError("No node could be find for trace element $traceElement")
              when (node) {
                is StartNode<*,*> -> testStartNode(nodeInstanceF, traceElement)
                is EndNode<*, *> -> testEndNode(transaction, nodeInstanceF, traceElement)
                is Join<*, *> -> test("Join $traceElement should already be finished") {
                  val nodeInstance= nodeInstanceF()
                  Assertions.assertEquals(Complete, nodeInstance.state) {
                    "There are still active predecessors: ${processInstanceF().getActivePredecessorsFor(
                      transaction.readableEngineData, nodeInstanceF as JoinInstance)}"
                  }
                }
                is Split<*, *> -> test("Split $traceElement should already be finished") {
                  val nodeInstance= nodeInstanceF()
                  Assertions.assertEquals(Complete, nodeInstance.state) { "Node $traceElement should be finished. The current nodes are: ${processInstanceF().toDebugString(transaction)}" }
                }
                is Activity<*, *>           -> {
                  if (node.childModel==null) {
                    test("$traceElement should not be in a final state") {
                      val nodeInstance= nodeInstanceF()
                      Assertions.assertFalse(
                        nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node.javaClass.simpleName} is in final state ${nodeInstance.state}" }
                    }
                    test("node instance ${traceElement} should be committed after starting") {
                      val processInstance = processInstanceF()
                      nodeInstanceF().startTask(transaction.writableEngineData, processInstance)
                      val nodeInstance= nodeInstanceF()
                      Assertions.assertTrue(nodeInstance.state.isCommitted) { "The instance state was ${processInstance.toDebugString(transaction)}" }
                      Assertions.assertEquals(IProcessNodeInstance.NodeInstanceState.Started, nodeInstance.state)
                    }
                    test("the node instance ${traceElement} should be final after finishing") {
                      processInstanceF().finishTask(transaction.writableEngineData, nodeInstanceF(), traceElement.resultPayload)
                    }
                  } else {
                    test("A child instance should have been created for $traceElement") {
                      Assertions.assertTrue((nodeInstanceF() as CompositeInstance).hChildInstance.valid) { "No child instance was recorded" }
                    }
                    test("The child instance was finished for $traceElement") {
                      val childInstance = transaction.readableEngineData.instance((nodeInstanceF() as CompositeInstance).hChildInstance).withPermission()
                      Assertions.assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
                    }
                  }
                }
                else -> {
                  test("$traceElement should not be in a final state") {
                    val nodeInstance = nodeInstanceF()
                    Assertions.assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} of type ${node.javaClass.simpleName} is in final state ${nodeInstance.state}" }
                  }
                }
              } // when

            } // group
          }



        }
      }

    }

  }) {}

private fun SpecBody.testEndNode(transaction: StubProcessTransaction,
                                 nodeInstanceF: Getter<ProcessNodeInstance>,
                                 traceElement: TraceElement) {
  assertNodeFinished(nodeInstanceF, traceElement)
  test("$traceElement should be part of the completion nodes") {
    val nodeInstance = nodeInstanceF()
    val parentInstance = transaction.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
    Assertions.assertTrue(
      parentInstance.completedNodeInstances.any { it.withPermission().node.id == traceElement.id }) {
      "Instance is: ${parentInstance.toDebugString(transaction)}"
    }
  }
}

private fun SpecBody.assertNodeFinished(nodeInstanceF: Getter<ProcessNodeInstance>, traceElement: TraceElement) {
  test("$traceElement should be finished") {
    Assertions.assertEquals(Complete, nodeInstanceF().state)
  }
}

private fun SpecBody.testStartNode(nodeInstanceF: Getter<ProcessNodeInstance>, traceElement: TraceElement) {
  assertNodeFinished(nodeInstanceF, traceElement)
}

val SubjectDsl<ModelData>.engine get() = subject.engineData().engine
val SubjectDsl<ModelData>.transaction get() = subject.engineData().engine.startTransaction()
val SubjectDsl<ModelData>.model get() = subject.model
val SubjectDsl<ModelData>.principal get() = subject.model.owner

fun StubProcessTransaction.finishNodeInstance(hProcessInstance: HProcessInstance, traceElement: TraceElement) {
  val instance = readableEngineData.instance(hProcessInstance).withPermission()
  val nodeInstance = instance.getNodeInstance(traceElement) ?: throw ProcessTestingException("No node instance for the trace elemnt $traceElement could be found in instance: ${instance.toDebugString(this)}")
  if (nodeInstance.state != Complete) {
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
