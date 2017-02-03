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

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.CharArrayWriter
import java.security.Principal
import java.sql.SQLException
import java.util.concurrent.Future
import java.util.logging.Level
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Base interface for process instance.
 * @property node The node that this is an instance of.
 * @param predecessors The node instances that are direct predecessors of this one
 * @property hProcessInstance The handle to the owning process instance.
 * @property owner The owner of the node (generally the owner of the instance)
 * @param handle The handle for this instance (or invalid if not registered yet)
 * @property state The current state of the instance
 * @param results A list of the results associated with this node. This would imply a state of [NodeInstanceState.Complete]
 * @property entryNo The sequence number of this instance. Normally this will be 1, but for nodes that allow reentry,
 *                   this may be a higher number. Values below 1 are invalid.
 * @property failureCause For a failure, the cause of the failure
 */
abstract class ProcessNodeInstance<T: ProcessNodeInstance<T>>(override val node: ExecutableProcessNode,
                                                              predecessors: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
                                                              val hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                                                              override final val owner: Principal,
                                                              override final val entryNo: Int,
                                                              private var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
                                                              override final val state: NodeInstanceState = Pending,
                                                              results: Iterable<ProcessData> = emptyList(),
                                                              val failureCause: Throwable? = null) : SecureObject<ProcessNodeInstance<T>>, ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>>, IProcessNodeInstance {
  val results: List<ProcessData> = results.toList()
  override val predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  init {
    @Suppress("LeakingThis")
    if (entryNo!=1 && !(node.isMultiInstance || ((node as? ExecutableJoin)?.isMultiMerge ?: false))) throw ProcessException("Attempting to create a new instance $entryNo for node $node that does not support reentry")
  }

  constructor(builder: ProcessNodeInstance.Builder<*, T>) : this(builder.node, builder.predecessors,
                                                                  builder.hProcessInstance, builder.owner,
                                                                  builder.entryNo, builder.handle, builder.state,
                                                                  builder.results, builder.failureCause)


  override fun getHandle() = handle
  final override fun handle() = handle

  override abstract fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<out ExecutableProcessNode, T>

  fun precedingClosure(processData: ProcessEngineDataAccess): Sequence<SecureObject<ProcessNodeInstance<*>>> {
    return predecessors.asSequence().flatMap { predHandle ->
      val pred = processData.nodeInstance(predHandle).withPermission()
      pred.precedingClosure(processData) + sequenceOf(pred)
    }
  }

    /** Update the node. This will store the update based on the transaction. It will return the new object. The old object
   *  may be invalid afterwards.
   */
  fun update(writableEngineData: MutableProcessEngineDataAccess,
                  body: Builder<out ExecutableProcessNode, T>.() -> Unit): PNIPair<T> {
    var nodeFuture: Future<out T>? = null
    val newInstance = writableEngineData.instance(hProcessInstance).withPermission().update(writableEngineData) {
      nodeFuture = update(this, body)
    }

    @Suppress("UNCHECKED_CAST")
    val newNode = nodeFuture?.get() ?: this as T

    return PNIPair<T>(newInstance, newNode).also { newPair ->
      assert(newPair.node == writableEngineData.nodeInstance(this@ProcessNodeInstance.getHandle())) { "The returned node and the stored node don't match for node ${newPair.node.node.id}-${newPair.node.handle}(${newPair.node.state})" }
      assert(newPair.instance.getChild(newPair.node.node.id, newPair.node.entryNo)==newPair.node) { "The instance node and the node don't match for node ${newPair.node.node.id}-${newPair.node.handle}(${newPair.node.state})" }
    }
  }

  fun update(processInstanceBuilder: ProcessInstance.Builder, body: Builder<out ExecutableProcessNode, T>.() -> Unit): Future<out T>? {
    val builder = builder(processInstanceBuilder).apply(body)

    return processInstanceBuilder.storeChild(builder).let { if (builder !is ExtBuilder<*,*> || builder.changed) it else null }
  }

  @Suppress("UNCHECKED_CAST")
  private inline val asT get() = this as T

  override fun withPermission(): ProcessNodeInstance<T> = this

  @Deprecated("Use builder")
  @Throws(SQLException::class)
  open fun tickle(engineData: MutableProcessEngineDataAccess, instance: ProcessInstance, messageService: IMessageService<*>): PNIPair<T> {
    @Suppress("DEPRECATION")
    return when (state) {
      NodeInstanceState.FailRetry,
      Pending -> provideTask(engineData, instance)
      else                      -> PNIPair(instance, asT)
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

  private fun hasDirectPredecessor(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>): Boolean {
    for (pred in predecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(engineData: ProcessEngineDataAccess): Collection<ProcessNodeInstance<*>> {
    return predecessors.asSequence().map {
            engineData.nodeInstance(it).withPermission()
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ComparableHandle<SecureObject<ProcessNodeInstance<*>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    predecessors
          .asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .forEach {
            if (nodeName == it.node.id) {
              return it.getHandle()
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
  fun resolvePredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ProcessNodeInstance<*>? {
    val handle = getPredecessor(engineData, nodeName) ?: throw NullPointerException("Missing predecessor with name ${nodeName} referenced from node ${node.id}")
    return engineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  fun condition(engineData: ProcessEngineDataAccess) = node.condition(engineData, this)

  @Deprecated("Use builder")
  @Throws(SQLException::class)
  fun provideTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    return update(engineData) {
      provideTask(engineData)
    }
  }

  @Deprecated("Use builder")
  @Throws(SQLException::class)
  open fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    val startNext = tryTask(engineData, processInstance) { node.startTask(this) }
    val updatedInstances = update(engineData) { state = Started }
    return if (startNext) {
      updatedInstances.instance.finishTask(engineData, updatedInstances.node, null)
    } else updatedInstances
  }

  @Throws(SQLException::class)
  @Deprecated("This is dangerous, it will not update the instance")
  internal open fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node? = null): PNIPair<T> {
    if (state.isFinal) {
      throw ProcessException("instance ${node.id}:${getHandle()}(${state}) cannot be finished as it is already in a final state.")
    }
    return update(engineData) {
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = Complete
    }.apply { engineData.commit() }
  }

  fun cancelAndSkip(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    return when (state) {
      Pending,
      NodeInstanceState.FailRetry    -> update(engineData) { state = NodeInstanceState.Skipped }
      Sent,
      Taken,
      Acknowledged ->
          cancelTask(engineData, processInstance).update(engineData) { state = NodeInstanceState.Skipped }
      else                           -> PNIPair(processInstance, asT)
    }
  }

  @Throws(SQLException::class)
  open fun cancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    return update(engineData) { state = Cancelled }
  }

  @Throws(SQLException::class)
  open fun tryCancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    try {
      return cancelTask(engineData, processInstance)
    } catch (e: IllegalArgumentException) {
      DefaultProcessNodeInstance.logger.log(Level.WARNING, "Task could not be cancelled")
      return PNIPair(processInstance, asT)
    }
  }

  override fun toString(): String {
    return "${node.javaClass.simpleName}  (${getHandle()}, ${node.id}[$entryNo] - $state)"
  }

  @Throws(SQLException::class)
  open fun failTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<T> {
    return update(engineData) {
      failureCause = cause
      state = if (state == Pending) NodeInstanceState.FailRetry else Failed
    }
  }

  @Throws(SQLException::class)
  open fun failTaskCreation(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<T> {
    return update(engineData) {
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
                                                                      state == Complete, localEndpoint),
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

  @Throws(XmlException::class)
  fun serialize(engineData: ProcessEngineDataAccess, out: XmlWriter, localEndpoint: EndpointDescriptor) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", hProcessInstance.handleValue)

      if (handle.valid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      predecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*, *>)?.message?.messageBody?.let { body ->
        instantiateXmlPlaceholders(engineData, XMLFragmentStreamReader.from(body), out, true, localEndpoint)
      }
    }
  }

  protected inline fun <R> tryCreate(engineData: MutableProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     body: () -> R): R =
    _tryHelper(engineData, processInstance, body) { d, i, e ->
      failTaskCreation(d, i, e)
    }

  protected inline fun <R> tryTask(engineData: MutableProcessEngineDataAccess,
                                   processInstance: ProcessInstance,
                                   body: () -> R): R = _tryHelper(
    engineData, processInstance, body) { d, i, e -> failTask(d, i, e) }

  interface Builder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>>: IProcessNodeInstance {
    override var node: N
    override val predecessors: MutableSet<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
    val processInstanceBuilder: ProcessInstance.Builder
    val hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>> get() = processInstanceBuilder.handle
    var owner: Principal
    var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
    override var state: NodeInstanceState
    val results: MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?): XmlProcessNodeInstance
    override val entryNo: Int
    var failureCause: Throwable?
    fun build(): T

    override fun builder(processInstanceBuilder: ProcessInstance.Builder) = this

    fun failTaskCreation(cause: Throwable) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    }

    /**
     * Store the current state of the builder to the database.
     */
    fun store(engineData: MutableProcessEngineDataAccess) {
      val mutableNodeInstances = engineData.nodeInstances as MutableHandleMap<SecureObject<ProcessNodeInstance<*>>>
      if (handle.valid) mutableNodeInstances[handle] = build() else { processInstanceBuilder.storeChild(this); processInstanceBuilder.store(engineData) }
      engineData.commit()
    }

    fun failTask(engineData: MutableProcessEngineDataAccess, cause: Exception)

    /** Function that will eventually do progression */
    fun provideTask(engineData: MutableProcessEngineDataAccess)
    /** Function that will do provision, but not progress. This is where custom implementations live */
    fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean

    fun takeTask(engineData: MutableProcessEngineDataAccess)
    fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean

    fun startTask(engineData: MutableProcessEngineDataAccess)
    fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean

    fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState)
    fun doSkipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
      if (state.isFinal && state!=newState) throw ProcessException("Attempting to skip a finalised node")
      state = newState
    }

    fun cancel(engineData: MutableProcessEngineDataAccess)
    fun doCancel(engineData: MutableProcessEngineDataAccess) { state = Cancelled }

    fun cancelAndSkip(engineData: MutableProcessEngineDataAccess)
    fun doCancelAndSkip(engineData: MutableProcessEngineDataAccess) {
      @Suppress("NON_EXHAUSTIVE_WHEN")
      when (state) {
        Pending,
        NodeInstanceState.FailRetry    -> state = NodeInstanceState.Skipped
        Sent,
        Taken,
        Acknowledged -> cancel(engineData).also { state = Skipped }
      }
    }

    fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node? = null)

    fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node? = null) {
      if (state.isFinal) {
        throw ProcessException("instance ${node.id}:${handle}(${state}) cannot be finished as it is already in a final state.")
      }
      state= Complete
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
    }
  }

  abstract class AbstractBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>> : Builder<N, T> {

    override fun toXmlInstance(body: CompactFragment?): XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<XmlProcessNodeInstance>(it.handleValue) },
                                    processInstance = hProcessInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    final override fun handle() = handle

    override var failureCause: Throwable? = null

    /**
     * Update the state if the current state would indicate that to be the expected action
     */
    private fun softUpdateState(targetState: NodeInstanceState) {
      if (state==targetState) return
      val doSet = when (targetState) {
        Pending       -> throw IllegalArgumentException ("Updating a state to pending is not allowed")
        Sent          -> state == Pending
        Acknowledged  -> state == Pending || state == Sent
        Taken         -> state == Sent || state == Acknowledged
        Started       -> state == Taken
        Complete      -> state == Started
        SkippedCancel -> state == Pending
        SkippedFail   -> state == Pending
        Skipped       -> false
        else -> TODO("Not needed, not yet implemented, the semantics are not clear yet ($targetState)")
      }
      if (doSet) { state = targetState }
    }

    final override fun provideTask(engineData: MutableProcessEngineDataAccess) {
      if (doProvideTask(engineData).also { softUpdateState(Sent) } ) {
        takeTask(engineData)
      }
    }

    final override fun takeTask(engineData: MutableProcessEngineDataAccess) {
      if (doTakeTask(engineData).also { softUpdateState(Taken) })
        startTask(engineData)
    }

    final override fun startTask(engineData: MutableProcessEngineDataAccess) {
      if (doStartTask(engineData).also { softUpdateState(Started) }) {
        finishTask(engineData)
      }
    }

    final override fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      doFinishTask(engineData, resultPayload)
      softUpdateState(Complete)
      store(engineData)
      processInstanceBuilder.startSuccessors(engineData, this)
    }

    final override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
      assert(newState == Skipped || newState == SkippedCancel || newState == SkippedFail)
      doSkipTask(engineData, newState)
      softUpdateState(newState)
      assert(state == Skipped || state == SkippedCancel || state == SkippedFail)
      processInstanceBuilder.skipSuccessors(engineData, this, newState)
    }

    final override fun failTask(engineData: MutableProcessEngineDataAccess, cause: Exception) {
      failureCause = cause
      state = if (state == Pending) NodeInstanceState.FailRetry else Failed
      processInstanceBuilder.skipSuccessors(engineData, this, SkippedFail)
    }

    final override fun cancel(engineData: MutableProcessEngineDataAccess) {
      doCancel(engineData)
      softUpdateState(Cancelled)
      processInstanceBuilder.skipSuccessors(engineData, this, SkippedCancel)
    }

    final override fun cancelAndSkip(engineData: MutableProcessEngineDataAccess) {
      doCancelAndSkip(engineData)
      processInstanceBuilder.skipSuccessors(engineData, this, SkippedCancel)
    }

    override fun toString(): String {
      return "${node.javaClass.simpleName}  (${handle()}, ${node.id}[$entryNo] - $state)"
    }

  }

  abstract class BaseBuilder<N:ExecutableProcessNode, T: ProcessNodeInstance<T>>(
    final override var node: N,
    predecessors: Iterable<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
    final override val processInstanceBuilder: ProcessInstance.Builder,
    final override var owner: Principal,
    final override val entryNo: Int,
    final override var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
    final override var state: NodeInstanceState = Pending) : AbstractBuilder<N, T>() {

    final override var predecessors :MutableSet<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> = predecessors.toMutableArraySet()

    final override val results = mutableListOf<ProcessData>()
  }

  abstract class ExtBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>>(protected val base: T, override val processInstanceBuilder: ProcessInstance.Builder) : AbstractBuilder<N, T>() {
    protected val observer = { changed = true }

    final override var predecessors = ObservableSet(base.predecessors.toMutableArraySet(), { changed = true })
    final override var owner by overlay(observer) { base.owner }
    final override var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> by overlay(observer) { base.getHandle() }
    final override var state by overlay(observer) { base.state }
    final override var results = ObservableList(base.results.toMutableList(), { changed = true })
    var changed: Boolean = false
    final override val entryNo: Int = base.entryNo

    override abstract fun build(): T

  }

  companion object {

    @JvmStatic
    protected inline fun <R> ProcessNodeInstance.Builder<*,*>.tryTask(body: () -> R): R = _tryHelper(
      body) { e -> failTaskCreation(e) }

    @PublishedApi
    internal inline fun <R> _tryHelper(engineData: MutableProcessEngineDataAccess,
                                       processInstance: ProcessInstance,
                                       body: () -> R, failHandler: (MutableProcessEngineDataAccess, ProcessInstance, Exception) -> Unit): R {
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

    @PublishedApi
    internal inline fun <R> _tryHelper(body: () -> R, failHandler: (Exception) -> Unit): R {
      return try {
        body()
      } catch (e: Exception) {
        try {
          failHandler(e)
        } catch (f: Exception) {
          e.addSuppressed(f)
        }
        throw e
      }
    }
  }

}