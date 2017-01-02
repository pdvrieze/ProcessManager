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

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.StringCache
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xml.XmlStreaming
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.ColumnType
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table
import java.io.StringReader


/**
 * A factory to create process models from the database.
 */
internal class ProcessModelFactory(val stringCache: StringCache) : AbstractElementFactory<ExecutableProcessModel.Builder, SecureObject<ExecutableProcessModel>, ProcessDBTransaction>() {

  override val table: Table
    get() = pm

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pm.pmhandle, pm.owner, pm.model)

  override fun create(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): ExecutableProcessModel.Builder {
    val owner = pm.owner.value(columns, values)?.let { SimplePrincipal(it) }
    val handle = pm.pmhandle.value(columns, values)!!
    return pm.model.value(columns, values)
          ?.let { ExecutableProcessModel.Builder.deserialize(XmlStreaming.newReader(StringReader(it)))}
       ?: ExecutableProcessModel.Builder().apply {
      owner?.let { this.owner = it }
      this.owner = owner ?: SecurityProvider.SYSTEMPRINCIPAL
      this.handle
    }
  }

  override fun postCreate(transaction: ProcessDBTransaction, builder: ExecutableProcessModel.Builder): ExecutableProcessModel {
    return builder.build()
  }

  override fun getHandleCondition(where: Database._Where, handle: Handle<out SecureObject<ExecutableProcessModel>>): Database.WhereClause? {
    return where.run { pm.pmhandle eq handle.handleValue }
  }

  override fun getPrimaryKeyCondition(where: Database._Where, instance: SecureObject<ExecutableProcessModel>): Database.WhereClause? {
    return getHandleCondition(where, instance.withPermission().getHandle())
  }

  override fun asInstance(obj: Any) = obj as? ExecutableProcessModel

  override fun store(update: Database._UpdateBuilder, value: SecureObject<ExecutableProcessModel>) {
    value.withPermission().let { processModel ->
      update.SET(pm.owner, processModel.owner.name)
      update.SET(pm.model, XmlStreaming.toString(processModel))
    }
  }

  override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
    get() = pm.pmhandle

  override fun insertStatement(value: SecureObject<ExecutableProcessModel>): Database.Insert {
    return value.withPermission().let { processModel ->
      ProcessEngineDB
            .INSERT(pm.owner, pm.model)
            .VALUES(processModel.owner.name, XmlStreaming.toString(processModel))
    }
  }

  companion object {
    private val pm = ProcessEngineDB.processModels
  }

}
