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

import io.github.pdvrieze.kotlinsql.ddl.Column
import io.github.pdvrieze.kotlinsql.ddl.Table
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._UpdateBuilder
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.actions.*
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.DBTransactionFactory
import net.devrieze.util.Handle
import net.devrieze.util.HandleNotFoundException
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.db.MonadicDBTransaction
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.toComparableHandle
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.client.ServletProcessEngineClient
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.soapEnvelope.Envelope
import uk.ac.bournemouth.ac.db.darwin.usertasks.UserTaskDB
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class UserTaskMap(connectionProvider: DBTransactionFactory<MonadicDBTransaction<UserTaskDB>, UserTaskDB>) :
    DBHandleMap<XmlTask, XmlTask, MonadicDBTransaction<UserTaskDB>, UserTaskDB>(connectionProvider, UserTaskFactory()),
    IMutableUserTaskMap<MonadicDBTransaction<UserTaskDB>> {


    private class UserTaskFactory :
        AbstractElementFactory<XmlTask, XmlTask, MonadicDBTransaction<UserTaskDB>, UserTaskDB>() {

        override val table: Table
            get() {
                return UserTaskDB.usertasks
            }

        override val createColumns: List<Column<*, *, *>>
            get() {
                return listOf(u.taskhandle, u.remotehandle)
            }

        override val keyColumn: Column<Handle<XmlTask>, *, *>
            get() = u.taskhandle


        // XXX  This needs some serious overhaul
        override fun createBuilder(
            transaction: MonadicDBTransaction<UserTaskDB>,
            row: SelectResultSetRow<_ListSelect>
        ): DBAction<UserTaskDB, XmlTask> {
            fun handleException(e: Exception): Nothing {
                var f: Throwable = e
                while (f.cause != null && (f.cause is ExecutionException || f.cause is MessagingException)) {
                    f = f.cause!!
                }
                val cause = f.cause
                when {
                    cause is HandleNotFoundException -> throw cause
                    cause is RuntimeException        -> throw cause
                    f is ExecutionException ||
                        f is MessagingException      -> throw f
                    else                             -> throw e
                }

            }

            val handle = row.value(u.taskhandle, 1)!!
            val remoteHandle = row.value(u.remotehandle, 2)!! as Handle<Unit>

            val instanceFuture = ServletProcessEngineClient
                .getProcessNodeInstance(
                    remoteHandle.handleValue,
                    SYSTEMPRINCIPAL,
                    null,
                    XmlTask::class.java,
                    Envelope::class.java
                )
            return transaction.value {
                val instance: XmlProcessNodeInstance?
                try {
                    instance = instanceFuture.get(TASK_LOOKUP_TIMEOUT_MILIS.toLong(), TimeUnit.MILLISECONDS)
                } catch (e: ExecutionException) {
                    handleException(e)
                } catch (e: MessagingException) {
                    handleException(e)
                }
                instance?.body?.let { body ->
                    val env = XML.decodeFromReader<Envelope<XmlTask>>(body.getXmlReader())
                    env.body?.bodyContent?.apply {
                        setHandleValue(handle.handleValue)
                        this.remoteHandle = remoteHandle
                        state = instance.state
                    } ?: throw IllegalStateException("Could not properly deserialize the task")
                }

                XmlTask(handle.handleValue)

            }
        }

        override fun createFromBuilder(
            transaction: MonadicDBTransaction<UserTaskDB>,
            setAccess: DBSetAccess<XmlTask>,
            builder: XmlTask
        ): DBAction<UserTaskDB, XmlTask> {
            return with(transaction) {
                SELECT(nd.name, nd.data)
                    .WHERE { nd.taskhandle eq builder.handle }
                    .mapSingleOrNull { name, data ->
                        if (name!=null) builder[name]?.let { it.value = data }
                        builder
                    }.map { it!! }
            }
        }

        override fun getPrimaryKeyCondition(where: _Where, instance: XmlTask): WhereClause =
            getHandleCondition(where, instance.handle)

        override fun getHandleCondition(where: _Where, handle: Handle<XmlTask>): WhereClause {
            return with (where) { u.taskhandle eq handle }
        }

        override fun asInstance(obj: Any) = obj as? XmlTask

        override fun insertStatement(transaction: MonadicDBTransaction<UserTaskDB>): ValuelessInsertAction<UserTaskDB, Insert> {
            return transaction.INSERT(u.remotehandle)
        }

        override fun insertValues(
            transaction: MonadicDBTransaction<UserTaskDB>,
            insert: InsertActionCommon<UserTaskDB, Insert>,
            value: XmlTask
        ): InsertAction<UserTaskDB, Insert> {
            return insert.listVALUES(value.remoteHandle)
        }

        override fun store(update: _UpdateBuilder, value: XmlTask) {
            update.run { SET(u.remotehandle, value.remoteHandle) }
        }

        override fun postStore(
            transaction: MonadicDBTransaction<UserTaskDB>,
            handle: Handle<XmlTask>,
            oldValue: XmlTask?,
            newValue: XmlTask
        ): DBAction<UserTaskDB, Boolean> {

            val values = sequence {
                for (item in newValue.items) {
                    val itemName = item.name
                    if (itemName != null && item.type != "label") {
                        oldValue?.getItem(itemName)?.let { oldItem ->
                            if (!(oldItem.value == null && item.value == null)) {
                                yield(item)
                            }
                        }
                    }
                }
            }

            val insert = transaction.INSERT_OR_UPDATE(nd.taskhandle, nd.name, nd.data)
                .VALUES(values) { item ->
                    VALUES(handle, item.name, item.value)
                }
            return insert.map { it.sum() > 0 }
        }

        override fun preClear(transaction: MonadicDBTransaction<UserTaskDB>): DBAction<UserTaskDB, Any> {
            return transaction.DELETE_FROM(nd)
        }

        override fun preRemove(
            transaction: MonadicDBTransaction<UserTaskDB>,
            handle: Handle<XmlTask>
        ): DBAction<UserTaskDB, Boolean> {
            return transaction.DELETE_FROM(nd).WHERE { nd.taskhandle eq handle }.map { it > 0 }
        }

        override fun preRemove(
            transaction: MonadicDBTransaction<UserTaskDB>,
            element: XmlTask
        ): DBAction<UserTaskDB, Boolean> {
            return preRemove(transaction, element.handle)
        }

        override fun preRemove(
            transaction: MonadicDBTransaction<UserTaskDB>,
            columns: List<Column<*, *, *>>,
            values: List<Any?>
        ): DBAction<UserTaskDB, Boolean> {
            val handleVal = u.taskhandle.value(columns, values)
            return preRemove(transaction, handleVal.toComparableHandle())
        }

        companion object {
            private val TASK_LOOKUP_TIMEOUT_MILIS = 5
        }

    }

    override fun containsRemoteHandle(
        transaction: MonadicDBTransaction<UserTaskDB>,
        remoteHandle: Handle<*>
    ): Handle<XmlTask>? {
        return with(transaction) {
            SELECT(u.taskhandle)
                .WHERE { u.remotehandle eq remoteHandle }
                .mapSeq { it.singleOrNull() }
                .evaluateNow()
        }
    }

    companion object {

        val u = UserTaskDB.usertasks
        val nd = UserTaskDB.nodedata

    }
}
