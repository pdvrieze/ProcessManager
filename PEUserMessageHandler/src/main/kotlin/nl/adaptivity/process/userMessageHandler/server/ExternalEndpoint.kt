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

import net.devrieze.util.Handles
import net.devrieze.util.Transaction
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.MessagingRegistry
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.messaging.GenericEndpoint
import nl.adaptivity.process.util.Constants
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod
import nl.adaptivity.rest.annotations.RestParam
import nl.adaptivity.rest.annotations.RestParam.ParamType
import java.io.FileNotFoundException
import java.net.URI
import java.net.URISyntaxException
import java.security.Principal
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.ServletConfig
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.namespace.QName


/**
 * The external interface to the user task management system. This is for interacting with tasks, not for the process
 * engine to use. The process engine uses the [internal endpoint][InternalEndpoint].

 * The default context path for the methods in this endpoind is `/PEUserMessageHandler/UserMessageService`

 * Note that task states are ordered and ultimately determined by the process engine. Task states may not always be
 * downgraded.
 */
@XmlSeeAlso(XmlTask::class)
class ExternalEndpoint @JvmOverloads constructor(private val mService: UserMessageService<out Transaction> = UserMessageService.instance) : GenericEndpoint {

  private var endpointUri: URI? = null

  override fun getServiceName(): QName {
    return SERVICENAME
  }

  override fun getEndpointName(): String {
    return ENDPOINT
  }

  override fun getEndpointLocation(): URI? {
    return endpointUri
  }

  override fun isSameService(other: EndpointDescriptor): Boolean {
    return Constants.USER_MESSAGE_HANDLER_NS == other.serviceName.namespaceURI &&
          SERVICE_LOCALNAME == other.serviceName.localPart &&
          endpointName == other.endpointName
  }

  override fun initEndpoint(config: ServletConfig) {
    val path = StringBuilder(config.servletContext.contextPath)
    path.append("/UserMessageService")
    try {
      endpointUri = URI(null, null, path.toString(), null)
    } catch (e: URISyntaxException) {
      throw RuntimeException(e) // Should never happen
    }

    MessagingRegistry.getMessenger().registerEndpoint(this)
  }

  /**
   * Get a list of pending tasks.
   * @return All tasks available
   * *
   * @throws SQLException
   * *
   */
  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/allPendingTasks")
  @Deprecated("The version that takes the user should be used.", ReplaceWith("getPendingTasks(user)"))
  fun getPendingTasks() = getPendingTasks(mService, null)

  /**
   * Get a list of pending tasks.
   * @param user The user whose tasks to display.
   * *
   * @return All tasks available
   * *
   * @throws SQLException
   */
  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks")
  @Throws(SQLException::class)
  fun getPendingTasks(@RestParam(type = ParamType.PRINCIPAL) user: Principal): Collection<XmlTask> {
    return getPendingTasks(mService, user)
  }

  /**
   * Update a task. This takes an xml task whose values will be used to update this one. Task items get
   * overwritten with their new values, as well as the state. Missing items in the update will be ignored (the old value
   * used. The item state is a draft state, not the final version that the process engine gets until it has a
   * completed state.

   * @param handle The handle/id of the task
   * *
   * @param partialNewTask The partial task to use for updating.
   * *
   * @param user The user whose task state to update.
   * *
   * @return The Updated, complete, task.
   * *
   * @throws SQLException When something went wrong with the query.
   * *
   * @throws FileNotFoundException When the task handle is not valid. This will be translated into a 404 error.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/\${handle}")
  @Throws(SQLException::class, FileNotFoundException::class)
  fun updateTask(
        @RestParam(name = "handle", type = ParamType.VAR) handle: String,
        @RestParam(type = ParamType.BODY) partialNewTask: XmlTask,
        @RestParam(type = ParamType.PRINCIPAL) user: Principal): XmlTask {
    return updateTask(mService, handle, partialNewTask, user)
  }

  /**
   * Retrieve the current pending task for the given handle.
   * @param handle The task handle (as recorded in the task handler, not the process engine handle).
   * *
   * @param user The user whose task to retrieve.
   * *
   * @return The task.
   */
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks/\${handle}")
  @Throws(SQLException::class)
  fun getPendingTask(@RestParam(name = "handle", type = ParamType.VAR) handle: String,
                     @RestParam(type = ParamType.PRINCIPAL) user: Principal): XmlTask {
    mService.inTransaction {
      return commit { getPendingTask(Handles.handle<XmlTask>(
            java.lang.Long.parseLong(handle)),
                                     user) } ?: throw FileNotFoundException("The task with handle ${handle} does not exist")
    }
  }

