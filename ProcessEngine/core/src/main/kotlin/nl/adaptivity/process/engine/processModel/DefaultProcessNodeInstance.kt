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
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableActivity
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
import java.util.logging.Logger
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Class to represent the instanciation of a node. Subclasses may add behaviour.
 *
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
@XmlDeserializer(DefaultProcessNodeInstance.Factory::class)
class DefaultProcessNodeInstance : ProcessNodeInstance<DefaultProcessNodeInstance> {

  constructor(node: ExecutableProcessNode,
              predecessors: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
              owner: Principal,
              entryNo: Int,
              handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList(),
              failureCause: Throwable? = null)
    : super(node, predecessors, hProcessInstance, owner, entryNo, handle, state, results, failureCause)

  constructor(node: ExecutableProcessNode,
              predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
              processInstance: ProcessInstance,
              entryNo: Int)
    : this(node, if (predecessor.valid) listOf(predecessor) else emptyList(), processInstance.getHandle(),
           processInstance.owner, entryNo = entryNo)

  constructor(builder: Builder) : super(builder)

  override fun withPermission() = this

  override final fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<out ExecutableProcessNode, DefaultProcessNodeInstance> {
    return ExtBuilderImpl(this, processInstanceBuilder)
  }

  @Throws(SQLException::class, XmlException::class)
  fun toSerializable(engineData: ProcessEngineDataAccess, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
    val builder = ExtBuilderImpl(this, engineData.instance(hProcessInstance).withPermission().builder())

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as DefaultProcessNodeInstance

    if (hProcessInstance != other.hProcessInstance) return false
    if (state != other.state) return false
    if (failureCause != other.failureCause) return false
    if (node != other.node) return false
    if (handle != other.handle) return false
    if (results != other.results) return false
    if (predecessors != other.predecessors) return false
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
    result = 31 * result + predecessors.hashCode()
    result = 31 * result + owner.hashCode()
    return result
  }

  interface Builder: ProcessNodeInstance.Builder<ExecutableProcessNode, DefaultProcessNodeInstance> {

    override fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean {

      if (! handle.valid) store(engineData)
      assert(handle.valid)

      val node = this.node // Create a local copy to prevent races - and shut up Kotlin about the possibilities as it should be immutable

      fun <MSG_T> impl(messageService: IMessageService<MSG_T>): Boolean {

        val shouldProgress = tryTask { node.provideTask(engineData, this) }

        if (node is ExecutableActivity) {
          val preparedMessage = messageService.createMessage(node.message)
          if (! tryTask() { messageService.sendMessage(engineData, preparedMessage, this) }) {
            failTaskCreation(MessagingException("Failure to send message"))
          }
        }

        return shouldProgress

      }
      return impl(engineData.messageService())
    }

    override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.takeTask(this)
    }

    override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return node.startTask(this)
    }
  }

  private class ExtBuilderImpl(base: DefaultProcessNodeInstance, processInstanceBuilder: ProcessInstance.Builder) : ExtBuilder<ExecutableProcessNode, DefaultProcessNodeInstance>(base, processInstanceBuilder), Builder {
    override var node: ExecutableProcessNode by overlay { base.node }
    override fun build() = if (changed) DefaultProcessNodeInstance(this) else base

  }

  class BaseBuilder(
    node: ExecutableProcessNode,
    predecessors: Iterable<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
    processInstanceBuilder: ProcessInstance.Builder,
    owner: Principal,
    entryNo: Int,
    handle: ComparableHandle<SecureObject<DefaultProcessNodeInstance>> = Handles.getInvalid(),
    state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableProcessNode, DefaultProcessNodeInstance>(node, predecessors, processInstanceBuilder, owner, entryNo, handle, state), Builder {

    override fun build() = DefaultProcessNodeInstance(this)
  }

  class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance.deserialize(reader)
    }
  }

  companion object {


    internal val logger by lazy { Logger.getLogger(DefaultProcessNodeInstance::class.java.getName()) }

    fun build(node: ExecutableProcessNode,
              predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstanceBuilder: ProcessInstance.Builder,
              handle: ComparableHandle<SecureObject<DefaultProcessNodeInstance>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              entryNo: Int,
              body: Builder.() -> Unit): DefaultProcessNodeInstance {
      return DefaultProcessNodeInstance(BaseBuilder(node, predecessors, processInstanceBuilder, processInstanceBuilder.owner,
                                                    entryNo, handle, state).apply(body))
    }


    fun build(node: ExecutableProcessNode,
              predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
              processInstance: ProcessInstance,
              handle: ComparableHandle<SecureObject<DefaultProcessNodeInstance>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              entryNo: Int,
              body: Builder.() -> Unit): DefaultProcessNodeInstance {
      return build(node, predecessors, processInstance.builder(), handle, state, entryNo, body)
    }


  }

}
