package nl.adaptivity.process.userMessageHandler.server;

import java.util.Arrays;
import java.util.Collection;

import javax.jbi.component.ComponentContext;

import net.devrieze.util.HandleMap;

import nl.adaptivity.jbi.components.genericSE.EndpointProvider;
import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.exec.Task.TaskState;



public class UserMessageService implements EndpointProvider {

  public static final String UMH_NS="http://adaptivity.nl/userMessageHandler";

  private InternalEndpoint internalEndpoint;
  private ExternalEndpoint externalEndpoint;

  private HandleMap<UserTask> tasks;

  private ComponentContext aContext;

  public UserMessageService() {
    internalEndpoint = new InternalEndpoint(this);
    externalEndpoint = new ExternalEndpoint(this);
    tasks = new HandleMap<UserTask>();
//    DummyTask task = new DummyTask("blabla");
//    task.setHandle(1);
//    tasks.add(task);
  }

  @Override
  public Collection<GenericEndpoint> getEndpoints() {
    return Arrays.asList(internalEndpoint, externalEndpoint);
  }

  public boolean postTask(UserTask pTask) {
    pTask.setContext(getContext());
    return tasks.put(pTask) >= 0;
  }

  public Collection<UserTask> getPendingTasks() {
    return tasks.toCollection();
  }

  public TaskState finishTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Complete);
    return TaskState.Complete;
  }

  private UserTask getTask(long pHandle) {
    return tasks.get(pHandle);
  }

  public TaskState takeTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Taken);
    return TaskState.Taken;
  }

  public TaskState startTask(long pHandle) {
    getTask(pHandle).setState(TaskState.Started);
    return TaskState.Taken;
  }

  @Override
  public void setContext(ComponentContext context) {
    aContext = context;
  }

  public ComponentContext getContext() {
    return aContext;
  }

}
