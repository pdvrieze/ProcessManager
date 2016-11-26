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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.StartNodeImpl
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.CharArrayWriter
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.transform.Result
import javax.xml.transform.Source

@XmlDeserializer(ProcessNodeInstance.Factory::class)
open class ProcessNodeInstance<T : ProcessTransaction<T>>(node: ExecutableProcessNode,
                                                          predecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
                                                          val hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance<T>>>,
                                                          owner: Principal,
                                                          handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
                                                          override final val state: NodeInstanceState = NodeInstanceState.Pending,
                                                          results: Iterable<ProcessData> = emptyList(),
                                                          val failureCause: Throwable? = null) : IProcessNodeInstance<T, ProcessNodeInstance<T>>, SecureObject<ProcessNodeInstance<T>>, HandleMap.ReadableHandleAware<SecureObject<ProcessNodeInstance<T>>> {


  @Suppress("CanBePrimaryConstructorProperty")
  open val node: ExecutableProcessNode = node

  override val handle: ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance<T>>> = Handles.handle(handle)

  val results: List<ProcessData> = results.toList()

  val directPredecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  override val owner: Principal = owner

  interface Builder<T:ProcessTransaction<T>, N:ExecutableProcessNode> {
    var node: N
    var predecessors: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>
    var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance<T>>>
    var owner: Principal
    var handle: Handle<out SecureObject<ProcessNodeInstance<T>>>
    var state: NodeInstanceState
    val results:MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance
    var failureCause: Throwable?
    fun  build(): ProcessNodeInstance<T>
  }

  abstract class AbstractBuilder<T:ProcessTransaction<T>, N:ExecutableProcessNode> :Builder<T, N> {

    override fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<IProcessNodeInstance<*,*>>(it.handleValue) },
                                    processInstance = hProcessInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    override var failureCause: Throwable? = null
  }

  abstract class ExtBuilderBase<T:ProcessTransaction<T>, N:ExecutableProcessNode>(base:ProcessNodeInstance<T>) : AbstractBuilder<T, N>() {
    override var predecessors = base.directPredecessors.toMutableArraySet()
    override var hProcessInstance by overlay { base.hProcessInstance }
    override var owner by overlay { base.owner }
    override var handle: Handle<out SecureObject<ProcessNodeInstance<T>>> by overlay { base.handle }
    override var state by overlay { base.state }
    override var results = base.results.toMutableList()
  }

  class ExtBuilder<T:ProcessTransaction<T>>(base:ProcessNodeInstance<T>) : ExtBuilderBase<T, ExecutableProcessNode>(base) {
    override var node: ExecutableProcessNode by overlay { base.node }
    override fun build() = ProcessNodeInstance(this)
  }

  open class BaseBuilder<T:ProcessTransaction<T>, N:ExecutableProcessNode>(
        override var node: N,
        predecessors: Iterable<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
        override var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance<T>>>,
        override var owner: Principal,
        override var handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
        override var state: NodeInstanceState = NodeInstanceState.Pending) : AbstractBuilder<T, N>() {

    override var predecessors :MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>> = predecessors.toMutableArraySet()

    override val results = mutableListOf<ProcessData>()

    override fun build() = ProcessNodeInstance(this)
  }

  class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance.deserialize(reader)
    }
  }

  init {
    if (node is StartNode<*, *>) {
      if (predecessors.any { it.valid }) throw IllegalArgumentException("Start nodes don't have (valid) predecessors.")
    } else {
      if (predecessors.asSequence().filter { it.valid }.firstOrNull()==null) {
        throw IllegalArgumentException("Nodes that are not startNodes need predecessors")
      }
    }
  }

  constructor(node: StartNodeImpl, processInstance: ProcessInstance<T>) : this(node, emptyList(), processInstance.handle, processInstance.owner)

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>) : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance.handle, processInstance.owner)

  constructor(builder:Builder<T, out ExecutableProcessNode>): this(builder.node, builder.predecessors, builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results, builder.failureCause)

  @Throws(SQLException::class)
  internal constructor(transaction: T, node: ExecutableProcessNode, processInstance: ProcessInstance<T>, state: NodeInstanceState)
        : this(node, resolvePredecessors(transaction, processInstance, node), processInstance.handle, processInstance.owner, state=state)

  @Throws(SQLException::class)
  constructor(transaction: T, processEngine: ProcessEngine<T>, nodeInstance: XmlProcessNodeInstance) :
        this(transaction, processEngine.getProcessInstance(
              transaction, Handles.handle<ProcessInstance<T>>(nodeInstance.processInstance),
              SecurityProvider.SYSTEMPRINCIPAL).processModel.getNode(nodeInstance.nodeId),
             processEngine.getProcessInstance(transaction,
                                              Handles.handle<ProcessInstance<T>>(nodeInstance.processInstance),
                                              SecurityProvider.SYSTEMPRINCIPAL),
             nodeInstance.state ?: throw NullPointerException("Missing state")) {
  }

  override fun withPermission() = this

  open fun builder(): Builder<T, out ExecutableProcessNode> = ExtBuilder(this)

  /** Update the node. This will store the update based on the transaction. It will return the new object. The old object
   *  may be invalid afterwards.
   */
  open fun update(transaction: T, body: Builder<T, out ExecutableProcessNode>.() -> Unit):ProcessNodeInstance<T> {
    val origHandle = handle
    return builder().apply { body() }.build().apply {
      if (origHandle.valid)
      if (handle.valid)
        transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  @Throws(SQLException::class)
  open fun tickle(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    return when (state) {
      NodeInstanceState.FailRetry,
      NodeInstanceState.Pending -> provideTask(transaction, messageService)
      else -> this
    }// ignore
  }

  @Throws(SQLException::class)
  override fun getResult(transaction: T, name: String): ProcessData? {
    return results.firstOrNull { name == it.name }
  }

  @Throws(SQLException::class)
  fun getDefines(transaction: T): List<ProcessData> {
    val result = ArrayList<ProcessData>()
    node.defines.forEach { define ->
      result.add(define.apply(transaction, this))
    }
    return result
  }

  private fun hasDirectPredecessor(handle: Handle<out SecureObject<ProcessNodeInstance<T>>>): Boolean {
    for (pred in directPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(transaction: T): Collection<ProcessNodeInstance<T>> {
    return directPredecessors.asSequence().map {
            transaction.readableEngineData.nodeInstances[it].mustExist(it).withPermission()
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(transaction: T, nodeName: String): Handle<out SecureObject<ProcessNodeInstance<T>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    directPredecessors
          .asSequence()
          .map { transaction.readableEngineData.nodeInstances[it].mustExist(it).withPermission() }
          .forEach {
            if (nodeName == it.node.id) {
              return it.handle
            } else {
              val result = it.getPredecessor(transaction, nodeName)
              if (result != null) {
                return result
              }
            }
          }
    return null
  }

  @Throws(SQLException::class)
  override fun resolvePredecessor(transaction: T, nodeName: String): ProcessNodeInstance<T>? {
    val handle = getPredecessor(transaction, nodeName) ?: throw NullPointerException("Missing predecessor with name ${nodeName} referenced from node ${node.id}")
    return transaction.readableEngineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  @Throws(SQLException::class)
  override fun <U> provideTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    try {
      val shouldProgress = node.provideTask(transaction, messageService, this)
      if (shouldProgress) {
        val newInstance = transaction.commit(update(transaction) { state = NodeInstanceState.Sent })
        return newInstance.takeTask(transaction, messageService)
      } else
      return this
    } catch (e: RuntimeException) {
      // TODO later move failretry to fail
      //      if (state!=TaskState.FailRetry) {
      failTaskCreation(transaction, e)
      //      }
      throw e
    }

  }

  @Throws(SQLException::class)
  override fun <U> takeTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    val result = node.takeTask(messageService, this)
    val newObj = update(transaction) { state = NodeInstanceState.Taken }

    return if (result) newObj.startTask(transaction, messageService) else newObj
  }

  @Throws(SQLException::class)
  override fun <U> startTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): ProcessNodeInstance<T> {
    val startNext = node.startTask(messageService, this)
    val updatedInstance = update(transaction) { state = NodeInstanceState.Started }
    return if (startNext) {
      val pi = transaction.readableEngineData.instances[hProcessInstance].mustExist(hProcessInstance).withPermission()
      pi.finishTask(transaction, messageService, updatedInstance, null)
    }else updatedInstance
  }

  @Throws(SQLException::class)
  override fun finishTask(transaction: T, resultPayload: Node?): ProcessNodeInstance<T> {
    return transaction.commit(update(transaction) {
      node.getResults().mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = NodeInstanceState.Complete
    })
  }

  @Throws(SQLException::class)
  override fun cancelTask(transaction: T): ProcessNodeInstance<T> {
    return update(transaction) { state = NodeInstanceState.Cancelled }
  }

  @Throws(SQLException::class)
  override fun tryCancelTask(transaction: T): ProcessNodeInstance<T> {
    try {
      return cancelTask(transaction)
    } catch (e: IllegalArgumentException) {
      logger.log(Level.WARNING, "Task could not be cancelled")
      return this
    }
  }

  override fun toString(): String {
    return node.javaClass.simpleName + " (" + state + ")"
  }

  @Throws(SQLException::class)
  override fun failTask(transaction: T, cause: Throwable): ProcessNodeInstance<T> {
    return update(transaction) {
      failureCause = cause
      state = if (state == NodeInstanceState.Pending) NodeInstanceState.FailRetry else NodeInstanceState.Failed
    }
  }

  @Throws(SQLException::class)
  override fun failTaskCreation(transaction: T, cause: Throwable): ProcessNodeInstance<T> {
    return transaction.commit(update(transaction) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    })
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T,
                                 source: Source,
                                 result: Result,
                                 localEndpoint: EndpointDescriptor) {
    instantiateXmlPlaceholders(transaction, source, true, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T,
                                 xmlReader: XmlReader,
                                 out: XmlWriter,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor) {
    val defines = getDefines(transaction)
    val transformer = PETransformer.create(ProcessNodeInstanceContext(this,
                                                                      defines,
                                                                      state == NodeInstanceState.Complete, localEndpoint),
                                           removeWhitespace)
    transformer.transform(xmlReader, out.filterSubstream())
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T,
                                 source: Source,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): CompactFragment {
    val xmlReader = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(transaction, xmlReader, removeWhitespace, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T,
                                 xmlReader: XmlReader,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): WritableCompactFragment {
    val caw = CharArrayWriter()

    val writer = XmlStreaming.newWriter(caw, true)
    instantiateXmlPlaceholders(transaction, xmlReader, writer, removeWhitespace, localEndpoint)
    writer.close()
    return WritableCompactFragment(emptyList<Namespace>(), caw.toCharArray())
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(transaction: T, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
    val builder = ExtBuilder(this)

    val body:CompactFragment? = (node as? Activity<*,*>)?.let { act ->
      try {
        val xmlReader = XMLFragmentStreamReader.from(act.getMessage().messageBody)
        instantiateXmlPlaceholders(transaction, xmlReader, true, localEndpoint)
      } catch (e: XmlException) {
        logger.log(Level.WARNING, "Error processing body", e)
        throw e
      }
    }

    return builder.toXmlInstance(body)
  }

  @Throws(XmlException::class)
  override fun serialize(transaction: T,
                         out: XmlWriter,
                         localEndpoint: EndpointDescriptor) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", hProcessInstance.handleValue)

      if (handle.valid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*,*>)?.getMessage()?.messageBody?.let { body ->
        instantiateXmlPlaceholders(transaction, XMLFragmentStreamReader.from(body), out, true, localEndpoint)
      }
    }
  }

  companion object {

    @Throws(XmlException::class)
    fun <T: ProcessTransaction<T>> deserialize(transaction: T, processEngine: ProcessEngine<T>, xmlReader: XmlReader)
          = ProcessNodeInstance(transaction, processEngine, XmlProcessNodeInstance.deserialize(xmlReader))

    private val logger by lazy { Logger.getLogger(ProcessNodeInstance::class.java.getName()) }

    @Throws(SQLException::class)
    private fun <T:ProcessTransaction<T>> resolvePredecessors(transaction: T,
                                    processInstance: ProcessInstance<T>,
                                    node: ExecutableProcessNode): List<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>> {

      return node.predecessors.asSequence()
            .map { processInstance.getNodeInstance(transaction, it) }
            .filterNotNull()
            .map { it.handle }
            .toList()
    }

    fun <T:ProcessTransaction<T>> build(node: ExecutableProcessNode,
                                        predecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
                                        processInstance: ProcessInstance<T>,
                                        handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
                                        state: NodeInstanceState = NodeInstanceState.Pending,
                                        body: Builder<T, ExecutableProcessNode>.() -> Unit):ProcessNodeInstance<T> {
      return ProcessNodeInstance(BaseBuilder(node, predecessors, processInstance.handle, processInstance.owner, handle, state).apply(body))
    }


  }

}
