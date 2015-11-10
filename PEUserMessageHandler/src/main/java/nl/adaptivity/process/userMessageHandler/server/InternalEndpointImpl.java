package nl.adaptivity.process.userMessageHandler.server;

import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.ws.soap.SoapSeeAlso;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


@XmlSeeAlso(XmlTask.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InternalEndpointImpl extends InternalEndpoint.Descriptor implements GenericEndpoint, InternalEndpoint {

  public class TaskUpdateCompletionListener implements CompletionListener<TaskState> {

    XmlTask aTask;

    public TaskUpdateCompletionListener(final XmlTask task) {
      aTask = task;
    }

    @Override
    public void onMessageCompletion(final Future<? extends TaskState> future) {
      if (!future.isCancelled()) {
        try {
          final TaskState result = (TaskState) future.get();
          aTask.aState = result;
        } catch (final InterruptedException e) {
          Logger.getAnonymousLogger().log(Level.INFO, "Messaging interrupted", e);
        } catch (final ExecutionException e) {
          Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
        }
      }
    }

  }

  private final UserMessageService aService;

  private URI aURI;

  public InternalEndpointImpl() {
    aService = UserMessageService.getInstance();
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }


  @Override
  public URI getEndpointLocation() {
    // TODO Do this better
    return aURI;
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
    path.append("/internal");
    try {
      aURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl endPoint, @WebParam(name = "taskParam", mode = Mode.IN) @SoapSeeAlso(XmlTask.class) final UserTask<?> task) {
    try {
      task.setEndpoint(endPoint);
      final boolean result = aService.postTask(XmlTask.get(task));
      task.setState(TaskState.Acknowledged, task.getOwner()); // Only now mark as acknowledged
      return ActivityResponse.create(TaskState.Acknowledged, Boolean.class, Boolean.valueOf(result));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error posting task", e);
      throw e;
    }
  }

  @Override
  public void destroy() {
    aService.destroy();
    MessagingRegistry.getMessenger().unregisterEndpoint(this);
  }
}
