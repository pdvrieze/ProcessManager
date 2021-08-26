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

package nl.adaptivity.process.engine.processModel

import io.github.pdvrieze.kotlinsql.ddl.Column
import io.github.pdvrieze.kotlinsql.ddl.Table
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._MaybeWhere
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.DBReceiver
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.processModel.engine.ExecutableCompositeActivity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableSplit
import nl.adaptivity.xmlutil.util.CompactFragment
import java.sql.SQLException

/**
 * Factory object to help with process node creation from a database.
 */

internal class ProcessNodeInstanceFactory(val processEngine: ProcessEngine<ProcessDBTransaction, *>) :
    AbstractElementFactory<ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>, SecureObject<ProcessNodeInstance<*>>, ProcessDBTransaction, ProcessEngineDB>() {

    companion object {
        private val tbl_pni = ProcessEngineDB.processNodeInstances
        private val tbl_pi = ProcessEngineDB.processInstances
        private val tbl_pred = ProcessEngineDB.pnipredecessors
        private val tbl_nd = ProcessEngineDB.nodedata
        private val tbl_pm = ProcessEngineDB.processModels
        const val FAILURE_CAUSE = "failureCause"
    }

    override fun getHandleCondition(where: _Where, handle: Handle<SecureObject<ProcessNodeInstance<*>>>): WhereClause {
        return where.run { tbl_pni.pnihandle eq handle }
    }

    override val table: Table
        get() = tbl_pni

    override val createColumns: List<Column<*, *, *>>
        get() = listOf(tbl_pni.pnihandle, tbl_pni.nodeid, tbl_pni.pihandle, tbl_pni.state, tbl_pni.entryno)

    override fun createBuilder(
        transaction: ProcessDBTransaction,
        row: SelectResultSetRow<_ListSelect>
    ): DBAction<ProcessEngineDB, ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>> {
        val pnihandle = tbl_pni.pnihandle.value(row).toComparableHandle()
        val nodeId = tbl_pni.nodeid.value(row)
        val pihandle = tbl_pni.pihandle.value(row).toComparableHandle()
        val state = tbl_pni.state.value(row)
        val entryNo = tbl_pni.entryno.nullableValue(row) ?: 1

        with (transaction) {
            val processInstanceBuilder = transaction.pendingProcessInstance(pihandle)
                ?: processEngine.getProcessInstance(transaction, pihandle, SYSTEMPRINCIPAL).builder()

            val processModel = processInstanceBuilder.processModel

            val node = processModel.requireNode(nodeId)

            SELECT(tbl_pred.predecessor)
                .WHERE { tbl_pred.pnihandle eq pnihandle }
                .mapEach {  }
                .getList(transaction.connection)
                .map { it?.let { handle(it) } }
                .requireNoNulls()

        }

        val predecessors = ProcessEngineDB
            .SELECT(tbl_pred.predecessor)
            .WHERE { tbl_pred.pnihandle eq pnihandle }
            .getList(transaction.connection)
            .map { it?.let { handle(it) } }
            .requireNoNulls()

        return when {
            node is ExecutableJoin              -> {
                JoinInstance.BaseBuilder(
                    node, predecessors, processInstanceBuilder, processInstanceBuilder.owner, entryNo,
                    handle<SecureObject<ProcessNodeInstance<*>>>(handle = pnihandle.handleValue), state
                )
            }
            node is ExecutableSplit             -> {
                SplitInstance.BaseBuilder(
                    node, predecessors.single(), processInstanceBuilder, processInstanceBuilder.owner,
                    entryNo, handle<SecureObject<ProcessNodeInstance<*>>>(handle = pnihandle.handleValue), state
                )
            }
            node is ExecutableCompositeActivity -> {
                val childInstance = ProcessEngineDB
                    .SELECT(tbl_pi.pihandle)
                    .WHERE { tbl_pi.parentActivity eq pnihandle }
                    .getSingleOrNull(transaction.connection)?.let { handle<SecureObject<ProcessInstance>>(it) }
                    ?: getInvalidHandle()

                CompositeInstance.BaseBuilder(
                    node, predecessors.single(), processInstanceBuilder, childInstance, processInstanceBuilder.owner,
                    entryNo,
                    handle<SecureObject<ProcessNodeInstance<*>>>(handle = pnihandle.handleValue), state
                )
            }
            else                                -> {
                DefaultProcessNodeInstance.BaseBuilder(
                    node, predecessors, processInstanceBuilder,
                    processInstanceBuilder.owner, entryNo,
                    handle<SecureObject<DefaultProcessNodeInstance>>(handle = pnihandle.handleValue), state
                )
            }
        }
    }

    override fun createFromBuilder(
        dbReceiver: DBReceiver<ProcessEngineDB>,
        setAccess: DbSet.DBSetAccess<ProcessEngineDB>,
        builder: ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>
    ): DBAction<ProcessEngineDB, SecureObject<ProcessNodeInstance<*>>> {
        return with(dbReceiver) {
            SELECT(tbl_nd.name, tbl_nd.data)
                .WHERE { tbl_nd.pnihandle eq builder.handle }
                .mapEach { name, data ->
                    if (FAILURE_CAUSE == name && (builder.state == NodeInstanceState.Failed || builder.state == NodeInstanceState.FailRetry)) {
                        builder.failureCause = Exception(data)
                        null
                    } else {
                        ProcessData(name, CompactFragment(data!!))
                    }
                }
                .map { results ->
                    builder.results.replaceBy(results.filterNotNull())
                    builder.build()
                }
        }
    }

    override fun getPrimaryKeyCondition(
        where: Database._Where,
        instance: SecureObject<ProcessNodeInstance<*>>
    ): Database.WhereClause? {
        return getHandleCondition(where, instance.withPermission().handle)
    }

    @Suppress("UNCHECKED_CAST")
    override fun asInstance(obj: Any) = obj as? DefaultProcessNodeInstance

    override fun store(update: Database._UpdateBuilder, value: SecureObject<ProcessNodeInstance<*>>) {
        update.run {
            value.withPermission().let { value ->
                SET(tbl_pni.nodeid, value.node.id)
                SET(tbl_pni.pihandle, value.hProcessInstance)
                SET(tbl_pni.state, value.state)
            }
        }
    }

    override fun postStore(
        connection: DBConnection,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        oldValue: SecureObject<ProcessNodeInstance<*>>?,
        newValue: SecureObject<ProcessNodeInstance<*>>
    ) {
        if (oldValue != null) { // update
            ProcessEngineDB
                .DELETE_FROM(tbl_pred)
                .WHERE { tbl_pred.pnihandle eq handle }
                .executeUpdate(connection)
        }
        newValue.withPermission().let { newValue ->
            if (newValue.predecessors.isNotEmpty()) {
                val insert = ProcessEngineDB
                    .INSERT(tbl_pred.pnihandle, tbl_pred.predecessor)

                for (predecessor in newValue.predecessors) {
                    insert.VALUES(handle, predecessor)
                }

                insert.executeUpdate(connection)
            }

            val isFailure = newValue.state == NodeInstanceState.Failed || newValue.state == NodeInstanceState.FailRetry
            val results = newValue.results
            if (results.isNotEmpty() || (isFailure && newValue.failureCause != null)) {
                val insert = ProcessEngineDB.INSERT_OR_UPDATE(tbl_nd.pnihandle, tbl_nd.name, tbl_nd.data)
                for (data in results) {
                    insert.VALUES(newValue.handle, data.name, data.content.contentString)
                }
                if (isFailure) {
                    newValue.failureCause?.let { cause ->
                        insert.VALUES(newValue.handle, FAILURE_CAUSE, cause.message)
                    }
                }
                insert.executeUpdate(connection)
            }
        }
    }

    override fun insertStatement(value: SecureObject<ProcessNodeInstance<*>>): Database.Insert {
        return value.withPermission().let { value ->
            ProcessEngineDB
                .INSERT(tbl_pni.nodeid, tbl_pni.pihandle, tbl_pni.state)
                .VALUES(value.node.id, value.hProcessInstance, value.state)
        }
    }

    override val keyColumn get() = tbl_pni.pnihandle

    override fun preRemove(transaction: ProcessDBTransaction, handle: Handle<SecureObject<ProcessNodeInstance<*>>>) {

        val connection = transaction.connection
        ProcessEngineDB
            .DELETE_FROM(tbl_pred)
            .WHERE { tbl_pred.pnihandle eq handle }
            .executeUpdate(connection)

        ProcessEngineDB
            .DELETE_FROM(tbl_nd)
            .WHERE { tbl_nd.pnihandle eq handle }
            .executeUpdate(connection)
    }

    override fun preRemove(transaction: ProcessDBTransaction, element: SecureObject<ProcessNodeInstance<*>>) {
        preRemove(transaction, element.withPermission().handle)
    }

    override fun preRemove(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
        val handle = tbl_pni.pnihandle.value(columns, values)
        preRemove(transaction, handle)
    }

    @Throws(SQLException::class)
    override fun preClear(transaction: ProcessDBTransaction) {

        val connection = transaction.connection

        ProcessEngineDB
            .DELETE_FROM(tbl_pred)
            .WHERE { filter(this) }
            .executeUpdate(connection)

        ProcessEngineDB
            .DELETE_FROM(tbl_nd)
            .WHERE { filter(this) }
            .executeUpdate(connection)

    }

}
