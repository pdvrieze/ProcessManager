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

package nl.adaptivity.process.engine.servlet

import net.devrieze.util.ComparableHandle
import net.devrieze.util.*
import net.devrieze.util.security.AuthenticationNeededException
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import nl.adaptivity.io.Writable
import nl.adaptivity.io.WritableReader
import nl.adaptivity.messaging.*
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance
import nl.adaptivity.process.messaging.ActivityResponse
import nl.adaptivity.process.messaging.EndpointServlet
import nl.adaptivity.process.messaging.GenericEndpoint
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.rest.annotations.HttpMethod
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.rest.annotations.RestParam
import nl.adaptivity.rest.annotations.RestParamType
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xml.*
import org.jetbrains.annotations.TestOnly
import org.w3.soapEnvelope.Envelope
import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebParam.Mode
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.namespace.QName
import javax.xml.stream.*
import javax.xml.stream.events.*
import javax.xml.stream.events.Namespace
import javax.xml.transform.Result
import javax.xml.transform.Source

import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger


/**
 * The service representing a process engine.
 *
 * @param TR The type of transaction used. Mainly used for testing with memory based storage
 */
/*
@ServiceInfo(targetNamespace = ServletProcessEngine.SERVICE_NS,
             interfaceNS = ServletProcessEngine.SERVICE_NS,
             interfaceLocalname = "soap",
             interfacePrefix = "pe",
             serviceLocalname = ServletProcessEngine.SERVICE_LOCALNAME)
*/
open class ServletProcessEngine<TR : ProcessTransaction> : EndpointServlet(), GenericEndpoint {

    private lateinit var processEngine: ProcessEngine<TR>
    private lateinit var messageService: MessageService

    override val serviceName: QName
        get() = QName(Constants.PROCESS_ENGINE_NS, SERVICE_LOCALNAME)

    override val endpointName: String
        get() = "soap"

    override val endpointLocation: URI?
        get() = null

    inner class MessageService(localEndpoint: EndpointDescriptor) : IMessageService<ServletProcessEngine.NewServletMessage> {

        override var localEndpoint: EndpointDescriptor = localEndpoint
            internal set


        override fun createMessage(message: IXmlMessage): NewServletMessage {
            return NewServletMessage(message, localEndpoint)
        }

        override fun sendMessage(engineData: MutableProcessEngineDataAccess,
                                 protoMessage: NewServletMessage,
                                 instanceBuilder: ProcessNodeInstance.Builder<*, *>): Boolean {
            val nodeHandle = instanceBuilder.handle

            protoMessage.setHandle(engineData, instanceBuilder)

            val result = MessagingRegistry.sendMessage(protoMessage,
                                                       MessagingCompletionListener(nodeHandle,
                                                                                   protoMessage.owner),
                                                       DataSource::class.java, emptyArray())
            if (result.isCancelled) {
                return false
            }
            if (result.isDone) {
                try {
                    result.get()
                } catch (e: ExecutionException) {
                    val cause = e.cause
                    if (cause is RuntimeException) {
                        throw cause
                    }
                    throw RuntimeException(cause)
                } catch (e: InterruptedException) {
                    return false
                }

            }
            return true
        }

    }

    private inner class MessagingCompletionListener(private val handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                                                    private val owner: Principal) : CompletionListener<DataSource> {

        override fun onMessageCompletion(future: Future<out DataSource>) {
            try {
                this@ServletProcessEngine.onMessageCompletion(future, handle, owner)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }

        }

    }

