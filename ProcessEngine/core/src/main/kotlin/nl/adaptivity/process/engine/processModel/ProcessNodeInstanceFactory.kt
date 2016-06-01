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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.StringCache
import net.devrieze.util.Transaction
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.db.OldDBTransaction
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.engine.JoinImpl
import nl.adaptivity.util.xml.CompactFragment
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.ColumnType
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.sql.*
import java.util.*

/**
 * Created by pdvrieze on 29/05/16.
 */

class ProcessNodeInstanceFactory(val processEngine:ProcessEngine<DBTransaction>): AbstractElementFactory<ProcessNodeInstance<DBTransaction>>() {

  companion object {
    val pni = ProcessEngineDB.processNodeInstances
    val pred = ProcessEngineDB.pnipredecessors
    val nd = ProcessEngineDB.nodedata
  }

  override fun getHandleCondition(where: Database._Where, handle: Handle<ProcessNodeInstance<DBTransaction>>): Database.WhereClause? {
    return where.run { pni.pnihandle eq handle.handleValue }
  }

  override val table: Table
    get() = pni

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pni.pnihandle, pni.nodeid, pni.pihandle, pni.state)

  override fun create(transaction: DBTransaction,
                      columns: List<Column<*, *, *>>,
                      values: List<Any?>): ProcessNodeInstance<DBTransaction> {
    val pnihandle = Handles.handle<ProcessNodeInstance<DBTransaction>>(pni.pnihandle.value(columns, values)!!)
    val nodeId = pni.nodeid.value(columns, values)!!
    val pihandle = Handles.handle<ProcessInstance<DBTransaction>>(pni.pihandle.value(columns, values)!!)
    val state = pni.state.value(columns, values)!!.let { IProcessNodeInstance.NodeInstanceState.valueOf(it) }

    val processInstance = processEngine.getProcessInstance(transaction, pihandle, SecurityProvider.SYSTEMPRINCIPAL)

    val node = processInstance.processModel.getNode(nodeId)

    val predecessors = ProcessEngineDB
          .SELECT(pred.predecessor)
          .WHERE { pred.pnihandle eq pnihandle.handleValue }
          .getList(transaction.connection)
          .map { it?.let { Handles.handle<ProcessNodeInstance<DBTransaction>>(it)} }
          .requireNoNulls()

    return if (node is JoinImpl) {
      JoinInstance(node, predecessors, processInstance, state)
    } else {
      ProcessNodeInstance(node, predecessors, processInstance, state)
    }.apply { handleValue = pnihandle.handleValue }
  }

  override fun postCreate(transaction: DBTransaction, element: ProcessNodeInstance<DBTransaction>) {
    for (handle in element.directPredecessors) {
      element.ensurePredecessor(handle)
    }

    val results = ProcessEngineDB
          .SELECT(nd.name, nd.data)
          .WHERE { nd.pnihandle eq element.handleValue }
          .getList(transaction.connection) { name, data ->
            if (ProcessNodeInstanceMap.FAILURE_CAUSE == name && (element.state == IProcessNodeInstance.NodeInstanceState.Failed || element.state == IProcessNodeInstance.NodeInstanceState.FailRetry)) {
              element.setFailureCause(data)
              null
            } else {
              ProcessData(name, CompactFragment(data!!))
            }
          }.filterNotNull()
    element.setResult(results)
  }

  override fun getPrimaryKeyCondition(where: Database._Where,
                             instance: ProcessNodeInstance<DBTransaction>): Database.WhereClause? {
    return getHandleCondition(where, instance.handle);
  }

  override fun asInstance(value: Any) = value as? ProcessNodeInstance<DBTransaction>

  override fun store(update: Database._UpdateBuilder, value: ProcessNodeInstance<DBTransaction>) {
    update.run {
      SET(pni.nodeid, value.node.id)
      SET(pni.pihandle, value.processInstance.handleValue)
      SET(pni.state, value.state.name)
    }
  }

  override fun postStore(connection: DBConnection,
                         handle: Handle<ProcessNodeInstance<DBTransaction>>,
                         oldValue: ProcessNodeInstance<DBTransaction>?,
                         newValue: ProcessNodeInstance<DBTransaction>) {
    if (oldValue != null) { // update
      ProcessEngineDB
            .DELETE_FROM(pred)
            .WHERE { pred.pnihandle eq handle.handleValue }
            .executeUpdate(connection)
    }

    if (newValue.directPredecessors.isNotEmpty()) {
      val insert = ProcessEngineDB
            .INSERT(pred.pnihandle, pred.predecessor)

      for (predecessor in newValue.directPredecessors) {
        insert.VALUES(newValue.handleValue, predecessor.handleValue)
      }

      insert.executeUpdate(connection)
    }

    val isFailure = newValue.state == IProcessNodeInstance.NodeInstanceState.Failed || newValue.state == IProcessNodeInstance.NodeInstanceState.FailRetry
    val results = newValue.results
    if (results.isNotEmpty() || (isFailure && newValue.failureCause!=null)) {
      val insert = ProcessEngineDB.INSERT(nd.pnihandle, nd.name, nd.data)
      for(data in results) {
        insert.VALUES(newValue.handleValue, data.name, data.content.contentString)
      }
      if (isFailure && newValue.failureCause!=null) {
        insert.VALUES(newValue.handleValue, ProcessNodeInstanceMap.FAILURE_CAUSE, newValue.failureCause.message)
      }
      insert.executeUpdate(connection)
    }

  }

  override fun insertStatement(value: ProcessNodeInstance<DBTransaction>): Database.Insert {
    return ProcessEngineDB
          .INSERT(pni.nodeid, pni.pihandle, pni.state)
          .VALUES(value.node.id, value.processInstance.handleValue, value.state.name)
  }

  override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
    get() = pni.pnihandle

  override fun preRemove(transaction: DBTransaction, handle: Handle<ProcessNodeInstance<DBTransaction>>) {

    val connection = transaction.connection
    ProcessEngineDB
          .DELETE_FROM(pred)
          .WHERE { pred.pnihandle eq handle.handleValue }
          .executeUpdate(connection)

    ProcessEngineDB
          .DELETE_FROM(nd)
          .WHERE { nd.pnihandle eq handle.handleValue }
          .executeUpdate(connection)
  }

  override fun preRemove(transaction: DBTransaction, element: ProcessNodeInstance<DBTransaction>) {
    preRemove(transaction, element.handle)
  }

  override fun preRemove(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
    val handle = pni.pnihandle.value(columns, values)!!
    preRemove(transaction, Handles.handle<ProcessNodeInstance<DBTransaction>>(handle))
  }

  @Throws(SQLException::class)
  override fun preClear(transaction: DBTransaction) {

    val connection = transaction.connection

    ProcessEngineDB
          .DELETE_FROM(pred)
          .WHERE { filter(this) }
          .executeUpdate(connection)

    ProcessEngineDB
          .DELETE_FROM(nd)
          .WHERE { filter(this) }
          .executeUpdate(connection)

  }

}