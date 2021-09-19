/*
 * Copyright (c) 2018.
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

import kotlinx.serialization.json.Json
import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.spek.*
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.spek.DelegateTestBody
import nl.adaptivity.util.Getter
import nl.adaptivity.util.assertJsonEquals
import nl.adaptivity.util.getter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.meta.*
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe
import java.security.Principal
import java.util.*

data class ModelData(
    val engineData: () -> EngineTestData,
    val model: ExecutableProcessModel,
    val valid: List<Trace>,
    val invalid: List<Trace>
) {
    internal constructor(model: TestConfigurableModel, valid: List<Trace>, invalid: List<Trace>) :
        this({ EngineTestData.defaultEngine() }, model.rootModel, valid, invalid)

    constructor(model: ProcessModel<*>, valid: List<Trace>, invalid: List<Trace>) :
        this(
            { EngineTestData.defaultEngine() },
            model.rootModel as? ExecutableProcessModel ?: ExecutableProcessModel(model.rootModel.builder()),
            valid,
            invalid
        )

    constructor(
        engineData: () -> EngineTestData,
        model: ProcessModel<*>,
        valid: List<Trace>,
        invalid: List<Trace>
    ) : this(
        engineData,
        model.rootModel as? ExecutableProcessModel ?: ExecutableProcessModel(model.rootModel.builder()),
        valid,
        invalid
    )
}

/*
class ModelSpekSubjectContext(private val subjectProviderDsl: SubjectProviderDsl<ModelData>) {
    internal fun ModelData(model: ConfigurableModel, valid: List<Trace>, invalid: List<Trace>): ModelData {
        val engineData = { EngineTestData.defaultEngine() }
        return ModelData(engineData, model.rootModel, valid, invalid)
    }

}
*/

//val SubjectDsl<EngineTestData>.engine get() = subject.engine

private var subjectCreated = false

/**
 * This function takes n elements from the list by sampling. This differs from [List.take] as that function will take
 * the first n elements where this function will sample.
 */
fun <T> List<T>.selectN(max: Int): List<T> {
    val origSize = size
    if (max >= origSize) return this
    return filterIndexed { idx, _ ->
        idx == 0 || (((idx - 1) * max) / origSize < (idx * max) / origSize)
    }
}

class EngineTestBody(delegate: TestBody) : DelegateTestBody(delegate) {
    val engineData by memoized<EngineTestData>()
}

open class EngineSuite(val delegate: Suite) : LifecycleAware by delegate {
    val engineData by memoized<EngineTestData>()

    @Synonym(SynonymType.GROUP)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun describe(description: String, skip: Skip = Skip.No, body: EngineSuite.() -> Unit) {
        delegate.describe(description, skip) { EngineSuite(this).body() }
    }

    @Synonym(SynonymType.GROUP)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun context(description: String, skip: Skip = Skip.No, body: EngineSuite.() -> Unit) {
        delegate.context(description, skip) { EngineSuite(this).body() }
    }

    @Synonym(SynonymType.GROUP, excluded = true)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun xdescribe(description: String, reason: String = "", body: EngineSuite.() -> Unit) {
        delegate.xdescribe(description, reason) { EngineSuite(this).body() }
    }

    @Synonym(SynonymType.GROUP, excluded = true)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun xcontext(description: String, reason: String = "", body: EngineSuite.() -> Unit) {
        delegate.xcontext(description, reason) { EngineSuite(this).body() }
    }

    @Synonym(SynonymType.TEST)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun it(
        description: String,
        skip: Skip = Skip.No,
        timeout: Long = delegate.defaultTimeout,
        body: EngineTestBody.() -> Unit
    ) {
        delegate.it(description, skip, timeout) { EngineTestBody(this).body() }
    }

    @Synonym(SynonymType.TEST, excluded = true)
    @Descriptions(Description(DescriptionLocation.VALUE_PARAMETER, 0))
    fun xit(description: String, reason: String = "", body: EngineTestBody.() -> Unit) {
        delegate.xit(description, reason) { EngineTestBody(this).body() }
    }

    fun before(cb: () -> Unit) {
        beforeGroup(cb)
    }

    fun after(cb: () -> Unit) {
        afterGroup(cb)
    }

    fun beforeEach(cb: () -> Unit) {
        beforeEachTest(cb)
    }

    fun afterEach(cb: () -> Unit) {
        afterEachTest(cb)
    }

}

