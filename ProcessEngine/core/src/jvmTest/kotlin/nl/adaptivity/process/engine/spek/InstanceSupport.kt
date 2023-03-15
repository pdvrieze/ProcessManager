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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.writer
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Created by pdvrieze on 15/01/17.
 */
interface InstanceSupport { // TODO add context type parameter
    val transaction: StubProcessTransaction<*>
    val engine: ProcessEngine<StubProcessTransaction<*>, *>

    fun ProcessInstance<*>.allChildren(): Sequence<ProcessNodeInstance<*,*>> {
        return transitiveChildren(this@InstanceSupport.transaction)
    }


    fun ProcessInstance<*>.trace(filter: (ProcessNodeInstance<*,*>)->Boolean) = trace(transaction, filter)

    val ProcessInstance<*>.trace: Trace get() = trace(transaction)

    fun ProcessInstance<*>.toDebugString():String {
        return toDebugString(this@InstanceSupport.transaction)
    }


    fun ProcessInstance<*>.assertTracePossible(trace: Trace) {
        (this as ProcessInstance<ActivityInstanceContext>).assertTracePossible(transaction as StubProcessTransaction<ActivityInstanceContext>, trace)
    }


    fun TraceElement.getNodeInstance(hInstance: Handle<SecureObject<ProcessInstance<*>>>): ProcessNodeInstance<*,*>? {
        val instance = transaction.readableEngineData.instance(hInstance).withPermission()
        return getNodeInstance(transaction, instance)
    }
}


fun ProcessInstance<*>.transitiveChildren(transaction: StubProcessTransaction<*>): Sequence<ProcessNodeInstance<*,*>> {
    return childNodes.asSequence().flatMap {
        when (val child = it.withPermission()) {
            is CompositeInstance ->
                if (child.hChildInstance.isValid) {
                    sequenceOf(child) +
                        transaction.readableEngineData
                            .instance(child.hChildInstance)
                            .withPermission().transitiveChildren(transaction)
                } else {
                    sequenceOf(child)
                }
            else -> sequenceOf(child)
        }
    }.filter { it.state != NodeInstanceState.SkippedInvalidated }
}

fun ProcessInstance<*>.toDebugString(transaction: StubProcessTransaction<*>): String {
    fun StringBuilder.appendChildNodeState(processInstance: ProcessInstance<*>) {
        processInstance.childNodes.asSequence()
            .map { it.withPermission() }
            .sortedBy { it.handle }
            .joinTo(this) {
                when (val inst = it.withPermission()) {
                    is CompositeInstance -> when {
                        !inst.hChildInstance.isValid ->
                            "${inst.node.id}[${inst.entryNo}]:(<Not started>) = ${inst.state}"

                        else -> {
                            val childStatus = buildString {
                                appendChildNodeState(
                                    transaction.readableEngineData.instance(inst.hChildInstance).withPermission()
                                )
                            }

                            "${inst.node.id}[${inst.entryNo}]:($childStatus) = ${inst.state}"
                        }
                    }
                    else -> "${inst.node.id}[${inst.entryNo}]:${inst.state}"
                }
            }
    }

    return buildString {
        append("process(")
        append(processModel.rootModel.name)
        name?.let {
            append(", instance: ")
            append(it)
        }
        append('[').append(handle).append("] - ").append(state)
        append(", allnodes: [")
        appendChildNodeState(this@toDebugString)

        appendLine("])\n\nModel:")
        XmlStreaming.newWriter(this.writer()).use { XML.encodeToWriter(it, processModel.rootModel) }
        appendLine("\n")
    }
}

fun ProcessInstance<*>.findChild(transaction: StubProcessTransaction<*>, id: String) = transitiveChildren(transaction).firstOrNull { it.node.id==id }
fun ProcessInstance<*>.findChild(transaction: StubProcessTransaction<*>, id: Identified) = findChild(transaction, id.id)

fun ProcessInstance<*>.trace(transaction: StubProcessTransaction<*>,
                           filter: (ProcessNodeInstance<*,*>) -> Boolean): Sequence<TraceElement> {
    return transitiveChildren(transaction)
        .map { it.withPermission() }
        .filter(filter)
        .sortedBy { handle.handleValue }
        .map { TraceElement(it.node.id, it.entryNo) }
}

fun ProcessInstance<*>.trace(transaction: StubProcessTransaction<*>): Array<TraceElement> {
    return trace(transaction) { true }
        .toList()
        .toTypedArray()
}

fun <C : ActivityInstanceContext> ProcessInstance<C>.assertTracePossible(transaction: StubProcessTransaction<C>,
                                         trace: Trace) {
    val nonSeenChildNodes = this.childNodes.asSequence()
        .map(SecureObject<ProcessNodeInstance<*, C>>::withPermission)
        .filter { it.state.isFinal &&
            ! (it.state.isSkipped || it.state == NodeInstanceState.AutoCancelled)
        }
        .toMutableSet()

    var nonFinal: ProcessNodeInstance<*,C>? = null
    for(traceElementPos in trace.indices) {
        val traceElement = trace[traceElementPos]
        val nodeInstance = traceElement.getNodeInstance(transaction, this)?.takeIf { it.state.isFinal }
        if (nodeInstance != null) {
            if(nonFinal!=null) {
                kfail("Found gap in the trace [${trace.joinToString()}]#$traceElementPos before node: $nodeInstance - ${toDebugString(transaction)}")
            }
            nonSeenChildNodes.remove(nodeInstance)
        } else {
            nonFinal = nodeInstance
        }
    }
    if (! state.isFinal) {
        for (otherChild in nonSeenChildNodes.toList()) {
            if (otherChild.node is StartNode) {
                val successors = getDirectSuccessors(transaction.readableEngineData, otherChild)
                    .map { getChildNodeInstance(it) }

                if (successors.all { it.state.isActive && it.state!=NodeInstanceState.Started }) {
                    nonSeenChildNodes.remove(otherChild)
                }
            }
        }
    }

    if (nonSeenChildNodes.isNotEmpty()) {
        kfail("All actual child nodes should be in the full trace or skipped. Nodes that were not seen: [${nonSeenChildNodes.joinToString()}]" )
    }
}

fun  ProcessInstance<*>.assertComplete(transaction: StubProcessTransaction<*>, vararg nodeIds: String) {
    val complete = transitiveChildren(transaction)
        .filter { it.state.isFinal && it.node is EndNode }
        .mapNotNull { nodeInstance ->
            assertTrue(nodeInstance.state.isFinal) {
                "The node instance state should be final (but is ${nodeInstance.state})"
            }
            assertTrue(nodeInstance.node is EndNode, "Completion nodes should be EndNodes")
            if (nodeInstance.state.isSkipped) null else nodeInstance.node.id
        }.sorted().toList()
    Assertions.assertEquals(nodeIds.sorted(), complete) {
        "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], " +
            "found: [${complete.joinToString()}], ${this.toDebugString(transaction)})"
    }
}
