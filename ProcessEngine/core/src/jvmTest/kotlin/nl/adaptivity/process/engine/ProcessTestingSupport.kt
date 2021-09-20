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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.spek.InstanceSupport
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import org.junit.jupiter.api.Assertions.*

@Retention(AnnotationRetention.SOURCE)
@DslMarker
annotation class ProcessTestingDslMarker

typealias PNIHandle = Handle<SecureObject<ProcessNodeInstance<*>>>

fun ExecutableProcessModel.findNode(nodeIdentified: Identified): ExecutableProcessNode? {
    val nodeId = nodeIdentified.id
    return modelNodes.firstOrNull { it.id == nodeId } ?: childModels.asSequence().flatMap { it.modelNodes.asSequence() }
        .firstOrNull { it.id == nodeId }
}

@Throws(ProcessTestingException::class)
fun InstanceSupport.testTraceExceptionThrowing(
    hProcessInstance: Handle<SecureObject<ProcessInstance>>,
    trace: Trace
) {
    try {
        transaction.readableEngineData.instance(hProcessInstance).withPermission().assertTracePossible(trace)
    } catch (e: AssertionError) {
        throw ProcessTestingException(e)
    }
    for (traceElement in trace) {

        run {
            val nodeInstance = traceElement.getNodeInstance(hProcessInstance)
                ?: throw ProcessTestingException("The node instance (${traceElement}) should exist")

            if (nodeInstance.state != NodeInstanceState.Complete) {
                if (nodeInstance is JoinInstance) {
                    transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
                        startTask(transaction.writableEngineData)
                    }
                } else if (nodeInstance.node !is Split) {
                    if (nodeInstance is CompositeInstance) {
                        val childInstance =
                            transaction.readableEngineData.instance(nodeInstance.hChildInstance).withPermission()
                        if (childInstance.state != ProcessInstance.State.FINISHED && nodeInstance.state != NodeInstanceState.Complete) {
                            try {
                                transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
                                    finishTask(transaction.writableEngineData, null)
                                }
                            } catch (e: ProcessException) {
                                if (e.message?.startsWith(
                                        "A Composite task cannot be finished until its child process is. The child state is:"
                                    ) == true
                                ) {
                                    throw ProcessTestingException("The composite instance cannot be finished yet")
                                } else throw e
                            }
                        }
                    } else if (nodeInstance.state.isFinal && nodeInstance.state != NodeInstanceState.Complete) {
                        try {
                            transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
                                finishTask(transaction.writableEngineData, null)
                            }
                            engine.processTickleQueue(transaction)
                        } catch (e: ProcessException) {
                            assertNotNull(e.message)
                            assertTrue(
                                e.message!!.startsWith("instance ${nodeInstance.node.id}") &&
                                    e.message!!.endsWith(" cannot be finished as it is already in a final state.")
                            )
                        }
                        throw ProcessTestingException("The node is final but not complete (failed, skipped)")
                    }
                    try {
                        transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
                            finishTask(transaction.writableEngineData, traceElement.resultPayload)
                        }
                    } catch (e: ProcessException) {
                        throw ProcessTestingException(e)
                    }
                }
            }
        }
        try {
            engine.processTickleQueue(transaction)
        } catch (e: ProcessException) {
            throw ProcessTestingException(e)
        }

        val nodeInstance = traceElement.getNodeInstance(hProcessInstance)
            ?: throw ProcessTestingException("The node instance should exist")

        if (nodeInstance.state != NodeInstanceState.Complete) {
            val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
            throw ProcessTestingException(
                "At trace $traceElement -  State of node $nodeInstance not complete but ${nodeInstance.state} ${instance.toDebugString()}"
            )
        }
    }
}

internal class ProcessTestingException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(cause.message, cause)
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

fun kfail(message: String): Nothing {
    fail<Any?>(message)
    throw UnsupportedOperationException("This code should not be reachable")
}

internal fun Boolean.toXPath() = if (this) "true()" else "false()"
internal fun Boolean.toCondition() = if (this) ExecutableCondition.TRUE else ExecutableCondition.FALSE

operator fun ProcessTransaction.get(handle: Handle<SecureObject<ProcessInstance>>): ProcessInstance {
    return this.readableEngineData.instance(handle).withPermission()
}
