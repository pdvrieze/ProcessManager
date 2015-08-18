package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.db.DbSet;

import net.devrieze.util.security.PermissionDeniedException;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


public class UserMessageService implements CompletionListener {

  private static class MyDBTransactionFactory implements TransactionFactory<DBTransaction> {
    private final Context mContext;

    private javax.sql.DataSource mDBResource = null;

    MyDBTransactionFactory() {
      InitialContext ic = null;
      try {
        ic = new InitialContext();
        mContext = (Context) ic.lookup("java:/comp/env");
      } catch (NamingException e) {
        throw new RuntimeException(e);
      }
    }

    private javax.sql.DataSource getDBResource() {
      if (mDBResource ==null) {
        if (mContext!=null) {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        } else {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        }
      }
      return mDBResource;
    }

    @Override
    public DBTransaction startTransaction() {
      try {
        return new DBTransaction(getDBResource());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Connection getConnection() throws SQLException {
      return mDBResource.getConnection();
    }

    @Override
    public boolean isValidTransaction(final DBTransaction pTransaction) {
      return pTransaction.providerEquals(mDBResource);
    }
  }

  public static final String CONTEXT_PATH = "java:/comp/env";
  public static final String DB_RESOURCE = "jdbc/usertasks";
  public static final String DBRESOURCENAME= CONTEXT_PATH +'/'+ DB_RESOURCE;


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private UserTaskMap aTasks;
  private TransactionFactory<DBTransaction> mTransactionFactory = new MyDBTransactionFactory();

  public UserMessageService() {
  }

  private UserTaskMap getTasks() {
    if (aTasks==null) {
      aTasks = new UserTaskMap(mTransactionFactory);
    }
    return aTasks;
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
    if (pUser==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
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
    if (pUser==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    getTask(pHandle).setState(TaskState.Taken, pUser);
    return TaskState.Taken;
  }

  public XmlTask updateTask(DBTransaction transaction, long pHandle, XmlTask pPartialNewTask, Principal pUser) throws SQLException {
    if (pUser==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    // This needs to be a copy otherwise the cache will interfere with the changes
    XmlTask currentTask;
    {
      final XmlTask t = getTask(pHandle);
      if (t==null) { return null; }
      currentTask = new XmlTask(t);
    }
    for(XmlItem newItem: pPartialNewTask.getItems()) {
      if (newItem.getName()!=null && newItem.getName().length()>0) {
        XmlItem currentItem = currentTask.getItem(newItem.getName());
        if (currentItem!=null) {
          currentItem.setValue(newItem.getValue());
        }
      }
    }

    // This may update the server.
    currentTask.setState(pPartialNewTask.getState(), pUser);

    getTasks().set(transaction, pHandle, currentTask);
    return currentTask;
  }

  public TaskState startTask(final long pHandle, final Principal pUser) {
    if (pUser==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
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
    return mTransactionFactory.startTransaction();
  }

}
