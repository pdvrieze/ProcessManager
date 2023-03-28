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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.process.engine.spek.*
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.assertJsonEquals
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.URI
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.random.Random
import kotlin.reflect.KClass

@Execution(ExecutionMode.CONCURRENT)
abstract class TraceTest(val config: ConfigBase) {

    val model: ExecutableProcessModel get() = config.modelData.model

    @Suppress("unused")
    fun newEngineData() = config.modelData.engineData()

    val validTraces get() = config.modelData.valid
    val inValidTraces get() = config.modelData.invalid

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
            return serializeToFormat(config.expectedXml, XML { autoPolymorphic = true; indent=4 }, "getExpectedXml")
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
                        val actual = format.encodeToString(model)
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
                        assertEquals(model, actual) {
                            "They should be equal"
                        }
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
                is XML -> {
                    assertXmlEquals(expected, actual, { message })
                }

                else -> assertEquals(expected, actual, message)
            }
        }

        @Test
        @DisplayName("Round trip serialization to xml and back should be correct.")
        fun testRoundTrip() {
            val xml = XML { autoPolymorphic = true }
            val serialized = xml.encodeToString(model)
            val deSerialized = xml.decodeFromString<ExecutableProcessModel>(serialized)
            assertEquals(model, deSerialized, "The result of deserialization should be equal to the original")
            val reserialized = xml.encodeToString(deSerialized)
            assertEquals(serialized, reserialized, "Serializing and back should have equal result")
        }
    }

    @TestFactory
    @DisplayName("Valid traces")
    open fun testValidTraces(): List<DynamicNode> {
        return validTraces.mapIndexed { idx, trace -> createValidTraceTest(config, trace, idx) }
    }

    @TestFactory
    @DisplayName("Fuzz tests")
    open fun testFuzzTests(): List<DynamicNode> {
        val random = Random(config.hashCode())
        return (1..100).map { createFuzzTest(config, random.nextLong()) }
    }

    @TestFactory
    @DisplayName("Invalid traces")
    open fun testInvalidTraces(): List<DynamicNode> {
        return inValidTraces.mapIndexed { idx, trace -> createInvalidTraceTest(config, trace, idx) }
    }

    abstract class ConfigBase {

        abstract val modelData: ModelData

        open val expectedJson: String? get() = null
        open val expectedXml: String? get() = null


        fun ConfigurableNodeContainer<*>.activity(predecessor: Identified): MessageActivity.Builder =
            MessageActivityBase.Builder().apply {
                this.message = DummyMessage
                this.predecessor = predecessor
            }


        fun ConfigurableNodeContainer<*>.activity(
            predecessor: Identified,
            config: @ConfigurationDsl MessageActivity.Builder.() -> Unit
        ): MessageActivity.Builder = activity(predecessor).apply(config)


    }
}

class TestContext(private val config: TraceTest.ConfigBase) {

//    val model: ExecutableProcessModel get() = modelData.model
//    fun newEngineData() = modelData.engineData()
//    val validTraces get() = modelData.valid
//    val inValidTraces get() = modelData.invalid

    val engineData = config.modelData.engineData()
    val transaction = engineData.engine.startTransaction()
    val model get() = config.modelData.model
    val principal get() = model.owner

    val hmodel: Handle<ExecutableProcessModel> = when {
        model.handle.isValid &&
            model.handle in transaction.readableEngineData.processModels &&
            transaction.readableEngineData.processModels[model.handle]?.withPermission()?.uuid == model.uuid
        -> model.handle

        else -> {
            model.setHandleValue(-1)
            engineData.engine.addProcessModel(transaction, model.builder(), model.owner).handle
        }
    }

    var hInstance: PIHandle =
        Handle.invalid()
        get() {
            if (!field.isValid) {
                field = startProcess()
            }
            return field
        }
        private set

    val instanceUuid: UUID = UUID.randomUUID()

    fun dbgInstance(): String {
        return getProcessInstance().toDebugString(transaction)
    }

    fun startProcess(): PIHandle {
        val name = "${model.name} instance"
        hInstance = engineData.engine.startProcess(transaction, principal, hmodel, name, instanceUuid, null)
        return hInstance
    }

