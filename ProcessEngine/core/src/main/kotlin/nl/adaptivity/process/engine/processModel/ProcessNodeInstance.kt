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
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableStartNode
import nl.adaptivity.process.processModel.engine.XmlStartNode
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
open class ProcessNodeInstance(node: ExecutableProcessNode,
                               predecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                               val hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
                               owner: Principal,
                               handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                               override final val state: NodeInstanceState = NodeInstanceState.Pending,
                               results: Iterable<ProcessData> = emptyList(),
                               val failureCause: Throwable? = null) : IExecutableProcessNodeInstance<ProcessNodeInstance>, SecureObject<ProcessNodeInstance>, ReadableHandleAware<SecureObject<ProcessNodeInstance>> {


  @Suppress("CanBePrimaryConstructorProperty")
  open val node: ExecutableProcessNode = node

  private var handle: ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance>>
        = Handles.handle(handle)

  override fun getHandle(): ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance>>
        = handle

  val results: List<ProcessData> = results.toList()

  val directPredecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  override val owner: Principal = owner

  interface Builder<N:ExecutableProcessNode> {
    var node: N
    var predecessors: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>
    var owner: Principal
    var handle: Handle<out SecureObject<ProcessNodeInstance>>
    var state: NodeInstanceState
    val results:MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance
    var failureCause: Throwable?
    fun  build(): ProcessNodeInstance
  }

  abstract class AbstractBuilder<N:ExecutableProcessNode> : Builder<N> {

    override fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<IProcessNodeInstance<*>>(it.handleValue) },
                                    processInstance = hProcessInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    override var failureCause: Throwable? = null
  }

  abstract class ExtBuilderBase<N:ExecutableProcessNode>(base:ProcessNodeInstance) : AbstractBuilder<N>() {
    override var predecessors = base.directPredecessors.toMutableArraySet()
    override var hProcessInstance by overlay { base.hProcessInstance }
    override var owner by overlay { base.owner }
    override var handle: Handle<out SecureObject<ProcessNodeInstance>> by overlay { base.handle }
    override var state by overlay { base.state }
    override var results = base.results.toMutableList()
  }

  class ExtBuilder(base:ProcessNodeInstance) : ExtBuilderBase<ExecutableProcessNode>(base) {
    override var node: ExecutableProcessNode by overlay { base.node }
    override fun build() = ProcessNodeInstance(this)
  }

  open class BaseBuilder<N:ExecutableProcessNode>(
        override var node: N,
        predecessors: Iterable<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
        override var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
        override var owner: Principal,
        override var handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
        override var state: NodeInstanceState = NodeInstanceState.Pending) : AbstractBuilder<N>() {

    override var predecessors :MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance>>> = predecessors.toMutableArraySet()

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

  constructor(node: ExecutableStartNode, processInstance: ProcessInstance) : this(node, emptyList(), processInstance.handle, processInstance.owner)

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>, processInstance: ProcessInstance) : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance.handle, processInstance.owner)

  constructor(builder:Builder<out ExecutableProcessNode>): this(builder.node, builder.predecessors, builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results, builder.failureCause)

  @Throws(SQLException::class)
  internal constructor(transaction: ProcessTransaction, node: ExecutableProcessNode, processInstance: ProcessInstance, state: NodeInstanceState)
        : this(node, resolvePredecessors(transaction, processInstance, node), processInstance.handle, processInstance.owner, state=state)

  override fun withPermission() = this

  open fun builder(): Builder<out ExecutableProcessNode> = ExtBuilder(this)

  /** Update the node. This will store the update based on the transaction. It will return the new object. The old object
   *  may be invalid afterwards.
   */
  open fun update(transaction: ProcessTransaction, body: Builder<out ExecutableProcessNode>.() -> Unit):ProcessNodeInstance {
    val origHandle = handle
    return builder().apply { body() }.build().apply {
      if (origHandle.valid)
      if (handle.valid)
        transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  @Throws(SQLException::class)
  open fun tickle(transaction: ProcessTransaction, messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    return when (state) {
      NodeInstanceState.FailRetry,
      NodeInstanceState.Pending -> provideTask(transaction, messageService)
      else -> this
    }// ignore
  }

  @Throws(SQLException::class)
  override fun getResult(transaction: ProcessTransaction, name: String): ProcessData? {
    return results.firstOrNull { name == it.name }
  }

  @Throws(SQLException::class)
  fun getDefines(transaction: ProcessTransaction): List<ProcessData> {
    return node.defines.map {
      it.apply(transaction, this)
    }
  }

  private fun hasDirectPredecessor(handle: Handle<out SecureObject<ProcessNodeInstance>>): Boolean {
    for (pred in directPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(transaction: ProcessTransaction): Collection<ProcessNodeInstance> {
    return directPredecessors.asSequence().map {
            transaction.readableEngineData.nodeInstance(it).withPermission()
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(transaction: ProcessTransaction, nodeName: String): Handle<out SecureObject<ProcessNodeInstance>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    directPredecessors
          .asSequence()
          .map { transaction.readableEngineData.nodeInstance(it).withPermission() }
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
  override fun resolvePredecessor(transaction: ProcessTransaction, nodeName: String): ProcessNodeInstance? {
    val handle = getPredecessor(transaction, nodeName) ?: throw NullPointerException("Missing predecessor with name ${nodeName} referenced from node ${node.id}")
    return transaction.readableEngineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  @Throws(SQLException::class)
  override fun <U> provideTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
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
  override fun <U> takeTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    val result = node.takeTask(messageService, this)
    val newObj = update(transaction) { state = NodeInstanceState.Taken }

    return if (result) newObj.startTask(transaction, messageService) else newObj
  }

  @Throws(SQLException::class)
  override fun <U> startTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): ProcessNodeInstance {
    val startNext = node.startTask(messageService, this)
    val updatedInstance = update(transaction) { state = NodeInstanceState.Started }
    return if (startNext) {
      val pi = transaction.readableEngineData.instance(hProcessInstance).withPermission()
      pi.finishTask(transaction, messageService, updatedInstance, null)
    }else updatedInstance
  }

  @Throws(SQLException::class)
  override fun finishTask(transaction: ProcessTransaction, resultPayload: Node?): ProcessNodeInstance {
    return transaction.commit(update(transaction) {
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = NodeInstanceState.Complete
    })
  }

  @Throws(SQLException::class)
  override fun cancelTask(transaction: ProcessTransaction): ProcessNodeInstance {
    return update(transaction) { state = NodeInstanceState.Cancelled }
  }

  @Throws(SQLException::class)
  override fun tryCancelTask(transaction: ProcessTransaction): ProcessNodeInstance {
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
  override fun failTask(transaction: ProcessTransaction, cause: Throwable): ProcessNodeInstance {
    return update(transaction) {
      failureCause = cause
      state = if (state == NodeInstanceState.Pending) NodeInstanceState.FailRetry else NodeInstanceState.Failed
    }
  }

  @Throws(SQLException::class)
  override fun failTaskCreation(transaction: ProcessTransaction, cause: Throwable): ProcessNodeInstance {
    return transaction.commit(update(transaction) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    })
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: ProcessTransaction,
                                 source: Source,
                                 result: Result,
                                 localEndpoint: EndpointDescriptor) {
    instantiateXmlPlaceholders(transaction, source, true, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: ProcessTransaction,
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
  fun instantiateXmlPlaceholders(transaction: ProcessTransaction,
                                 source: Source,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): CompactFragment {
    val xmlReader = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(transaction, xmlReader, removeWhitespace, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: ProcessTransaction,
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
  fun toSerializable(transaction: ProcessTransaction, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
    val builder = ExtBuilder(this)

    val body:CompactFragment? = (node as? Activity<*,*>)?.message?.let { message ->
      try {
        val xmlReader = XMLFragmentStreamReader.from(message.messageBody)
        instantiateXmlPlaceholders(transaction, xmlReader, true, localEndpoint)
      } catch (e: XmlException) {
        logger.log(Level.WARNING, "Error processing body", e)
        throw e
      }
    }

    return builder.toXmlInstance(body)
  }

  @Throws(XmlException::class)
  override fun serialize(transaction: ProcessTransaction,
                         out: XmlWriter,
                         localEndpoint: EndpointDescriptor) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", hProcessInstance.handleValue)

      if (handle.valid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*,*>)?.message?.messageBody?.let { body ->
        instantiateXmlPlaceholders(transaction, XMLFragmentStreamReader.from(body), out, true, localEndpoint)
      }
    }
  }

  companion object {

    @Throws(XmlException::class)
    fun <T: ProcessTransaction> deserialize(transaction: T, processEngine: ProcessEngine<T>, xmlReader: XmlReader): ProcessNodeInstance {

      val nodeInstance = XmlProcessNodeInstance.deserialize(xmlReader)
      val instance = transaction.readableEngineData.instance(Handles.handle(nodeInstance.processInstance)).withPermission()
      val processNode = instance.processModel.getNode(nodeInstance.nodeId ?: throw NullPointerException("Missing node id"))?: throw ProcessException("Missing node in process model")
      return ProcessNodeInstance(transaction, processNode, instance, nodeInstance.state ?: throw NullPointerException("Missing state"))
    }

    private val logger by lazy { Logger.getLogger(ProcessNodeInstance::class.java.getName()) }

    @Throws(SQLException::class)
    private fun <T:ProcessTransaction> resolvePredecessors(transaction: T,
                                                           processInstance: ProcessInstance,
                                                           node: ExecutableProcessNode): List<ComparableHandle<out SecureObject<ProcessNodeInstance>>> {

      return node.predecessors.asSequence()
            .map { processInstance.getNodeInstance(transaction, it) }
            .filterNotNull()
            .map { it.handle }
            .toList()
    }

    fun <T:ProcessTransaction> build(node: ExecutableProcessNode,
                                     predecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                                     processInstance: ProcessInstance,
                                     handle: Handle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder<ExecutableProcessNode>.() -> Unit):ProcessNodeInstance {
      return ProcessNodeInstance(BaseBuilder(node, predecessors, processInstance.handle, processInstance.owner, handle, state).apply(body))
    }


  }

}
