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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
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
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import java.io.CharArrayWriter
import java.security.Principal
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.transform.Result
import javax.xml.transform.Source

@XmlDeserializer(ProcessNodeInstance.Factory::class)
open class ProcessNodeInstance(open val node: ExecutableProcessNode,
                               predecessors: Collection<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                               val hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
                               override val owner: Principal,
                               handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                               override final val state: NodeInstanceState = NodeInstanceState.Pending,
                               results: Iterable<ProcessData> = emptyList(),
                               val failureCause: Throwable? = null) : IProcessNodeInstance<ProcessNodeInstance>, SecureObject<ProcessNodeInstance>, ReadableHandleAware<SecureObject<ProcessNodeInstance>> {

  private var handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>
        = Handles.handle(handle)

  override fun getHandle() = handle

  val results: List<ProcessData> = results.toList()

  val directPredecessors: Set<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  fun precedingClosure(processData: ProcessEngineDataAccess): Sequence<SecureObject<ProcessNodeInstance>> {
    return directPredecessors.asSequence().flatMap { predHandle ->
      val pred = processData.nodeInstance(predHandle).withPermission()
      pred.precedingClosure(processData) + sequenceOf(pred)
    }
  }

  interface Builder<N:ExecutableProcessNode> {
    var node: N
    val predecessors: MutableSet<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>
    var owner: Principal
    var handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>
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
    override var handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> by overlay(observer) { base.handle }
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
      predecessors: Iterable<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
      override var hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
      override var owner: Principal,
      override var handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
      override var state: NodeInstanceState = NodeInstanceState.Pending) : AbstractBuilder<N>() {

    override var predecessors :MutableSet<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>> = predecessors.toMutableArraySet()

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
      throw IllegalArgumentException("ProcessNodeInstances cannot be created for joins. Use JoinInstance")
    }
    if (node is StartNode<*, *>) {
      if (predecessors.any { it.valid }) throw IllegalArgumentException("Start nodes don't have (valid) predecessors.")
    } else {
      if (predecessors.asSequence().filter { it.valid }.firstOrNull()==null) {
        throw IllegalArgumentException("Nodes that are not startNodes need predecessors")
      }
    }
  }

  constructor(node: ExecutableProcessNode, predecessor: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, processInstance: ProcessInstance) : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance.getHandle(), processInstance.owner)

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
  open fun tickle(engineData: MutableProcessEngineDataAccess, instance: ProcessInstance, messageService: IMessageService<*>): PNIPair<ProcessNodeInstance> {
    return when (state) {
      NodeInstanceState.FailRetry,
      NodeInstanceState.Pending -> provideTask(engineData, instance)
      else -> PNIPair(instance, this)
    }// ignore
  }

  @Throws(SQLException::class)
  fun getResult(engineData: ProcessEngineDataAccess, name: String): ProcessData? {
    return results.firstOrNull { name == it.name }
  }

  @Throws(SQLException::class)
  fun getDefines(engineData: ProcessEngineDataAccess): List<ProcessData> {
    return node.defines.map {
      it.applyData(engineData, this)
    }
  }

  private fun hasDirectPredecessor(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>): Boolean {
    for (pred in directPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(engineData: ProcessEngineDataAccess): Collection<ProcessNodeInstance> {
    return directPredecessors.asSequence().map {
            engineData.nodeInstance(it).withPermission()
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(engineData: ProcessEngineDataAccess, nodeName: String): net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    directPredecessors
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

  @Throws(SQLException::class)
  fun resolvePredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ProcessNodeInstance? {
    val handle = getPredecessor(engineData, nodeName) ?: throw NullPointerException("Missing predecessor with name ${nodeName} referenced from node ${node.id}")
    return engineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  fun condition(engineData: ProcessEngineDataAccess) = node.condition(engineData, this)

  @Throws(SQLException::class)
  open fun provideTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {

    fun <MSG_T> impl(messageService: IMessageService<MSG_T>):PNIPair<ProcessNodeInstance> {

      val shouldProgress = tryCreate(engineData, processInstance) {
        node.provideTask(engineData, processInstance, this)
      }

      val pniPair = run {
        // TODO, get the updated state out of provideTask
        val newInstance = engineData.instance(hProcessInstance).withPermission()
        val newNodeInstance = engineData.nodeInstance(handle).withPermission()
        newNodeInstance.update(engineData, newInstance) { state = NodeInstanceState.Sent }.apply { engineData.commit() }
      }
      if (shouldProgress) {
        return ProcessInstance.Updater(pniPair.instance).takeTask(engineData, pniPair.node)
      } else
        return pniPair

    }

    return impl(engineData.messageService())
  }

  @Throws(SQLException::class)
  open fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    val startNext = tryTask(engineData, processInstance) { node.startTask(this) }
    val updatedInstances = update(engineData, processInstance) { state = NodeInstanceState.Started }
    return if (startNext) {
      updatedInstances.instance.finishTask(engineData, updatedInstances.node, null)
    } else updatedInstances
  }

  @Throws(SQLException::class)
  @Deprecated("This is dangerous, it will not update the instance")
  internal open fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node? = null): PNIPair<ProcessNodeInstance> {
    if (state.isFinal) {
      throw ProcessException("instance $this cannot be finished as it is already in a final state: ${state}")
    }
    return update(engineData, processInstance) {
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = NodeInstanceState.Complete
    }.apply { engineData.commit() }
  }

  fun cancelAndSkip(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    return when (state) {
      NodeInstanceState.Pending,
      NodeInstanceState.FailRetry -> update(engineData, processInstance) { state = NodeInstanceState.Skipped }
      NodeInstanceState.Sent,
      NodeInstanceState.Taken,
      NodeInstanceState.Acknowledged ->
          cancelTask(engineData, processInstance).update(engineData) { state = NodeInstanceState.Skipped }
      else -> PNIPair(processInstance, this)
    }
  }

  @Throws(SQLException::class)
  open fun cancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    return update(engineData, processInstance) { state = NodeInstanceState.Cancelled }
  }

  @Throws(SQLException::class)
  open fun tryCancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<ProcessNodeInstance> {
    try {
      return cancelTask(engineData, processInstance)
    } catch (e: IllegalArgumentException) {
      logger.log(Level.WARNING, "Task could not be cancelled")
      return PNIPair(processInstance, this)
    }
  }

  override fun toString(): String {
    return node.javaClass.simpleName + " (" + state + ")"
  }

  @Throws(SQLException::class)
  open fun failTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<ProcessNodeInstance> {
    return update(engineData, processInstance) {
      failureCause = cause
      state = if (state == NodeInstanceState.Pending) NodeInstanceState.FailRetry else NodeInstanceState.Failed
    }
  }

  @Throws(SQLException::class)
  open fun failTaskCreation(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<ProcessNodeInstance> {
    return update(engineData, processInstance) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    }.apply { engineData.commit() }
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 source: Source,
                                 result: Result,
                                 localEndpoint: EndpointDescriptor) {
    instantiateXmlPlaceholders(engineData, source, true, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 xmlReader: XmlReader,
                                 out: XmlWriter,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor) {
    val defines = getDefines(engineData)
    val transformer = PETransformer.create(ProcessNodeInstanceContext(this,
        defines,
        state == NodeInstanceState.Complete, localEndpoint),
        removeWhitespace)
    transformer.transform(xmlReader, out.filterSubstream())
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 source: Source,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): CompactFragment {
    val xmlReader = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(engineData, xmlReader, removeWhitespace, localEndpoint)
  }

  @Throws(XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 xmlReader: XmlReader,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): WritableCompactFragment {
    val caw = CharArrayWriter()

    val writer = XmlStreaming.newWriter(caw, true)
    instantiateXmlPlaceholders(engineData, xmlReader, writer, removeWhitespace, localEndpoint)
    writer.close()
    return WritableCompactFragment(emptyList<Namespace>(), caw.toCharArray())
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(engineData: ProcessEngineDataAccess, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
    val builder = ExtBuilder(this)

    val body:CompactFragment? = (node as? Activity<*,*>)?.message?.let { message ->
      try {
        val xmlReader = XMLFragmentStreamReader.from(message.messageBody)
        instantiateXmlPlaceholders(engineData, xmlReader, true, localEndpoint)
      } catch (e: XmlException) {
        logger.log(Level.WARNING, "Error processing body", e)
        throw e
      }
    }

    return builder.toXmlInstance(body)
  }

  @Throws(XmlException::class)
  fun serialize(engineData: ProcessEngineDataAccess, out: XmlWriter, localEndpoint: EndpointDescriptor) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", hProcessInstance.handleValue)

      if (handle.valid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*,*>)?.message?.messageBody?.let { body ->
        instantiateXmlPlaceholders(engineData, XMLFragmentStreamReader.from(body), out, true, localEndpoint)
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

  protected inline fun <R> tryCreate(engineData: MutableProcessEngineDataAccess,
                           processInstance: ProcessInstance,
                           body: () -> R): R =
    _tryHelper(engineData, processInstance, body) { d, i, e -> failTaskCreation(d, i, e) }

  protected inline fun <R> tryTask(engineData: MutableProcessEngineDataAccess,
                         processInstance: ProcessInstance,
                         body: () -> R): R = _tryHelper(engineData, processInstance, body) { d, i, e -> failTask(d, i, e) }

  @PublishedApi
  internal inline fun <R> _tryHelper(engineData: MutableProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     body: () -> R, failHandler: (MutableProcessEngineDataAccess, ProcessInstance, Exception)->Unit): R {
    return try {
      body()
    } catch (e: Exception) {
      try {
        failHandler(engineData, processInstance, e)
      } catch (f: Exception) {
        e.addSuppressed(f)
      }
      throw e
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
                                                           node: ExecutableProcessNode): List<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>> {

      return node.predecessors.asSequence()
            .map { processInstance.getNodeInstance(it) }
            .filterNotNull()
            .map { it.handle }
            .toList()
    }

    fun <T:ProcessTransaction> build(node: ExecutableProcessNode,
                                     predecessors: Set<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>>,
                                     processInstance: ProcessInstance,
                                     handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
                                     state: NodeInstanceState = NodeInstanceState.Pending,
                                     body: Builder<ExecutableProcessNode>.() -> Unit):ProcessNodeInstance {
      return ProcessNodeInstance(BaseBuilder(node, predecessors, processInstance.getHandle(), processInstance.owner, handle, state).apply(body))
    }


  }

}
