package nl.adaptivity.process.userMessageHandler.server;

import java.util.Arrays;
import java.util.Collection;

import net.devrieze.util.HandleMap;

import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.EndpointProvider;
import nl.adaptivity.process.messaging.GenericEndpoint;



public class UserMessageService implements EndpointProvider {

  public static final String UMH_NS="http://adaptivity.nl/userMessageHandler";

  private InternalEndpoint internalEndpoint;
  private ExternalEndpoint externalEndpoint;

  private HandleMap<UserTask<?>> tasks;

  private AsyncMessenger aContext;

  public UserMessageService() {
    internalEndpoint = new InternalEndpoint(this);
    externalEndpoint = new ExternalEndpoint(this);
    tasks = new HandleMap<UserTask<?>>();
//    DummyTask task = new DummyTask("blabla");
//    task.setHandle(1);
//    tasks.add(task);
  }

  @Override
  public Collection<GenericEndpoint> getEndpoints() {
    return Arrays.asList(internalEndpoint, externalEndpoint);
  }

  public boolean postTask(UserTask<?> pTask) {
    pTask.setContext(getContext());
    return tasks.put(pTask) >= 0;
  }

  public Collection<UserTask<?>> getPendingTasks() {
    return tasks.toCollection();
  }

  public TaskState finishTask(long pHandle) {
    final UserTask<?> task = getTask(pHandle);
    task.setState(TaskState.Complete);
    if (task.getState()==TaskState.Complete|| task.getState()==TaskState.Failed) {
      tasks.remove(task);
    }
    return task.getState();
  }

  private UserTask<?> getTask(long pHandle) {
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
  public void setContext(AsyncMessenger context) {
    aContext = context;
  }

  public AsyncMessenger getContext() {
    return aContext;
  }

}
