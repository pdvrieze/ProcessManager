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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.generateXmlString
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xml.WritableCompactFragment
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.filterSubstream

/**
 * Simple base interface for process node instances that can also be implemented by builders
 */
interface IProcessNodeInstance<C: ActivityInstanceContext>: ReadableHandleAware<SecureObject<ProcessNodeInstance<*, *>>> {
    val node: ExecutableProcessNode
    val predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*, C>>>>
//    override val owner: PrincipalCompat
    val assignedUser: PrincipalCompat? get() = null

    override val handle: Handle<SecureObject<ProcessNodeInstance<*, C>>>

    val hProcessInstance: Handle<SecureObject<ProcessInstance<*>>>

    val entryNo: Int
    val state: NodeInstanceState
    val results: List<ProcessData>

    fun builder(processInstanceBuilder: ProcessInstance.Builder<C>): ProcessNodeInstance.Builder<*, ProcessNodeInstance<*, C>, C>

    fun build(processInstanceBuilder: ProcessInstance.Builder<C>): ProcessNodeInstance<*, C> {
        return builder(processInstanceBuilder).build()
    }

    fun isOtherwiseCondition(predecessor: IProcessNodeInstance<C>) = node.isOtherwiseCondition(predecessor.node)

    fun condition(
        nodeInstanceSource: IProcessInstance<C>,
        predecessor: IProcessNodeInstance<C>
    ) = node.evalCondition(nodeInstanceSource, predecessor, this)

    fun resolvePredecessor(nodeInstanceSource: IProcessInstance<C>, nodeName: String): IProcessNodeInstance<C>? {
        val handle = getPredecessor(nodeInstanceSource, nodeName)
            ?: throw NullPointerException("Missing predecessor with name $nodeName referenced from node ${node.id}")
        return nodeInstanceSource.getChildNodeInstance(handle)
    }

    fun getResult(name: String): ProcessData? {
        return results.firstOrNull { name == it.name }
    }

    fun createActivityContext(engineData: MutableProcessEngineDataAccess<C>): C =
        engineData.processContextFactory.newActivityInstanceContext(engineData, this)

}

private fun <C: ActivityInstanceContext> IProcessNodeInstance<C>.getPredecessor(
    nodeInstanceSource: IProcessInstance<C>,
    nodeName: String
): Handle<SecureObject<ProcessNodeInstance<*, C>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    predecessors.asSequence()
        .map { hPred -> nodeInstanceSource.getChildNodeInstance(hPred) }
        .forEach { predNode ->
            if (nodeName == predNode.node.id) {
                return predNode.handle
            } else {
                val result = predNode.getPredecessor(nodeInstanceSource, nodeName)
                if (result != null) {
                    return result
                }
            }
        }
    return null
}

fun <C: ActivityInstanceContext> C.getDefines(nodeInstanceSource: IProcessInstance<C>): List<ProcessData> {
    return node.defines.map {
        it.applyData(nodeInstanceSource, this)
    }
}

fun <C : ActivityInstanceContext> C.instantiateXmlPlaceholders(
    nodeInstanceSource: IProcessInstance<C>,
    xmlReader: XmlReader,
    removeWhitespace: Boolean,
    localEndpoint: EndpointDescriptor
): WritableCompactFragment {
    val charArray = generateXmlString(true) { writer ->
        instantiateXmlPlaceholders(nodeInstanceSource, xmlReader, writer, removeWhitespace, localEndpoint)
    }

    return WritableCompactFragment(emptyList<Namespace>(), charArray)
}

fun <C : ActivityInstanceContext> C.instantiateXmlPlaceholders(
    nodeInstanceSource: IProcessInstance<C>,
    xmlReader: XmlReader,
    out: XmlWriter,
    removeWhitespace: Boolean,
    localEndpoint: EndpointDescriptor
) {
    val defines = getDefines(nodeInstanceSource)
    val pni = nodeInstanceSource.getChildNodeInstance(nodeInstanceHandle)
    val transformer = PETransformer.create(
        ProcessNodeInstanceContext(
            pni,
            defines,
            state == NodeInstanceState.Complete, localEndpoint
        ),
        removeWhitespace
    )
    transformer.transform(xmlReader, out.filterSubstream())
}
