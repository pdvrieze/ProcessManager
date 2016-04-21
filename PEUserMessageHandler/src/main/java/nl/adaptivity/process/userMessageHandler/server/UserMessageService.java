/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.Transaction;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.TransactionedHandleMap;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.db.DbSet;
import net.devrieze.util.security.PermissionDeniedException;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;


public class UserMessageService<T extends Transaction> implements CompletionListener<Boolean /*<TODO Placeholder type*/> {

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
    public boolean isValidTransaction(final Transaction transaction) {
      return (transaction instanceof DBTransaction) &&  ((DBTransaction) transaction).providerEquals(mDBResource);
    }
  }

  public static final String CONTEXT_PATH = "java:/comp/env";
  public static final String DB_RESOURCE = "jdbc/usertasks";
  public static final String DBRESOURCENAME= CONTEXT_PATH +'/'+ DB_RESOURCE;


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = UserMessageService.newDBInstance();

  }

  private final TransactionedHandleMap<XmlTask, T> mTasks;
  private final TransactionFactory<T> mTransactionFactory;

  private UserMessageService(TransactionFactory<T> transactionFactory, TransactionedHandleMap<XmlTask, T> taskMap) {
    mTransactionFactory = transactionFactory;
    mTasks = taskMap;
  }

  public static UserMessageService<DBTransaction> newDBInstance() {
    MyDBTransactionFactory transactionFactory = new MyDBTransactionFactory();
    return new UserMessageService<DBTransaction>(transactionFactory, new UserTaskMap(transactionFactory));
  }

  public static <T extends Transaction> UserMessageService<T> newTestInstance(final TransactionFactory<T> transactionFactory, final TransactionedHandleMap<XmlTask, T> taskMap) {
    return new UserMessageService<>(transactionFactory, taskMap);
  }

  private TransactionedHandleMap<XmlTask, T> getTasks() {
    return mTasks;
  }

  public boolean postTask(final XmlTask task) {
    return getTasks().put(task) >= 0;
  }

  public Collection<XmlTask> getPendingTasks(T transaction, final Principal user) {
    final Iterable<XmlTask> tasks = getTasks().iterable(transaction);
    ArrayList<XmlTask> result = new ArrayList<>();
    for(XmlTask task:tasks) {
      result.add(task);
    }
    return result;
  }

  public XmlTask getPendingTask(long handle, Principal user) {
    return getTasks().get(handle);
  }

  public NodeInstanceState finishTask(final long handle, final Principal user) {
    try (T transaction = mTransactionFactory.startTransaction()) {
      return finishTask(transaction, handle, user);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public NodeInstanceState finishTask(final T transaction, final long handle, final Principal user) throws SQLException {
    if (user == null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    final XmlTask task = mTasks.get(transaction, handle);
    task.setState(NodeInstanceState.Complete, user);
    if ((task.getState() == NodeInstanceState.Complete) || (task.getState() == NodeInstanceState.Failed)) {
      mTasks.remove(transaction, task);
    }
    return task.getState();
  }

  private XmlTask getTask(final long handle) {
    return getTasks().get(handle);
  }

  public NodeInstanceState takeTask(final long handle, final Principal user) {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    getTask(handle).setState(NodeInstanceState.Taken, user);
    return NodeInstanceState.Taken;
  }

  public XmlTask updateTask(T transaction, long handle, XmlTask partialNewTask, Principal user) throws SQLException {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    // This needs to be a copy otherwise the cache will interfere with the changes
    XmlTask currentTask;
    {
      final XmlTask t = getTask(handle);
      if (t==null) { return null; }
      currentTask = new XmlTask(t);
    }
    for(XmlItem newItem: partialNewTask.getItems()) {
      if (newItem.getName()!=null && newItem.getName().length()>0) {
        XmlItem currentItem = currentTask.getItem(newItem.getName());
        if (currentItem!=null) {
          currentItem.setValue(newItem.getValue());
        }
      }
    }

    // This may update the server.
    currentTask.setState(partialNewTask.getState(), user);

    getTasks().set(transaction, handle, currentTask);
    return currentTask;
  }

  public NodeInstanceState startTask(final long handle, final Principal user) {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    getTask(handle).setState(NodeInstanceState.Started, user);
    return NodeInstanceState.Taken;
  }

  public static UserMessageService getInstance() {
    return InstantiationHelper.INSTANCE;
  }

  public void destroy() {
    // For now do nothing. Put deinitialization here.
  }

  @Override
  public void onMessageCompletion(final Future<? extends Boolean> future) {
    // TODO Auto-generated method stub
    //
  }

  public T newTransaction() throws SQLException {
    return mTransactionFactory.startTransaction();
  }

}
