package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.Future;

import net.devrieze.util.HandleMap;
import net.devrieze.util.MemHandleMap;
import net.devrieze.util.db.DbSet;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


public class UserMessageService implements CompletionListener {

  public static final String DBRESOURCENAME="java:/comp/env/jdbc/usertasks";


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private UserTaskMap aTasks;

  public UserMessageService() {
    //    DummyTask task = new DummyTask("blabla");
    //    task.setHandle(1);
    //    tasks.add(task);
  }

  private UserTaskMap getTasks() {
    if (aTasks==null) {
      aTasks = new UserTaskMap(DbSet.resourceNameToDataSource(DBRESOURCENAME));
    }
    return aTasks;
  }

  public boolean postTask(final UserTask<?> pTask) {
    return getTasks().put(pTask) >= 0;
  }

  public Collection<UserTask<?>> getPendingTasks() {
    return getTasks().toCollection();
  }

  public UserTask getPendingTask(long pHandle, Principal pUser) {
    return getTasks().get(pHandle);
  }

  public TaskState finishTask(final long pHandle, final Principal pUser) {
    final UserTask<?> task = getTask(pHandle);
    task.setState(TaskState.Complete, pUser);
    if ((task.getState() == TaskState.Complete) || (task.getState() == TaskState.Failed)) {
      getTasks().remove(task);
    }
    return task.getState();
  }

  private UserTask<?> getTask(final long pHandle) {
    return getTasks().get(pHandle);
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
