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

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.TransactionFactory
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.AbstractOldElementFactory
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.client.ServletProcessEngineClient
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.*
import nl.adaptivity.xml.XmlStreaming.EventType
import org.w3.soapEnvelope.Envelope
import uk.ac.bournemouth.ac.db.darwin.usertasks.UserTaskDB
import uk.ac.bournemouth.ac.db.darwin.usertasks.UserTaskDB.usertasks
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import uk.ac.bournemouth.kotlinsql.*
import uk.ac.bournemouth.util.kotlin.sql.DBConnection

import javax.xml.bind.JAXBException

import java.io.FileNotFoundException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class UserTaskMap(connectionProvider: TransactionFactory<out DBTransaction>) :
      DBHandleMap<XmlTask>(connectionProvider, UserTaskDB, UserTaskMap.UserTaskFactory()), IUserTaskMap<DBTransaction> {


  private class PostTaskFactory : XmlDeserializerFactory<XmlTask> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlTask {
      reader.skipPreamble()

      reader.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask")

      var result: XmlTask? = null
      while (reader.hasNext() && reader.next() !== EventType.END_ELEMENT) {
        when (reader.eventType) {
          XmlStreaming.EventType.START_ELEMENT -> if ("taskParam" == reader.localName) {
            reader.next() // Go to the contents
            result = XmlTask.deserialize(reader)
            reader.nextTag()
            reader.require(EventType.END_ELEMENT, null, "taskParam")
          } else {
            reader.skipElement()
            reader.require(EventType.END_ELEMENT, null, null)
          }
          else                                 -> reader.unhandledEvent()
        }
      }
      reader.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask")

      return result ?: throw IllegalArgumentException("Missing task parameters")
    }
  }


  private class UserTaskFactory : AbstractElementFactory<XmlTask>() {
    private var mColNoHandle: Int = 0
    private var mColNoRemoteHandle: Int = 0

    override val table: Table get() {
      return UserTaskDB.usertasks
    }

    override val createColumns: List<Column<*,*,*>> get() {
      return listOf(u.taskhandle, u.remotehandle)
    }

    override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
      get() = u.taskhandle

    // XXX  This needs some serious overhaul
    override fun create(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): XmlTask {
      val handle = u.taskhandle.value(columns,values)!!
      val remoteHandle = u.remotehandle.value(columns, values)!!

      val instance: XmlProcessNodeInstance?
      try {
        try {
          val future = ServletProcessEngineClient
                .getProcessNodeInstance(remoteHandle, SecurityProvider.SYSTEMPRINCIPAL,
                                        null, XmlTask::class.java, Envelope::class.java)
          instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS.toLong(), TimeUnit.MILLISECONDS)
          if (instance == null) {
            throw RuntimeException("No instance could be looked up")
          }
        } catch (e: ExecutionException) {

          var f:Throwable = e
          while (f.cause != null && (f.cause is ExecutionException || f.cause is MessagingException)) {
            f = f.cause!!
          }
          if (f.cause is FileNotFoundException) {
            throw f.cause as Throwable
          } else if (f.cause is RuntimeException) {
            throw f.cause as Throwable
          } else if (f is ExecutionException || f is MessagingException) {
            throw f
          }
          throw e
        } catch (e: MessagingException) {
          var f:Throwable = e
          while (f.cause != null && (f.cause is ExecutionException || f.cause is MessagingException)) {
            f = f.cause!!
          }
          if (f.cause is FileNotFoundException) {
            throw f.cause as FileNotFoundException
          } else if (f.cause is RuntimeException) {
            throw f.cause as RuntimeException
          } else if (f is ExecutionException) {
            throw f
          } else if (f is MessagingException) {
            throw f
          }
          throw e
        }

        if (instance.body != null) {
          val reader = XMLFragmentStreamReader.from(instance.body)
          val env = Envelope.deserialize(reader, PostTaskFactory())
          val task = env.body.bodyContent
          task.handleValue = handle
          task.remoteHandle = remoteHandle
          task.state = instance.state
          return task
        }
      } catch (e: JAXBException) {
        throw RuntimeException(e)
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      } catch (e: ExecutionException) {
        throw RuntimeException(e)
      } catch (e: TimeoutException) {
        throw RuntimeException(e)
      } catch (e: XmlException) {
        throw RuntimeException(e)
      }

      throw RuntimeException("This code should be unreachable")
    }

    @Throws(SQLException::class)
    override fun postCreate(connection: DBConnection, element: XmlTask) {
      if (element==null) throw NullPointerException()
      UserTaskDB.SELECT(nd.name, nd.data).WHERE { nd.taskhandle eq element.handleValue }.execute(connection) {
        name, data ->
        val item = element.getItem(name)
        if (item != null) {
          item.value = data
        }
      }
    }

    override fun getPrimaryKeyCondition(where: Database._Where,
                                        task: XmlTask): Database.WhereClause? {
      if (task ==null) throw NullPointerException()
      return getHandleCondition(where, task.handle)
    }

    override fun getHandleCondition(where: Database._Where, handle: Handle<XmlTask>): Database.WhereClause? {
      return where.run {
        u.taskhandle eq handle.handleValue
      }

    }

    override fun asInstance(value: Any): XmlTask? {
      if (value is XmlTask) {
        return value
      }
      return null
    }

    override fun insertStatement(value: XmlTask): Database.Insert {
      return UserTaskDB.INSERT(u.remotehandle).VALUES(value.remoteHandle)
    }

    override fun store(update: Database._UpdateBuilder, value: XmlTask) {
      if (value==null) throw NullPointerException()
      update.run { SET(u.remotehandle, value.remoteHandle) }
    }

    override fun postStore(connection: DBConnection, handle: Handle<XmlTask>, oldValue: XmlTask?, newValue: XmlTask) {
      val insert = UserTaskDB.INSERT(nd.taskhandle, nd.name, nd.data)
      for(item in newValue.items) {
        if (item.name!=null && item.type!="label") {
          oldValue?.getItem(item.name)?.let { oldItem ->
            if (!((oldItem == null || oldItem.value == null) && item.value == null)) {
              insert.VALUES(handle.handleValue, item.name, item.value)
            }
          }
        }
      }
      insert.executeUpdate(connection)
    }

    @Throws(SQLException::class)
    override fun preClear(connection: DBConnection) {
      WebAuthDB.DELETE_FROM(nd).executeUpdate(connection)
    }

    override fun preRemove(connection: DBConnection, handle: Handle<XmlTask>) {
      WebAuthDB.DELETE_FROM(nd).WHERE { nd.taskhandle eq handle.handleValue }.executeUpdate(connection)
    }

    @Throws(SQLException::class)
    override fun preRemove(connection: DBConnection, element: XmlTask) {
      preRemove(connection, element!!.handle)
    }

    override fun preRemove(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
      val handleVal = u.taskhandle.value(columns, values)!!
      preRemove(transaction.connection, Handles.handle<XmlTask>(handleVal))
    }

    companion object {


      private val TASK_LOOKUP_TIMEOUT_MILIS = 1
      private val QUERY_GET_DATA_FOR_TASK = "SELECT $COL_NAME, $COL_DATA FROM $TABLEDATA WHERE $COL_HANDLE = ?"
    }

  }

  @Throws(SQLException::class)
  override fun containsRemoteHandle(connection: DBTransaction, remoteHandle: Long): Handle<XmlTask>? {
    connection.connection.prepareStatement("SELECT $COL_HANDLE FROM $TABLE WHERE $COL_REMOTEHANDLE = ?") {
      setLong(1, remoteHandle)
      execute { rs ->
        if (!rs.next()) {
          return null
        }
        return Handles.handle<XmlTask>(rs.getLong(1))
      }
    }
  }

  companion object {

    val u = UserTaskDB.usertasks
    val nd = UserTaskDB.nodedata

    val TABLE = usertasks._name
    val TABLEDATA = UserTaskDB.nodedata._name
    val COL_HANDLE = "taskhandle"
    val COL_REMOTEHANDLE = "remotehandle"
    val COL_NAME = "name"
    val COL_DATA = "data"
  }
}