internal fun EngineSuite.testValidTrace(
    model: ExecutableProcessModel,
    principal: Principal,
    validTrace: Trace
) {
    context("For valid trace [${validTrace.joinToString()}]") {
        val traceTransaction by memoized(CachingMode.EACH_GROUP) { engineData.engine.startTransaction() }

        val transaction = getter { traceTransaction }
        val hinstance = startProcess(
            transaction, model, principal,
            "${model.name} instance for [${validTrace.joinToString()}]"
        )
        val processInstanceF = getter {
            transaction().readableEngineData.instance(hinstance()).withPermission()
        }

        testTraceStarting(processInstanceF)

        val queue = StateQueue()
        for (pos in validTrace.indices) {
            val traceElement = validTrace[pos]
            val previous = queue.solidify()
            // TODO we want to properly support the trace
            val nodeInstanceF: Getter<ProcessNodeInstance<*>> = getter {
                processInstanceF().let { processInstance: ProcessInstance ->
                    traceElement.getNodeInstance(transaction(), processInstance)
                        ?: throw NoSuchElementException(
                            "No node instance for $traceElement found in ${processInstance.toDebugString(transaction())}}"
                        )
                }
            }

            queue.add {
                transaction().finishNodeInstance(hinstance(), traceElement)
                engineData.engine.processTickleQueue(transaction())
            }

            context("trace element #$pos -> ${traceElement}") {
                beforeGroup { previous(); }
                val node = model.findNode(traceElement) ?: throw AssertionError(
                    "No node could be find for trace element $traceElement"
                )
                when (node) {
                    is StartNode         -> testStartNode(nodeInstanceF, traceElement)
                    is EndNode           -> testEndNode(transaction, nodeInstanceF, traceElement)
                    is Join              -> testJoin(transaction, nodeInstanceF, traceElement)
                    is Split             -> testSplit(transaction, nodeInstanceF, traceElement)
                    is MessageActivity   -> testActivity(transaction, nodeInstanceF, traceElement)
                    is CompositeActivity -> testComposite(transaction, nodeInstanceF, traceElement)
//                    is Activity -> { fail("Unsupported activity subtype") }
                    else                 -> it("$traceElement should not be in a final state") {
                        val nodeInstance = nodeInstanceF()
                        assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id}[${nodeInstance.entryNo}] of type ${node.javaClass.simpleName} is in final state ${nodeInstance.state}" }
                    }
                } // when

            } // trace element group


        } // test everything
        testTraceCompletion(model, queue.solidify(), transaction, processInstanceF, validTrace)


    }
}

internal fun EngineSuite.testInvalidTrace(
    model: ExecutableProcessModel,
    principal: Principal,
    invalidTrace: Trace,
    failureExpected: Boolean = true
) {
    val transaction = getter { engineData.engine.startTransaction() }
    val hinstance = startProcess(
        transaction, model, principal,
        "${model.name} instance for [${invalidTrace.joinToString()}]}"
    )
    val processInstanceF = getter {
        transaction().readableEngineData.instance(hinstance()).withPermission()
    }
    context(
        "given ${if (failureExpected) "invalid" else "valid"} trace ${
            invalidTrace.joinToString(
                prefix = "[",
                postfix = "]"
            )
        }"
    ) {
        it("Executing the trace should ${if (!failureExpected) "not fail" else "fail"}") {
            var success = false
            try {
                val instanceSupport = object : InstanceSupport {
                    override val transaction: StubProcessTransaction get() = transaction()
                    override val engine: ProcessEngine<StubProcessTransaction, *>
                        get() = engineData.engine

                }
                instanceSupport.testTraceExceptionThrowing(hinstance(), invalidTrace)
            } catch (e: ProcessTestingException) {
                if (!failureExpected) {
                    throw e
                }
                success = true
            }
            if (failureExpected && !success) kfail(
                "The invalid trace ${invalidTrace.joinToString(prefix = "[", postfix = "]")} could be executed"
            )
        }
        if (!failureExpected) {
            it("The process instance should have a finished state") {
                assertEquals(ProcessInstance.State.FINISHED, processInstanceF().state)
            }
        }
    }
}

private fun EngineSuite.startProcess(
    transactionGetter: Getter<StubProcessTransaction>,
    model: ExecutableProcessModel,
    owner: Principal,
    name: String,
    payload: CompactFragment? = null
): Lazy<HProcessInstance> {
    return lazy {
        val transaction = transactionGetter()
        val hmodel = if (model.handle.isValid &&
            model.handle in transaction.readableEngineData.processModels &&
            transaction.readableEngineData.processModels[model.handle]?.withPermission()?.uuid == model.uuid
        ) {
            model.handle
        } else {
            model.setHandleValue(-1)
            engineData.engine.addProcessModel(transaction, model.builder(), owner)
        }
        engineData.engine.startProcess(transaction, owner, hmodel, name, UUID.randomUUID(), payload)
    }
}

