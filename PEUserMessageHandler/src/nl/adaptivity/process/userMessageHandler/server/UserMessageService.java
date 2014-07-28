package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.Future;

import net.devrieze.util.HandleMap;
import net.devrieze.util.MemHandleMap;

import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


public class UserMessageService implements CompletionListener {


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private final HandleMap<UserTask<?>> tasks;

  public UserMessageService() {
    tasks = new MemHandleMap<UserTask<?>>();

    //    DummyTask task = new DummyTask("blabla");
    //    task.setHandle(1);
    //    tasks.add(task);
  }

  public boolean postTask(final UserTask<?> pTask) {
    return tasks.put(pTask) >= 0;
  }

  public Collection<UserTask<?>> getPendingTasks() {
    return tasks.toCollection();
  }

  public UserTask getPendingTask(long pHandle, Principal pUser) {
    return tasks.get(pHandle);
  }

  public TaskState finishTask(final long pHandle, final Principal pUser) {
    final UserTask<?> task = getTask(pHandle);
    task.setState(TaskState.Complete, pUser);
    if ((task.getState() == TaskState.Complete) || (task.getState() == TaskState.Failed)) {
      tasks.remove(task);
    }
    return task.getState();
  }

  private UserTask<?> getTask(final long pHandle) {
    return tasks.get(pHandle);
  }

  public TaskState takeTask(final long pHandle, final Principal pUser) {
    getTask(pHandle).setState(TaskState.Taken, pUser);
    return TaskState.Taken;
  }

  public TaskState startTask(final long pHandle, final Principal pUser) {
    getTask(pHandle).setState(TaskState.Started, pUser);
    return TaskState.Taken;
  }

  public static UserMessageService getInstance() {
    return InstantiationHelper.INSTANCE;
  }

  public void destroy() {
    // For now do nothing. Put deinitialization here.
  }

  @Override
  public void onMessageCompletion(final Future<?> pFuture) {
    // TODO Auto-generated method stub
    //
  }

}
