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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.sql.SQLException

/**
 * Factory object to help with process node creation from a database.
 */

internal class ProcessNodeInstanceFactory(val processEngine:ProcessEngine<ProcessDBTransaction>): AbstractElementFactory<ProcessNodeInstance.Builder<out ExecutableProcessNode>, SecureObject<ProcessNodeInstance>, ProcessDBTransaction>() {

  companion object {
    private val tbl_pni = ProcessEngineDB.processNodeInstances
    private val tbl_pi = ProcessEngineDB.processInstances
    private val tbl_pred = ProcessEngineDB.pnipredecessors
    private val tbl_nd = ProcessEngineDB.nodedata
    const val FAILURE_CAUSE = "failureCause"
  }

  override fun getHandleCondition(where: Database._Where, handle: Handle<out SecureObject<ProcessNodeInstance>>): Database.WhereClause? {
    return where.run { tbl_pni.pnihandle eq handle }
  }

  override val table: Table
    get() = tbl_pni

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(tbl_pni.pnihandle, tbl_pni.nodeid, tbl_pni.pihandle, tbl_pni.state)

  override fun create(transaction: ProcessDBTransaction,
             columns: List<Column<*, *, *>>,
             values: List<Any?>): ProcessNodeInstance.Builder<out ExecutableProcessNode> {
    val pnihandle = Handles.handle(tbl_pni.pnihandle.value(columns, values))
    val nodeId = tbl_pni.nodeid.value(columns, values)
    val pihandle = Handles.handle(tbl_pni.pihandle.value(columns, values))
    val state = tbl_pni.state.value(columns, values)

    val processInstance = processEngine.getProcessInstance(transaction, pihandle, SecurityProvider.SYSTEMPRINCIPAL)

    val node = processInstance.processModel.requireNode(nodeId)

    val predecessors = ProcessEngineDB
          .SELECT(tbl_pred.predecessor)
          .WHERE { tbl_pred.pnihandle eq pnihandle }
          .getList(transaction.connection)
          .map { it?.let { Handles.handle(it)} }
          .requireNoNulls()

    return if (node is ExecutableActivity && node.childModel!=null) {
      val childInstance = ProcessEngineDB
        .SELECT(tbl_pi.pihandle)
        .WHERE { tbl_pi.parentActivity eq pnihandle }
        .getSingleOrNull(transaction.connection)?.let { Handles.handle<SecureObject<ProcessInstance>>(it) } ?: Handles.getInvalid()

      CompositeInstance.BaseBuilder(node, predecessors, processInstance.getHandle(), childInstance, processInstance.owner,
                                    Handles.handle(pnihandle.handleValue), state)
    } else if (node is ExecutableJoin) {
      JoinInstance.BaseBuilder(node, predecessors, processInstance.getHandle(), processInstance.owner, Handles.handle(pnihandle.handleValue), state)
    } else {
      ProcessNodeInstance.BaseBuilder<ExecutableProcessNode>(node, predecessors, processInstance.getHandle(), processInstance.owner, Handles.handle(pnihandle.handleValue), state)
    }
  }

  override fun postCreate(transaction: ProcessDBTransaction,
                          builder: ProcessNodeInstance.Builder<out ExecutableProcessNode>): ProcessNodeInstance {

    val results = ProcessEngineDB
          .SELECT(tbl_nd.name, tbl_nd.data)
          .WHERE { tbl_nd.pnihandle eq builder.handle }
          .getList(transaction.connection) { name, data ->
            if (FAILURE_CAUSE == name && (builder.state == NodeInstanceState.Failed || builder.state == NodeInstanceState.FailRetry)) {
              builder.failureCause = Exception(data)
              null
            } else {
              ProcessData(name, CompactFragment(data!!))
            }
          }.filterNotNull()
    builder.results.replaceBy(results)
    return builder.build()
  }

  override fun getPrimaryKeyCondition(where: Database._Where,
                             instance: SecureObject<ProcessNodeInstance>): Database.WhereClause? {
    return getHandleCondition(where, instance.withPermission().getHandle());
  }

  @Suppress("UNCHECKED_CAST")
  override fun asInstance(obj: Any) = obj as? ProcessNodeInstance

  override fun store(update: Database._UpdateBuilder, value: SecureObject<ProcessNodeInstance>) {
    update.run {
      value.withPermission().let { value ->
        SET(tbl_pni.nodeid, value.node.id)
        SET(tbl_pni.pihandle, value.hProcessInstance)
        SET(tbl_pni.state, value.state)
      }
    }
  }

  override fun postStore(connection: DBConnection,
                         handle: Handle<out SecureObject<ProcessNodeInstance>>,
                         oldValue: SecureObject<ProcessNodeInstance>?,
                         newValue: SecureObject<ProcessNodeInstance>) {
    if (oldValue != null) { // update
      ProcessEngineDB
            .DELETE_FROM(tbl_pred)
            .WHERE { tbl_pred.pnihandle eq handle }
            .executeUpdate(connection)
    }
    newValue.withPermission().let { newValue ->
      if (newValue.directPredecessors.isNotEmpty()) {
        val insert = ProcessEngineDB
              .INSERT(tbl_pred.pnihandle, tbl_pred.predecessor)

        for (predecessor in newValue.directPredecessors) {
          insert.VALUES(handle, predecessor)
        }

        insert.executeUpdate(connection)
      }

      val isFailure = newValue.state == NodeInstanceState.Failed || newValue.state == NodeInstanceState.FailRetry
      val results = newValue.results
      if (results.isNotEmpty() || (isFailure && newValue.failureCause != null)) {
        val insert = ProcessEngineDB.INSERT_OR_UPDATE(tbl_nd.pnihandle, tbl_nd.name, tbl_nd.data)
        for (data in results) {
          insert.VALUES(newValue.getHandle(), data.name, data.content.contentString)
        }
        if (isFailure) {
          newValue.failureCause?.let { cause ->
            insert.VALUES(newValue.getHandle(), FAILURE_CAUSE, cause.message)
          }
        }
        insert.executeUpdate(connection)
      }
    }
  }

  override fun insertStatement(value: SecureObject<ProcessNodeInstance>): Database.Insert {
    return value.withPermission().let { value ->
      ProcessEngineDB
            .INSERT(tbl_pni.nodeid, tbl_pni.pihandle, tbl_pni.state)
            .VALUES(value.node.id, value.hProcessInstance, value.state)
    }
  }

  override val keyColumn get() = tbl_pni.pnihandle

  override fun preRemove(transaction: ProcessDBTransaction, handle: Handle<out SecureObject<ProcessNodeInstance>>) {

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

  override fun preRemove(transaction: ProcessDBTransaction, element: SecureObject<ProcessNodeInstance>) {
    preRemove(transaction, element.withPermission().getHandle())
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