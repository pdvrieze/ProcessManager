/*
 * Copyright (c) 2017.
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
import io.github.pdvrieze.kotlinsql.ddl.columns.CustomColumnType
import io.github.pdvrieze.kotlinsql.ddl.columns.NumericColumnType
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._UpdateBuilder
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertActionCommon
import io.github.pdvrieze.kotlinsql.monadic.actions.ValuelessInsertAction
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.Handle
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import java.io.StringReader


/**
 * A factory to create process models from the database.
 */
internal class ProcessModelFactory() :
    AbstractElementFactory<XmlProcessModel.Builder, SecureObject<ExecutableProcessModel>, ProcessDBTransaction, ProcessEngineDB>() {

    override val table: Table
        get() = pm

    override val createColumns: List<Column<*, *, *>>
        get() = listOf(pm.pmhandle, pm.owner, pm.model)

    override fun createBuilder(
        transaction: ProcessDBTransaction,
        row: SelectResultSetRow<_ListSelect>
    ): DBAction<ProcessEngineDB, XmlProcessModel.Builder> {
        val owner = pm.owner.nullableValue(row)?.let(::SimplePrincipal)
        val handle = pm.pmhandle.value(row)

        val r: XmlProcessModel.Builder = pm.model.nullableValue(row)?.let {
            XmlProcessModel.Builder.deserialize(XmlStreaming.newReader(StringReader(it))).also { xmlModel ->
                xmlModel.handle = handle.handleValue
            }
        } ?: XmlProcessModel.Builder().apply {
            this.owner = owner ?: SYSTEMPRINCIPAL
            this.handle = handle.handleValue
        }

        return transaction.value(r)
    }

    override fun createFromBuilder(
        transaction: ProcessDBTransaction,
        setAccess: DbSet.DBSetAccess<XmlProcessModel.Builder>,
        builder: XmlProcessModel.Builder
    ): DBAction<ProcessEngineDB, SecureObject<ExecutableProcessModel>> {
        return transaction.value(ExecutableProcessModel(builder))
    }


    override fun getHandleCondition(
        where: _Where,
        handle: Handle<SecureObject<ExecutableProcessModel>>
    ): WhereClause {
        return where.run { pm.pmhandle eq handle }
    }

    override fun getPrimaryKeyCondition(
        where: _Where,
        instance: SecureObject<ExecutableProcessModel>
    ): WhereClause {
        return getHandleCondition(where, instance.withPermission().handle)
    }

    override fun asInstance(obj: Any) = obj as? ExecutableProcessModel

    override fun store(update: _UpdateBuilder, value: SecureObject<ExecutableProcessModel>) {
        value.withPermission().let { processModel ->
            update.SET(pm.owner, processModel.owner.name)
            update.SET(pm.model, XML.encodeToString(processModel))
        }
    }

    override val keyColumn: CustomColumnType<Handle<SecureObject<ExecutableProcessModel>>, Long, NumericColumnType.BIGINT_T, *, *>.CustomColumn
        get() = pm.pmhandle

    override fun insertStatement(transaction: ProcessDBTransaction): ValuelessInsertAction<ProcessEngineDB, Insert> {
        return with(transaction) { INSERT(pm.owner, pm.model) }
    }

    override fun insertValues(
        transaction: ProcessDBTransaction,
        insert: InsertActionCommon<ProcessEngineDB, Insert>,
        value: SecureObject<ExecutableProcessModel>
    ): InsertAction<ProcessEngineDB, Insert> {
        return value.withPermission().let { processModel ->
            insert.listVALUES(processModel.owner.name, XML.encodeToString(processModel))
        }
    }

    companion object {
        private val pm = ProcessEngineDB.processModels
    }

}
