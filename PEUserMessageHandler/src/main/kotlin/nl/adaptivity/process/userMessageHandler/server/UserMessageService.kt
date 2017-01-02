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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.Handle
import net.devrieze.util.Transaction
import net.devrieze.util.TransactionFactory
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.AuthenticationNeededException
import nl.adaptivity.messaging.CompletionListener
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import uk.ac.bournemouth.ac.db.darwin.usertasks.UserTaskDB
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.concurrent.Future
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException


class UserMessageService<T : Transaction> private constructor(private val transactionFactory: TransactionFactory<T>, taskMap: IMutableUserTaskMap<T>) : CompletionListener<Boolean>/*<TODO Placeholder type*/ {

  public inner class UserMessageServiceTransaction(val transaction:T) {
    fun postTask(task: XmlTask) = postTask(transaction, task)
    fun getPendingTasks(user:Principal) = getPendingTasks(transaction, user)
    fun getPendingTask(handle: Handle<XmlTask>, user: Principal) = getPendingTask(transaction, handle, user)
    fun finishTask(handle: Handle<XmlTask>, user: Principal) = finishTask(transaction, handle, user)
    fun getTask(handle: Handle<XmlTask>) = getTask(transaction, handle)
    fun takeTask(handle: Handle<XmlTask>, user: Principal) = takeTask(transaction, handle, user)
    fun updateTask(handle: Handle<XmlTask>, partialNewTask: XmlTask, user: Principal) =
          updateTask(transaction, handle, partialNewTask, user)
    fun startTask(handle: Handle<XmlTask>, user: Principal) = startTask(transaction, handle, user)

    inline fun <R> commit(block: ()-> R) = block().apply { transaction.commit() }
  }

  private class MyDBTransactionFactory internal constructor() : TransactionFactory<DBTransaction> {
    private val mContext: Context?

    private var mDBResource: javax.sql.DataSource? = null

    init {
      try {
        val ic = InitialContext()
        mContext = ic.lookup("java:/comp/env") as Context
      } catch (e: NamingException) {
        throw RuntimeException(e)
      }

    }

    private val dbResource: javax.sql.DataSource
      get() {
        return mDBResource ?: return DbSet.resourceNameToDataSource(mContext, DB_RESOURCE).apply { mDBResource = this }
      }

    override fun startTransaction(): DBTransaction {
      return DBTransaction(dbResource, UserTaskDB)
    }

    override fun isValidTransaction(transaction: Transaction): Boolean {
      return transaction is DBTransaction && transaction.connection.db === UserTaskDB
    }
  }


  private object InstantiationHelper {

    val INSTANCE: UserMessageService<DBTransaction> = UserMessageService.newDBInstance()

  }

  private val tasks: IMutableUserTaskMap<T> = taskMap

  @Throws(SQLException::class)
  fun postTask(transaction: T, task: XmlTask): Boolean {
    // This must be handled as the response can get lost without the transaction failing.
    val existingHandle = tasks.containsRemoteHandle(transaction, task.remoteHandle)
    if (existingHandle != null) {
      task.setHandleValue(existingHandle.handleValue)
      return false // no proper update
    }

    val taskOwner = task.owner ?: throw NullPointerException("The task owner is not specified")

    tasks.put(transaction, task)
    task.setState(NodeInstanceState.Acknowledged, taskOwner) // Only now mark as acknowledged
    return true

  }

  fun getPendingTasks(transaction: T, user: Principal): Collection<XmlTask> {
    val tasks = tasks.iterable(transaction)
    val result = ArrayList<XmlTask>()
    for (task in tasks) {
      if (!(task.state?.isFinal ?: false)) {
        result.add(task)
      }
    }
    return result
  }

  @Throws(SQLException::class)
  fun getPendingTask(transaction: T, handle: Handle<XmlTask>, user: Principal): XmlTask? {
    return tasks[transaction, handle]
  }