  /**
   * Mark a task as started.
   * @param handle The task handle.
   * *
   * @param user The owner.
   * *
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/\${handle}", post = arrayOf("state=Started"))
  @Throws(SQLException::class)
  fun startTask(@RestParam(name = "handle", type = ParamType.VAR) handle: String,
                @RestParam(type = ParamType.PRINCIPAL) user: Principal): NodeInstanceState {
    mService.inTransaction {
      return startTask(Handles.handle<XmlTask>(handle), user)
    }
  }

  /**
   * Mark a task as Taken.
   * @param handle The task handle.
   * *
   * @param user The owner.
   * *
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/\${handle}", post = arrayOf("state=Taken"))
  @Throws(SQLException::class)
  fun takeTask(@RestParam(name = "handle", type = ParamType.VAR) handle: String,
               @RestParam(type = ParamType.PRINCIPAL) user: Principal): NodeInstanceState {
    mService.inTransaction {
      return commit { takeTask(Handles.handle<XmlTask>(handle), user) }
    }
  }


  /**
   * Mark a task as Finished. This will allow the process engine to take the data, and transition it to completed once
   * it has fully handled the finishing of the task.
   * @param handle The task handle.
   * *
   * @param user The owner.
   * *
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/\${handle}", post = arrayOf("state=Finished"))
  @Throws(SQLException::class)
  fun finishTask(@RestParam(name = "handle", type = ParamType.VAR) handle: String,
                 @RestParam(type = ParamType.PRINCIPAL) user: Principal): NodeInstanceState {
    mService.inTransaction {
      return commit {finishTask(
            Handles.handle<XmlTask>(java.lang.Long.parseLong(handle)),
            user)}
    }
  }

  override fun destroy() {
    mService.destroy()
    MessagingRegistry.getMessenger().registerEndpoint(this)
  }

  companion object {

    val ENDPOINT = "external"

    val SERVICE_LOCALNAME = "userMessageHandler"
    val SERVICENAME = QName(Constants.USER_MESSAGE_HANDLER_NS, SERVICE_LOCALNAME)

    /**
     * Helper method that is generic that can record the "right" transaction type.
     */
    @Throws(SQLException::class)
    private fun <T : Transaction> getPendingTasks(service: UserMessageService<T>,
                                                  user: Principal?): Collection<XmlTask> {
      try {
        service.newTransaction().use { transaction ->
          return transaction.commit(service.getPendingTasks(transaction,
                                                            user!!))
        }
      } catch (e: Exception) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Error retrieving tasks", e)
        throw e
      }

    }

    @Throws(SQLException::class, FileNotFoundException::class)
    private fun <T : Transaction> updateTask(service: UserMessageService<T>,
                                             handle: String,
                                             partialNewTask: XmlTask?,
                                             user: Principal): XmlTask {
      if (partialNewTask == null) {
        throw IllegalArgumentException("No task information provided")
      }
      try {
        service.newTransaction().use { transaction ->
          val result = service.updateTask(transaction,
                                          Handles.handle<XmlTask>(handle),
                                          partialNewTask,
                                          user) ?: throw FileNotFoundException()
          transaction.commit()
          return result
        }
      } catch (e: Exception) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e)
        throw e
      }

    }
  }

}
