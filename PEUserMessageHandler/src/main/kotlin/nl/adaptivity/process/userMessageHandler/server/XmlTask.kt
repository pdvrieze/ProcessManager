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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.devrieze.util.Handle
import net.devrieze.util.HandleSerializer
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.client.ServletProcessEngineClient
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
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

@Serializable
@XmlSerialName("task", "http://adaptivity.nl/userMessageHandler", "umh")
class XmlTask : UserTask<XmlTask> {

    @Serializable(HandleSerializer::class)
    override var handle = Handle.invalid<XmlTask>()
        private set

    override val handleValue: Long
        get() = handle.handleValue

    override fun setHandleValue(handleValue: Long) {
        if (handle.handleValue != handleValue) this.handle =
            if (handleValue < 0) Handle.invalid() else Handle(handleValue)
    }

    @Serializable(HandleSerializer::class)
    @SerialName("remotehandle")
    override var remoteHandle: Handle<Unit> = Handle.invalid()

    @Serializable(HandleSerializer::class)
    @SerialName("instancehandle")
    override var instanceHandle: Handle<Unit> = Handle.invalid()

    // TODO make this not-nullable
    @XmlElement(false)
    override var state: NodeInstanceState? = NodeInstanceState.Sent

    override var summary: String? = null

    private var endPoint: EndpointDescriptorImpl? = null

    @Serializable(AccessRestrictionSerializer::class)
    @SerialName("restrictTo")
    @XmlElement(false)
    override var accessRestriction: AccessRestriction? = null

    @Serializable(PrincipalNameSerializer::class)
    override var owner: PrincipalCompat? = null

    @SerialName("item")
    private val _items = mutableListOf<XmlItem>()

    /*, namespace=Constants.USER_MESSAGE_HANDLER_NS*/
    override var items: List<XmlItem>
        get() = _items
        set(value) {
            _items.replaceBy(value)
        }

    constructor()

    constructor(handle: Long) {
        setHandleValue(handle)
    }

    constructor(task: UserTask<*>) : this(task.handleValue) {
        @Suppress("UNCHECKED_CAST")
        this.remoteHandle = task.remoteHandle as Handle<Unit>
        @Suppress("UNCHECKED_CAST")
        this.instanceHandle = task.instanceHandle as Handle<Unit>
        this.state = task.state
        this.summary = task.summary
        this.endPoint = null
        this.owner = task.owner
        this.items = XmlItem.get(task.items)
    }

    override fun setState(newState: NodeInstanceState, user: Principal) { // TODO handle transactions
        try {
            state = when (newState) {
                NodeInstanceState.Complete     -> finishRemoteTask(user).get()
                NodeInstanceState.Cancelled    -> cancelRemoteTask(user).get()
                NodeInstanceState.Acknowledged -> newState // Just shortcircuit. This is just record keeping
                else                           -> updateRemoteTaskState(newState, user).get()
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
        return ServletProcessEngineClient.finishTask(
            remoteHandle.handleValue,
            createResult(),
            user,
            null
        ) // Ignore completion???
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
        this.endPoint = endPoint
    }

    fun getItem(name: String) = items.firstOrNull { name == it.name }
    operator fun get(name: String) = getItem(name)

    override fun hashCode(): Int {
        var result = handle.hashCode()
        result = 31 * result + remoteHandle.handleValue.hashCode()
        result = 31 * result + instanceHandle.handleValue.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + endPoint.hashCode()
        result = 31 * result + accessRestriction.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + _items.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XmlTask

        if (handle != other.handle) return false
        if (remoteHandle != other.remoteHandle) return false
        if (instanceHandle != other.instanceHandle) return false
        if (state != other.state) return false
        if (summary != other.summary) return false
        if (endPoint != other.endPoint) return false
        if (accessRestriction != other.accessRestriction) return false
        if (owner != other.owner) return false
        if (_items != other._items) return false

        return true
    }

    companion object {

        val ELEMENTLOCALNAME = "task"
        val ELEMENTNAME = QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh")

        @Throws(XmlException::class)
        fun deserialize(xmlReader: XmlReader): XmlTask {
            return XML.decodeFromReader<XmlTask>(xmlReader)
        }

        @JvmStatic
        fun get(task: UserTask<*>): XmlTask {
            if (task is XmlTask) {
                return task
            }
            return XmlTask(task)
        }
    }

    internal class PrincipalNameSerializer: KSerializer<Principal> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Principal", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Principal) {
            encoder.encodeString(value.name)
        }

        override fun deserialize(decoder: Decoder): Principal {
            return SimplePrincipal(decoder.decodeString())
        }
    }

    internal class AccessRestrictionSerializer: KSerializer<AccessRestriction> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AccessRestriction", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: AccessRestriction) {
            encoder.encodeString(value.serializeToString())
        }

        override fun deserialize(decoder: Decoder): AccessRestriction {
            val str = decoder.decodeString().trim()
            return when(str[0]) {
                '@' -> when(str[1]) {
                    '[' -> {
                        check(str.last()==']')
                        str.substring(2, str.length-1).splitToSequence(",").map { it.trim() }.let { roles ->
                            RoleRestriction(roles.toSet())
                        }
                    }

                    else -> RoleRestriction(str.substring(1).trimStart())
                }
                '[' -> {
                    check(str.last()==']')
                    str.substring(1, str.length-1).splitToSequence(",").map { it.trim() }.let { users ->
                        UserRestriction(users.toSet())
                    }
                }
                else -> UserRestriction(str)
            }
        }
    }

}