    fun getProcessInstance(instanceHandle: PIHandle = hInstance): ProcessInstance {
        return transaction.readableEngineData.instance(instanceHandle).withPermission()
    }

    fun TraceElement.getNodeInstance(): ProcessNodeInstance<*, *>? {
        return getNodeInstance(transaction, getProcessInstance())
    }

    fun getNodeInstance(handle: PNIHandle): ProcessNodeInstance<*, *> {
        return transaction.readableEngineData.nodeInstance(handle).withPermission()
    }

    inline fun updateNodeInstance(
        traceElement: TraceElement,
        instanceHandle: PIHandle = hInstance,
        crossinline action: ProcessNodeInstance.Builder<out ExecutableProcessNode, *, *>.() -> Unit
    ) {

        val nodeInstance = traceElement.getNodeInstance(transaction, getProcessInstance(instanceHandle))
            ?: fail("Missing node instance for $traceElement")

        transaction.writableEngineData.updateInstance(nodeInstance.hProcessInstance) {
            updateChild(nodeInstance.handle, action)
        }
    }

    fun updateNodeInstance(
        nodeInstanceHandle: PNIHandle,
        action: ProcessNodeInstance.Builder<out ExecutableProcessNode, *, *>.() -> Unit
    ) {
        transaction.writableEngineData.updateNodeInstance(nodeInstanceHandle, action)
    }

    fun runFuzz(random: Random, maxIters: Int = 1000): List<TraceElement> {


        val nodeData = mutableMapOf<String, MutableSet<TraceElement>>()
        for (trace in config.modelData.valid) {
            for (element in trace) {
                nodeData.getOrElse(element.nodeId) {
                    mutableSetOf<TraceElement>().also { nodeData[element.nodeId] = it }
                }.add(element)
            }
        }

        val trace = mutableListOf<TraceElement>()

        try {
            for (iter in 1..maxIters) {
                val processInstance = getProcessInstance()
                processInstance.allDescendentNodeInstances(transaction.readableEngineData)
                    .asSequence()
                    .filter { completed ->
                        completed.state.run { isFinal && !isSkipped } && trace.none {
                            it.id == completed.node.id && (it.instanceNo < 0 || it.instanceNo == completed.entryNo)
                        }
                    }
                    .shuffled(random)
                    .mapTo(trace) { it.toTraceElement() }

                val activeNodes = processInstance.allDescendentNodeInstances(transaction.readableEngineData)
                    .filter {
                        it.state.isActive && when (it.node) {
                            is Join,
                            is Split,
                            is CompositeActivity -> false

                            else -> true
                        }
                    }
                    .toList()
                if (activeNodes.isEmpty()) break

                var nextNodeInstance = activeNodes.random(random)
                val nextPayload = nodeData[nextNodeInstance.node.id]?.random(random)?.resultPayload
                val nextTraceElement = nextNodeInstance.toTraceElement(nextPayload)

                trace.add(nextTraceElement)

                var shouldBeComplete = true

                when (nextNodeInstance.node) {
                    is MessageActivity -> {
                        updateNodeInstance(nextNodeInstance.handle) {
                            startTask(transaction.writableEngineData)
                        }
                        updateNodeInstance(nextNodeInstance.handle) {
                            finishTask(transaction.writableEngineData, nextTraceElement.resultPayload)
                        }
                        nextNodeInstance = getNodeInstance(nextNodeInstance.handle)
                    }

                    is CompositeActivity -> {
                        if (nextNodeInstance.state == NodeInstanceState.Started) {
                            val childInstanceHandle = when (nextNodeInstance) {
                                is CompositeInstance<*> -> nextNodeInstance.hChildInstance
                                is CompositeInstance.Builder<*> -> nextNodeInstance.hChildInstance
                                else -> throw UnsupportedOperationException("Composite activity with unexpected instance type")
                            }
                            assertTrue(
                                childInstanceHandle.isValid,
                                "When fuzzing sees a composite activity, the child should be started"
                            )

                            updateNodeInstance(nextNodeInstance.handle) {
                                startTask(transaction.writableEngineData)
                            }
                            shouldBeComplete = false
                        }
                    }
                }

                if (shouldBeComplete) {
                    assertEquals(NodeInstanceState.Complete, nextNodeInstance.state) {
                        "Expected the state of $nextNodeInstance to be complete, not ${nextNodeInstance.state}\n${dbgInstance()}"
                    }
                }

                engineData.engine.processTickleQueue(transaction)
            }
        } catch (e: ProcessTestingException) {
            throw fuzzException(e, trace)
        } catch (e: ProcessException) {
            throw fuzzException(e, trace)
        }

        for (childNode in getProcessInstance().allDescendentNodeInstances(transaction.readableEngineData)) {
            if (childNode.state.isSkipped) {
                trace.removeIf { it.nodeId == childNode.node.id && it.instanceNo < 0 || it.instanceNo == childNode.entryNo }
            }
        }

        return trace
    }

