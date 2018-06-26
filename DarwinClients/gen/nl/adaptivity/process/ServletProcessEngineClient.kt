/*
 * Generated by MessagingSoapClientGenerator.
 * Source class: nl.adaptivity.process.engine.servlet.ServletProcessEngine
 */

@file:Suppress("all")
package nl.adaptivity.process.client

import java.net.URI
import java.security.Principal
import java.util.Arrays
import java.util.concurrent.Future

import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.namespace.QName
import javax.xml.transform.Source

import net.devrieze.util.Tripple

import nl.adaptivity.messaging.CompletionListener
import nl.adaptivity.messaging.Endpoint
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.messaging.MessagingRegistry
import nl.adaptivity.messaging.SendableSoapSource
import nl.adaptivity.process.engine.XmlHandle
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.processModel.engine.XmlProcessModel.Builder
import nl.adaptivity.ws.soap.SoapHelper
import nl.adaptivity.xml.XmlException

import org.w3c.dom.Node

object ServletProcessEngineClient {

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun finishTask(handle: Long, payload: Node, principal: Principal, completionListener: CompletionListener<NodeInstanceState>?, vararg jaxbcontext: Class<*>): Future<NodeInstanceState> {
    val param0 = Tripple.tripple("handle", Long::class.java, handle)
    val param1 = Tripple.tripple("payload", Node::class.java, payload)

    val message = SoapHelper.createMessage(QName("finishTask"), listOf(JAXBElement<String>(QName("http://adaptivity.nl/ProcessEngine/","principal"), String::class.java, principal?.name)), listOf(param0, param1))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, NodeInstanceState::class.java, jaxbcontext)
  }

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun getProcessNodeInstance(handle: Long, user: Principal, completionListener: CompletionListener<XmlProcessNodeInstance?>?, vararg jaxbcontext: Class<*>): Future<XmlProcessNodeInstance?> {
    val param0 = Tripple.tripple("handle", Long::class.java, handle)
    val param1 = Tripple.tripple("user", Principal::class.java, user)

    val message = SoapHelper.createMessage(QName("getProcessNodeInstance"), listOf(param0, param1))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, XmlProcessNodeInstance::class.java, jaxbcontext)
  }

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun postProcessModel(processModel: Builder?, principal: Principal?, completionListener: CompletionListener<ProcessModelRef<*,*,*>?>?, vararg jaxbcontext: Class<*>): Future<ProcessModelRef<*,*,*>?> {
    val param0 = Tripple.tripple("processModel", Builder::class.java, processModel)

    val message = SoapHelper.createMessage(QName("postProcessModel"), listOf(JAXBElement<String>(QName("http://adaptivity.nl/ProcessEngine/","principal"), String::class.java, principal?.name)), listOf(param0))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, ProcessModelRef::class.java, jaxbcontext)
  }

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun startProcess(handle: Long, name: String?, uuid: String?, owner: Principal, completionListener: CompletionListener<XmlHandle<*>>?, vararg jaxbcontext: Class<*>): Future<XmlHandle<*>> {
    val param0 = Tripple.tripple("handle", Long::class.java, handle)
    val param1 = Tripple.tripple("name", String::class.java, name)
    val param2 = Tripple.tripple("uuid", String::class.java, uuid)
    val param3 = Tripple.tripple("owner", Principal::class.java, owner)

    val message = SoapHelper.createMessage(QName("startProcess"), listOf(param0, param1, param2, param3))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, XmlHandle::class.java, jaxbcontext)
  }

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun updateProcessModel(handle: Long, processModel: nl.adaptivity.process.processModel.RootProcessModel.Builder<*,*>?, principal: Principal?, completionListener: CompletionListener<ProcessModelRef<*,*,*>>?, vararg jaxbcontext: Class<*>): Future<ProcessModelRef<*,*,*>> {
    val param0 = Tripple.tripple("handle", Long::class.java, handle)
    val param1 = Tripple.tripple("processModel", nl.adaptivity.process.processModel.RootProcessModel.Builder::class.java, processModel)
    val param2 = Tripple.tripple("principal", Principal::class.java, principal)

    val message = SoapHelper.createMessage(QName("updateProcessModel"), listOf(param0, param1, param2))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, ProcessModelRef::class.java, jaxbcontext)
  }

  @JvmStatic
  @Throws(JAXBException::class, XmlException::class)  fun updateTaskState(handle: Long, state: NodeInstanceState, user: Principal, completionListener: CompletionListener<NodeInstanceState>?, vararg jaxbcontext: Class<*>): Future<NodeInstanceState> {
    val param0 = Tripple.tripple("handle", Long::class.java, handle)
    val param1 = Tripple.tripple("state", NodeInstanceState::class.java, state)
    val param2 = Tripple.tripple("user", Principal::class.java, user)

    val message = SoapHelper.createMessage(QName("updateTaskState"), listOf(param0, param1, param2))

    val endpoint = EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION)

    return MessagingRegistry.sendMessage(SendableSoapSource(endpoint, message), completionListener, NodeInstanceState::class.java, jaxbcontext)
  }

  @JvmStatic  private val SERVICE: QName = QName("http://adaptivity.nl/ProcessEngine/", "ProcessEngine", "")
  private const val ENDPOINT = "soap"
  @JvmStatic  private val LOCATION: URI? = null

}
