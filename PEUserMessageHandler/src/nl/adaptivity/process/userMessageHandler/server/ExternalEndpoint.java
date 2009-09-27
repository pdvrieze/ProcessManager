package nl.adaptivity.process.userMessageHandler.server;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.userMessageHandler.server.InternalEndpoint.XmlTask;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


@XmlSeeAlso(XmlTask.class)
public class ExternalEndpoint implements GenericEndpoint {

  public static final String ENDPOINT = "external";
  public static final QName SERVICENAME = new QName(UserMessageService.UMH_NS, "userMessageHandler");
  UserMessageService aService;

  public ExternalEndpoint(UserMessageService pService) {
    aService = pService;
  }

  @Override
  public QName getService() {
    return SERVICENAME;
  }

  @Override
  public String getEndpoint() {
    return ENDPOINT;
  }

  @XmlElementWrapper(name="tasks", namespace=UserMessageService.UMH_NS)
  @RestMethod(method=HttpMethod.GET, path="/pendingTasks")
  public Collection<UserTask> getPendingTasks() {
    return aService.getPendingTasks();
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Started"})
  public Task.TaskState startTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle) {
    return aService.startTask(Long.parseLong(pHandle));
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Taken"})
  public Task.TaskState takeTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle) {
    return aService.takeTask(Long.parseLong(pHandle));
  }

  @RestMethod(method=HttpMethod.POST, path="/pendingTasks/${handle}", post={"state=Finished"})
  public Task.TaskState finishTask(@RestParam(name="handle", type=ParamType.VAR) String pHandle) {
    return aService.finishTask(Long.parseLong(pHandle));
  }

}
