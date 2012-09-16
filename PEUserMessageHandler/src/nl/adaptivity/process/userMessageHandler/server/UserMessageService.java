package nl.adaptivity.process.userMessageHandler.server;

import java.util.Collection;

import net.devrieze.util.HandleMap;
import net.devrieze.util.Urls;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.AsyncMessenger.AsyncFuture;
import nl.adaptivity.process.messaging.AsyncMessenger.CompletionListener;



public class UserMessageService implements CompletionListener {

  
  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  public static final String UMH_NS="http://adaptivity.nl/userMessageHandler";

  private HandleMap<UserTask<?>> tasks;

  private AsyncMessenger aContext;

  public UserMessageService() {
    tasks = new HandleMap<UserTask<?>>();
    aContext = AsyncMessenger.getInstance(Urls.newURL("http://localhost:9080/"));
    aContext.addCompletionListener(this);
    
    
//    DummyTask task = new DummyTask("blabla");
//    task.setHandle(1);
//    tasks.add(task);
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
  public void onMessageCompletion(AsyncFuture pFuture) {
    // TODO Auto-generated method stub
    // 
  }

}