private fun EngineSuite.testTraceStarting(processInstanceF: Getter<ProcessInstance>) {
    context("After starting") {
        it("Only start nodes should be finished") {
            val processInstance = processInstanceF()
            val predicate: (ProcessNodeInstance<*>) -> Boolean =
                { it.state == Skipped || it.node is StartNode || it.node is Split || it.node is Join }
            val onlyStartNodesCompleted = processInstance.finishedNodes.all(predicate)
            assertTrue(onlyStartNodesCompleted) {
                processInstance.finishedNodes
                    .filterNot(predicate)
                    .joinToString(
                        prefix = "Nodes [",
                        postfix = "] are not startnodes, but already finished."
                    )
            }
        }
    }
}

private fun EngineSuite.testTraceCompletion(
    model: ExecutableProcessModel,
    queue: StateQueue.SolidQueue,
    transactionF: Getter<StubProcessTransaction>,
    processInstanceF: Getter<ProcessInstance>,
    validTrace: Trace
) {
    context("The trace should be finished correctly") {
        beforeGroup { queue.invoke() }
        it("The trace should be valid") {
            processInstanceF().assertTracePossible(transactionF(), validTrace)
        }
        it("All non-endnodes are finished") {
            val expectedFinishedNodes = validTrace.asSequence()
                .map { model.findNode(it)!! }
                .filterNot { it is EndNode }.map { it.id }.toList().toTypedArray()
            processInstanceF().assertFinished(transactionF(), *expectedFinishedNodes)
        }
        it("No nodes are active") {
            processInstanceF().assertActive(transactionF())
        }
        it("The process itself is marked finished") {
            assertEquals(
                ProcessInstance.State.FINISHED, processInstanceF().state,
                "Instance state should be finished, but is not. ${
                    processInstanceF().toDebugString(
                        transactionF()
                    )
                }"
            )
        }
        it("All endNodes in the trace are complete, skipped, cancelled or failed") {
            val transaction = transactionF()
            val processInstance = processInstanceF()
            val expectedCompletedNodes = validTrace.asSequence()
                .map { model.findNode(it)!! }
                .filterIsInstance(EndNode::class.java)
                .filter { endNode ->
                    val nodeInstance = processInstance.transitiveChildren(transaction).firstOrNull { it.node.id == endNode.id }
                        ?: kfail(
                            "Nodeinstance ${endNode.identifier} does not exist, the instance is ${
                                processInstance.toDebugString(
                                    transaction
                                )
                            }"
                        )
                    nodeInstance.state !in listOf(
                        Skipped,
                        SkippedCancel,
                        SkippedFail
                    )
                }
                .map { it.id!! }
                .toList().toTypedArray()
            processInstanceF().assertComplete(transactionF(), *expectedCompletedNodes)
        }
    }
}

private fun EngineSuite.testStartNode(nodeInstanceF: Getter<ProcessNodeInstance<*>>, traceElement: TraceElement) {
    it("Start node $traceElement should be finished") {
        testAssertNodeFinished(nodeInstanceF, traceElement)
    }
}

private fun EngineSuite.testComposite(
    transaction: Getter<StubProcessTransaction>,
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    it("A child instance should have been created for $traceElement") {
        assertTrue(
            (nodeInstanceF() as CompositeInstance).hChildInstance.isValid
        ) { "No child instance was recorded" }
    }
    it("The child instance was finished for $traceElement") {
        val childInstance = transaction().readableEngineData.instance(
            (nodeInstanceF() as CompositeInstance).hChildInstance
        ).withPermission()
        assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
    }
    it("The activity itself should be finished for $traceElement") {
        assertEquals(Complete, nodeInstanceF().state)
    }

}

private fun EngineSuite.testActivity(
    transaction: Getter<StubProcessTransaction>,
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    it("$traceElement should not be in a final state") {
        val nodeInstance = nodeInstanceF()
        assertFalse(nodeInstance.state.isFinal) {
            "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in final state ${nodeInstance.state}"
        }
    }
/*
    it("$traceElement should be pending before processing pending tickles") {
        val nodeInstance = nodeInstanceF()
        assertEquals(NodeInstanceState.Pending, nodeInstance.state)
        engineData.engine.processTickleQueue(transaction())
    }
*/
    it("$traceElement should not be in pending or sent state") {
        val nodeInstance = nodeInstanceF()
        assertNotEquals(
            Pending,
            nodeInstance.state
        ) { "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in pending state" }
        assertNotEquals(
            Sent,
            nodeInstance.state
        ) { "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in sent (not acknowledged) state" }
    }
    it("node instance ${traceElement} should be committed after starting") {
        var nodeInstance = nodeInstanceF()
        assertEquals(traceElement.nodeId, nodeInstance.node.id)
        val tr = transaction()
        nodeInstance = tr.writableEngineData.updateNodeInstance(nodeInstance.handle) {
            startTask(tr.writableEngineData)
        }.withPermission()
        val processInstance = tr.readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()

        assertTrue(nodeInstance.state.isCommitted) {
            "The instance state was ${processInstance.toDebugString(transaction)}"
        }
        assertEquals(Started, nodeInstance.state)
    }
    it("the node instance ${traceElement} should be final after finishing") {
        val tr = transaction()
        tr.writableEngineData.updateNodeInstance(nodeInstanceF().handle) {
            finishTask(tr.writableEngineData, traceElement.resultPayload)
        }
        assertEquals(Complete, nodeInstanceF().state)
    }
}

