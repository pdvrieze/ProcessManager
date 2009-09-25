package nl.adaptivity.process.userMessageHandler.server;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.jbi.components.genericSE.EndpointProvider;
import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;



public class UserMessageService implements EndpointProvider {

  @XmlRootElement(name="dummyTask")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class DummyTask implements Task {

    private TaskState aState = TaskState.Available;
    private long aHandle = -1;
    private String aSummary;
    
    public DummyTask() {}

    public DummyTask(String pSummary) {
      setSummary(pSummary);
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public void setState(TaskState pNewState) {
      aState  = pNewState;
    }

    @XmlAttribute
    @Override
    public long getHandle() {
      return aHandle ;
    }

    @Override
    public void setHandle(long pHandle) {
      aHandle = pHandle;
    }

    @XmlAttribute(name="summary")
    public String getSummary() {
      return aSummary;
    }

    public void setSummary(String summary) {
      aSummary = summary;
    }

    @Override
    public void failTask() {
      // TODO Auto-generated method stub
      // 
      throw new UnsupportedOperationException("Not yet implemented");
      
    }

    @Override
    public void finishTask(Object pPayload) {
      setState(TaskState.Complete);
    }

    @Override
    public boolean provideTask() {
      setState(TaskState.Available);
      return false;
    }

    @Override
    public <T> boolean startTask(IMessageService<T> pMessageService) {
      setState(TaskState.Started);
      return false;
    }

    @Override
    public boolean takeTask() {
      // TODO Auto-generated method stub
      // return false;
      throw new UnsupportedOperationException("Not yet implemented");
      
    }

  }

  private InternalEndpoint internalEndpoint;
  private ExternalEndpoint externalEndpoint;

  private Queue<Task> tasks;
  
  public UserMessageService() {
    internalEndpoint = new InternalEndpoint(this);
    externalEndpoint = new ExternalEndpoint(this);
    tasks = new ArrayDeque<Task>();
    DummyTask task = new DummyTask("blabla");
    task.setHandle(1);
    tasks.add(task);
  }
  
  @Override
  public Collection<GenericEndpoint> getEndpoints() {
    return Arrays.asList(internalEndpoint, externalEndpoint);
  }

  public boolean postTask(Task pTask) {
    return tasks.add(pTask);
  }

  public Collection<Task> getPendingTasks() {
    return tasks;
  }

  public TaskState finishTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Complete);
    return TaskState.Complete;
  }

  private Task getTask(long pHandle) {
    for(Task candidate:tasks) {
      if (candidate.getHandle()==pHandle) {
        return candidate;
      }
    }
    return null;
  }

  public TaskState takeTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Taken);
    return TaskState.Taken;
  }

  public TaskState startTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Started);
    return TaskState.Taken;
  }

}
