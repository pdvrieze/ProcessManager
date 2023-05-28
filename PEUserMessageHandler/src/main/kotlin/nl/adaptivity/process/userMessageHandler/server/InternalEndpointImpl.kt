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

import net.devrieze.util.Transaction
import nl.adaptivity.messaging.CompletionListener
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.messaging.MessagingRegistry
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.messaging.ActivityResponse
import nl.adaptivity.process.userMessageHandler.server.XmlTask.Companion.get
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.ws.soap.SoapSeeAlso
import nl.adaptivity.xmlutil.QName
import java.net.URISyntaxException
import java.sql.SQLException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebParam.Mode
import jakarta.servlet.ServletConfig


class InternalEndpointImpl @JvmOverloads constructor(
    private val service: UserMessageService<out Transaction> = UserMessageService.instance
) : UserTaskServiceDescriptor(), InternalEndpoint {

    inner class TaskUpdateCompletionListener(internal var task: XmlTask) : CompletionListener<NodeInstanceState> {

        override fun onMessageCompletion(future: Future<out NodeInstanceState>) {
            if (!future.isCancelled) {
                try {
                    task.state = future.get()
                } catch (e: InterruptedException) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e)
                } catch (e: ExecutionException) {
                    Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e)
                }

            }
        }

    }

    private var endpointUri: URI? = null

    override val serviceName: QName
        get() = SERVICENAME

    override val endpointLocation: URI?
        get() {
            // TODO Do this better
            return endpointUri
        }

    override fun initEndpoint(config: ServletConfig) {
        val path = StringBuilder(config.servletContext.contextPath)
        path.append("/internal")
        try {
            endpointUri = URI(null, null, path.toString(), null)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e) // Should never happen
        }

        MessagingRegistry.messenger.registerEndpoint(this)
    }

    @WebMethod
    @Throws(SQLException::class)
    override fun postTask(
        @WebParam(name = "repliesParam", mode = Mode.IN) endPoint: EndpointDescriptorImpl,
        @WebParam(name = "taskParam", mode = Mode.IN) @SoapSeeAlso(XmlTask::class)
        task: UserTask<*>
    ): ActivityResponse<Boolean, Boolean> {
        try {
            return service.inTransaction {
                task.setEndpoint(endPoint)
                val result = postTask(get(task))
                commit { ActivityResponse.create(NodeInstanceState.Acknowledged, Boolean::class, result) }
            }
        } catch (e: Exception) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Error posting task", e)
            throw e
        }

    }

    override fun destroy() {
        service.destroy()
        MessagingRegistry.messenger.unregisterEndpoint(this)
    }
}