private fun EngineSuite.testSplit(
    transaction: Getter<StubProcessTransaction>,
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    it("Split $traceElement should already be finished") {
        val nodeInstance = nodeInstanceF()
        assertEquals(Complete, nodeInstance.state) {
            val processInstance = transaction().readableEngineData.instance(
                nodeInstance.hProcessInstance
            ).withPermission()
            "Node $traceElement should be finished. The current nodes are: ${
                processInstance.toDebugString(
                    transaction
                )
            }"
        }
    }
}

private fun EngineSuite.testJoin(
    transaction: Getter<StubProcessTransaction>,
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    it("Join $traceElement should already be finished") {
        val nodeInstance = nodeInstanceF() as JoinInstance
        val processInstance = transaction().readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
        val activePredecessors = processInstance.getActivePredecessorsFor(
            transaction().readableEngineData,
            nodeInstanceF() as JoinInstance
        )
        // Allow this to continue when there are
        if (!(activePredecessors.isEmpty() && nodeInstance.canFinish())) {
            assertEquals(Complete, nodeInstance.state) {
                "There are still active predecessors: $activePredecessors, instance: ${
                    processInstance.toDebugString(
                        transaction
                    )
                }"
            }
        }
    }
}

private fun EngineSuite.testEndNode(
    transaction: Getter<StubProcessTransaction>,
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    testAssertNodeFinished(nodeInstanceF, traceElement)
    it("$traceElement should be part of the completion nodes") {
        val nodeInstance = nodeInstanceF()
        val parentInstance = transaction().readableEngineData.instance(nodeInstance.hProcessInstance).withPermission()
        assertTrue(
            parentInstance.completedNodeInstances.any { it.withPermission().node.id == traceElement.id }) {
            "Instance is: ${parentInstance.toDebugString(transaction)}"
        }
    }
    it("The predecessors of $traceElement should be final") {
        assertTrue(
            nodeInstanceF().predecessors.map {
                transaction().readableEngineData.nodeInstance(it).withPermission()
            }.all { it.state.isFinal },
            "All immediate predecessors of an end node should be final"
        )
    }
}

private fun EngineSuite.testAssertNodeFinished(
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    it("$traceElement should be finished") {
        val nodeInstance = nodeInstanceF()
        assertEquals(Complete, nodeInstance.state) {
            "Node ${nodeInstance} is not finished in process instance"
        }
    }
}

private fun EngineTestBody.testAssertNodeFinished(
    nodeInstanceF: Getter<ProcessNodeInstance<*>>,
    traceElement: TraceElement
) {
    assertEquals(Complete, nodeInstanceF().state)
}

fun StubProcessTransaction.finishNodeInstance(
    hProcessInstance: Handle<SecureObject<ProcessInstance>>,
    traceElement: TraceElement
):
    Handle<SecureObject<ProcessNodeInstance<*>>> {
    val instance = readableEngineData.instance(hProcessInstance).withPermission()
    val nodeInstance: ProcessNodeInstance<*> =
        traceElement.getNodeInstance(this, instance)
            ?: throw ProcessTestingException(
                "No node instance for the trace element $traceElement could be found in instance: ${
                    instance.toDebugString(this)
                }"
            )
    if (nodeInstance.state != Complete) {
        System.err.println("Re-finishing node ${nodeInstance.node.id} $nodeInstance for instance $instance")
        writableEngineData.updateNodeInstance(nodeInstance.handle) {
            finishTask(writableEngineData, traceElement.resultPayload)
        }
    }
    assert(nodeInstance.state == Complete)
    return nodeInstance.handle
}

class CustomDsl(
    delegate: Suite,
    val model: ExecutableProcessModel,
    val valid: List<Trace>,
    val invalid: List<Trace>
) : EngineSuite(delegate) {
}

private inline operator fun <T> Lazy<T>.invoke() = this.value
