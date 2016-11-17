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
import net.devrieze.util.Handles
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import nl.adaptivity.util.xml.CompactFragment
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.ColumnType
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table
import java.util.*


/**
 * Created by pdvrieze on 30/05/16.
 */
internal class ProcessInstanceElementFactory(private val mProcessEngine: ProcessEngine<DBTransaction>) : AbstractElementFactory<ProcessInstance<DBTransaction>>() {

  override fun getHandleCondition(where: Database._Where,
                                  handle: Handle<out ProcessInstance<DBTransaction>>): Database.WhereClause? {
    return where.run { pi.pihandle eq handle.handleValue }
  }

  override val table: Table
    get() = pi

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pi.owner, pi.pmhandle, pi.name, pi.pihandle, pi.state, pi.uuid)

  override fun create(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): ProcessInstance<DBTransaction> {
    val owner = SimplePrincipal(pi.owner.value(columns, values))
    val hProcessModel = Handles.handle<ProcessModelImpl>(pi.pmhandle.value(columns, values)!!)
    val processModel = mProcessEngine.getProcessModel(transaction, hProcessModel, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hProcessModel)
    val instancename = pi.name.value(columns, values)
    val piHandle = Handles.handle<ProcessInstance<DBTransaction>>(pi.pihandle.value(columns, values)!!)
    val state = toState(pi.state.value(columns, values))
    val uuid = toUUID(pi.uuid.value(columns, values)) ?: throw IllegalStateException("Missing UUID")

    val result = ProcessInstance(piHandle, owner, processModel, instancename, uuid, state, mProcessEngine)
    return result
  }

  private fun toUUID(string: String?): UUID? {
    if (string == null) {
      return null
    }
    return UUID.fromString(string)
  }

  override fun postCreate(transaction: DBTransaction, element: ProcessInstance<DBTransaction>) {
    run {
      val handles = ProcessEngineDB
            .SELECT(pni.pnihandle)
            .WHERE { pni.pihandle eq element.handleValue }
            .getList(transaction.connection)
            .asSequence()
            .filterNotNull()
            .map { Handles.handle<ProcessNodeInstance<DBTransaction>>(it) }
            .toList()

      element.setChildren(transaction, handles)
    }

    run {

      val inputs = ArrayList<ProcessData>()
      val outputs = ArrayList<ProcessData>()

      ProcessEngineDB
            .SELECT(id.name, id.data, id.isoutput)
            .WHERE { id.pihandle eq element.handleValue }
            .execute(transaction.connection) { name, data, isoutput ->
              val procdata = ProcessData(name, CompactFragment(data!!))
              if (isoutput ?: false) {
                outputs.add(procdata)
              } else {
                inputs.add(procdata)
              }
            }
      element.inputs = inputs
      element.setOutputs(outputs)
    }
    element.reinitialize(transaction)
  }

  override fun preRemove(transaction: DBTransaction, element: ProcessInstance<DBTransaction>) {
    preRemove(transaction, element.handle)
  }

  override fun preRemove(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
    val handle = Handles.handle<ProcessInstance<DBTransaction>>(pi.pihandle.value(columns, values)!!)
    preRemove(transaction, handle)
  }

  override fun preRemove(transaction: DBTransaction, handle: Handle<out ProcessInstance<DBTransaction>>) {
    ProcessEngineDB
          .DELETE_FROM(id)
          .WHERE { id.pihandle eq handle.handleValue }
          .executeUpdate(transaction.connection)

    val nodes = ProcessEngineDB
          .SELECT(pni.pnihandle)
          .WHERE { pni.pihandle eq handle.handleValue }
          .getList(transaction.connection)
          .asSequence()
          .filterNotNull()
          .map { Handles.handle<ProcessNodeInstance<DBTransaction>>(it) }

    for (node in nodes) { // Delete through the process engine so caches get invalidated.
      mProcessEngine.removeNodeInstance(transaction, node)
    }
  }

  override fun preClear(transaction: DBTransaction) {
    throw UnsupportedOperationException("Clearing the instance database is not supported at this point")
  }

  override fun getPrimaryKeyCondition(where: Database._Where,
                                      instance: ProcessInstance<DBTransaction>): Database.WhereClause? {
    return getHandleCondition(where, instance.handle)
  }

  override fun asInstance(obj: Any): ProcessInstance<DBTransaction>? {
    @Suppress("UNCHECKED_CAST")
    return obj as? ProcessInstance<DBTransaction>
  }

  override fun insertStatement(value: ProcessInstance<DBTransaction>): Database.Insert {
    return ProcessEngineDB
          .INSERT(pi.pmhandle, pi.name, pi.owner, pi.state, pi.uuid)
          .VALUES(value.processModel.handleValue, value.name, value.owner.name, value.state?.name, value.uuid?.toString())
  }

  override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
    get() = pi.pihandle

  override fun store(update: Database._UpdateBuilder, value: ProcessInstance<DBTransaction>) {
    update.run {
      SET(pi.pmhandle, value.processModel.handleValue)
      SET(pi.name, value.name)
      SET(pi.owner, value.owner.name)
      SET(pi.state, value.state?.name)
      SET(pi.uuid, value.uuid?.toString())
    }
  }

  companion object {
    private val pi = ProcessEngineDB.processInstances
    private val pni = ProcessEngineDB.processNodeInstances
    private val id = ProcessEngineDB.instancedata

    @JvmStatic
    private fun toState(string: String?): State? {
      if (string == null) {
        return null
      }
      return State.valueOf(string)
    }
  }

}
