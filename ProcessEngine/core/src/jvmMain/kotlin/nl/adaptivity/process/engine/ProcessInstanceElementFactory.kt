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

package nl.adaptivity.process.engine

import io.github.pdvrieze.kotlinsql.ddl.Column
import io.github.pdvrieze.kotlinsql.ddl.Table
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._UpdateBuilder
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.actions.*
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.Handle
import net.devrieze.util.MutableHandleMap
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.xmlutil.util.CompactFragment


/**
 * Factory that helps in storing and retrieving process instances from the database.
 */
internal class ProcessInstanceElementFactory<C: ActivityInstanceContext>(private val processEngine: ProcessEngine<*, C>) :
    AbstractElementFactory<ProcessInstance.BaseBuilder<C>, SecureObject<ProcessInstance<C>>, ProcessDBTransaction<C>, ProcessEngineDB>() {

    override fun getHandleCondition(where: _Where, handle: Handle<SecureObject<ProcessInstance<C>>>): WhereClause {
        return where.run { pi.pihandle eq handle }
    }

    override val table: Table
        get() = pi

    override val createColumns: List<Column<*, *, *>>
        get() = listOf(pi.owner, pi.pmhandle, pi.name, pi.pihandle, pi.state, pi.uuid, pi.parentActivity)

    override fun createBuilder(
        transaction: ProcessDBTransaction<C>,
        row: SelectResultSetRow<_ListSelect>
    ): DBAction<ProcessEngineDB, ProcessInstance.BaseBuilder<C>> {
        val owner = pi.owner.nullableValue(row)?.let(::SimplePrincipal) ?: SYSTEMPRINCIPAL
        val hProcessModel = pi.pmhandle.value(row)
        val parentActivity = pi.parentActivity.value(row)
        val processModel = processEngine.getProcessModel(transaction.readableEngineData, hProcessModel, SYSTEMPRINCIPAL)
            .mustExist(hProcessModel)
        val instancename = pi.name.nullableValue(row)
        val piHandle = pi.pihandle.value(row).coerce<C>()
        val state = pi.state.nullableValue(row) ?: State.NEW
        val uuid = pi.uuid.nullableValue(row) ?: throw IllegalStateException("Missing UUID")

        return transaction.value(
            ProcessInstance.BaseBuilder<C>(
                piHandle,
                owner,
                processModel,
                instancename,
                uuid,
                state,
                parentActivity
            )
        )
    }

    override fun createFromBuilder(
        transaction: ProcessDBTransaction<C>,
        setAccess: DbSet.DBSetAccess<ProcessInstance.BaseBuilder<C>>,
        builder: ProcessInstance.BaseBuilder<C>
    ): DBAction<ProcessEngineDB, SecureObject<ProcessInstance<C>>> {
        val builderHandle = builder.handle

        return with(transaction) {
            SELECT(pni.pnihandle)
                .WHERE { pni.pihandle eq builderHandle }
                .mapSeq { seq ->
                    seq.filterNotNull()
                        .mapTo(builder.rememberedChildren.apply { clear() }) {
                            transaction.readableEngineData.nodeInstance(it).withPermission()
                        }
                }.then {
                    val inputs = builder.inputs.apply { clear() }
                    val outputs = builder.outputs.apply { clear() }

                    SELECT(id.name, id.data, id.isoutput)
                        .WHERE { id.pihandle eq builderHandle }
                        .map { wrapper ->
                            while (wrapper.next()) {
                                val (name, data, isOutput) = wrapper.rowData
                                val procdata = ProcessData(name, CompactFragment(data!!))
                                if (isOutput ?: false) {
                                    outputs.add(procdata)
                                } else {
                                    inputs.add(procdata)
                                }
                            }
                            builder.build(transaction.writableEngineData)
                        }
                }

        }
    }

    override fun preRemove(
        transaction: ProcessDBTransaction<C>,
        element: SecureObject<ProcessInstance<C>>
    ): DBAction<ProcessEngineDB, Boolean> {
        return preRemove(transaction, element.withPermission().handle)
    }

    override fun preRemove(
        transaction: ProcessDBTransaction<C>,
        columns: List<Column<*, *, *>>,
        values: List<Any?>
    ): DBAction<ProcessEngineDB, Boolean> {
        return preRemove(transaction, pi.pihandle.value(columns, values).coerce())
    }

    override fun preRemove(
        transaction: ProcessDBTransaction<C>,
        handle: Handle<SecureObject<ProcessInstance<C>>>
    ): DBAction<ProcessEngineDB, Boolean> {
        return with(transaction) {
            DELETE_FROM(id)
                .WHERE { id.pihandle eq handle }
                .then(
                    SELECT(pni.pnihandle)
                        .WHERE { pni.pihandle eq handle }
                        .mapSeq { it.filterNotNull() }
                        .flatMap { nodes ->
                            val newActions = mutableListOf<DBAction<ProcessEngineDB, *>>()
                            for (nodeHandle in nodes) {
                                readableEngineData.nodeInstances.invalidateCache(nodeHandle.coerce())
                                newActions.add(DELETE_FROM(pnipred)
                                                   .WHERE { (pnipred.predecessor eq nodeHandle) OR (pnipred.pnihandle eq nodeHandle) })

                                newActions.add(DELETE_FROM(nd).WHERE { nd.pnihandle eq nodeHandle })

                                newActions.add(DELETE_FROM(pni).WHERE { pni.pnihandle eq nodeHandle })
                            }

                            newActions.add(value {
                                nodes.sortedByDescending { it.handleValue }.forEach { nodeHandle ->
                                    (transaction.writableEngineData.nodeInstances as MutableHandleMap).remove(nodeHandle.coerce())
                                }
                            })


                            newActions
                        }
                ).map { it.isNotEmpty() }
        }
    }

    override fun preClear(transaction: ProcessDBTransaction<C>): DBAction<ProcessEngineDB, Any> {
        throw UnsupportedOperationException("Clearing the instance database is not supported at this point")
    }

    override fun getPrimaryKeyCondition(where: _Where, instance: SecureObject<ProcessInstance<C>>): WhereClause {
        return getHandleCondition(where, instance.withPermission().handle)
    }

    override fun asInstance(obj: Any): ProcessInstance<C>? {
        @Suppress("UNCHECKED_CAST")
        return obj as? ProcessInstance<C>
    }

    override fun insertStatement(transaction: ProcessDBTransaction<C>): ValuelessInsertAction<ProcessEngineDB, Insert> {
        return transaction.INSERT(pi.pmhandle, pi.parentActivity, pi.name, pi.owner, pi.state, pi.uuid)
    }

    override fun insertValues(
        transaction: ProcessDBTransaction<C>,
        insert: InsertActionCommon<ProcessEngineDB, Insert>,
        value: SecureObject<ProcessInstance<C>>
    ): InsertAction<ProcessEngineDB, Insert> {
        return value.withPermission().let { value ->
            insert.listVALUES(value.processModel.rootModel.handle, value.parentActivity, value.name, value.owner.name, value.state, value.uuid)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override val keyColumn : Column<Handle<SecureObject<ProcessInstance<C>>>, *, *>
        get() = pi.pihandle as Column<Handle<SecureObject<ProcessInstance<C>>>, *, *>

    override fun store(update: _UpdateBuilder, value: SecureObject<ProcessInstance<C>>) {
        update.run {
            value.withPermission().let { value ->
                SET(pi.pmhandle, value.processModel.rootModel.handle)
                SET(pi.parentActivity, value.parentActivity)
                SET(pi.name, value.name)
                SET(pi.owner, value.owner.name)
                SET(pi.state, value.state)
                SET(pi.uuid, value.uuid)
            }
        }
        // TODO Store inputs and outputs in postStore
    }

    override fun isEqualForStorage(
        oldValue: SecureObject<ProcessInstance<C>>?,
        newValue: SecureObject<ProcessInstance<C>>
    ): Boolean {
        if (oldValue == null) {
            return false; }
        if (oldValue === newValue) {
            return true; }
        val actualOldValue: ProcessInstance<*> = oldValue.withPermission()
        return isEqualForStorage(actualOldValue, newValue.withPermission())
    }

    fun isEqualForStorage(oldValue: ProcessInstance<*>, newValue: ProcessInstance<*>): Boolean {
        return oldValue.uuid == newValue.uuid &&
            oldValue.handle == newValue.handle &&
            oldValue.state == newValue.state &&
            oldValue.name == newValue.name &&
            oldValue.owner == newValue.owner
    }

    companion object {
        private val pi = ProcessEngineDB.processInstances
        private val pni = ProcessEngineDB.processNodeInstances
        private val pnipred = ProcessEngineDB.pnipredecessors
        private val id = ProcessEngineDB.instancedata
        private val nd = ProcessEngineDB.nodedata
    }

}
