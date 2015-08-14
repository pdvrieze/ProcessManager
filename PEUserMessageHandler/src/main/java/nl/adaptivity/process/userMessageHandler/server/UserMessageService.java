package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.db.DbSet;

import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


public class UserMessageService implements CompletionListener {

  public static final String DBRESOURCENAME="java:/comp/env/jdbc/usertasks";


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private UserTaskMap aTasks;
  private DataSource mDataSource;

  public UserMessageService() {
    //    DummyTask task = new DummyTask("blabla");
    //    task.setHandle(1);
    //    tasks.add(task);
  }

  private UserTaskMap getTasks() {
    if (aTasks==null) {
      aTasks = new UserTaskMap(getDataSource());
    }
    return aTasks;
  }

  private DataSource getDataSource() {
    if (mDataSource==null) {
      mDataSource = DbSet.resourceNameToDataSource(DBRESOURCENAME);
    }
    return mDataSource;
  }

  public boolean postTask(final XmlTask pTask) {
    return getTasks().put(pTask) >= 0;
  }

  public Collection<XmlTask> getPendingTasks(DBTransaction pTransaction) {
    final Iterable<XmlTask> tasks = getTasks().iterable(pTransaction);
    ArrayList<XmlTask> result = new ArrayList<>();
    for(XmlTask task:tasks) {
      result.add(task);
    }
    return result;
  }

  public XmlTask getPendingTask(long pHandle, Principal pUser) {
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

  private XmlTask getTask(final long pHandle) {
    return getTasks().get(pHandle);
  }

  public TaskState takeTask(final long pHandle, final Principal pUser) {
    getTask(pHandle).setState(TaskState.Taken, pUser);
    return TaskState.Taken;
  }

  public XmlTask updateTask(DBTransaction transaction, long pHandle, XmlTask pNewTask, Principal pUser) throws SQLException {
    // This needs to be a copy otherwise the cache will interfere with the changes
    XmlTask currentTask;
    {
      final XmlTask t = getTask(pHandle);
      if (t==null) { return null; }
      currentTask = new XmlTask(t);
    }
    for(XmlItem newItem: pNewTask.getItems()) {
      if (newItem.getName()!=null && newItem.getName().length()>0) {
        XmlItem currentItem = currentTask.getItem(newItem.getName());
        if (currentItem!=null) {
          currentItem.setValue(newItem.getValue());
        }
      }
    }

    // This may update the server.
    currentTask.setState(pNewTask.getState(), pUser);

    getTasks().set(transaction, pHandle, currentTask);
    return currentTask;
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

  public DBTransaction newTransaction() throws SQLException {
    return new DBTransaction(getDataSource());
  }

}
