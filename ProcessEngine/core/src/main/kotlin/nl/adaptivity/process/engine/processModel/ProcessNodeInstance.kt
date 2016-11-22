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
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
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
                                                val processInstance: ProcessInstance<T>,
                                                handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
                                                state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending,
                                                results: Iterable<ProcessData> = emptyList(),
                                                failureCause: Throwable? = null) : IProcessNodeInstance<T, ProcessNodeInstance<T>>, SecureObject<ProcessNodeInstance<T>> {

  interface Builder<T:ProcessTransaction<T>, N:ExecutableProcessNode> {
    var node: N
    var predecessors: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>
    var processInstance: ProcessInstance<T>
    var handle: Handle<out SecureObject<ProcessNodeInstance<T>>>
    var state: IProcessNodeInstance.NodeInstanceState
    val results:MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance
    var failureCause: Throwable?
    fun  build(): ProcessNodeInstance<T>
  }

  abstract class AbstractBuilder<T:ProcessTransaction<T>, N:ExecutableProcessNode> :Builder<T, N> {

    override fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<IProcessNodeInstance<*,*>>(it.handleValue) },
                                    processInstance = processInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    override var failureCause: Throwable? = null
  }

  abstract class ExtBuilderBase<T:ProcessTransaction<T>, N:ExecutableProcessNode>(base:ProcessNodeInstance<T>) : AbstractBuilder<T, N>() {
    override var predecessors = base.directPredecessors.toMutableArraySet()
    override var processInstance by overlay { base.processInstance }
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
        override var processInstance: ProcessInstance<T>,
        override var handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
        override var state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending) : AbstractBuilder<T, N>() {

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

  @Suppress("CanBePrimaryConstructorProperty")
  open val node: ExecutableProcessNode = node

  override final var state: IProcessNodeInstance.NodeInstanceState = state
    private set

  private var _handleValue: Long = handle.handleValue
  override val handle: ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance<T>>>
    get() = Handles.handle<ProcessNodeInstance<T>>(_handleValue)

  private val _results: MutableList<ProcessData> = results.toMutableList()
  val results: List<ProcessData>
    get() = _results

  var failureCause: Throwable? = null
    private set

  val directPredecessors: Set<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  override val owner: Principal
    get() = processInstance.owner

  constructor(node: StartNodeImpl, processInstance: ProcessInstance<T>) : this(node, emptyList(), processInstance)

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>) : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance)

  constructor(builder:Builder<T, out ExecutableProcessNode>): this(builder.node, builder.predecessors, builder.processInstance, builder.handle, builder.state, builder.results, builder.failureCause)

  @Throws(SQLException::class)
  internal constructor(transaction: T, node: ExecutableProcessNode, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState)
        : this(node, resolvePredecessors(transaction, processInstance, node), processInstance, state=state)

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

  open fun update(transaction: T, body: Builder<T, out ExecutableProcessNode>.() -> Unit):ProcessNodeInstance<T> {
    val origHandle = handle
    return ProcessNodeInstance(ExtBuilder(this).apply { body() }).apply {
      if (origHandle.valid)
      if (handle.valid)
        transaction.writableEngineData.nodeInstances[handle] = this
    }
  }

  fun setFailureCause(failureCause: String?) {
    this.failureCause = Exception(failureCause).apply {
      (this as java.lang.Throwable).stackTrace = arrayOfNulls<StackTraceElement>(0)
      // wipe the stacktrace, it is irrelevant
    }
  }

  @Throws(SQLException::class)
  open fun tickle(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>) {
    when (state) {
      IProcessNodeInstance.NodeInstanceState.FailRetry,
      IProcessNodeInstance.NodeInstanceState.Pending -> processInstance.provideTask(
            transaction,
            messageService,
            this)
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
            processInstance.engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL).mustExist(it)
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(transaction: T, nodeName: String): Handle<out SecureObject<ProcessNodeInstance<T>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for (hpred in directPredecessors) {
      val instance: ProcessNodeInstance<T> = processInstance.engine.getNodeInstance(transaction, hpred, SecurityProvider.SYSTEMPRINCIPAL)
            ?: throw NullPointerException("Missing predecessor for node")

      if (nodeName == instance.node.id) {
        return instance.handle
      } else {
        val result = instance.getPredecessor(transaction, nodeName)
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

  @Throws(SQLException::class)
  override fun setState(transaction: T, newState: IProcessNodeInstance.NodeInstanceState) {
    if (state > newState) {
      throw IllegalArgumentException("State can only be increased (was:$state new:$newState")
    }
    state = newState
    processInstance.engine.updateStorage(transaction, this)
  }

  fun getHandleValue(): Long {
    return _handleValue
  }

  override fun setHandleValue(handleValue: Long) {
    _handleValue = handleValue
  }

  @Throws(SQLException::class)
  override fun <U> provideTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): Boolean {
    try {
      val result = node.provideTask(transaction, messageService, this)
      if (result) { // the task must be automatically taken. Mostly this is false and we don't set the state.
        setState(transaction, IProcessNodeInstance.NodeInstanceState.Sent)
      }
      return result
    } catch (e: RuntimeException) {
      // TODO later move failretry to fail
      //      if (state!=TaskState.FailRetry) {
      failTaskCreation(transaction, e)
      //      }
      throw e
    }

  }

  @Throws(SQLException::class)
  override fun <U> takeTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): Boolean {
    val result = node.takeTask(messageService, this)
    setState(transaction, IProcessNodeInstance.NodeInstanceState.Taken)
    return result
  }

  @Throws(SQLException::class)
  override fun <U> startTask(transaction: T, messageService: IMessageService<U, T, ProcessNodeInstance<T>>): Boolean {
    val startTask = node.startTask(messageService, this)
    setState(transaction, IProcessNodeInstance.NodeInstanceState.Started)
    return startTask
  }

  @Throws(SQLException::class)
  override fun finishTask(transaction: T, resultPayload: Node?) {
    for (resultType in node.getResults()) {
      _results.add(resultType.apply(resultPayload))
    } //TODO ensure this is stored
    setState(transaction,
             IProcessNodeInstance.NodeInstanceState.Complete)// This triggers a database store. So do it after setting the results
  }

  @Throws(SQLException::class)
  override fun cancelTask(transaction: T) {
    setState(transaction, IProcessNodeInstance.NodeInstanceState.Cancelled)
  }

  @Throws(SQLException::class)
  override fun tryCancelTask(transaction: T) {
    try {
      setState(transaction, IProcessNodeInstance.NodeInstanceState.Cancelled)
    } catch (e: IllegalArgumentException) {
      logger.log(Level.WARNING, "Task could not be cancelled")
    }

  }

  override fun toString(): String {
    return node.javaClass.simpleName + " (" + state + ")"
  }

  @Throws(SQLException::class)
  override fun failTask(transaction: T, cause: Throwable) {
    failureCause = cause
    setState(transaction,
             if (state == IProcessNodeInstance.NodeInstanceState.Pending) IProcessNodeInstance.NodeInstanceState.FailRetry else IProcessNodeInstance.NodeInstanceState.Failed)
  }

  @Throws(SQLException::class)
  override fun failTaskCreation(transaction: T, cause: Throwable) {
    failureCause = cause
    setState(transaction, IProcessNodeInstance.NodeInstanceState.FailRetry)
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param results the new results.
   */
  internal fun setResult(results: List<ProcessData>) {
    _results.clear()
    _results.addAll(results)
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T, source: Source, result: Result) {
    instantiateXmlPlaceholders(transaction, source, true)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T, xmlReader: XmlReader, out: XmlWriter, removeWhitespace: Boolean) {
    val defines = getDefines(transaction)
    val transformer = PETransformer.create(ProcessNodeInstanceContext(this,
                                                                      defines,
                                                                      state == IProcessNodeInstance.NodeInstanceState.Complete),
                                           removeWhitespace)
    transformer.transform(xmlReader, out.filterSubstream())
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T, source: Source, removeWhitespace: Boolean): CompactFragment {
    val xmlReader = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(transaction, xmlReader, removeWhitespace)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T, xmlReader: XmlReader, removeWhitespace: Boolean): WritableCompactFragment {
    val caw = CharArrayWriter()

    val writer = XmlStreaming.newWriter(caw, true)
    instantiateXmlPlaceholders(transaction, xmlReader, writer, removeWhitespace)
    writer.close()
    return WritableCompactFragment(emptyList<Namespace>(), caw.toCharArray())
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(transaction: T): XmlProcessNodeInstance {
    val builder = ExtBuilder(this)

    val body:CompactFragment? = (node as? Activity<*,*>)?.let { act ->
      try {
        val xmlReader = XMLFragmentStreamReader.from(act.getMessage().messageBody)
        instantiateXmlPlaceholders(transaction, xmlReader, true)
      } catch (e: XmlException) {
        logger.log(Level.WARNING, "Error processing body", e)
        throw e
      }
    }

    return builder.toXmlInstance(body)
  }

  @Throws(XmlException::class)
  override fun serialize(transaction: T, out: XmlWriter) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", processInstance.handleValue)

      if (_handleValue != -1L) writeAttribute("handle", _handleValue)

      writeAttribute("nodeid", node.id)

      directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(_results)

      (node as? Activity<*,*>)?.getMessage()?.messageBody?.let { body ->
        instantiateXmlPlaceholders(transaction, XMLFragmentStreamReader.from(body), out, true)
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
                                        state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending,
                                        body: Builder<T, ExecutableProcessNode>.() -> Unit):ProcessNodeInstance<T> {
      return ProcessNodeInstance(BaseBuilder(node, predecessors, processInstance, handle, state).apply(body))
    }


  }

}
