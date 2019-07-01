/*
 * Copyright (c) 2018.
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
import net.devrieze.util.TransactionFactory
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.handle
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.client.ServletProcessEngineClient
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.EventType
import org.w3.soapEnvelope.Envelope
import uk.ac.bournemouth.ac.db.darwin.usertasks.UserTaskDB
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.io.FileNotFoundException
import java.sql.SQLException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class UserTaskMap(connectionProvider: TransactionFactory<out DBTransaction>) :
      DBHandleMap<XmlTask, XmlTask, DBTransaction>(connectionProvider, UserTaskDB, UserTaskMap.UserTaskFactory()), IMutableUserTaskMap<DBTransaction> {


  private class PostTaskFactory : XmlDeserializerFactory<XmlTask> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlTask {
      reader.skipPreamble()

      reader.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask")

      var result: XmlTask? = null
      while (reader.hasNext() && reader.next() !== EventType.END_ELEMENT) {
        when (reader.eventType) {
          EventType.START_ELEMENT -> if ("taskParam" == reader.localName) {
            reader.next() // Go to the contents
            result = XmlTask.deserialize(reader)
            reader.nextTag()
            reader.require(EventType.END_ELEMENT, null, "taskParam")
          } else {
            reader.skipElement()
            reader.require(EventType.END_ELEMENT, null, null)
          }
          else                                      -> throw XmlException("unsupported event ${reader.eventType} in reading postTask")
        }
      }
      reader.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask")

      return result ?: throw IllegalArgumentException("Missing task parameters")
    }
  }


  private class UserTaskFactory : AbstractElementFactory<XmlTask, XmlTask, DBTransaction>() {
    private var mColNoHandle: Int = 0
    private var mColNoRemoteHandle: Int = 0

    override val table: Table get() {
      return UserTaskDB.usertasks
    }

    override val createColumns: List<Column<*,*,*>> get() {
      return listOf(u.taskhandle, u.remotehandle)
    }

    override val keyColumn: Column<Handle<XmlTask>, *, *>
      get() = u.taskhandle

    // XXX  This needs some serious overhaul
    override fun create(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): XmlTask {
      val handle = u.taskhandle.value(columns, values)
      val remoteHandle = u.remotehandle.value(columns, values)

      val instance: XmlProcessNodeInstance?
      try {
        val future = ServletProcessEngineClient
              .getProcessNodeInstance(remoteHandle.handleValue, SYSTEMPRINCIPAL, null, XmlTask::class.java, Envelope::class.java)
        instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS.toLong(), TimeUnit.MILLISECONDS)
      } catch (e: ExecutionException) {

        var f:Throwable = e
        while (f.cause != null && (f.cause is ExecutionException || f.cause is MessagingException)) {
          f = f.cause!!
        }
        if (f.cause is FileNotFoundException) {
          throw f.cause!!
        } else if (f.cause is RuntimeException) {
          throw f.cause!!
        } else if (f is ExecutionException || f is MessagingException) {
          throw f
        }
        throw e
      } catch (e: MessagingException) {
        var f:Throwable = e
        while (f.cause != null && (f.cause is ExecutionException || f.cause is MessagingException)) {
          f = f.cause!!
        }
        when {
            f.cause is FileNotFoundException -> throw f.cause as FileNotFoundException
            f.cause is RuntimeException      -> throw f.cause as RuntimeException
            f is ExecutionException          -> throw f
            f is MessagingException          -> throw f
            else                             -> throw e
        }
      }

      instance?.body?.let { body ->
        val reader = body.getXmlReader()
        val env = Envelope.deserialize(reader, PostTaskFactory())
        val task = env.body?.bodyContent?.apply {
            setHandleValue(handle.handleValue)
            this.remoteHandle = remoteHandle
            state = instance.state
        } ?: throw IllegalStateException("Could not properly deserialize the task")
        return task
      }

      return XmlTask(handle.handleValue)
    }

    @Throws(SQLException::class)
    override fun postCreate(transaction: DBTransaction, element: XmlTask): XmlTask {
      UserTaskDB.SELECT(nd.name, nd.data).WHERE { nd.taskhandle eq element.getHandle() }.execute(transaction.connection) {
        name, data ->
        if (name!=null) {
          element[name]?.let { it.value = data }
        }
      }
      return element
    }

    override fun getPrimaryKeyCondition(where: Database._Where, instance: XmlTask) =
          getHandleCondition(where, instance.getHandle())

    override fun getHandleCondition(where: Database._Where, handle: Handle<out XmlTask>) = where.run {
      u.taskhandle eq handle
    }

    override fun asInstance(obj: Any) = obj as? XmlTask

    override fun insertStatement(value: XmlTask): Database.Insert {
      return UserTaskDB.INSERT(u.remotehandle).VALUES(value.remoteHandle)
    }

    override fun store(update: Database._UpdateBuilder, value: XmlTask) {
      update.run { SET(u.remotehandle, value.remoteHandle) }
    }

    override fun postStore(connection: DBConnection, handle: Handle<out XmlTask>, oldValue: XmlTask?, newValue: XmlTask) {
      val insert = UserTaskDB.INSERT_OR_UPDATE(nd.taskhandle, nd.name, nd.data)
      for(item in newValue.items) {
        val itemName = item.name
        if (itemName !=null && item.type!="label") {
          oldValue?.getItem(itemName)?.let { oldItem ->
            if (!(oldItem.value == null && item.value == null)) {
              insert.VALUES(handle, itemName, item.value)
            }
          }
        }
      }
      insert.executeUpdate(connection)
    }

    @Throws(SQLException::class)
    override fun preClear(transaction: DBTransaction) {
      WebAuthDB.DELETE_FROM(nd).executeUpdate(transaction.connection)
    }

    override fun preRemove(transaction: DBTransaction, handle: Handle<XmlTask>) {
      WebAuthDB.DELETE_FROM(nd).WHERE { nd.taskhandle eq handle }.executeUpdate(transaction.connection)
    }

    @Throws(SQLException::class)
    override fun preRemove(transaction: DBTransaction, element: XmlTask) {
      preRemove(transaction, element.getHandle())
    }

    override fun preRemove(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
      val handleVal = u.taskhandle.value(columns, values)
      preRemove(transaction, handle<XmlTask>(handleVal))
    }

    companion object {
      private val TASK_LOOKUP_TIMEOUT_MILIS = 5
    }

  }

  @Throws(SQLException::class)
  override fun containsRemoteHandle(transaction: DBTransaction, remoteHandle: Handle<*>): Handle<XmlTask>? {
    return UserTaskDB.SELECT(u.taskhandle).WHERE { u.remotehandle eq remoteHandle }.getSingleOrNull(transaction.connection)
  }

  companion object {

    val u = UserTaskDB.usertasks
    val nd = UserTaskDB.nodedata

  }
}
