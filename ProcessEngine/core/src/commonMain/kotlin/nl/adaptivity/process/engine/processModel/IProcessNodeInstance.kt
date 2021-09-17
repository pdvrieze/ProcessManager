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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.Result
import nl.adaptivity.process.engine.impl.Source
import nl.adaptivity.process.engine.impl.dom.newReader
import nl.adaptivity.process.engine.impl.dom.newWriter
import nl.adaptivity.process.engine.impl.generateXmlString
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xml.WritableCompactFragment
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.filterSubstream
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 * Simple base interface for process node instances that can also be implemented by builders
 */
interface IProcessNodeInstance: ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>>, ActivityInstanceContext {
    override val node: ExecutableProcessNode
    val predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
    override val owner: Principal

    override val handle: Handle<SecureObject<ProcessNodeInstance<*>>>
    val hProcessInstance: Handle<SecureObject<ProcessInstance>>

    val entryNo: Int
    override val state: NodeInstanceState
    val results: List<ProcessData>

    fun builder(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance.Builder<*, *>

    fun build(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance<*> = builder(processInstanceBuilder).build()

    fun condition(engineData: ProcessEngineDataAccess, predecessor: IProcessNodeInstance) =
        node.evalCondition(engineData, predecessor, this)

    fun resolvePredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ProcessNodeInstance<*>? {
        val handle = getPredecessor(engineData, nodeName)
            ?: throw NullPointerException("Missing predecessor with name $nodeName referenced from node ${node.id}")
        return engineData.nodeInstances[handle]?.withPermission()
    }

}

private fun IProcessNodeInstance.getPredecessor(
    engineData: ProcessEngineDataAccess,
    nodeName: String
                                               ): Handle<SecureObject<ProcessNodeInstance<*>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    predecessors
        .asSequence()
        .map { engineData.nodeInstance(it).withPermission() }
        .forEach {
            if (nodeName == it.node.id) {
                return it.handle
            } else {
                val result = it.getPredecessor(engineData, nodeName)
                if (result != null) {
                    return result
                }
            }
        }
    return null
}


fun ActivityInstanceContext.getDefines(engineData: ProcessEngineDataAccess): List<ProcessData> {
    return node.defines.map {
        it.applyData(engineData, this)
    }
}

fun ActivityInstanceContext.instantiateXmlPlaceholders(
    engineData: ProcessEngineDataAccess,
    source: Source,
    result: Result,
    localEndpoint: EndpointDescriptor
                              ) {
    instantiateXmlPlaceholders(engineData, source.newReader(), result.newWriter(), true, localEndpoint)
}

fun ActivityInstanceContext.instantiateXmlPlaceholders(
    engineData: ProcessEngineDataAccess,
    source: Source,
    removeWhitespace: Boolean,
    localEndpoint: EndpointDescriptor
                              ): ICompactFragment {
    val xmlReader = source.newReader()
    return instantiateXmlPlaceholders(engineData, xmlReader, removeWhitespace, localEndpoint)
}

fun ActivityInstanceContext.instantiateXmlPlaceholders(
    engineData: ProcessEngineDataAccess,
    xmlReader: XmlReader,
    removeWhitespace: Boolean,
    localEndpoint: EndpointDescriptor
                              ): WritableCompactFragment {
    val charArray = generateXmlString(true) { writer ->
        instantiateXmlPlaceholders(engineData, xmlReader, writer, removeWhitespace, localEndpoint)
    }

    return WritableCompactFragment(emptyList(), charArray)
}

fun ActivityInstanceContext.instantiateXmlPlaceholders(
    engineData: ProcessEngineDataAccess,
    xmlReader: XmlReader,
    out: XmlWriter,
    removeWhitespace: Boolean,
    localEndpoint: EndpointDescriptor
                                                     ) {
    val defines = getDefines(engineData)
    val pni = engineData.nodeInstance(handle).withPermission()
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
