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

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.StringCache
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import nl.adaptivity.process.processModel.engine.ProcessModelImpl.Factory
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
internal class ProcessModelFactory(val stringCache: StringCache) : AbstractElementFactory<ProcessModelImpl, SecureObject<ProcessModelImpl>, ProcessDBTransaction>() {
  private var mColNoOwner: Int = 0
  private var mColNoModel: Int = 0
  private var mColNoHandle: Int = 0

  override val table: Table
    get() = pm

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pm.pmhandle, pm.owner, pm.model)

  override fun create(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): ProcessModelImpl {
    val owner = pm.owner.value(columns, values)?.let { SimplePrincipal(it) }
    val handle = pm.pmhandle.value(columns, values)!!
    return pm.model.value(columns, values)
          ?.let { ProcessModelImpl.deserialize(Factory(), XmlStreaming.newReader(StringReader(it)))}
          ?.apply {
      handleValue = handle
      cacheStrings(stringCache)
      if (this.owner==SecurityProvider.SYSTEMPRINCIPAL) { this.setOwner(owner) }

    } ?: ProcessModelImpl(emptyList()).apply {
      this.setOwner(owner)
      handleValue = handle
    }
  }

  override fun postCreate(transaction: ProcessDBTransaction, builder: ProcessModelImpl): ProcessModelImpl {
    return builder
  }

  override fun getHandleCondition(where: Database._Where, handle: Handle<out SecureObject<ProcessModelImpl>>): Database.WhereClause? {
    return where.run { pm.pmhandle eq handle.handleValue }
  }

  override fun getPrimaryKeyCondition(where: Database._Where, instance: SecureObject<ProcessModelImpl>): Database.WhereClause? {
    return getHandleCondition(where, instance.withPermission().handle)
  }

  override fun asInstance(obj: Any) = obj as? ProcessModelImpl

  override fun store(update: Database._UpdateBuilder, value: SecureObject<ProcessModelImpl>) {
    value.withPermission().let { processModel ->
      update.SET(pm.owner, processModel.owner.name)
      update.SET(pm.model, XmlStreaming.toString(processModel))
    }
  }

  override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
    get() = pm.pmhandle

  override fun insertStatement(value: SecureObject<ProcessModelImpl>): Database.Insert {
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
