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
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.messaging.ActivityResponse
import nl.adaptivity.process.messaging.GenericEndpoint
import nl.adaptivity.process.userMessageHandler.server.XmlTask.Companion.get
import nl.adaptivity.ws.soap.SoapSeeAlso
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebParam.Mode
import javax.servlet.ServletConfig
import javax.xml.namespace.QName


class InternalEndpointImpl @JvmOverloads constructor(private val mService: UserMessageService<out Transaction> = UserMessageService.instance) : UserTaskServiceDescriptor(), GenericEndpoint, InternalEndpoint {

  inner class TaskUpdateCompletionListener(internal var mTask: XmlTask) : CompletionListener<NodeInstanceState> {

    override fun onMessageCompletion(future: Future<out NodeInstanceState>) {
      if (!future.isCancelled) {
        try {
          mTask.state = future.get()
        } catch (e: InterruptedException) {
          Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e)
        } catch (e: ExecutionException) {
          Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e)
        }

      }
    }

  }

  private var endpointUri: URI? = null

  override fun getServiceName(): QName {
    return ProcessConsts.Endpoints.UserTaskServiceDescriptor.SERVICENAME
  }


  override fun getEndpointLocation(): URI? {
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

    MessagingRegistry.getMessenger().registerEndpoint(this)
  }

  @WebMethod
  @Throws(SQLException::class)
  override fun postTask(@WebParam(name = "repliesParam", mode = Mode.IN) endPoint: EndpointDescriptorImpl,
                        @WebParam(name = "taskParam",
                                  mode = Mode.IN) @SoapSeeAlso(XmlTask::class) task: UserTask<*>): ActivityResponse<Boolean> {
    try {
      mService.inTransaction {
        task.setEndpoint(endPoint)
        val result = postTask(get(task))
        return commit {ActivityResponse.create(NodeInstanceState.Acknowledged, Boolean::class.java, result)}

      }
    } catch (e: Exception) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error posting task", e)
      throw e
    }

  }

  override fun destroy() {
    mService.destroy()
    MessagingRegistry.getMessenger().unregisterEndpoint(this)
  }
}