    fun fuzzException(cause: Throwable?, trace: List<TraceElement>): FuzzException {
        val message =
            "Error in fuzzing${cause?.message?.let { ": $it - " ?: ", " }} - trace: [${trace.joinToString()}]}\n    -${dbgInstance()}"
        return FuzzException(message, cause, trace)
    }

    fun fuzzException(trace: List<TraceElement>): FuzzException {
        return fuzzException(null, trace)
    }

    fun runTrace(
        trace: Trace,
        lastElement: Int = -1,
        instanceHandle: PIHandle = hInstance
    ): PNIHandle {
        var lastInstance: PNIHandle = Handle.invalid()
        for (idx in 0 until (if (lastElement < 0) trace.size else lastElement)) {
            val traceElement = trace[idx]
            when (model.findNode(traceElement)) {
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
            assertEquals(NodeInstanceState.Complete, ni.state) {
                "Expected the state of $ni to be complete, not ${ni.state}\n${dbgInstance()}"
            }
            lastInstance = ni.handle

            engineData.engine.processTickleQueue(transaction)
        }
        return lastInstance
    }

    inline fun <reified T : ProcessNode> Trace.nodes(): Sequence<T> {
        return asSequence().map { model.findNode(it) }.filterIsInstance<T>()
    }

    @Suppress("unused")
    inline fun <reified T : ProcessNode> Trace.nodeInstances(): Sequence<ProcessNodeInstance<*, *>?> {
        return asSequence()
            .filter { model.findNode(it) is T }
            .map { traceElement ->
                traceElement.getNodeInstance(transaction, getProcessInstance())
            }
    }

    inline fun <reified T : ProcessNode> Trace.allNodeInstances(): Sequence<ProcessNodeInstance<*, *>> {
        return allNodeInstances(T::class)
    }

    fun Trace.allNodeInstances(type: KClass<*>): Sequence<ProcessNodeInstance<*, *>> {
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

class ContainerContext(val config: TraceTest.ConfigBase, private val children: MutableCollection<DynamicNode>) {

    val model get() = config.modelData.model

    fun addTest(node: DynamicNode) {
        children.add(node)
    }

    inline fun addTest(displayName: String, crossinline executable: TestContext.() -> Unit) {
        addTest(dynamicTest(displayName) { TestContext(config).executable() })
    }

    @Suppress("unused")
    inline fun addTest(displayName: String, testSourceUri: URI, crossinline executable: TestContext.() -> Unit) {
        addTest(dynamicTest(displayName, testSourceUri) { TestContext(config).executable() })
    }

    inline fun dynamicContainer(displayName: String, configure: ContainerContext.() -> Unit) {
        val children = mutableListOf<DynamicNode>()
        ContainerContext(config, children).apply(configure)
        addTest(dynamicContainer(displayName, children))
    }

}

fun createFuzzTest(config: TraceTest.ConfigBase, seed: Long, expectSuccess: Boolean = true): DynamicContainer {
    return config.dynamicContainer("Fuzzing with seed $seed") {

        if (expectSuccess) {
            addTest("Fuzzing should not throw an exception") {
                val trace = runFuzz(Random(seed)).toTypedArray()
                System.out.println("Fuzz $seed has trace [${trace.joinToString()}]")

                if (config.modelData.valid.none { validTrace -> validTrace.contentEquals(trace) }) {
                    System.err.println(
                        """|
                    |    Found unknown valid trace:
                    |        ${trace.joinToString()}
                    |""".trimMargin()
                    )
                }
            }

            addTest("Fuzzed traces should finish the instance") {
                val trace = try {
                    runFuzz(Random(seed))
                } catch (e: FuzzException) {
                    e.trace
                }
                val processInstance = getProcessInstance()
                assertEquals(ProcessInstance.State.FINISHED, processInstance.state) {
                    "The process instance should be finished ${dbgInstance()}"
                }
                val nonFinishedNodes = processInstance.allChildNodeInstances()
                    .filterNot { it.state == NodeInstanceState.Complete || it.state.isSkipped }
                    .map { "${it.toTraceElement()}-${it.state}" }
                    .sorted()
                    .toList()

                assertEquals(emptyList<String>(), nonFinishedNodes) {
                    "There should not be nodes in the trace that aren't finished."
                }

            }
        } else {
            addTest("Fuzzing should throw an exception") {
                var trace: List<TraceElement> = emptyList()
                assertThrows<FuzzException>({ "Expected exception, trace: [${trace.joinToString()}], ${dbgInstance()}" }) {
                    trace = runFuzz(Random(seed))
                    val pi = getProcessInstance()
                    if (pi.state!= ProcessInstance.State.FINISHED) {
                        throw FuzzException("The process shouldn't have finished: ${dbgInstance()}", trace)
                    }
                    System.out.println("Fuzz $seed has trace [${trace.joinToString()}]")
                }.trace.also { trace = it }

                if (config.modelData.invalid.none { invalidTrace ->
                    val comparator = trace.subList(0, minOf(trace.size, invalidTrace.size)).toTypedArray()
                    invalidTrace.contentEquals(comparator)
                }) {
                    System.err.println(
                        """|
                    |    Found unknown invalid trace:
                    |        ${trace.joinToString()}
                    |""".trimMargin()
                    )
                }
            }

        }
    }
}

fun createValidTraceTest(config: TraceTest.ConfigBase, trace: Trace, traceNo: Int): DynamicContainer {
    return config.dynamicContainer("For valid trace #$traceNo [${trace.joinToString()}]") {
        addTest("After starting only start nodes should be finished") {
            val processInstance = getProcessInstance()

            val predicate: (ProcessNodeInstance<*, *>) -> Boolean =
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

                val actualFinishedNodes = getProcessInstance().transitiveChildren(transaction)
                    .map { it.withPermission() }
                    .filter { it.state.isFinal && it.node !is EndNode }
                    .onEach(ProcessNodeInstance<*, *>::assertFinished)
                    .filterNot { it.state.isSkipped }
                    .map { TraceElement(it.node.id, it.entryNo) }
                    .sorted()
                    .toList()

                assertEquals(expectedFinishedNodes, actualFinishedNodes) {
                    "\"The list of finished nodes does not match (" +
                        "Expected: [${expectedFinishedNodes.joinToString()}], " +
                        "found: [${actualFinishedNodes.joinToString()}])\""
                }
            }
            addTest("No nodes are active") {
                runTrace(trace)
                val activeNodes = getProcessInstance().activeNodes
                    .filterNot { it.node is Join && it.state == NodeInstanceState.Pending }
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
        addTest(createInvalidTraceTest(config, trace, traceNo, false))
    }
}

fun ContainerContext.createTraceElementTest(trace: Trace, elementIdx: Int) {
    val node = model.findNode(trace[elementIdx])
        ?: kfail("Node ${trace[elementIdx]} not found in the model")

    when (node) {
        is StartNode         -> createStartElementTest(trace, elementIdx)
        is Split             -> createSplitElementTest(trace, elementIdx)
        is Join              -> createJoinElementTest(trace, elementIdx)
        is MessageActivity   -> createMessageElementTest(trace, elementIdx, node)
        is CompositeActivity -> createCompositeElementTest(trace, elementIdx)
        is EndNode           -> createEndElementTest(trace, elementIdx)
        else                 -> kfail("Unexpected node type found: ${node.javaClass}")
    }
}

private fun ContainerContext.createStartElementTest(trace: Trace, elementIdx: Int) {
    addTest("Start node ${trace[elementIdx]} should be finished") {
        runTrace(trace, elementIdx) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }
    addTest("Start node ${trace[elementIdx]} should still be finished on completion") {
        runTrace(trace, elementIdx + 1) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }
}

private fun ContainerContext.createSplitElementTest(trace: Trace, elementIdx: Int) {
    val traceElement = trace[elementIdx]
    addTest("Split $traceElement should already be finished") {
        runTrace(trace, elementIdx)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state) {
            "Node $traceElement should be finished. The current nodes are: ${dbgInstance()}"
        }
    }
}

private fun ContainerContext.createJoinElementTest(trace: Trace, elementIdx: Int) {
    val traceElement = trace[elementIdx]
    addTest("Join $traceElement is finished or can finish") {
        runTrace(trace, elementIdx)
        val pni = traceElement.getNodeInstance()
            ?: fail("An element for ${traceElement} shoulld exist")

        val activePredecessors =
            getProcessInstance().getActivePredecessorsFor(transaction.readableEngineData, pni as JoinInstance<ActivityInstanceContext>)

        if (!(activePredecessors.isEmpty() && pni.canFinish())) {
            assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state) {
                "There are still active predecessors $activePredecessors for $traceElement;${dbgInstance()}"
            }
        }
    }
    addTest("Join $traceElement should be finished afterwards") {
        runTrace(trace, elementIdx + 1)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state) {
            "Node $traceElement should be finished. The current nodes are: ${dbgInstance()}"
        }
    }

}

private fun ContainerContext.createMessageElementTest(trace: Trace, elementIdx: Int, node: MessageActivity) {
    val traceElement = trace[elementIdx]
    addTest("Activity node $traceElement should not be be finished before completion") {
        runTrace(trace, elementIdx) // just before the element
        val nodeInstance = traceElement.getNodeInstance()!!
        assertFalse(nodeInstance.state.isFinal) {
            "Node ${nodeInstance} should not be final"
        }
    }
    addTest("Activity node $traceElement should be finished on completion") {
        runTrace(trace, elementIdx + 1) // just before the element
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
        nodeInstance = transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
            startTask(transaction.writableEngineData)
        }.withPermission()
        val finalNodeInstance = traceElement.getNodeInstance()!!
        assertEquals(finalNodeInstance, nodeInstance) {
            "A node from the data access and returned from update should be the same."
        }

        assertTrue(nodeInstance.state.isCommitted) {
            "The instance state was ${getProcessInstance(nodeInstance.hProcessInstance).toDebugString(transaction)}"
        }
        assertEquals(NodeInstanceState.Started, nodeInstance.state)
    }
    addTest("the node instance $traceElement should be final after finishing") {
        runTrace(trace, elementIdx)
        run {
            val nodeInstance = traceElement.getNodeInstance()!!
            transaction.writableEngineData.updateNodeInstance(nodeInstance.handle) {
                finishTask(transaction.writableEngineData, traceElement.resultPayload)
            }
        }
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state)
    }
}

