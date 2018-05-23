/*
 * Copyright (c) 2018.
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
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.ICompactFragment
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
                                                              processInstanceBuilder: ProcessInstance.Builder,
                                                              val hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                                                              override final val owner: Principal,
                                                              override final val entryNo: Int,
                                                              private var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
                                                              override final val state: NodeInstanceState = Pending,
                                                              results: Iterable<ProcessData> = emptyList(),
                                                              val failureCause: Throwable? = null) : SecureObject<ProcessNodeInstance<T>>, ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>>, IProcessNodeInstance {
  val results: List<ProcessData> = results.toList()
  override val predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> = predecessors.asSequence().filter { it.isValid }.toArraySet()

  init {
    @Suppress("LeakingThis")
    if (state!=SkippedInvalidated && !(node.isMultiInstance || ((node as? ExecutableJoin)?.isMultiMerge ?: false))) {
      if(processInstanceBuilder.allChildren { it.node==node && it.entryNo!=entryNo && it.state!=SkippedInvalidated }.any()) {
        throw ProcessException("Attempting to create a new instance $entryNo for node $node that does not support reentry")
      }
    }
  }

  constructor(builder: ProcessNodeInstance.Builder<*, T>) : this(builder.node, builder.predecessors,
                                                                  builder.processInstanceBuilder,
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

  fun update(processInstanceBuilder: ProcessInstance.Builder, body: Builder<out ExecutableProcessNode, T>.() -> Unit): Future<out T>? {
    val builder = builder(processInstanceBuilder).apply(body)

    return processInstanceBuilder.storeChild(builder).let { if (builder !is ExtBuilder<*,*> || builder.changed) it else null }
  }

  @Suppress("UNCHECKED_CAST")
  private inline val asT get() = this as T

  override fun withPermission(): ProcessNodeInstance<T> = this

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
    return predecessors.any { it.handleValue == handle.handleValue }
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
    val handle = getPredecessor(engineData, nodeName) ?: throw NullPointerException("Missing predecessor with name $nodeName referenced from node ${node.id}")
    return engineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  override fun toString(): String {
    return "${node.javaClass.simpleName}  (${getHandle()}, ${node.id}[$entryNo] - $state)"
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
                                 localEndpoint: EndpointDescriptor): ICompactFragment
  {
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

      if (handle.isValid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      predecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*, *>)?.message?.messageBody?.let { body ->
        instantiateXmlPlaceholders(engineData, body.getXmlReader(), out, true, localEndpoint)
      }
    }
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(engineData: ProcessEngineDataAccess, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
    val builder = builder(engineData.instance(hProcessInstance).withPermission().builder())

    val body: ICompactFragment? = (node as? Activity<*,*>)?.message?.let { message ->
      try {
        val xmlReader = message.messageBody.getXmlReader()
        instantiateXmlPlaceholders(engineData, xmlReader, true, localEndpoint)
      } catch (e: XmlException) {
        DefaultProcessNodeInstance.logger.log(Level.WARNING, "Error processing body", e)
        throw e
      }
    }

    return builder.toXmlInstance(body)
  }

  interface Builder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>>: IProcessNodeInstance {
    override var node: N
    override val predecessors: MutableSet<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
    val processInstanceBuilder: ProcessInstance.Builder
    val hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>> get() = processInstanceBuilder.handle
    var owner: Principal
    var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
    override var state: NodeInstanceState
    val results: MutableList<ProcessData>
    fun toXmlInstance(body: ICompactFragment?): XmlProcessNodeInstance
    override val entryNo: Int
    var failureCause: Throwable?

    fun invalidateBuilder(engineData: ProcessEngineDataAccess)

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
      if (handle.isValid) mutableNodeInstances[handle] = build() else { processInstanceBuilder.storeChild(this) }
      // Must be updated as well as the process node instance may mean the process instance is changed.
      processInstanceBuilder.store(engineData)
      engineData.commit()
    }

    fun failTask(engineData: MutableProcessEngineDataAccess, cause: Throwable)

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
      if (state.isFinal && state!=newState) {
        throw ProcessException("Attempting to skip a finalised node ${node.id}(${handle}-$entryNo)")
      }
      state = newState
    }

    fun invalidateTask(engineData: MutableProcessEngineDataAccess)

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
        throw ProcessException("instance ${node.id}:$handle($state) cannot be finished as it is already in a final state.")
      }
      state= Complete
      node.results.mapTo(results.apply{clear()}) { it.applyData(resultPayload) }
    }


    @Throws(SQLException::class)
    fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
      when (state) {
        NodeInstanceState.FailRetry,
        Pending -> provideTask(engineData)
      }// ignore
    }

  }

  abstract class AbstractBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>> : Builder<N, T> {

    override fun toXmlInstance(body: ICompactFragment?): XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { handle<XmlProcessNodeInstance>(handle= it.handleValue) },
                                    processInstance = hProcessInstance.handleValue,
                                    handle = handle(handle= handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    final override fun handle() = handle

    override var failureCause: Throwable? = null

    /**
     * Update the state if the current state would indicate that to be the expected action
     */
    private fun softUpdateState(engineData: MutableProcessEngineDataAccess, targetState: NodeInstanceState) {
      invalidateBuilder(engineData)
      if (state==targetState) return
      val doSet = when (targetState) {
        Pending       -> throw IllegalArgumentException ("Updating a state to pending is not allowed")
        Sent          -> state == Pending
        Acknowledged  -> state == Pending || state == Sent
        Taken         -> state == Sent || state == Acknowledged
        Started       -> state == Taken || state == Sent || state == Acknowledged
        Complete      -> state == Started
        SkippedCancel -> state == Pending
        SkippedFail   -> state == Pending
        Skipped       -> false
        else -> TODO("Not needed, not yet implemented, the semantics are not clear yet ($targetState)")
      }
      if (doSet) {
        state = targetState
        store(engineData)
      }
    }

    final override fun provideTask(engineData: MutableProcessEngineDataAccess) {
      if (this !is JoinInstance.Builder) {
        val predecessors = predecessors.map { engineData.nodeInstance(it).withPermission() }
        for (predecessor in predecessors) {
          if (predecessor !is SplitInstance && !predecessor.state.isFinal) {
            throw ProcessException("Attempting to start successor ${node.id}[$handle] for non-final predecessor ${predecessor.node.id}[${predecessor.handle} - ${predecessor.state}]")
          }
        }
      }
      if (doProvideTask(engineData).also { softUpdateState(engineData, Sent) } ) {
        takeTask(engineData)
      }
    }

    final override fun takeTask(engineData: MutableProcessEngineDataAccess) {
      if (doTakeTask(engineData).also { softUpdateState(engineData, Taken) })
        startTask(engineData)
    }

    final override fun startTask(engineData: MutableProcessEngineDataAccess) {
      if (doStartTask(engineData).also { softUpdateState(engineData, Started) }) {
        finishTask(engineData)
      }
    }

    final override fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      if (state.isFinal) {
        throw ProcessException("instance ${node.id}:${handle()}($state) cannot be finished as it is already in a final state.")
      }
      doFinishTask(engineData, resultPayload)
      state = Complete
      store(engineData)
      engineData.commit()

      // The splits need to be updated before successors are started. This prevents unneeded/unexpected cancellations.
      // Joins should trigger updates before cancellations anyway though as a safeguard.
      processInstanceBuilder.updateSplits(engineData)
      processInstanceBuilder.startSuccessors(engineData, this)
      processInstanceBuilder.updateSplits(engineData)
      processInstanceBuilder.updateState(engineData)
    }

    override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
      assert(newState == Skipped || newState == SkippedCancel || newState == SkippedFail)
      doSkipTask(engineData, newState)
      softUpdateState(engineData, newState)
      store(engineData)
      processInstanceBuilder.storeChild(this)
      assert(state == Skipped || state == SkippedCancel || state == SkippedFail)
      processInstanceBuilder.skipSuccessors(engineData, this, newState)
    }

    override fun invalidateTask(engineData: MutableProcessEngineDataAccess) {
      if (! (state.isSkipped || state == Pending ||state==Sent)) {
        throw ProcessException("Attempting to invalidate a non-skipped node $this with state: $state")
      }
      state = SkippedInvalidated
      store(engineData)
      processInstanceBuilder.storeChild(this)
    }

    final override fun failTask(engineData: MutableProcessEngineDataAccess, cause: Throwable) {
      failureCause = cause
      state = if (state == Pending) NodeInstanceState.FailRetry else Failed
      processInstanceBuilder.skipSuccessors(engineData, this, SkippedFail)
    }

    final override fun cancel(engineData: MutableProcessEngineDataAccess) {
      doCancel(engineData)
      softUpdateState(engineData, Cancelled)
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
      final override var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
      state: NodeInstanceState = Pending) : AbstractBuilder<N, T>() {

    final override var state = state
      set(value) {
        field = value
        processInstanceBuilder.storeChild(this)
      }


    override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
      engineData.nodeInstances[handle]?.withPermission()?.let { newBase ->
        @Suppress("UNCHECKED_CAST")
        node = newBase.node as N
        predecessors.replaceBy(newBase.predecessors)
        owner = newBase.owner
        state = newBase.state
      }
    }

    final override var predecessors :MutableSet<net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> = predecessors.toMutableArraySet()
    final override val results = mutableListOf<ProcessData>()
  }

  abstract class ExtBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>>(protected var base: T, override val processInstanceBuilder: ProcessInstance.Builder) : AbstractBuilder<N, T>() {
    protected val observer = { changed = true }

    final override var predecessors = ObservableSet(base.predecessors.toMutableArraySet(), { changed = true })
    final override var owner by overlay(observer) { base.owner }
    final override var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> by overlay(observer) { base.getHandle() }
    final override var state = base.state
      set(value) {
        if (field != value) {
          field = value
          changed = true
          processInstanceBuilder.storeChild(this)
        }
      }
    final override var results = ObservableList(base.results.toMutableList(), { changed = true })
    var changed: Boolean = false
    final override val entryNo: Int = base.entryNo

    @Suppress("UNCHECKED_CAST")
    override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
      changed = false
      base = engineData.nodeInstance(handle).withPermission() as T
    }

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