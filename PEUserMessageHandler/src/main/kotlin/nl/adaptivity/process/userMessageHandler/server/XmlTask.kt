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

package nl.adaptivity.process.userMessageHandler.server

import kotlinx.serialization.SerialName
import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.handle
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.client.ServletProcessEngineClient
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import java.security.Principal
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.bind.JAXBException
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class XmlTask: UserTask<XmlTask> {

  override var handle = getInvalidHandle<XmlTask>()
    private set

  override val handleValue: Long
    get() = handle.handleValue

  override fun setHandleValue(handleValue: Long) {
    if (handle.handleValue!=handleValue) this.handle = handle(handleValue)
  }

  override var remoteHandle: Handle<*> = getInvalidHandle<Any>()

  override var instanceHandle: Handle<*> = getInvalidHandle<Any>()

  // TODO make this not-nullable
  override var state: NodeInstanceState? = NodeInstanceState.Sent

  override var summary: String? = null

  private var mEndPoint: EndpointDescriptorImpl? = null

  override var owner: Principal? = null

  private val _items by lazy { mutableListOf<XmlItem>() }

  /*, namespace=Constants.USER_MESSAGE_HANDLER_NS*/
  @SerialName(value = "item")
  override var items: List<XmlItem>
    get() = _items
    set(value) {
      _items.replaceBy(value)
    }

  constructor()

  constructor(handle: Long) {
    setHandleValue(handle)
  }

  constructor(task: UserTask<*>):this(task.handleValue) {
    this.remoteHandle = task.remoteHandle
    this.instanceHandle = task.instanceHandle
    this.state = task.state
    this.summary = task.summary
    this.mEndPoint = null
    this.owner = task.owner
    this.items = XmlItem.get(task.items)
  }

  override fun setState(newState: NodeInstanceState, user: Principal) { // TODO handle transactions
    try {
      state = when (newState) {
        NodeInstanceState.Complete -> finishRemoteTask(user).get()
        NodeInstanceState.Cancelled -> cancelRemoteTask(user).get()
        NodeInstanceState.Acknowledged -> newState // Just shortcircuit. This is just record keeping
        else -> updateRemoteTaskState(newState, user).get()
      }
    } catch (e: XmlException) {
      Logger.getLogger(javaClass.canonicalName).throwing("XmlTask", "setState", e)
    } catch (e: JAXBException) {
      Logger.getLogger(javaClass.canonicalName).throwing("XmlTask", "setState", e)
    } catch (e: MessagingException) {
      Logger.getLogger(javaClass.canonicalName).throwing("XmlTask", "setState", e)
    } catch (e: InterruptedException) {
      Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e)
    } catch (e: ExecutionException) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e)
    }

  }

  @Throws(JAXBException::class, MessagingException::class, XmlException::class)
  private fun updateRemoteTaskState(state: NodeInstanceState, user: Principal): Future<NodeInstanceState> {
    return ServletProcessEngineClient.updateTaskState(remoteHandle.handleValue, state, user, null)
  }

  @Throws(JAXBException::class, MessagingException::class, XmlException::class)
  private fun finishRemoteTask(user: Principal): Future<NodeInstanceState> {
    return ServletProcessEngineClient.finishTask(remoteHandle.handleValue, createResult(), user, null) // Ignore completion???
  }

  @Throws(JAXBException::class, MessagingException::class, XmlException::class)
  private fun cancelRemoteTask(user: Principal): Future<NodeInstanceState> {
    // For now don't do anything special
    return updateRemoteTaskState(NodeInstanceState.Cancelled, user)
  }

  private fun createResult(): DocumentFragment {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = true
    val document: Document
    try {
      document = dbf.newDocumentBuilder().newDocument()
    } catch (e: ParserConfigurationException) {
      throw RuntimeException(e)
    }

    val fragment = document.createDocumentFragment()

    val outer = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "result")
    outer.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", Constants.USER_MESSAGE_HANDLER_NS)
    for (item in items) {
      if ("label" != item.type) {
        val inner = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "value")
        inner.setAttribute("name", item.name)
        if (item.value != null) {
          inner.textContent = item.value
        }
        outer.appendChild(inner)
      }
    }

    fragment.appendChild(outer)
    return fragment
  }

  /** Set the endpoint that is used for updating the task state  */
  override fun setEndpoint(endPoint: EndpointDescriptorImpl) {
    mEndPoint = endPoint
  }

  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    result = prime * result + if (mEndPoint == null) 0 else mEndPoint!!.hashCode()
    result = prime * result + (handleValue xor handleValue.ushr(32)).toInt()
    result = prime * result + if (_items.isEmpty()) 0 else _items.hashCode()
    result = prime * result + if (owner == null) 0 else owner!!.hashCode()
    result = prime * result + (remoteHandle.handleValue xor remoteHandle.handleValue.ushr(32)).toInt()
    result = prime * result + if (state == null) 0 else state!!.hashCode()
    result = prime * result + if (summary == null) 0 else summary!!.hashCode()
    return result
  }

  override fun equals(obj: Any?): Boolean {
    if (this === obj)
      return true
    if (obj == null)
      return false
    if (javaClass != obj.javaClass)
      return false
    val other = obj as XmlTask?
    if (mEndPoint == null) {
      if (other!!.mEndPoint != null)
        return false
    } else if (mEndPoint != other!!.mEndPoint)
      return false
    if (handleValue != other.handleValue)
      return false
    if (_items.isEmpty()) {
      if (other._items.isNotEmpty())
        return false
    } else if (_items != other._items)
      return false
    if (owner == null) {
      if (other.owner != null)
        return false
    } else if (owner != other.owner)
      return false
    if (remoteHandle != other.remoteHandle)
      return false
    if (state != other.state)
      return false
    if (summary == null) {
      if (other.summary != null)
        return false
    } else if (summary != other.summary)
      return false
    return true
  }

  fun getItem(name: String) = items.firstOrNull { name == it.name }
  operator fun get(name:String) = getItem(name)

  companion object {

    val ELEMENTLOCALNAME = "task"
    val ELEMENTNAME = QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh")

    @Throws(XmlException::class)
    fun deserialize(xmlReader: XmlReader): XmlTask {
        return XML.decodeFromReader<XmlTask>(xmlReader)
    }

    @JvmStatic fun get(task:UserTask<*>) : XmlTask {
      if (task is XmlTask) {
        return task
      }
      return XmlTask(task)
    }
  }


}