private fun ContainerContext.createCompositeElementTest(trace: Trace, elementIdx: Int) {
    val traceElement = trace[elementIdx]
    addTest("A child instance should have been created for $traceElement") {
        runTrace(trace, elementIdx)
        assertTrue((traceElement.getNodeInstance() as CompositeInstance).hChildInstance.isValid) {
            "No child instance was recorded"
        }
    }

    addTest("The child instance was finished for $traceElement") {
        runTrace(trace, elementIdx + 1)
        val nodeInstance = traceElement.getNodeInstance() as CompositeInstance
        val childInstance = getProcessInstance(nodeInstance.hChildInstance)

        assertEquals(ProcessInstance.State.FINISHED, childInstance.state)
    }

    addTest("The activity itself should be finished for $traceElement") {
        runTrace(trace, elementIdx + 1)
        assertEquals(NodeInstanceState.Complete, traceElement.getNodeInstance()?.state)
    }

}

private fun ContainerContext.createEndElementTest(trace: Trace, elementIdx: Int) {
    val traceElement = trace[elementIdx]
    addTest("$traceElement should be part of the completion nodes") {
        runTrace(trace, elementIdx)
        val parentInstance = getProcessInstance(traceElement.getNodeInstance()!!.hProcessInstance)
        assertTrue(
            parentInstance.completedNodeInstances.asSequence()
                .map { it.withPermission() }
                .any { it.node.id == traceElement.nodeId }
        ) { "The end node should be complete; ${dbgInstance()}" }
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
        runTrace(trace, elementIdx + 1) // just before the element
        assertEquals(NodeInstanceState.Complete, trace[elementIdx].getNodeInstance()?.state)
    }

}

