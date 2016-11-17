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
import nl.adaptivity.process.engine.PETransformer
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.CharArrayWriter
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.transform.Result
import javax.xml.transform.Source

@XmlDeserializer(ProcessNodeInstance.Factory::class)
open class ProcessNodeInstance<T : Transaction> : IProcessNodeInstance<T, ProcessNodeInstance<T>>, SecureObject {

  class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance.deserialize(reader)
    }
  }


  open val node: ExecutableProcessNode

  private val mResults = ArrayList<ProcessData>()

  private var mPredecessors: MutableList<ComparableHandle<out ProcessNodeInstance<T>>>

  override final var state: IProcessNodeInstance.NodeInstanceState = IProcessNodeInstance.NodeInstanceState.Pending
    private set

  private var mHandle: Long = -1

  private val mProcessInstance: ProcessInstance<T>

  val results: List<ProcessData>
    get() = mResults

  var failureCause: Throwable? = null
    private set

  val directPredecessors: MutableCollection<ComparableHandle<out ProcessNodeInstance<T>>>
    get() = mPredecessors

  val processInstance: ProcessInstance<T>
    get() = mProcessInstance

  constructor(node: ExecutableProcessNode, predecessor: ComparableHandle<out ProcessNodeInstance<T>>, processInstance: ProcessInstance<T>) : super() {
    this.node = node
    if (!predecessor.valid) {
      if (node is StartNode<*, *>) {
        mPredecessors = ArrayList<ComparableHandle<out ProcessNodeInstance<T>>>()
      } else {
        throw NullPointerException("Nodes that are not startNodes need predecessors")
      }
    } else {
      mPredecessors = mutableListOf(predecessor)
    }
    mProcessInstance = processInstance
  }

  internal constructor(node: ExecutableProcessNode, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>) : super() {
    this.node = node
    mPredecessors = ArrayList(predecessors)
    mProcessInstance = processInstance
    if ((mPredecessors == null || mPredecessors.size == 0) && node !is StartNode<*, *>) {
      throw NullPointerException("Non-start-node process node instances need predecessors")
    }
  }

  internal constructor(node: ExecutableProcessNode, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState) : super() {
    this.node = node
    mPredecessors = ArrayList(predecessors)
    mProcessInstance = processInstance
    this.state = state
    if ((mPredecessors == null || mPredecessors.size == 0) && node !is StartNode<*, *>) {
      throw NullPointerException("Non-start-node process node instances need predecessors")
    }
  }

  @Throws(SQLException::class)
  internal constructor(transaction: T, node: ExecutableProcessNode, processInstance: ProcessInstance<T>, state: IProcessNodeInstance.NodeInstanceState) {
    this.node = node
    mProcessInstance = processInstance
    this.state = state
    mPredecessors = resolvePredecessors(transaction, processInstance, node)
    if ((mPredecessors == null || mPredecessors.size == 0) && node !is StartNode<*, *>) {
      throw NullPointerException("Non-start-node process node instances need predecessors")
    }
  }

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

  @Throws(SQLException::class)
  private fun resolvePredecessors(transaction: T,
                                  processInstance: ProcessInstance<T>,
                                  node: ExecutableProcessNode): MutableList<ComparableHandle<out ProcessNodeInstance<T>>> {
    val result = ArrayList<ComparableHandle<out ProcessNodeInstance<T>>>()
    for (pred in node.predecessors) {
      val nodeInstance: ProcessNodeInstance<T>? = processInstance.getNodeInstance(transaction, pred)
      if (nodeInstance != null) {
        result.add(nodeInstance.handle)
      }
    }
    return result
  }

  /** Add the node as predecessor if not added yet.  */
  fun ensurePredecessor(handle: ComparableHandle<out ProcessNodeInstance<T>>) {
    if (!hasDirectPredecessor(handle)) {
      mPredecessors.add(handle)
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
      IProcessNodeInstance.NodeInstanceState.Pending -> mProcessInstance.provideTask(
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

  private fun hasDirectPredecessor(handle: Handle<out ProcessNodeInstance<*>>): Boolean {
    for (pred in mPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(transaction: T): Collection<ProcessNodeInstance<T>> {
    val result = ArrayList<ProcessNodeInstance<T>>(mPredecessors.size)
    for (i in mPredecessors.indices) {
      val nodeInstance: ProcessNodeInstance<T> = processInstance.engine.getNodeInstance(transaction, mPredecessors[i], SecurityProvider.SYSTEMPRINCIPAL)
            ?: throw NullPointerException("Missing predecessor")

      mPredecessors[i] = nodeInstance.handle
      result.add(nodeInstance)
    }
    return result
  }

  @Throws(SQLException::class)
  fun setDirectPredecessors(transaction: T, predecessors: Collection<ComparableHandle<out ProcessNodeInstance<T>>>?) {
    if (predecessors == null || predecessors.isEmpty()) {
      mPredecessors = ArrayList<ComparableHandle<out ProcessNodeInstance<T>>>()
    } else {
      mPredecessors = ArrayList(predecessors)
    }
    mProcessInstance.engine.updateStorage(transaction, this)
  }

  @Throws(SQLException::class)
  fun getPredecessor(transaction: T, nodeName: String): Handle<out ProcessNodeInstance<T>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    for (hpred in mPredecessors) {
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
    mProcessInstance.engine.updateStorage(transaction, this)
  }

  fun getHandleValue(): Long {
    return mHandle
  }

  override fun setHandleValue(handleValue: Long) {
    mHandle = handleValue
  }

  override val handle: ComparableHandle<out @JvmWildcard ProcessNodeInstance<T>>
    get() = Handles.handle<ProcessNodeInstance<T>>(mHandle)

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
      mResults.add(resultType.apply(resultPayload))
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
    mResults.clear()
    mResults.addAll(results)
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T, source: Source, result: Result) {
    instantiateXmlPlaceholders(transaction, source, true)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T, `in`: XmlReader, out: XmlWriter, removeWhitespace: Boolean) {
    val defines = getDefines(transaction)
    val transformer = PETransformer.create(ProcessNodeInstanceContext(this,
                                                                      defines,
                                                                      state == IProcessNodeInstance.NodeInstanceState.Complete),
                                           removeWhitespace)
    transformer.transform(`in`, out.filterSubstream())
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(transaction: T, source: Source, removeWhitespace: Boolean): CompactFragment {
    val `in` = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(transaction, `in`, removeWhitespace)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(transaction: T, `in`: XmlReader, removeWhitespace: Boolean): WritableCompactFragment {
    val caw = CharArrayWriter()

    val writer = XmlStreaming.newWriter(caw, true)
    instantiateXmlPlaceholders(transaction, `in`, writer, removeWhitespace)
    writer.close()
    return WritableCompactFragment(emptyList<Namespace>(), caw.toCharArray())
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(transaction: T): XmlProcessNodeInstance {
    val xmlNodeInst = XmlProcessNodeInstance()
    xmlNodeInst.state = state
    xmlNodeInst.handle = mHandle

    if (node is Activity<*, *>) {
      val act = node as Activity<*, *>?
      val message = act!!.getMessage()
      try {
        val `in` = XMLFragmentStreamReader.from(message.messageBody)
        xmlNodeInst.body = instantiateXmlPlaceholders(transaction, `in`, true)
      } catch (e: XmlException) {
        logger.log(Level.WARNING, "Error processing body", e)
        throw RuntimeException(e)
      }

    }

    xmlNodeInst.processInstance = mProcessInstance.handleValue

    xmlNodeInst.nodeId = node.id

    if (mPredecessors.size > 0) {
      val predecessors = xmlNodeInst.predecessors
      predecessors.addAll(mPredecessors)
    }

    xmlNodeInst.results = results

    return xmlNodeInst
  }

  @Throws(XmlException::class)
  override fun serialize(transaction: T, out: XmlWriter) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME)
    out.writeAttribute("state", state.name)

    out.writeAttribute("processinstance", mProcessInstance.handleValue)

    if (mHandle != -1L) {
      out.writeAttribute("handle", mHandle)
    }
    out.writeAttribute("nodeid", node.id)
    for (predecessor in mPredecessors) {
      out.writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME,
                             java.lang.Long.toString(predecessor.handleValue))
    }

    for (result in mResults) {
      result.serialize(out)
    }

    if (node is Activity<*, *>) {
      out.smartStartTag(XmlProcessNodeInstance.BODY_ELEMENTNAME)
      val `in` = XMLFragmentStreamReader.from((node as Activity<*, *>).getMessage().messageBody)
      try {
        instantiateXmlPlaceholders(transaction, `in`, out, true)
      } catch (e: SQLException) {
        throw RuntimeException(e)
      }

      out.endTag(XmlProcessNodeInstance.BODY_ELEMENTNAME)
    }
    out.endTag(XmlProcessNodeInstance.ELEMENTNAME)
  }

  companion object {

    @Throws(XmlException::class)
    fun <T: Transaction> deserialize(transaction: T,
                    processEngine: ProcessEngine<T>,
                    `in`: XmlReader): ProcessNodeInstance<*> {
      try {
        return ProcessNodeInstance(transaction, processEngine, XmlProcessNodeInstance.deserialize(`in`))
      } catch (e: SQLException) {
        throw RuntimeException(e)
      }

    }

    private val logger by lazy { Logger.getLogger(ProcessNodeInstance::class.java.getName()) }

  }

}
