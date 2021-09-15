/*
 * Copyright (c) 2021.
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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.toComparableHandle
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.spek.InstanceSupport
import nl.adaptivity.process.engine.spek.assertComplete
import nl.adaptivity.process.engine.spek.assertTracePossible
import nl.adaptivity.process.engine.spek.toDebugString
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.assertJsonEquals
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.net.URI
import java.util.*
import kotlin.reflect.KClass

abstract class TraceTest(val config: CompanionBase) {

    @Nested
    @DisplayName("Serialization")
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("unused")
    inner class SerialTests {
        @TestFactory
        @DisplayName("Json tests")
        fun serializeToJson(): List<DynamicTest> {
            return serializeToFormat(config.expectedJson, Json, "getExpectedJson")
        }

        @TestFactory
        @DisplayName("Xml tests")
        fun serializeToXml(): List<DynamicTest> {
            return serializeToFormat(config.expectedXml, XML { autoPolymorphic = true }, "getExpectedXml")
        }

        private fun serializeToFormat(
            expectedSerial: String?,
            format: StringFormat,
            methodName: String
        ): List<DynamicTest> {
            return if (expectedSerial == null) {
                emptyList()
            } else {
                val sourceClass = config.javaClass.canonicalName
                val methodUri = URI("method:${sourceClass}#$methodName()")
                listOf(
                    dynamicTest("Serializing to ${format.name} should be valid", methodUri) {
                        val actual = format.encodeToString(config.model)
                        assertSerialEquals(
                            format,
                            expectedSerial,
                            actual,
                            "The ${format.name} content should match expectations"
                        )
                    },
                    dynamicTest(
                        "Deserializing from ${format.name} should be correct",
                        URI("method:${sourceClass}#${methodName}")
                    ) {
                        val actual = format.decodeFromString<ExecutableProcessModel>(expectedSerial)
                        assertEquals(config.model, actual, "They should be equal")
                    }
                )
            }
        }

        private val StringFormat.name: String
            get() = when (this) {
                is Json -> "Json"
                is XML  -> "XML"
                else    -> javaClass.simpleName
            }

        fun assertSerialEquals(format: StringFormat, expected: String, actual: String, message: String) {
            when (format) {
                is Json -> assertJsonEquals(expected, actual, message)
                is XML  -> assertXMLEqual(expected, actual, message)
                else    -> assertEquals(expected, actual, message)
            }
        }

        @Test
        @DisplayName("Round trip serialization to xml and back should be correct.")
        fun testRoundTrip() {
            val xml = XML { autoPolymorphic = true }
            val serialized = xml.encodeToString(config.model)
            val deSerialized = xml.decodeFromString<ExecutableProcessModel>(serialized)
            assertEquals(config.model, deSerialized, "The result of deserialization should be equal to the original")
            val reserialized = xml.encodeToString(deSerialized)
            assertEquals(serialized, reserialized, "Serializing and back should have equal result")
        }
    }

    @TestFactory
    @DisplayName("Valid traces")
    fun testValidTraces(): List<DynamicNode> {
        return config.validTraces.map { createValidTraceTest(config, it) }
    }

    @TestFactory
    @DisplayName("Invalid traces")
    fun testInvalidTraces(): List<DynamicNode> {
        return config.inValidTraces.map { createInvalidTraceTest(config, it) }
    }

    abstract class CompanionBase {

        val model: ExecutableProcessModel get() = modelData.model

        protected abstract val modelData: ModelData

        fun newEngineData() = modelData.engineData()
        val validTraces get() = modelData.valid
        val inValidTraces get() = modelData.invalid

        open val expectedJson: String? get() = null
        open val expectedXml: String? get() = null
    }
}

class TestContext(private val config: TraceTest.CompanionBase) {
    val engineData = config.newEngineData()
    val transaction = engineData.engine.startTransaction()
    val principal = config.model.owner
    val model = config.model

    val hmodel: Handle<ExecutableProcessModel> =
        with(config) {//engineData.engine.addProcessModel(transaction, config.model, principal)
            if (model.handle.isValid &&
                model.handle in transaction.readableEngineData.processModels &&
                transaction.readableEngineData.processModels[model.handle]?.withPermission()?.uuid == model.uuid
            ) {
                model.handle
            } else {
                model.setHandleValue(-1)
                engineData.engine.addProcessModel(transaction, model.builder(), model.owner)
            }
        }

    var hInstance: ComparableHandle<SecureObject<ProcessInstance>> = getInvalidHandle()
        get() {
            if (!field.isValid) {
                field = startProcess()
            }
            return field
        }
        private set

    val instanceUuid = UUID.randomUUID()

    fun dbgInstance(): String {
        return getProcessInstance().toDebugString(transaction)
    }

    fun startProcess(): ComparableHandle<SecureObject<ProcessInstance>> {
        val name = "${config.model.name} instance"
        hInstance = engineData.engine.startProcess(transaction, principal, hmodel, name, instanceUuid, null)
            .toComparableHandle()
        return hInstance
    }

    fun getProcessInstance(instanceHandle: Handle<SecureObject<ProcessInstance>> = hInstance): ProcessInstance {
        return transaction.readableEngineData.instance(instanceHandle).withPermission()
    }

    fun TraceElement.getNodeInstance(): ProcessNodeInstance<*>? {
        return getNodeInstance(transaction, getProcessInstance())
    }

    fun getNodeInstance(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): ProcessNodeInstance<*> {
        return transaction.readableEngineData.nodeInstance(handle).withPermission()
    }

    inline fun updateNodeInstance(
        traceElement: TraceElement,
        instanceHandle: ComparableHandle<SecureObject<ProcessInstance>> = hInstance,
        action: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
    ) {
        val processInstance = getProcessInstance(instanceHandle)
        processInstance.update(transaction.writableEngineData) {
            val nodeInstance = traceElement.getNodeInstance(transaction, processInstance)
                ?: fail("Missing node instance for $traceElement")
            updateChild(nodeInstance, action)
        }

    }

    fun runTrace(
        trace: Trace,
        lastElement: Int = -1,
        instanceHandle: ComparableHandle<SecureObject<ProcessInstance>> = hInstance
    ): Handle<SecureObject<ProcessNodeInstance<*>>> {
        var lastInstance: Handle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle()
        for (idx in 0 until (if (lastElement < 0) trace.size else lastElement)) {
            val traceElement = trace[idx]
            when (model.getNode(traceElement.nodeId)) {
                is MessageActivity -> {
                    updateNodeInstance(traceElement, instanceHandle) {
                        startTask(transaction.writableEngineData)
                    }
                    updateNodeInstance(traceElement, instanceHandle) {
                        finishTask(transaction.writableEngineData, traceElement.resultPayload)
                    }

                }
            }
            val ni = traceElement.getNodeInstance(transaction, getProcessInstance(instanceHandle))!!
            assertEquals(NodeInstanceState.Complete, ni.state, "Expected the state of $ni to be complete, not ${ni.state}\n${dbgInstance()}")
            lastInstance = ni.handle

            engineData.engine.processTickleQueue(transaction)
        }
        return lastInstance
    }

    inline fun <reified T:ProcessNode> Trace.nodes(): Sequence<T> {
        return asSequence().map { model.findNode(it) }.filterIsInstance<T>()
    }

    inline fun <reified T:ProcessNode> Trace.nodeInstances(): Sequence<ProcessNodeInstance<*>?> {
        return nodes<T>().zip(asSequence()) { node, traceElement ->
            traceElement.getNodeInstance(transaction, getProcessInstance())
        }
    }

    inline fun <reified T:ProcessNode> Trace.allNodeInstances(): Sequence<ProcessNodeInstance<*>> {
        return allNodeInstances(T::class)
    }

    fun Trace.allNodeInstances(type: KClass<*>): Sequence<ProcessNodeInstance<*>> {
        val processInstance = getProcessInstance()
        return asSequence().mapNotNull { traceElement ->
            val node = model.findNode(traceElement)
            when {
                node == null -> kfail("No node with name ${traceElement.nodeId} present in the model")
                type.isInstance(node) -> traceElement.getNodeInstance(transaction, processInstance)
                    ?: kfail("Nodeinstance $traceElement does not exist; ${dbgInstance()}")
                else -> null
            }
        }
    }

    fun assertTracePossible(trace: Trace) {
        getProcessInstance().assertTracePossible(transaction, trace)
    }

}

class ContainerContext(val config: TraceTest.CompanionBase, private val children: MutableCollection<DynamicNode>) {

    fun addTest(node: DynamicNode) {
        children.add(node)
    }

    inline fun addTest(displayName: String, crossinline executable: TestContext.() -> Unit) {
        addTest(dynamicTest(displayName) { TestContext(config).executable() })
    }

    inline fun addTest(displayName: String, testSourceUri: URI, crossinline executable: TestContext.() -> Unit) {
        addTest(dynamicTest(displayName, testSourceUri) { TestContext(config).executable() })
    }

    inline fun dynamicContainer(displayName: String, configure: ContainerContext.() -> Unit) {
        val children = mutableListOf<DynamicNode>()
        ContainerContext(config, children).apply(configure)
        addTest(dynamicContainer(displayName, children))
    }

}

fun createValidTraceTest(config: TraceTest.CompanionBase, trace: Trace): DynamicContainer {
    return config.dynamicContainer("For valid trace [${trace.joinToString()}]") {
        addTest("After starting only start nodes should be finished") {
            val processInstance = getProcessInstance()

            val predicate: (ProcessNodeInstance<*>) -> Boolean =
                { it.state == NodeInstanceState.Skipped || it.node is StartNode || it.node is Split || it.node is Join }

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
        dynamicContainer("On completion") {
            addTest("The trace should be valid") {
                assertTracePossible(trace)
            }
            addTest("All non-endnodes are finished") {
                runTrace(trace)
                val expectedFinishedNodes = trace.allNodeInstances<ExecutableProcessNode>()
                    .filterNot { it.node is EndNode }
                    .map { TraceElement(it.node.id, it.entryNo) }
                    .sorted()
                    .toList()

                val actualFinishedNodes = getProcessInstance().childNodes
                    .map { it.withPermission() }
                    .filter { it.state.isFinal && it.node !is EndNode }
                    .onEach(ProcessNodeInstance<*>::assertFinished)
                    .filterNot { it.state.isSkipped }
                    .map { TraceElement(it.node.id, it.entryNo) }
                    .sorted()

                assertEquals(expectedFinishedNodes, actualFinishedNodes) {
                    "\"The list of finished nodes does not match (" +
                        "Expected: [${expectedFinishedNodes.joinToString()}], " +
                        "found: [${actualFinishedNodes.joinToString()}])\""
                }
            }
            addTest("No nodes are active") {
                runTrace(trace)
                val activeNodes = getProcessInstance().activeNodes.toList()
                assertTrue(activeNodes.none()) {
                    "The list of active nodes is not empty (Expected: [], found: [${activeNodes.joinToString()}])"
                }
            }
            addTest("The process itself is marked as finished") {
                runTrace(trace)
                assertEquals(ProcessInstance.State.FINISHED, getProcessInstance().state) {
                    "Instance state should be finished, but is not. ${dbgInstance()}"
                }

            }
            addTest("all endNodes in the trace are complete, skipped, cancelled or failed") {
                runTrace(trace)

                val expectedCompletedNodes = trace.allNodeInstances<EndNode>()
                    .filter { !it.state.isSkipped }
                    .map { it.node.id }
                    .toList().toTypedArray()

                getProcessInstance().assertComplete(transaction, *expectedCompletedNodes)
            }
        }
        for (i in trace.indices) {
            dynamicContainer("trace element #$i → ${trace[i]}") {
                createTraceElementTest(trace, i)
            }
        }
        addTest(createInvalidTraceTest(config, trace, false))
    }
}

fun ContainerContext.createTraceElementTest(trace: Trace, elementIdx: Int) {
    val node = config.model.getNode(trace[elementIdx])
        ?: kfail("Node ${trace[elementIdx]} not found in the model")

    when (node) {
        is StartNode -> createStartElementTest(trace, elementIdx, node)
        is Split -> createSplitElementTest(trace, elementIdx, node)
        is Join -> createJoinElementTest(trace, elementIdx, node)
        is MessageActivity -> createMessageElementTest(trace, elementIdx, node)
        is CompositeActivity -> createCompositeElementTest(trace, elementIdx, node)
        is EndNode -> createEndElementTest(trace, elementIdx, node)
        else -> kfail("Unexpected node type found: ${node?.javaClass}")
    }
}

private fun ContainerContext.createStartElementTest(trace: Trace, elementIdx: Int, node: StartNode) {
    addTest("Start node ${trace[elementIdx]} should be finished") {
        runTrace(trace, elementIdx) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }
    addTest("Start node ${trace[elementIdx]} should still be finished on completion") {
        runTrace(trace, elementIdx+1) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }
}

private fun ContainerContext.createSplitElementTest(trace: Trace, elementIdx: Int, node: Split) {
    val traceElement = trace[elementIdx]
    addTest("Split $traceElement should already be finished") {
        runTrace(trace, elementIdx)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()) {
            "Node $traceElement should be finished. The current nodes are: ${dbgInstance()}"
        }
    }
}

private fun ContainerContext.createJoinElementTest(trace: Trace, elementIdx: Int, node: Join) {
    val traceElement = trace[elementIdx]
    addTest("Join $traceElement is finished or can finish") {
        runTrace(trace, elementIdx)
        val pni = traceElement.getNodeInstance()
        val activePredecessors = getProcessInstance().getActivePredecessorsFor(transaction.readableEngineData, pni as JoinInstance)

        if (! (activePredecessors.isEmpty() && pni.canFinish())) {
            assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()) {
                "There are still active predecessors $activePredecessors for $traceElement;${dbgInstance()}"
            }
        }
    }
    addTest("Join $traceElement should be finished afterwards") {
        runTrace(trace, elementIdx+1)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()) {
            "Node $traceElement should be finished. The current nodes are: ${dbgInstance()}"
        }
    }

}

private fun ContainerContext.createMessageElementTest(trace: Trace, elementIdx: Int, node: MessageActivity) {
    val traceElement = trace[elementIdx]
    addTest("Activity node $traceElement should be not be finished before completion") {
        runTrace(trace, elementIdx) // just before the element
        val nodeInstance = traceElement.getNodeInstance()!!
        assertFalse(nodeInstance.state.isFinal, nodeInstance.toString())
    }
    addTest("Activity node $traceElement should be finished on completion") {
        runTrace(trace, elementIdx+1) // just before the element
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state)
    }
    addTest("$traceElement should not be in pending or sent state") {
        runTrace(trace, elementIdx)
        val nodeInstance = traceElement.getNodeInstance()!!
        assertNotEquals(NodeInstanceState.Pending, nodeInstance.state) {
            "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in pending state"
        }
        assertNotEquals(NodeInstanceState.Sent, nodeInstance.state) {
            "The node ${nodeInstance.node.id} of type ${nodeInstance.node.javaClass.simpleName} is in sent (not acknowledged) state"
        }
    }
    addTest("node instance $traceElement should be committed after starting") {
        runTrace(trace, elementIdx)
        var nodeInstance = traceElement.getNodeInstance()!!
        assertEquals(traceElement.nodeId, node.id)
        val tr = transaction
        val processInstance = getProcessInstance(nodeInstance.hProcessInstance)
        processInstance.update(tr.writableEngineData) {
            updateChild(nodeInstance) {
                startTask(tr.writableEngineData)
            }
        }
        nodeInstance = traceElement.getNodeInstance()!!

        assertTrue(nodeInstance.state.isCommitted) {
            "The instance state was ${processInstance.toDebugString(transaction)}"
        }
        assertEquals(NodeInstanceState.Started, nodeInstance.state)
    }
    addTest("the node instance $traceElement should be final after finishing") {
        runTrace(trace, elementIdx)
        var nodeInstance = traceElement.getNodeInstance()!!
        getProcessInstance(nodeInstance.hProcessInstance).update(transaction.writableEngineData) {
            updateChild(nodeInstance) {
                finishTask(transaction.writableEngineData, traceElement.resultPayload)
            }
        }
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state)
    }
}

private fun ContainerContext.createCompositeElementTest(trace: Trace, elementIdx: Int, node: CompositeActivity) {
    val traceElement = trace[elementIdx]
    addTest("A child instance should have been created for $traceElement") {
        runTrace(trace, elementIdx)
        Assertions.assertTrue(
            (traceElement.getNodeInstance() as CompositeInstance).hChildInstance.isValid
        ) { "No child instance was recorded" }
    }
    addTest("The child instance was finished for $traceElement") {
        runTrace(trace, elementIdx+1)
        val nodeInstance = traceElement.getNodeInstance() as CompositeInstance
        val childInstance = getProcessInstance(nodeInstance.hChildInstance)

        assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
    }
    addTest("The activity itself should be finished for $traceElement") {
        runTrace(trace, elementIdx+1)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state)
    }

}

private fun ContainerContext.createEndElementTest(trace: Trace, elementIdx: Int, node: EndNode) {
    val traceElement = trace[elementIdx]
    addTest("$traceElement should be part of the completion nodes") {
        runTrace(trace, elementIdx)
        val parentInstance = getProcessInstance(traceElement.getNodeInstance()!!.hProcessInstance)
        assertTrue(
            parentInstance.completedNodeInstances.asSequence()
                .map { it.withPermission() }
                .any { it.node.id == traceElement.nodeId }
        )  { "The end node should be complete; ${dbgInstance()}" }
    }
    addTest("The predecessors of $traceElement should be final") {
        runTrace(trace, elementIdx)
        assertTrue(
            traceElement.getNodeInstance()!!.predecessors.map {
                getNodeInstance(it)
            }.all { it.state.isFinal },
            "All immediate predecessors of an end node should be final"
        )
    }
    addTest("End node ${trace[elementIdx]} should be finished") {
        runTrace(trace, elementIdx+1) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }

}

private fun <T> Boolean.pick(onTrue:T, onFalse: T): T =
    if (this) onTrue else onFalse

fun createInvalidTraceTest(config: TraceTest.CompanionBase, trace: Trace, failureExpected:Boolean = true): DynamicContainer {
    val label = when(failureExpected) {
        true -> "Given invalid trace [${trace.joinToString()}]"
        else -> "Executing using the invalid trace testing mechanism"
    }
    return config.dynamicContainer(label) {
        addTest("Executing the trace should ${failureExpected.pick("fail", "not fail")}") {
            var success = false
            try {
                val instanceSupport = object : InstanceSupport {
                    override val transaction: StubProcessTransaction get() = this@addTest.transaction
                    override val engine: ProcessEngine<StubProcessTransaction, *>
                        get() = engineData.engine

                }
                instanceSupport.testTraceExceptionThrowing(getProcessInstance(), trace)
            } catch (e: ProcessTestingException) {
                if (!failureExpected) {
                    throw e
                }
                success = true
            }
            if (failureExpected && !success) kfail(
                "The invalid trace ${trace.joinToString(prefix = "[", postfix = "]")} could be executed"
            )
        }
        if (!failureExpected) {
            addTest("The process instance should have a finished state") {
                runTrace(trace)
                assertEquals(ProcessInstance.State.FINISHED, getProcessInstance().state)
            }
        }
    }
}

fun ProcessNodeInstance<*>.assertFinished() {
    assertTrue(this.state.isFinal) { "The node instance state should be final (but is ${state})" }
    assertTrue(this.node !is EndNode) { "Completed nodes should not be endnodes" }
}

inline fun TraceTest.CompanionBase.dynamicContainer(
    displayName: String,
    configure: ContainerContext.() -> Unit
): DynamicContainer {
    val children = mutableListOf<DynamicNode>()
    ContainerContext(this, children).apply(configure)
    return dynamicContainer(displayName, children)
}
