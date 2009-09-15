package nl.adaptivity.process.userMessageHandler.server;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.jbi.rest.annotations.RestMethod;
import nl.adaptivity.jbi.rest.annotations.RestParam;
import nl.adaptivity.jbi.rest.annotations.RestService;
import nl.adaptivity.jbi.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.jbi.rest.annotations.RestParam.ParamType;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.userMessageHandler.server.UserMessageService.DummyTask;


@RestService
@XmlSeeAlso(DummyTask.class)
public class ExternalEndpoint implements GenericEndpoint {


  @XmlRootElement(name="tasks", namespace="http:://adaptivity.nl/userMessageHandler")
  @XmlAccessorType(XmlAccessType.NONE)
  private static class PendingTaskCollection {

    private final Collection<Task> aPendingTasks;

    public PendingTaskCollection() {
      aPendingTasks = new ArrayList<Task>();
    }
    
    public PendingTaskCollection(Collection<Task> pPendingTasks) {
      aPendingTasks = pPendingTasks;
    }

    @XmlElementRefs(@XmlElementRef(name="task", type=DummyTask.class, namespace="http:://adaptivity.nl/userMessageHandler"))
    public Collection<Task> getPendingTasks() {
      return aPendingTasks;
    }
    
    

  }

  public static final String ENDPOINT = "external";
  public static final QName SERVICENAME = new QName("http:://adaptivity.nl/userMessageHandler", "userMessageHandler");
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

//  @XmlElementWrapper(name="tasks", namespace="http:://adaptivity.nl/userMessageHandler")
  @RestMethod(method=HttpMethod.GET, path="/pendingTasks")
  public PendingTaskCollection getPendingTasks() {
    return new PendingTaskCollection(aService.getPendingTasks());
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
