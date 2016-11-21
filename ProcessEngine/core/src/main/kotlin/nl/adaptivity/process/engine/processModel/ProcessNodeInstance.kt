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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.Transaction
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
                                                state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending) : IProcessNodeInstance<T, ProcessNodeInstance<T>>, SecureObject<ProcessNodeInstance<T>> {

  data class Builder<T:ProcessTransaction<T>>(
        var node: ExecutableProcessNode,
        var predecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>,
        var processInstance: ProcessInstance<T>,
        var handle: Handle<out SecureObject<ProcessNodeInstance<T>>> = Handles.getInvalid(),
        var state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending) {
    val results = mutableListOf<ProcessData>()

    fun toXmlInstance(body: CompactFragment?):XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<IProcessNodeInstance<*,*>>(it.handleValue) },
                                    processInstance = processInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }
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

  private var _handleValue: Long = -1

  private val _results: MutableList<ProcessData> = ArrayList()
  val results: List<ProcessData>
    get() = _results

  var failureCause: Throwable? = null
    private set

  protected val _directPredecessors: MutableList<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>> = predecessors.asSequence().filter { it.valid }.toMutableList()

  val directPredecessors: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>
    get() = _directPredecessors

  override val owner: Principal
    get() = processInstance.owner

  override val handle: ComparableHandle<out @JvmWildcard SecureObject<ProcessNodeInstance<T>>>
    get() = Handles.handle<ProcessNodeInstance<T>>(_handleValue)

  constructor(node: StartNodeImpl, processInstance: ProcessInstance<T>) : this(node, emptyList(), processInstance)

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>) : this(node, listOf(predecessor), processInstance)

  @Throws(SQLException::class)
  internal constructor(transaction: T, node: ExecutableProcessNode, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState)
        : this(node, resolvePredecessors(transaction, processInstance, node), processInstance, state)

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

  /** Add the node as predecessor if not added yet.  */
  fun ensurePredecessor(handle: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>) {
    if (!hasDirectPredecessor(handle)) {
      _directPredecessors.add(handle)
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
    for (pred in _directPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(transaction: T): Collection<ProcessNodeInstance<T>> {
    return _directPredecessors.asSequence().map {
            processInstance.engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL).mustExist(it)
          }.toList()
  }

  @Throws(SQLException::class)
  fun setDirectPredecessors(transaction: T, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>?) {
    if (predecessors == null || predecessors.isEmpty()) {
      _directPredecessors.clear()
    } else {
      _directPredecessors.apply { clear() }.addAll(predecessors)
    }
    processInstance.engine.updateStorage(transaction, this)
  }

  @Throws(SQLException::class)
  fun getPredecessor(transaction: T, nodeName: String): Handle<out SecureObject<ProcessNodeInstance<T>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for (hpred in _directPredecessors) {
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
    val handle = getPredecessor(transaction, nodeName)
    return processInstance.engine.getNodeInstance(transaction, handle!!, SecurityProvider.SYSTEMPRINCIPAL)
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
    val builder = Builder(node, _directPredecessors, processInstance, handle, state ).apply {
      this.results.addAll(results)
    }

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

      _directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

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

  }

}
