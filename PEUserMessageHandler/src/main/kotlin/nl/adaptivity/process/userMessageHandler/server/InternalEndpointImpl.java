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
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.ws.soap.SoapSeeAlso;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.xml.namespace.QName;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class InternalEndpointImpl extends UserTaskServiceDescriptor implements GenericEndpoint, InternalEndpoint {

  public class TaskUpdateCompletionListener implements CompletionListener<NodeInstanceState> {

    XmlTask mTask;

    public TaskUpdateCompletionListener(final XmlTask task) {
      mTask = task;
    }

    @Override
    public void onMessageCompletion(final Future<? extends NodeInstanceState> future) {
      if (!future.isCancelled()) {
        try {
          final NodeInstanceState result = (NodeInstanceState) future.get();
          mTask.mState = result;
        } catch (final InterruptedException e) {
          Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
        } catch (final ExecutionException e) {
          Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
        }
      }
    }

  }

  private final UserMessageService<Transaction> mService;

  private URI mURI;

  public InternalEndpointImpl() {
    this(UserMessageService.getInstance());
  }

  public InternalEndpointImpl(UserMessageService<Transaction> service) {
    mService = service;
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }


  @Override
  public URI getEndpointLocation() {
    // TODO Do this better
    return mURI;
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
    path.append("/internal");
    try {
      mURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl endPoint, @WebParam(name = "taskParam", mode = Mode.IN) @SoapSeeAlso(XmlTask.class) final UserTask<?> task) throws SQLException {
    try(Transaction transaction = mService.newTransaction()) {
      task.setEndpoint(endPoint);
      final boolean result = mService.postTask(transaction, XmlTask.get(task));
      return transaction.commit(ActivityResponse.create(NodeInstanceState.Acknowledged, Boolean.class, result));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error posting task", e);
      throw e;
    }
  }

  @Override
  public void destroy() {
    mService.destroy();
    MessagingRegistry.getMessenger().unregisterEndpoint(this);
  }
}
