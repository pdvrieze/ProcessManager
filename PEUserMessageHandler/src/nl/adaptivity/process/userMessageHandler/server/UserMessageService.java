package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.Future;

import net.devrieze.util.HandleMap;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;



public class UserMessageService implements CompletionListener {


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private HandleMap<UserTask<?>> tasks;

  private AsyncMessenger aContext;

  public UserMessageService() {
    tasks = new HandleMap<UserTask<?>>();

//    DummyTask task = new DummyTask("blabla");
//    task.setHandle(1);
//    tasks.add(task);
  }

  public boolean postTask(UserTask<?> pTask) {
    return tasks.put(pTask) >= 0;
  }

  public Collection<UserTask<?>> getPendingTasks() {
    return tasks.toCollection();
  }

  public TaskState finishTask(long pHandle, Principal pUser) {
    final UserTask<?> task = getTask(pHandle);
    task.setState(TaskState.Complete, pUser);
    if (task.getState()==TaskState.Complete|| task.getState()==TaskState.Failed) {
      tasks.remove(task);
    }
    return task.getState();
  }

  private UserTask<?> getTask(long pHandle) {
    return tasks.get(pHandle);
  }

  public TaskState takeTask(long pHandle, Principal pUser) {
    getTask(pHandle).setState(TaskState.Taken, pUser);
    return TaskState.Taken;
  }

  public TaskState startTask(long pHandle, Principal pUser) {
    getTask(pHandle).setState(TaskState.Started, pUser);
    return TaskState.Taken;
  }

  public AsyncMessenger getContext() {
    return aContext;
  }

  public static UserMessageService getInstance() {
    return InstantiationHelper.INSTANCE;
  }

  public synchronized void destroy() {
    if (aContext!=null) {
      aContext.destroy();
      aContext = null;
    }
  }

  @Override
  public void onMessageCompletion(Future<?> pFuture) {
    // TODO Auto-generated method stub
    //
  }

}