private fun <T> Boolean.pick(onTrue: T, onFalse: T): T =
    if (this) onTrue else onFalse

fun createInvalidTraceTest(
    config: TraceTest.ConfigBase,
    trace: Trace,
    traceIdx: Int,
    failureExpected: Boolean = true,
    label: String = when (failureExpected) {
        true -> "Given invalid trace #$traceIdx [${trace.joinToString()}]"
        else -> "Executing using the invalid trace testing mechanism"
    }
): DynamicContainer {
    return config.dynamicContainer(label) {
        addTest("Executing the trace should ${failureExpected.pick("fail", "not fail")}") {
            var success = false
            try {
                val instanceSupport = object : InstanceSupport {
                    override val transaction: StubProcessTransaction get() = this@addTest.transaction
                    override val engine: ProcessEngine<StubProcessTransaction, ActivityInstanceContext>
                        get() = engineData.engine

                }
                instanceSupport.testTraceExceptionThrowing(hInstance, trace)
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

fun ProcessNodeInstance<*, *>.assertFinished() {
    assertTrue(this.state.isFinal) { "The node instance state should be final (but is $state)" }
    assertTrue(this.node !is EndNode) { "Completed nodes should not be endnodes" }
}

inline fun TraceTest.ConfigBase.dynamicContainer(
    displayName: String,
    configure: ContainerContext.() -> Unit
): DynamicContainer {
    val children = mutableListOf<DynamicNode>()
    ContainerContext(this, children).apply(configure)
    return dynamicContainer(displayName, children)
}

fun IProcessNodeInstance.toTraceElement(payload: CompactFragment? = null): TraceElement {
    val instanceNo = when {
        node.isMultiInstance -> entryNo
        (node as? Join)?.isMultiMerge == true -> entryNo
        else -> SINGLEINSTANCE
    }
    return TraceElement(node.id, instanceNo, payload)
}

class FuzzException(override val message: String?, cause: Throwable?, val trace: List<TraceElement>):
    Exception("Error in fuzzing${cause?.message?.let { ": $it - " } ?:", "}trace: [${trace.joinToString()}]}\n    -$", cause) {
        constructor(cause: Throwable, trace: List<TraceElement>) : this(
            "Error in fuzzing: ${cause.message} - trace: [${trace.joinToString()}]}",
            cause,
            trace
        )

        constructor(message: String, trace: List<TraceElement>) : this(
            "Error in fuzzing: trace: [${trace.joinToString()}]}",
            null,
            trace
        )
    }

fun IProcessInstance.allDescendentNodeInstances(engineData: ProcessEngineDataAccess<*>): List<IProcessNodeInstance> {
    val result = mutableListOf<IProcessNodeInstance>()
    val procQueue = ArrayDeque<IProcessInstance>().also { it.add(this) }
    while (procQueue.isNotEmpty()) {
        val inst = procQueue.removeFirst()
        for (nodeInst in inst.allChildNodeInstances()) {
            result.add(nodeInst)
            when (nodeInst) {
                is CompositeInstance<*> -> procQueue.add(engineData.instance(nodeInst.hChildInstance).withPermission())
                is CompositeInstance.Builder<*> -> procQueue.add(engineData.instance(nodeInst.hChildInstance).withPermission())
            }
        }
    }

    return result
}
