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
import io.github.pdvrieze.kotlinsql.ddl.columns.CustomColumnType
import io.github.pdvrieze.kotlinsql.ddl.columns.NumberColumnConfiguration
import io.github.pdvrieze.kotlinsql.ddl.columns.NumericColumn
import io.github.pdvrieze.kotlinsql.ddl.columns.NumericColumnType
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.Select1
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._UpdateBuilder
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.VALUES
import io.github.pdvrieze.kotlinsql.monadic.actions.*
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
        val pnihandle = tbl_pni.pnihandle.value(row)
        val nodeId = tbl_pni.nodeid.value(row)
        val pihandle = tbl_pni.pihandle.value(row)
        val state = tbl_pni.state.value(row)
        val entryNo = tbl_pni.entryno.nullableValue(row) ?: 1
        val assignedUser = tbl_pni.assigneduser.nullableValue(row)?.let(processEngine::getPrincipal)

        with(transaction) {
            val processInstanceBuilder = transaction.pendingProcessInstance(pihandle)
                ?: processEngine.getProcessInstance(transaction, pihandle, SYSTEMPRINCIPAL).builder()

            val processModel = processInstanceBuilder.processModel

            val node = processModel.requireNode(nodeId)

            val predecessorHandles = SELECT(tbl_pred.predecessor)
                .WHERE { tbl_pred.pnihandle eq pnihandle }
                .mapEach { it!! }

            return when (node) {
                is ExecutableJoin -> predecessorHandles.map { predecessors ->
                    JoinInstance.BaseBuilder(
                        node, predecessors, processInstanceBuilder, processInstanceBuilder.owner, entryNo,
                        if (pnihandle.handleValue < 0) Handle.invalid() else Handle(pnihandle.handleValue), state
                    )
                }

                is ExecutableSplit -> predecessorHandles.map { predecessors ->
                    SplitInstance.BaseBuilder(
                        node,
                        predecessors.single(),
                        processInstanceBuilder,
                        processInstanceBuilder.owner,
                        entryNo,
                        if (pnihandle.handleValue < 0) Handle.invalid() else Handle(pnihandle.handleValue),
                        state
                    )
                }

                is ExecutableCompositeActivity -> {
                    predecessorHandles.then<Pair<List<Handle<SecureObject<ProcessNodeInstance<*>>>>, Handle<SecureObject<ProcessInstance>>>> { predecessors ->
                        SELECT(tbl_pi.pihandle)
                            .WHERE { tbl_pi.parentActivity eq pnihandle }
                            .mapSeq<ProcessEngineDB, Select1<Handle<SecureObject<ProcessInstance>>, CustomColumnType<Handle<SecureObject<ProcessInstance>>, Long, NumericColumnType.BIGINT_T, NumericColumn<Long, NumericColumnType.BIGINT_T>, NumberColumnConfiguration<Long, NumericColumnType.BIGINT_T>>, CustomColumnType<Handle<SecureObject<ProcessInstance>>, Long, NumericColumnType.BIGINT_T, NumericColumn<Long, NumericColumnType.BIGINT_T>, NumberColumnConfiguration<Long, NumericColumnType.BIGINT_T>>.CustomColumn>, Pair<List<Handle<SecureObject<ProcessNodeInstance<*>>>>, Handle<SecureObject<ProcessInstance>>>, Handle<SecureObject<ProcessInstance>>, CustomColumnType<Handle<SecureObject<ProcessInstance>>, Long, NumericColumnType.BIGINT_T, NumericColumn<Long, NumericColumnType.BIGINT_T>, NumberColumnConfiguration<Long, NumericColumnType.BIGINT_T>>, CustomColumnType<Handle<SecureObject<ProcessInstance>>, Long, NumericColumnType.BIGINT_T, NumericColumn<Long, NumericColumnType.BIGINT_T>, NumberColumnConfiguration<Long, NumericColumnType.BIGINT_T>>.CustomColumn> {
                                Pair(
                                    predecessors,
                                    it.singleOrNull()
                                        ?: Handle.invalid()
                                )
                            }
                    }.map { (predecessors, childInstance) ->
                        CompositeInstance.BaseBuilder(
                            node,
                            predecessors.single(),
                            processInstanceBuilder,
                            childInstance,
                            processInstanceBuilder.owner,
                            entryNo,
                            Handle(pnihandle.handleValue),
                            state
                        )

                    }
                }

                else -> predecessorHandles.map { predecessors ->
                    DefaultProcessNodeInstance.BaseBuilder(
                        node,
                        predecessors,
                        processInstanceBuilder,
                        processInstanceBuilder.owner,
                        entryNo,
                        assignedUser = assignedUser,
                        handle = if (pnihandle.handleValue < 0) Handle.invalid() else Handle(pnihandle.handleValue),
                        state = state
                    )
                }
            }
        }
    }

    override fun createFromBuilder(
        transaction: ProcessDBTransaction,
        setAccess: DbSet.DBSetAccess<ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>>,
        builder: ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>
    ): DBAction<ProcessEngineDB, SecureObject<ProcessNodeInstance<*>>> {
        return with(transaction) {
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

    override fun getPrimaryKeyCondition(where: _Where, instance: SecureObject<ProcessNodeInstance<*>>): WhereClause {
        return getHandleCondition(where, instance.withPermission().handle)
    }

    @Suppress("UNCHECKED_CAST")
    override fun asInstance(obj: Any) = obj as? DefaultProcessNodeInstance

    override fun store(update: _UpdateBuilder, value: SecureObject<ProcessNodeInstance<*>>) {
        update.run {
            value.withPermission().let { value ->
                SET(tbl_pni.nodeid, value.node.id)
                SET(tbl_pni.pihandle, value.hProcessInstance)
                SET(tbl_pni.state, value.state)
            }
        }
    }

    override fun postStore(
        transaction: ProcessDBTransaction,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        oldValue: SecureObject<ProcessNodeInstance<*>>?,
        newValue: SecureObject<ProcessNodeInstance<*>>
    ): DBAction<ProcessEngineDB, Boolean> {
        return with(transaction) {

            var deleteAction: DBAction<ProcessEngineDB, *> = oldValue?.let { oldValue ->
                DELETE_FROM(tbl_pred)
                    .WHERE { tbl_pred.pnihandle eq oldValue.withPermission().handle }
            } ?: value(Unit)

            newValue.withPermission().let { newValue ->
                var action: DBAction<ProcessEngineDB, Any?> = deleteAction
                if (newValue.predecessors.isNotEmpty()) {
                    action = action.then(
                        INSERT(tbl_pred.pnihandle, tbl_pred.predecessor)
                            .VALUES(newValue.predecessors) { pred ->
                                VALUES(handle, pred)
                            })
                }

                val isFailure =
                    newValue.state == NodeInstanceState.Failed || newValue.state == NodeInstanceState.FailRetry
                val results = newValue.results
                if (results.isNotEmpty() || (isFailure && newValue.failureCause != null)) {
                    if (results.isNotEmpty()) {
                        val insertResult = INSERT_OR_UPDATE(tbl_nd.pnihandle, tbl_nd.name, tbl_nd.data)
                            .VALUES(results) { data -> VALUES(newValue.handle, data.name, data.content.contentString) }
                        val fc = newValue.failureCause

                        if (isFailure && fc != null) {
                            insertResult.VALUES(newValue.handle, FAILURE_CAUSE, fc.message)
                        }

                        action = action.then(insertResult)
                    }
                }
                action.map { true }
            }
        }
    }

    override fun insertStatement(transaction: ProcessDBTransaction): ValuelessInsertAction<ProcessEngineDB, Insert> {
        return transaction.INSERT(tbl_pni.nodeid, tbl_pni.pihandle, tbl_pni.state)
    }

    override fun insertValues(
        transaction: ProcessDBTransaction,
        insert: InsertActionCommon<ProcessEngineDB, Insert>,
        value: SecureObject<ProcessNodeInstance<*>>
    ): InsertAction<ProcessEngineDB, Insert> {
        val v = value.withPermission()
        return insert.listVALUES(v.node.id, v.hProcessInstance, v.state)
    }

    override val keyColumn get() = tbl_pni.pnihandle

    override fun preRemove(
        transaction: ProcessDBTransaction,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>>
    ): DBAction<ProcessEngineDB, Boolean> {
        return with(transaction) {
            DELETE_FROM(tbl_pred)
                .WHERE { tbl_pred.pnihandle eq handle }
                .then(DELETE_FROM(tbl_nd)
                    .WHERE { tbl_nd.pnihandle eq handle })
                .map { true }
        }
    }

    override fun preRemove(
        transaction: ProcessDBTransaction,
        element: SecureObject<ProcessNodeInstance<*>>
    ): DBAction<ProcessEngineDB, Boolean> {
        return preRemove(transaction, element.withPermission().handle)
    }

    override fun preRemove(
        transaction: ProcessDBTransaction,
        columns: List<Column<*, *, *>>,
        values: List<Any?>
    ): DBAction<ProcessEngineDB, Boolean> {
        val handle = tbl_pni.pnihandle.value(columns, values)
        return preRemove(transaction, handle)
    }

    override fun preClear(transaction: ProcessDBTransaction): DBAction<ProcessEngineDB, Any> {
        return with(transaction) {
            DELETE_FROM(tbl_pred)
                .maybeWHERE { filter(this) }
                .then(DELETE_FROM(tbl_nd).maybeWHERE { filter(this) })
        }
    }

}
