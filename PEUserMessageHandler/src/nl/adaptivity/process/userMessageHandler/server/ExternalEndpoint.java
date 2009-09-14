package nl.adaptivity.process.userMessageHandler.server;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.jbi.rest.annotations.RestMethod;
import nl.adaptivity.jbi.rest.annotations.RestParam;
import nl.adaptivity.jbi.rest.annotations.RestService;
import nl.adaptivity.jbi.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.jbi.rest.annotations.RestParam.ParamType;
import nl.adaptivity.process.exec.Task;


@RestService
public class ExternalEndpoint implements GenericEndpoint {

  UserMessageService aService;
  
  @Override
  public QName getService() {
    return new QName("http:://adaptivity.nl/userMessageHandler", "userMessageHandler");
  }

  @Override
  public String getEndpoint() {
    return "external";
  }

  @XmlElementWrapper(name="tasks", namespace="http:://adaptivity.nl/userMessageHandler")
  @RestMethod(method=HttpMethod.GET, path="/pendingTasks")
  public Collection<Task> getPendingTasks() {
    return aService.getPendingTasks();
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