  @Throws(SQLException::class)
  fun finishTask(transaction: T, taskHandle: Handle<out XmlTask>, user: Principal?): NodeInstanceState {
    if (user == null) {
      throw AuthenticationNeededException("There is no user associated with this request")
    }
    val task = tasks[transaction, taskHandle] ?: throw NullPointerException("Missing task")
    task.setState(NodeInstanceState.Complete, user)
    if (task.state == NodeInstanceState.Complete || task.state == NodeInstanceState.Failed) {
      tasks.remove(transaction, taskHandle)
    }
    return task.state ?: throw NullPointerException("Task has an unspecified state")
  }

  @Throws(SQLException::class)
  private fun getTask(transaction: T, handle: Handle<XmlTask>): XmlTask? {
    return tasks[transaction, handle]
  }

  @Throws(SQLException::class)
  fun takeTask(transaction: T, handle: Handle<XmlTask>, user: Principal?): NodeInstanceState {
    if (user == null) {
      throw AuthenticationNeededException("There is no user associated with this request")
    }
    getTask(transaction, handle)!!.setState(NodeInstanceState.Taken, user)
    return NodeInstanceState.Taken
  }

  @Throws(SQLException::class)
  fun updateTask(transaction: T, handle: Handle<XmlTask>, partialNewTask: XmlTask, user: Principal?): XmlTask? {
    if (user == null) {
      throw AuthenticationNeededException("There is no user associated with this request")
    }

    // This needs to be a copy otherwise the cache will interfere with the changes
    val currentTask: XmlTask = getTask(transaction, handle)?.let { XmlTask(it) } ?: return null

    for (newItem in partialNewTask.items) {
      val newItemName = newItem.name
      if (newItemName!=null && newItemName.isNotEmpty()) {
        val currentItem = currentTask.getItem(newItemName)
        if (currentItem != null) {
          currentItem.value = newItem.value
        }
      }
    }
    // Store the data
    tasks[transaction, handle] = currentTask
    transaction.commit() // the actual state isn't stored anyway.
    // This may update the server.
    partialNewTask.state?.let { currentTask.setState(it, user) }

    return currentTask
  }

  @Throws(SQLException::class)
  fun startTask(transaction: T, handle: Handle<XmlTask>, user: Principal?): NodeInstanceState {
    if (user == null) {
      throw AuthenticationNeededException("There is no user associated with this request")
    }
    getTask(transaction, handle)!!.setState(NodeInstanceState.Started, user)
    return NodeInstanceState.Taken
  }

  fun destroy() {
    // For now do nothing. Put deinitialization here.
  }

  override fun onMessageCompletion(future: Future<out Boolean>) {
    // TODO Auto-generated method stub
    //
  }

  @Throws(SQLException::class)
  fun newTransaction(): T {
    return transactionFactory.startTransaction()
  }

  inline fun <R> inTransaction(block: UserMessageServiceTransaction.()->R): R {
    return newTransaction().use { transaction ->
      UserMessageServiceTransaction(transaction).block()
    }
  }

  companion object {

    const val CONTEXT_PATH = "java:/comp/env"

    const val DB_RESOURCE = "jdbc/usertasks"

    const val DBRESOURCENAME = CONTEXT_PATH + '/' + DB_RESOURCE

    @JvmStatic
    fun newDBInstance(): UserMessageService<DBTransaction> {
      val transactionFactory = MyDBTransactionFactory()
      return UserMessageService(transactionFactory, UserTaskMap(transactionFactory))
    }

    @JvmStatic
    fun <T : Transaction> newTestInstance(transactionFactory: TransactionFactory<T>,
                                          taskMap: IMutableUserTaskMap<T>): UserMessageService<T> {
      return UserMessageService(transactionFactory, taskMap)
    }

    @JvmStatic
    val instance: UserMessageService<DBTransaction>
      get() = InstantiationHelper.INSTANCE
  }

}
