package nl.adaptivity.process.userMessageHandler.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.userMessageHandler.server.InternalEndpoint.XmlTask;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


@XmlSeeAlso(XmlTask.class)
public class ExternalEndpoint implements GenericEndpoint {

  public static final String ENDPOINT = "external";
  public static final QName SERVICENAME = new QName(UserMessageService.UMH_NS, "userMessageHandler");
  UserMessageService aService;
  private URI aURI;

  public ExternalEndpoint() {
    aService = UserMessageService.getInstance();
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
    return aURI;
  }

  @Override
  public void initEndpoint(ServletConfig pConfig) {
    StringBuilder path = new StringBuilder(pConfig.getServletContext().getContextPath());
    path.append("/internal");
    try {
      aURI = new URI(null, null, path.toString(), null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
  }

  @XmlElementWrapper(name="tasks", namespace=UserMessageService.UMH_NS)
  @RestMethod(method=HttpMethod.GET, path="/pendingTasks")
  public Collection<UserTask<?>> getPendingTasks() {
    return aService.getPendingTasks();
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Started"})
  public Task.TaskState startTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle, @RestParam(type=ParamType.PRINCIPAL) Principal pUser) {
    return aService.startTask(Long.parseLong(pHandle), pUser);
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Taken"})
  public Task.TaskState takeTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle, @RestParam(type=ParamType.PRINCIPAL) Principal pUser) {
    return aService.takeTask(Long.parseLong(pHandle), pUser);
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Finished"})
  public Task.TaskState finishTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle, @RestParam(type=ParamType.PRINCIPAL) Principal pUser) {
    return aService.finishTask(Long.parseLong(pHandle), pUser);
  }

  @Override
  public void destroy() {
    aService.destroy();
  }

}
