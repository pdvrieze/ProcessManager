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
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.Split
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

  typealias SecureT = IProcessNodeInstance<ProcessNodeInstance>.SecureT

  typealias HandleT = ComparableHandle<out SecureT>

  @Suppress("CanBePrimaryConstructorProperty")
  open val node: ExecutableProcessNode = node

  private var handle: ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance>>
        = Handles.handle(handle)

  override fun getHandle(): ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance>>
        = handle

  val results: List<ProcessData> = results.toList()

  val directPredecessors: Set<HandleT> = predecessors.asSequence().filter { it.valid }.toArraySet()

  override val owner: Principal = owner

  interface Builder<N:ExecutableProcessNode> {
    var node: N
    val predecessors: MutableSet<ProcessNodeInstance.HandleT>
    var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>
    var owner: Principal
    var handle: Handle<out SecureObject<ProcessNodeInstance>>
    var state: NodeInstanceState
    val results:MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance
    var failureCause: Throwable?
    fun  build(): ProcessNodeInstance
    // Cancel the instance
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
    private val observer = { changed = true }

    override var predecessors = ObservableSet(base.directPredecessors.toMutableArraySet(), { changed = true })
    override var hProcessInstance by overlay(update = observer) { base.hProcessInstance }
    override var owner by overlay(observer) { base.owner }
    override var handle: Handle<out SecureObject<ProcessNodeInstance>> by overlay(observer) { base.handle }
    override var state by overlay(observer) { base.state }
    override var results = ObservableList(base.results.toMutableList(), { changed = true })
    var changed: Boolean = false
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
    if (this.javaClass== ProcessNodeInstance::class.java && node is Split<*, *>) {
      throw IllegalArgumentException("ProcessNodeInstances cannot be created for joins. Use SplitInstance")
    }
    if (node is StartNode<*, *>) {
      if (predecessors.any { it.valid }) throw IllegalArgumentException("Start nodes don't have (valid) predecessors.")
    } else {
      if (predecessors.asSequence().filter { it.valid }.firstOrNull()==null) {
        throw IllegalArgumentException("Nodes that are not startNodes need predecessors")
      }
    }
  }

  constructor(node: ExecutableStartNode, processInstance: ProcessInstance) : this(node, emptyList(), processInstance.getHandle(), processInstance.owner)

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>, processInstance: ProcessInstance) : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance.getHandle(), processInstance.owner)

  constructor(builder:Builder<out ExecutableProcessNode>): this(builder.node, builder.predecessors, builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results, builder.failureCause)

  @Throws(SQLException::class)
  internal constructor(transaction: ProcessTransaction, node: ExecutableProcessNode, processInstance: ProcessInstance, state: NodeInstanceState)
        : this(node, resolvePredecessors(transaction, processInstance, node), processInstance.getHandle(), processInstance.owner, state=state)

  override fun withPermission() = this

  open fun builder(): ExtBuilderBase<out ExecutableProcessNode> {
    assert(this.javaClass == ProcessNodeInstance::class.java) { "Builders must be overridden" }
    return ExtBuilder(this)
  }

  /** Update the node. This will store the update based on the transaction. It will return the new object. The old object
   *  may be invalid afterwards.
   */
  open fun update(writableEngineData: MutableProcessEngineDataAccess, instance:ProcessInstance, body: Builder<*>.() -> Unit): PNIPair<ProcessNodeInstance> {
    val origHandle = handle
    val builder:ExtBuilderBase<*> = builder().apply(body)
    if (builder.changed) {
      if (origHandle.valid && handle.valid) {
        return instance.updateNode(writableEngineData, builder.build()).apply {
          assert(node == writableEngineData.nodeInstance(handle)) { "The returned node and the stored node don't match for node ${node.node.id}-${node.handle}(${node.state})" }
          assert(this.instance.getChild(node.node.id)==node) { "The instance node and the node don't match for node ${node.node.id}-${node.handle}(${node.state})" }
        }
      } else {
        return PNIPair(instance, this)
      }
    } else {
      return PNIPair(instance, this)
    }
  }

  @Throws(SQLException::class)
  open fun tickle(transaction: ProcessTransaction, instance: ProcessInstance, messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    return when (state) {
      NodeInstanceState.FailRetry,
      NodeInstanceState.Pending -> provideTask(transaction, instance, messageService)
      else -> PNIPair(instance, this)
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

  fun condition(transaction: ProcessTransaction) = node.condition(transaction, this)

  @Throws(SQLException::class)
  override fun <U> provideTask(transaction: ProcessTransaction, processInstance: ProcessInstance, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    val shouldProgress = try {
      node.provideTask(transaction, messageService, processInstance, this)
    } catch (e: Exception) {
      // TODO later move failretry to fail
      failTaskCreation(transaction,processInstance, e)
      throw e
    }
    val pniPair = run {
      // TODO, get the updated state out of provideTask
      val newInstance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
      val newNodeInstance = transaction.readableEngineData.nodeInstance(handle).withPermission()
      transaction.commit(newNodeInstance.update(transaction.writableEngineData, newInstance) { state = NodeInstanceState.Sent })
    }
    if (shouldProgress) {
      return pniPair.node.takeTask(transaction, pniPair.instance, messageService)
    } else
      return pniPair

  }

  @Throws(SQLException::class)
  override fun <U> takeTask(transaction: ProcessTransaction, processInstance: ProcessInstance, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    val startNext = node.takeTask(messageService, this)
    val updatedInstances = update(transaction.writableEngineData, processInstance) { state = NodeInstanceState.Taken }

    return if (startNext) updatedInstances.node.startTask(transaction, updatedInstances.instance, messageService) else updatedInstances
  }

  @Throws(SQLException::class)
  override fun <U> startTask(transaction: ProcessTransaction, processInstance: ProcessInstance, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): PNIPair<ProcessNodeInstance> {
    val startNext = node.startTask(messageService, this)
    val updatedInstances = update(transaction.writableEngineData, processInstance) { state = NodeInstanceState.Started }
    return if (startNext) {
      updatedInstances.instance.finishTask(transaction, messageService, updatedInstances.node, null)
    } else updatedInstances
  }

  @Throws(SQLException::class)
  @Deprecated("This is dangerous, it will not update the instance")
  internal open fun finishTask(transaction: ProcessTransaction, processInstance: ProcessInstance, resultPayload: Node? = null): PNIPair<ProcessNodeInstance> {
    return transaction.commit(update(transaction.writableEngineData, processInstance) {
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = NodeInstanceState.Complete
    })
  }

  fun cancelAndSkip(transaction: ProcessTransaction, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    return when (state) {
      NodeInstanceState.Pending,
      NodeInstanceState.FailRetry -> update(transaction.writableEngineData, processInstance) { state = NodeInstanceState.Skipped }
      NodeInstanceState.Sent,
      NodeInstanceState.Taken,
      NodeInstanceState.Acknowledged ->
      cancelTask(transaction, processInstance).update(transaction.writableEngineData) { state = NodeInstanceState.Skipped }
      else -> PNIPair(processInstance, this)
    }
  }

  @Throws(SQLException::class)
  override fun cancelTask(transaction: ProcessTransaction, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    return update(transaction.writableEngineData, processInstance) { state = NodeInstanceState.Cancelled }
  }

  @Throws(SQLException::class)
  override fun tryCancelTask(transaction: ProcessTransaction, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    try {
      return cancelTask(transaction, processInstance)
    } catch (e: IllegalArgumentException) {
      logger.log(Level.WARNING, "Task could not be cancelled")
      return PNIPair(processInstance, this)
    }
  }

  override fun toString(): String {
    return node.javaClass.simpleName + " (" + state + ")"
  }

  @Throws(SQLException::class)
  override fun failTask(transaction: ProcessTransaction, processInstance: ProcessInstance, cause: Throwable): PNIPair<ProcessNodeInstance> {
    return update(transaction.writableEngineData, processInstance) {
      failureCause = cause
      state = if (state == NodeInstanceState.Pending) NodeInstanceState.FailRetry else NodeInstanceState.Failed
    }
  }

  @Throws(SQLException::class)
  override fun failTaskCreation(transaction: ProcessTransaction, processInstance: ProcessInstance, cause: Throwable): PNIPair<ProcessNodeInstance> {
    return transaction.commit(update(transaction.writableEngineData, processInstance) {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as ProcessNodeInstance

    if (hProcessInstance != other.hProcessInstance) return false
    if (state != other.state) return false
    if (failureCause != other.failureCause) return false
    if (node != other.node) return false
    if (handle != other.handle) return false
    if (results != other.results) return false
    if (directPredecessors != other.directPredecessors) return false
    if (owner != other.owner) return false

    return true
  }

  override fun hashCode(): Int {
    var result = hProcessInstance.hashCode()
    result = 31 * result + state.hashCode()
    result = 31 * result + (failureCause?.hashCode() ?: 0)
    result = 31 * result + node.hashCode()
    result = 31 * result + handle.hashCode()
    result = 31 * result + results.hashCode()
    result = 31 * result + directPredecessors.hashCode()
    result = 31 * result + owner.hashCode()
    return result
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
      return ProcessNodeInstance(BaseBuilder(node, predecessors, processInstance.getHandle(), processInstance.owner, handle, state).apply(body))
    }


  }

}
