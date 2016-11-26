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
import net.devrieze.util.security.SecureObject
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
internal class ProcessInstanceElementFactory(private val mProcessEngine: ProcessEngine<ProcessDBTransaction>) : AbstractElementFactory<ProcessInstance.BaseBuilder<ProcessDBTransaction>, SecureObject<ProcessInstance<ProcessDBTransaction>>, ProcessDBTransaction>() {

  override fun getHandleCondition(where: Database._Where,
                                  handle: Handle<out SecureObject<ProcessInstance<ProcessDBTransaction>>>): Database.WhereClause? {
    return where.run { pi.pihandle eq handle.handleValue }
  }

  override val table: Table
    get() = pi

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pi.owner, pi.pmhandle, pi.name, pi.pihandle, pi.state, pi.uuid)

  override fun create(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): ProcessInstance.BaseBuilder<ProcessDBTransaction> {
    val owner = SimplePrincipal(pi.owner.value(columns, values))
    val hProcessModel = Handles.handle<ProcessModelImpl>(pi.pmhandle.value(columns, values)!!)
    val processModel = mProcessEngine.getProcessModel(transaction as ProcessDBTransaction, hProcessModel, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hProcessModel)
    val instancename = pi.name.value(columns, values)
    val piHandle = Handles.handle<ProcessInstance<ProcessDBTransaction>>(pi.pihandle.value(columns, values)!!)
    val state = toState(pi.state.value(columns, values))
    val uuid = toUUID(pi.uuid.value(columns, values)) ?: throw IllegalStateException("Missing UUID")

    return ProcessInstance.BaseBuilder<ProcessDBTransaction>(piHandle, owner, processModel, instancename, uuid, state)
  }

  private fun toUUID(string: String?): UUID? {
    if (string == null) {
      return null
    }
    return UUID.fromString(string)
  }

  override fun postCreate(transaction: ProcessDBTransaction, builder: ProcessInstance.BaseBuilder<ProcessDBTransaction>):ProcessInstance<ProcessDBTransaction> {
    val handleValue = builder.handle.handleValue
    ProcessEngineDB
          .SELECT(pni.pnihandle)
          .WHERE { pni.pihandle eq handleValue }
          .getList(transaction.connection)
          .asSequence()
          .filterNotNull()
          .mapTo(builder.children.apply { clear() }) {Handles.handle<ProcessNodeInstance<ProcessDBTransaction>>(it) }

    run {

      val inputs = builder.inputs.apply { clear() }
      val outputs = builder.outputs.apply { clear() }

      ProcessEngineDB
            .SELECT(id.name, id.data, id.isoutput)
            .WHERE { id.pihandle eq handleValue }
            .execute(transaction.connection) { name, data, isoutput ->
              val procdata = ProcessData(name, CompactFragment(data!!))
              if (isoutput ?: false) {
                outputs.add(procdata)
              } else {
                inputs.add(procdata)
              }
            }
    }
    return builder.build(transaction.readableEngineData)
  }

  override fun preRemove(transaction: ProcessDBTransaction, element: SecureObject<ProcessInstance<ProcessDBTransaction>>) {
    preRemove(transaction, element.withPermission().handle)
  }

  override fun preRemove(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
    preRemove(transaction, Handles.handle(pi.pihandle.value(columns, values)!!))
  }

  override fun preRemove(transaction: ProcessDBTransaction, handle: Handle<out SecureObject<ProcessInstance<ProcessDBTransaction>>>) {
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
          .map { Handles.handle<ProcessNodeInstance<ProcessDBTransaction>>(it) }

    for (node in nodes) { // Delete through the process engine so caches get invalidated.
      mProcessEngine.removeNodeInstance(transaction, node)
    }
  }

  override fun preClear(transaction: ProcessDBTransaction) {
    throw UnsupportedOperationException("Clearing the instance database is not supported at this point")
  }

  override fun getPrimaryKeyCondition(where: Database._Where,
                                      instance: SecureObject<ProcessInstance<ProcessDBTransaction>>): Database.WhereClause? {
    return getHandleCondition(where, instance.withPermission().handle)
  }

  override fun asInstance(obj: Any): ProcessInstance<ProcessDBTransaction>? {
    @Suppress("UNCHECKED_CAST")
    return obj as? ProcessInstance<ProcessDBTransaction>
  }

  override fun insertStatement(value: SecureObject<ProcessInstance<ProcessDBTransaction>>): Database.Insert {
    return value.withPermission().let { value -> ProcessEngineDB
          .INSERT(pi.pmhandle, pi.name, pi.owner, pi.state, pi.uuid)
          .VALUES(value.processModel.handleValue, value.name, value.owner.name, value.state?.name, value.uuid.toString()) }
  }

  override val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
    get() = pi.pihandle

  override fun store(update: Database._UpdateBuilder, value: SecureObject<ProcessInstance<ProcessDBTransaction>>) {
    update.run { value.withPermission().let { value ->
        SET(pi.pmhandle, value.processModel.handleValue)
        SET(pi.name, value.name)
        SET(pi.owner, value.owner.name)
        SET(pi.state, value.state?.name)
        SET(pi.uuid, value.uuid.toString())
      }
    }
  }

  companion object {
    private val pi = ProcessEngineDB.processInstances
    private val pni = ProcessEngineDB.processNodeInstances
    private val id = ProcessEngineDB.instancedata

    @JvmStatic
    private fun toState(string: String?): State {
      return State.valueOf(string?: throw NullPointerException("Missing state"))
    }
  }

}
