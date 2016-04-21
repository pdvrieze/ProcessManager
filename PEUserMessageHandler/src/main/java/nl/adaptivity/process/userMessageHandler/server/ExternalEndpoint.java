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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.Transaction;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The external interface to the user task management system. This is for interacting with tasks, not for the process
 * engine to use. The process engine uses the {@link InternalEndpoint internal endpoint}.
 *
 * Note that task states are ordered and ultimately determined by the process engine. Task states may not always be
 * downgraded.
 */
@XmlSeeAlso(XmlTask.class)
public class ExternalEndpoint implements GenericEndpoint {

  public static final String ENDPOINT = "external";

  public static final String SERVICE_LOCALNAME = "userMessageHandler";
  public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, SERVICE_LOCALNAME);

  private final UserMessageService<?> mService;

  private URI mURI;

  public ExternalEndpoint() {
    this(UserMessageService.getInstance());
  }

  public ExternalEndpoint(@NotNull UserMessageService<?> service) {
    mService = service;
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }

  @Override
  public String getEndpointName() {
    return ENDPOINT;
  }

  @Override
  public URI getEndpointLocation() {
    return mURI;
  }

  @Override
  public boolean isSameService(final EndpointDescriptor other) {
    return Constants.USER_MESSAGE_HANDLER_NS.equals(other.getServiceName().getNamespaceURI()) &&
           SERVICE_LOCALNAME.equals(other.getServiceName().getLocalPart()) &&
           getEndpointName().equals(other.getEndpointName());
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
    path.append("/UserMessageService");
    try {
      mURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  /**
   * Get a list of pending tasks.
   * @return All tasks available
   * @throws SQLException
   * @deprecated The version that takes the user should be used.
   */
  @Deprecated
  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/allPendingTasks")
  public Collection<XmlTask> getPendingTasks() throws SQLException {
    return getPendingTasks(mService, null);
  }


  /**
   * Get a list of pending tasks.
   * @param user The user whose tasks to display.
   * @return All tasks available
   * @throws SQLException
   */
  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks")
  public Collection<XmlTask> getPendingTasks(Principal user) throws SQLException{
    return getPendingTasks(mService, user);
  }

  /**
   * Helper method that is generic that can record the "right" transaction type.
   */
  private static <T extends Transaction> Collection<XmlTask> getPendingTasks(UserMessageService<T> service, Principal user) throws SQLException {
    try (T transaction = service.newTransaction()) {
      return transaction.commit(service.getPendingTasks(transaction, user));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error retrieving tasks", e);
      throw e;
    }
  }

  /**
   * Update a task. This takes an xml task whose values will be used to update this one. Task items get
   * overwritten with their new values, as well as the state. Missing items in the update will be ignored (the old value
   * used. The item state is a draft state, not the final version that the process engine gets until it has a
   * completed state.
   *
   * @param handle The handle/id of the task
   * @param partialNewTask The partial task to use for updating.
   * @param user The user whose task state to update.
   * @return The Updated, complete, task.
   * @throws SQLException When something went wrong with the query.
   * @throws FileNotFoundException When the task handle is not valid. This will be translated into a 404 error.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}")
  public XmlTask updateTask(
      @RestParam(name="handle", type=ParamType.VAR) final String handle,
      @RestParam(type=ParamType.BODY) final XmlTask partialNewTask,
      @RestParam(type = ParamType.PRINCIPAL) final Principal user) throws SQLException, FileNotFoundException
  {
    return updateTask(mService, handle, partialNewTask, user);
  }

  private static <T extends Transaction> XmlTask updateTask(UserMessageService<T> service, String handle, XmlTask partialNewTask, Principal user) throws SQLException, FileNotFoundException {
    try (T transaction = service.newTransaction()) {
      final XmlTask result = service.updateTask(transaction, Long.parseLong(handle), partialNewTask, user);
      if (result==null) { throw new FileNotFoundException(); }
      transaction.commit();
      return result;
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
      throw e;
    }
  }

  /**
   * Retrieve the current pending task for the given handle.
   * @param handle The task handle (as recorded in the task handler, not the process engine handle).
   * @param user The user whose task to retrieve.
   * @return The task.
   */
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks/${handle}")
  public XmlTask getPendingTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.getPendingTask(Long.parseLong(handle), user);
  }

  /**
   * Mark a task as started.
   * @param handle The task handle.
   * @param user The owner.
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Started" })
  public NodeInstanceState startTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.startTask(Long.parseLong(handle), user);
  }

  /**
   * Mark a task as Taken.
   * @param handle The task handle.
   * @param user The owner.
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Taken" })
  public NodeInstanceState takeTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.takeTask(Long.parseLong(handle), user);
  }


  /**
   * Mark a task as Finished. This will allow the process engine to take the data, and transition it to completed once
   * it has fully handled the finishing of the task.
   * @param handle The task handle.
   * @param user The owner.
   * @return The new state of the task after completion of the request.
   */
  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Finished" })
  public NodeInstanceState finishTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.finishTask(Long.parseLong(handle), user);
  }

  @Override
  public void destroy() {
    mService.destroy();
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

}