    class NewServletMessage(private val message: IXmlMessage,
                            private val localEndpoint: EndpointDescriptor) : ISendableMessage, Writable {

        private var nodeInstance: ProcessNodeInstance<*>? = null

        private var data: Writable? = null


        internal val owner: Principal
            get() = nodeInstance?.owner ?: throw IllegalStateException("The message has not been initialised with a node yet")

        val source: XmlReader
            get() = message.messageBody.getXmlReader()

        override fun getDestination(): EndpointDescriptor? {
            return message.endpointDescriptor
        }

        override fun getMethod(): String? {
            return message.method
        }

        override fun getHeaders(): Collection<ISendableMessage.IHeader> {
            val contentType = message.contentType
            return if (contentType.isNullOrEmpty()) {
                emptyList()
            } else {
                listOf(Header("Content-type", contentType))
            }
        }

        override fun getBodySource(): Writable? = data

        override fun getBodyReader(): Reader {
            return WritableReader(data) // XXX see if there's a better way
        }

        override fun getContentType(): String {
            return message.contentType
        }

        @Throws(IOException::class)
        override fun writeTo(destination: Writer) {
            data!!.writeTo(destination)
        }


        fun setHandle(engineData: MutableProcessEngineDataAccess,
                      nodeInstance: ProcessNodeInstance<*>) {
            this.nodeInstance = nodeInstance

            try {

                data = nodeInstance.instantiateXmlPlaceholders(engineData, source, false, localEndpoint)

            } catch (e: FactoryConfigurationError) {
                throw MessagingException(e)
            } catch (e: XmlException) {
                throw MessagingException(e)
            }

        }

        fun setHandle(engineData: MutableProcessEngineDataAccess,
                      nodeInstance: ProcessNodeInstance.Builder<*, *>) {
            setHandle(engineData, nodeInstance.build())
        }

        override fun getAttachments(): Map<String, DataSource> {
            return emptyMap()
        }

        companion object {


            @Throws(FactoryConfigurationError::class, XMLStreamException::class)
            fun fillInActivityMessage(messageBody: Source,
                                      result: Result,
                                      nodeInstance: ProcessNodeInstance<*>,
                                      localEndpoint: EndpointDescriptor) {
                // TODO use multiplatform api

                val xif = XMLInputFactory.newInstance()
                val xof = XMLOutputFactory.newInstance()
                val xer = xif.createXMLEventReader(messageBody)
                val xew = xof.createXMLEventWriter(result)

                while (xer.hasNext()) {
                    val event = xer.nextEvent()
                    if (event.isStartElement) {
                        val se = event.asStartElement()
                        val eName = se.name
                        if (Constants.MODIFY_NS_STR == eName.namespaceURI) {
                            val attributes: Iterator<Attribute> = se.attributes as Iterator<Attribute>
                            if (eName.localPart == "attribute") {
                                writeAttribute(nodeInstance, xer, attributes, xew)
                            } else if (eName.localPart == "element") {
                                writeElement(nodeInstance, xer, attributes, xew, localEndpoint)
                            } else {
                                throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                            "Unsupported activity modifier")
                            }
                        } else {
                            xew.add(se)
                        }
                    } else {
                        if (event.isCharacters) {
                            val c = event.asCharacters()
                            val charData = c.data
                            var i: Int
                            i = 0
                            while (i < charData.length) {
                                if (!Character.isWhitespace(charData[i])) {
                                    break
                                }
                                ++i
                            }
                            if (i == charData.length) {
                                continue // ignore it, and go to next event
                            }
                        }

                        if (event is Namespace) {

                            if (event.namespaceURI != Constants.MODIFY_NS_STR) {
                                xew.add(event)
                            }
                        } else {
                            try {
                                xew.add(event)
                            } catch (e: IllegalStateException) {
                                val errorMessage = StringBuilder("Error adding event: ")
                                errorMessage.append(event.toString()).append(' ')
                                if (event.isStartElement) {
                                    errorMessage.append('<').append(event.asStartElement().name).append('>')
                                } else if (event.isEndElement) {
                                    errorMessage.append("</").append(event.asEndElement().name).append('>')
                                }
                                logger.log(Level.WARNING, errorMessage.toString(), e)
                                //                baos.reset(); baos.close();
                                throw e
                            }

                        }
                    }
                }
            }

            @Throws(XMLStreamException::class)
            private fun writeElement(nodeInstance: ProcessNodeInstance<*>,
                                     `in`: XMLEventReader,
                                     attributes: Iterator<Attribute>,
                                     out: XMLEventWriter,
                                     localEndpoint: EndpointDescriptor) {
                var valueName: String? = null
                run {
                    while (attributes.hasNext()) {
                        val attr = attributes.next()
                        val attrName = attr.name.localPart
                        if ("value" == attrName) {
                            valueName = attr.value
                        }
                    }
                }
                run {
                    val ev = `in`.nextEvent()

                    while (!ev.isEndElement) {
                        if (ev.isStartElement) {
                            throw MessagingFormatException("Violation of schema")
                        }
                        if (ev.isAttribute) {
                            val attr = ev as Attribute
                            val attrName = attr.name.localPart
                            if ("value" == attrName) {
                                valueName = attr.value
                            }
                        }
                    }
                }
                if (valueName != null) {
                    val xef = XMLEventFactory.newInstance()

                    if ("handle" == valueName) {
                        out.add(xef.createCharacters(java.lang.Long.toString(nodeInstance.getHandleValue())))
                    } else if ("endpoint" == valueName) {
                        val qname1 = QName(Constants.MY_JBI_NS_STR, "endpointDescriptor", "")
                        val namespaces = listOf(xef.createNamespace("",
                                                                    Constants.MY_JBI_NS_STR))
                        out.add(xef.createStartElement(qname1, null, namespaces.iterator()))

                        run {
                            //            EndpointDescriptor localEndpoint = nodeInstance.getProcessInstance().getEngine().getLocalEndpoint();
                            out.add(xef.createAttribute("serviceNS", localEndpoint.serviceName!!.namespaceURI))
                            out.add(xef.createAttribute("serviceLocalName", localEndpoint.serviceName!!.localPart))
                            out.add(xef.createAttribute("endpointName", localEndpoint.endpointName))
                            out.add(xef.createAttribute("endpointLocation",
                                                        localEndpoint.endpointLocation!!.toString()))
                        }

                        out.add(xef.createEndElement(qname1, namespaces.iterator()))
                    }
                } else {
                    throw MessagingFormatException("Missing parameter name")
                }

            }

            @Throws(XMLStreamException::class)
            private fun writeAttribute(nodeInstance: ProcessNodeInstance<*>,
                                       `in`: XMLEventReader,
                                       attributes: Iterator<Attribute>,
                                       out: XMLEventWriter) {
                var valueName: String? = null
                var paramName: String? = null
                run {
                    while (attributes.hasNext()) {
                        val attr = attributes.next()
                        val attrName = attr.name.localPart
                        if ("value" == attrName) {
                            valueName = attr.value
                        } else if ("name" == attrName) {
                            paramName = attr.value
                        }
                    }
                }
                run {
                    val ev = `in`.nextEvent()

                    while (!ev.isEndElement) {
                        if (ev.isStartElement) {
                            throw MessagingFormatException("Violation of schema")
                        }
                        if (ev.isAttribute) {
                            val attr = ev as Attribute
                            val attrName = attr.name.localPart
                            if ("value" == attrName) {
                                valueName = attr.value
                            } else if ("name" == attrName) {
                                paramName = attr.value
                            }
                        }
                    }
                }
                if (valueName != null) {


                    val xef = XMLEventFactory.newInstance()

                    if ("handle" == valueName) {
                        val attr: Attribute
                        if (paramName != null) {
                            attr = xef.createAttribute(paramName,
                                                       java.lang.Long.toString(nodeInstance.getHandleValue()))
                        } else {
                            attr = xef.createAttribute("handle", java.lang.Long.toString(nodeInstance.getHandleValue()))
                        }
                        out.add(attr)
                    } else if ("owner" == valueName) {
                        val attr: Attribute
                        if (paramName != null) {
                            attr = xef.createAttribute(paramName, nodeInstance.owner.name)
                        } else {
                            attr = xef.createAttribute("owner", nodeInstance.owner.name)
                        }
                        out.add(attr)
                    } else if ("instancehandle" == valueName) {
                        val attr: Attribute
                        if (paramName != null) {
                            attr = xef.createAttribute(paramName, nodeInstance.owner.name)
                        } else {
                            attr = xef.createAttribute("instancehandle", nodeInstance.owner.name)
                        }
                        out.add(attr)
                    }


                } else {
                    throw MessagingFormatException("Missing parameter name")
                }

            }
        }


    }

    /*
     * Servlet methods
     */

    override fun getEndpointProvider(): GenericEndpoint {
        return this
    }

    override fun destroy() {
        MessagingRegistry.getMessenger().unregisterEndpoint(this)
    }

    override fun getServletInfo(): String {
        return "ServletProcessEngine"
    }

    @TestOnly
    protected fun init(engine: ProcessEngine<TR>) {
        processEngine = engine
    }

    @Throws(ServletException::class)
    override fun init(config: ServletConfig) {
        super.init(config)
        val hostname: String? = config.getInitParameter("hostname") ?: "localhost"
        val port = config.getInitParameter("port")
        val localURL: URI
        localURL = when (port) {
            null -> URI.create("http://" + hostname + config.servletContext.contextPath)
            else -> URI.create("http://" + hostname + ":" + port + config.servletContext.contextPath)
        }
        messageService = MessageService(asEndpoint(localURL))

        processEngine = ProcessEngine.newInstance(messageService) as ProcessEngine<TR>

        MessagingRegistry.getMessenger().registerEndpoint(this)
    }

    /**
     * Get the list of all process models in the engine. This will be limited to user owned ones. The list will contain only
     * a summary of each model including name, id and uuid, not the content.
     *
     * XXX check security properly.
     */
    @RestMethod(method = HttpMethod.GET, path = "/processModels")
    fun getProcesModelRefs(@RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): SerializableList<IProcessModelRef<*, *, *>> {
        try {
            processEngine!!.startTransaction().use { transaction ->
                val processModels = processEngine!!.getProcessModels(transaction.readableEngineData, user)

                val list = ArrayList<IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel>>()
                for (pm in processModels) {
                    list.add(pm.withPermission().ref)
                }
                return transaction.commit(SerializableList<IProcessModelRef<*, *, *>>(REFS_TAG, list))
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error getting process model references", e)
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * Get the full process model content for the model with the given handle.
     * @param handle The handle of the process model
     * @param user The user whose permissions are verified
     * @return The process model
     * @throws FileNotFoundException When the model does not exist. This is translated to a 404 error in http.
     */
    @RestMethod(method = HttpMethod.GET, path = "/processModels/\${handle}")
    @Throws(FileNotFoundException::class)
    fun getProcessModel(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): ExecutableProcessModel {
        try {
            processEngine!!.startTransaction().use { transaction ->
                val handle1 = handle<ExecutableProcessModel>(handle)
                processEngine!!.invalidateModelCache(handle1)
                return transaction.commit<ExecutableProcessModel>(
                        processEngine!!.getProcessModel(transaction.readableEngineData, handle1, user))
            }
        } catch (e: NullPointerException) {
            throw FileNotFoundException("Process handle invalid").initCause(e) as FileNotFoundException
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error getting process model", e)
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * Update the process model with the given handle.
     * @param handle The model handle
     * @param attachment The actual new model
     * @param user The user performing the update. This will be verified against ownership and permissions
     * @return A reference to the model. This may include a newly generated uuid if not was provided.
     * @throws IOException
     */
    @RestMethod(method = HttpMethod.POST, path = "/processModels/\${handle}")
    @Throws(IOException::class)
    fun updateProcessModel(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            name = "processUpload", type = RestParamType.ATTACHMENT) attachment: DataHandler, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): ProcessModelRef<*, *, *> {
        val builder = XmlStreaming.deSerialize(attachment.inputStream, XmlProcessModel.Builder::class.java)
        return updateProcessModel(handle, builder, user)
    }

    /**
     * Update the process model with the given handle.
     * @param handle The model handle
     * @param processModelBuilder The actual new model
     * @param user The user performing the update. This will be verified against ownership and permissions
     * @return A reference to the model. This may include a newly generated uuid if not was provided.
     */
    @WebMethod(operationName = "updateProcessModel")
    @Throws(FileNotFoundException::class)
    fun updateProcessModel(@WebParam(name = "handle") handle: Long, @WebParam(name = "processModel",
                                                                              mode = Mode.IN) processModelBuilder: RootProcessModel.Builder<*, *>?, @WebParam(
            name = "principal", mode = Mode.IN, header = true) @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal?): ProcessModelRef<*, *, *> {
        if (user == null) {
            throw AuthenticationNeededException("There is no user associated with this request")
        }
        if (processModelBuilder != null) {
            processModelBuilder.handle = handle

            try {
                processEngine!!.startTransaction().use { transaction ->
                    val updatedRef = processEngine!!.updateProcessModel(transaction, handle(handle),
                                                                        ExecutableProcessModel(processModelBuilder),
                                                                        user)
                    val update2 = ProcessModelRef.get(updatedRef)
                    return transaction.commit(update2)
                }
            } catch (e: SQLException) {
                logger.log(Level.WARNING, "Error updating process model", e)
                throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
            }

        }
        throw HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "The posted process model is not valid")
    }

    /**
     * Add a process model to the system.
     * @param attachment The process model to add.
     * @param owner The creator/owner of the model.
     * @return A reference to the model with handle and a new uuid if none was provided.
     */
    @RestMethod(method = HttpMethod.POST, path = "/processModels")
    @Throws(IOException::class)
    fun postProcessModel(@RestParam(name = "processUpload",
                                    type = RestParamType.ATTACHMENT) attachment: DataHandler, @RestParam(
            type = RestParamType.PRINCIPAL) owner: Principal): ProcessModelRef<*, *, *>? {
        val processModel = XmlStreaming.deSerialize(attachment.inputStream, XmlProcessModel.Builder::class.java)
        return postProcessModel(processModel, owner)
    }

    /**
     * Add a process model to the system.
     * @param processModel The process model to add.
     * @param owner The creator/owner of the model.
     * @return A reference to the model with handle and a new uuid if none was provided.
     */
    @WebMethod(operationName = "postProcessModel")
    fun postProcessModel(@WebParam(name = "processModel",
                                   mode = Mode.IN) processModel: XmlProcessModel.Builder?, @RestParam(
            type = RestParamType.PRINCIPAL) @WebParam(name = "principal", mode = Mode.IN,
                                                      header = true) owner: Principal?): ProcessModelRef<*, *, *>? {
        if (owner == null) {
            throw AuthenticationNeededException("There is no user associated with this request")
        }
        if (processModel != null) {
            processModel.handle = -1 // The handle cannot be set
            try {
                processEngine!!.startTransaction().use { transaction ->
                    val newModelRef = processEngine!!.addProcessModel(transaction, processModel, owner)

                    return transaction.commit(ProcessModelRef.get(newModelRef))
                }
            } catch (e: SQLException) {
                logger.log(Level.WARNING, "Error adding process model", e)
                throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
            }

        }

        return null
    }

    /**
     * Rename the given process model.
     * @param handle The handle to the model
     * @param name The new name
     * @param user The user performing the action.
     */
    @RestMethod(method = HttpMethod.POST, path = "/processModels/\${handle}")
    @Throws(FileNotFoundException::class)
    fun renameProcess(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(name = "name",
                                                                                                     type = RestParamType.QUERY) name: String, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal) {
        processEngine!!.renameProcessModel(user, handle(handle), name)
    }

    /**
     * Delete the process model.
     * @param handle The handle of the process model to delete
     * @param user A user with permission to delete the model.
     */
    @RestMethod(method = HttpMethod.DELETE, path = "/processModels/\${handle}")
    fun deleteProcess(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal) {
        try {
            processEngine!!.startTransaction().use { transaction ->
                if (!processEngine!!.removeProcessModel(transaction, handle(handle), user)) {
                    throw HttpResponseException(HttpServletResponse.SC_NOT_FOUND, "The given process does not exist")
                }
                transaction.commit()
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error renaming process model", e)
            throw HttpResponseException(500, e)
        }

    }

    /**
     * Create a new process instance and start it.
     *
     * @param handle The handle of the process to start.
     * @param name The name that will allow the user to remember the instance. If
     * `null` a name will be assigned. This name has no
     * semantic meaning.
     * @param owner The owner of the process instance. (Who started it).
     * @return A handle to the process
     */
    @WebMethod
    @RestMethod(method = HttpMethod.POST, path = "/processModels/\${handle}", query = arrayOf("op=newInstance"))
    @Throws(FileNotFoundException::class)
    fun startProcess(
            @WebParam(name = "handle") @RestParam(name = "handle", type = RestParamType.VAR) handle: Long,
            @WebParam(name = "name") @RestParam(name = "name", type = RestParamType.QUERY) name: String?,
            @WebParam(name = "uuid") @RestParam(name = "uuid", type = RestParamType.QUERY) uUID: String?,
            @WebParam(name = "owner", header = true) @RestParam(
                    type = RestParamType.PRINCIPAL) owner: Principal): XmlHandle<*> {
        try {
            processEngine!!.startTransaction().use { transaction ->
                val uuid = if (uUID == null) UUID.randomUUID() else UUID.fromString(uUID)
                return transaction.commit(
                        processEngine!!.startProcess(transaction, owner, handle<ExecutableProcessModel>(handle),
                                                     name ?: "<unnamed>", uuid, null))
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error starting process", e)
            throw HttpResponseException(500, e)
        }

    }

    /**
     * Get a list of all process instances owned by the current user. This will provide a summary list, providing a subset.
     * of information from the process instance.
     * @param owner The user.
     * @return A list of process instances.
     */
    @RestMethod(method = HttpMethod.GET, path = "/processInstances")
    @XmlElementWrapper(name = "processInstances", namespace = Constants.PROCESS_ENGINE_NS)
    fun getProcesInstanceRefs(@RestParam(
            type = RestParamType.PRINCIPAL) owner: Principal?): Collection<ProcessInstanceRef> {
        if (owner == null) throw AuthenticationNeededException()
        try {
            processEngine!!.startTransaction().use { transaction ->
                val list = ArrayList<ProcessInstanceRef>()
                for (pi in processEngine!!.getOwnedProcessInstances(transaction, owner)) {
                    list.add(pi.ref)
                }
                return transaction.commit(SerializableList(INSTANCEREFS_TAG, list))
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error getting process instances", e)
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * Get the given process instance.
     * @param handle The handle of the instance.
     * @param user A user with permission to see the model.
     * @return The full process instance.
     */
    @RestMethod(method = HttpMethod.GET, path = "/processInstances/\${handle}")
    fun getProcessInstance(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal?): XmlSerializable {
        try {
            processEngine!!.startTransaction().use { transaction ->
                return transaction.commit(
                        processEngine!!.getProcessInstance(transaction, handle<ProcessInstance>(handle),
                                                           user!!).serializable(transaction))
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error getting process instance", e)
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * Cause the process instance state to be re-evaluated. For failed service invocations, this will cause the server to
     * try again.
     * @param handle The handle of the instance
     * @param user A user with appropriate permissions.
     * @return A string indicating success.
     */
    @RestMethod(method = HttpMethod.GET, path = "/processInstances/\${handle}", query = arrayOf("op=tickle"))
    @Throws(FileNotFoundException::class)
    fun tickleProcessInstance(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): String {
        try {
            processEngine!!.startTransaction().use { transaction ->
                transaction.commit(processEngine!!.tickleInstance(transaction, handle, user))
                return "success"
            }
        } catch (e: SQLException) {
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failure", e)
        }

    }

    /**
     * Cancel the process instance execution. This will cause all aspects of the instance to be cancelled. Note that this
     * cannot undo the past.
     * @param handle The instance to cancel
     * @param user A user with permission to cancel the instance.
     * @return The instance that was cancelled.
     */
    @RestMethod(method = HttpMethod.DELETE, path = "/processInstances/\${handle}")
    fun cancelProcessInstance(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): ProcessInstance {
        try {
            processEngine!!.startTransaction().use { transaction ->
                return transaction.commit(
                        processEngine!!.cancelInstance(transaction, handle<ProcessInstance>(handle), user))
            }
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Error cancelling process intance", e)
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * Get the data for a specific task.
     * @param handle The handle
     * @param user A user with appropriate permissions
     * @return The node instance.
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws XmlException
     */
    @WebMethod(operationName = "getProcessNodeInstance")
    @Throws(FileNotFoundException::class, SQLException::class, XmlException::class)
    fun getProcessNodeInstanceSoap(
            @WebParam(name = "handle", mode = Mode.IN) handle: Long,
            @WebParam(name = "user", mode = Mode.IN) user: Principal): XmlProcessNodeInstance? {
        return getProcessNodeInstance(handle, user)
    }

    /**
     * Get the information for a specific task.
     * @param handle The handle of the task
     * @param user A user with appropriate permissions
     * @return the task
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws XmlException
     */
    @RestMethod(method = HttpMethod.GET, path = "/tasks/\${handle}")
    @Throws(FileNotFoundException::class, SQLException::class, XmlException::class)
    fun getProcessNodeInstance(
            @RestParam(name = "handle", type = RestParamType.VAR)
            handle: Long,
            @RestParam(type = RestParamType.PRINCIPAL)
            user: Principal): XmlProcessNodeInstance? {
        processEngine!!.startTransaction().use { transaction ->
            val result = processEngine!!.getNodeInstance(transaction, handle<ProcessNodeInstance<*>>(handle), user)
                         ?: return null
            return transaction.commit(
                    result.toSerializable(transaction.writableEngineData, messageService!!.localEndpoint))
        }
    }

    /**
     * Update the state of a task.
     * @param handle Handle of the task to update
     * @param newState The new state
     * @param user A user with appropriate permissions
     * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
     */
    @WebMethod(operationName = "updateTaskState")
    @Throws(FileNotFoundException::class)
    fun updateTaskStateSoap(@WebParam(name = "handle", mode = Mode.IN) handle: Long, @WebParam(name = "state",
                                                                                               mode = Mode.IN) newState: NodeInstanceState, @WebParam(
            name = "user", mode = Mode.IN) user: Principal): NodeInstanceState {
        return updateTaskState(handle, newState, user)
    }

    /**
     * Update the state of a task.
     * @param handle Handle of the task to update
     * @param newState The new state
     * @param user A user with appropriate permissions
     * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
     */
    @RestMethod(method = HttpMethod.POST, path = "/tasks/\${handle}", query = arrayOf("state"))
    @Throws(FileNotFoundException::class)
    fun updateTaskState(@RestParam(name = "handle", type = RestParamType.VAR) handle: Long, @RestParam(name = "state",
                                                                                                       type = RestParamType.QUERY) newState: NodeInstanceState, @RestParam(
            type = RestParamType.PRINCIPAL) user: Principal): NodeInstanceState {
        try {
            processEngine!!.startTransaction().use { transaction ->
                return transaction.commit(
                        processEngine!!.updateTaskState(transaction, handle<ProcessNodeInstance<*>>(handle), newState,
                                                        user))
            }
        } catch (e: SQLException) {
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }

    /**
     * finish a task. Process aware services will need to call this to signal completion.
     * @param handle Handle of the task to update
     * @param payload An XML document that is the "result" of the service.
     * @param user A user with appropriate permissions
     * @return the new state of the task. This may be different than requested, for example due to engine semantics. (either further, or no change at all)
     */
    @WebMethod(operationName = "finishTask")
    @RestMethod(method = HttpMethod.POST, path = "/tasks/\${handle}", query = arrayOf("state=Complete"))
    fun finishTask(
            @WebParam(name = "handle", mode = Mode.IN)
            @RestParam(name = "handle", type = RestParamType.VAR)
            handle: Long,
            @WebParam(name = "payload", mode = Mode.IN)
            @RestParam(name = "payload", type = RestParamType.QUERY)
            payload: Node,
            @RestParam(type = RestParamType.PRINCIPAL)
            @WebParam(name = "principal", mode = Mode.IN, header = true)
            user: Principal): NodeInstanceState {
        try {
            processEngine!!.startTransaction().use { transaction ->
                return transaction.commit(
                        processEngine!!.finishTask(transaction, handle<ProcessNodeInstance<*>>(handle), payload,
                                                   user).state)
            }
        } catch (e: SQLException) {
            throw HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e)
        }

    }


    /**
     * Handle the completing of sending a message and receiving some sort of
     * reply. If the reply is an ActivityResponse message we handle that
     * specially.
     * @throws SQLException
     */
    @Throws(FileNotFoundException::class)
    fun onMessageCompletion(future: Future<out DataSource>,
                            handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                            owner: Principal) {
        // XXX do this better
        try {
            if (future.isCancelled) {
                processEngine!!.startTransaction().use { transaction ->
                    processEngine!!.cancelledTask(transaction, handle, owner)
                    transaction.commit()
                }
            } else {
                try {
                    val result = future.get()
                    processEngine!!.startTransaction().use { transaction ->
                        val inst = processEngine!!.getNodeInstance(transaction, handle, SYSTEMPRINCIPAL)
                        assert(inst!!.state === NodeInstanceState.Pending)
                        if (inst!!.state === NodeInstanceState.Pending) {
                            val processInstance = transaction.readableEngineData.instance(
                                    inst!!.hProcessInstance).withPermission()

                            val builder = inst.builder(processInstance.builder())
                            builder.state = NodeInstanceState.Sent

                            builder.store(transaction.writableEngineData)
                        }
                        transaction.commit()
                    }
                    try {
                        val domResult = DomUtil.tryParseXml(result.inputStream)
                        var rootNode: Element? = domResult!!.documentElement
                        // If we are seeing a Soap Envelope, if there is an activity response in the header treat that as the root node.
                        if (Envelope.NAMESPACE == rootNode!!.namespaceURI && Envelope.ELEMENTLOCALNAME == rootNode.localName) {

                            val header = DomUtil.getFirstChild(rootNode, Envelope.NAMESPACE,
                                                               org.w3.soapEnvelope.Header.ELEMENTLOCALNAME)
                            if (header != null) {
                                rootNode = DomUtil.getFirstChild(header, Constants.PROCESS_ENGINE_NS,
                                                                 ActivityResponse.ELEMENTLOCALNAME)
                            }
                        }
                        if (rootNode != null) {
                            // If we receive an ActivityResponse, treat that specially.
                            if (Constants.PROCESS_ENGINE_NS == rootNode.namespaceURI && ActivityResponse.ELEMENTLOCALNAME == rootNode.localName) {
                                val taskStateAttr = rootNode.getAttribute(ActivityResponse.ATTRTASKSTATE)
                                try {
                                    processEngine!!.startTransaction().use { transaction ->
                                        val nodeInstanceState = NodeInstanceState.valueOf(taskStateAttr)
                                        processEngine!!.updateTaskState(transaction, handle, nodeInstanceState, owner)
                                        transaction.commit()
                                        return
                                    }
                                } catch (e: NullPointerException) {
                                    e.printStackTrace()
                                    // ignore
                                } catch (e: IllegalArgumentException) {
                                    processEngine!!.startTransaction().use { transaction ->
                                        processEngine!!.errorTask(transaction, handle, e, owner)
                                        transaction.commit()
                                    }
                                }

                            }
                        } else {
                            processEngine!!.startTransaction().use { transaction ->
                                // XXX By default assume that we have finished the task
                                processEngine!!.finishedTask(transaction, handle, result, owner)
                                transaction.commit()
                            }
                        }

                    } catch (e: NullPointerException) {
                        // ignore
                    } catch (e: IOException) {
                        // It's not xml or has more than one xml element ignore that and fall back to handling unknown services
                    }

                } catch (e: ExecutionException) {
                    logger.log(Level.INFO, "Task $handle: Error in messaging", e.cause)
                    processEngine!!.startTransaction().use { transaction ->
                        processEngine!!.errorTask(transaction, handle, e.cause ?: e, owner)
                        transaction.commit()
                    }
                } catch (e: InterruptedException) {
                    logger.log(Level.INFO, "Task $handle: Interrupted", e)
                    processEngine!!.startTransaction().use { transaction ->
                        processEngine!!.cancelledTask(transaction, handle, owner)
                        transaction.commit()
                    }
                }

            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    }

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return Constants.PROCESS_ENGINE_NS == other!!.serviceName!!.namespaceURI &&
               SERVICE_LOCALNAME == other.serviceName!!.localPart &&
               endpointName == other.endpointName
    }

    override fun initEndpoint(config: ServletConfig) {
        // We know our config, don't do anything.
    }

    protected open fun setLocalEndpoint(localURL: URI) {
        messageService.localEndpoint = asEndpoint(localURL)
    }

    private fun asEndpoint(localURL: URI): EndpointDescriptorImpl {
        return EndpointDescriptorImpl(serviceName, endpointName, localURL)
    }

    companion object {

        private val serialVersionUID = -6277449163953383974L

        const val SERVICE_NS = Constants.PROCESS_ENGINE_NS
        const val SERVICE_LOCALNAME = "ProcessEngine"
        val SERVICE_QNAME = QName(SERVICE_NS, SERVICE_LOCALNAME)


        /*
     * Methods inherited from JBIProcessEngine
     */

        internal val logger: Logger
            get() {
                val logger = Logger.getLogger(ServletProcessEngine::class.java.name)
                logger.level = Level.ALL
                return logger
            }

        private val REFS_TAG = QName(SERVICE_NS, "processModels")


        private val INSTANCEREFS_TAG = QName(SERVICE_NS, "processInstances")
    }


}
