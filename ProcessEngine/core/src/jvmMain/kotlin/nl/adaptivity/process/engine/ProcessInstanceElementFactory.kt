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

import net.devrieze.util.Handle
import net.devrieze.util.MutableHandleMap
import net.devrieze.util.db.AbstractElementFactory
import net.devrieze.util.handle
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.xmlutil.util.CompactFragment
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Table


/**
 * Factory that helps in storing and retrieving process instances from the database.
 */
internal class ProcessInstanceElementFactory(private val processEngine: ProcessEngine<*, *>) : AbstractElementFactory<ProcessInstance.BaseBuilder, SecureObject<ProcessInstance>, ProcessDBTransaction>() {

  override fun getHandleCondition(where: Database._Where,
                                  handle: Handle<SecureObject<ProcessInstance>>): Database.WhereClause? {
    return where.run { pi.pihandle eq handle }
  }

  override val table: Table
    get() = pi

  override val createColumns: List<Column<*, *, *>>
    get() = listOf(pi.owner, pi.pmhandle, pi.name, pi.pihandle, pi.state, pi.uuid, pi.parentActivity)

  override fun create(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): ProcessInstance.BaseBuilder {
    val owner = pi.owner.nullableValue(columns, values)?.let(::SimplePrincipal) ?: SYSTEMPRINCIPAL
    val hProcessModel = pi.pmhandle.value(columns, values)
    val parentActivity = pi.parentActivity.value(columns, values)
    val processModel = processEngine.getProcessModel(transaction.readableEngineData, hProcessModel, SYSTEMPRINCIPAL).mustExist(hProcessModel)
    val instancename = pi.name.nullableValue(columns, values)
    val piHandle = pi.pihandle.value(columns, values)
    val state = pi.state.nullableValue(columns, values) ?: State.NEW
    val uuid = pi.uuid.nullableValue(columns, values) ?: throw IllegalStateException("Missing UUID")

    return ProcessInstance.BaseBuilder(handle(piHandle), owner, processModel, instancename, uuid, state, handle(parentActivity))
  }

  override fun postCreate(transaction: ProcessDBTransaction, builder: ProcessInstance.BaseBuilder):ProcessInstance {
    val builderHandle = builder.handle
    ProcessEngineDB
          .SELECT(pni.pnihandle)
          .WHERE { pni.pihandle eq builderHandle }
          .getList(transaction.connection)
          .asSequence()
          .filterNotNull()
          .mapTo(builder.rememberedChildren.apply { clear() }) { transaction.readableEngineData.nodeInstance(handle(it)).withPermission() }

    run {

      val inputs = builder.inputs.apply { clear() }
      val outputs = builder.outputs.apply { clear() }

      ProcessEngineDB
            .SELECT(id.name, id.data, id.isoutput)
            .WHERE { id.pihandle eq builderHandle }
            .execute(transaction.connection) { name, data, isoutput ->
              val procdata = ProcessData(name, CompactFragment(data!!))
              if (isoutput ?: false) {
                outputs.add(procdata)
              } else {
                inputs.add(procdata)
              }
            }
    }
    return builder.build(transaction.writableEngineData)
  }

  override fun preRemove(transaction: ProcessDBTransaction, element: SecureObject<ProcessInstance>) {
    preRemove(transaction, element.withPermission().handle)
  }

  override fun preRemove(transaction: ProcessDBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
    preRemove(transaction, handle(pi.pihandle.value(columns, values)))
  }

  override fun preRemove(transaction: ProcessDBTransaction, handle: Handle<SecureObject<ProcessInstance>>) {
    ProcessEngineDB
          .DELETE_FROM(id)
          .WHERE { id.pihandle eq handle }
          .executeUpdate(transaction.connection)

    val nodes = ProcessEngineDB
          .SELECT(pni.pnihandle)
          .WHERE { pni.pihandle eq handle }
          .getList(transaction.connection)
          .asSequence()
          .filterNotNull()
          .map { handle(it) }

    transaction.readableEngineData.nodeInstances.apply {
      nodes.forEach { nodeHandle ->
        invalidateCache(nodeHandle)
        ProcessEngineDB
          .DELETE_FROM(pnipred)
          .WHERE { (pnipred.predecessor eq nodeHandle) OR (pnipred.pnihandle eq nodeHandle) }
          .executeUpdate(transaction.connection)

        ProcessEngineDB
          .DELETE_FROM(nd)
          .WHERE { nd.pnihandle eq nodeHandle }
          .executeUpdate(transaction.connection)

        ProcessEngineDB
          .DELETE_FROM(pni)
          .WHERE { pni.pnihandle eq nodeHandle }
          .executeUpdate(transaction.connection)
      }
    }


    nodes.sortedByDescending { it.handleValue }.forEach { node ->
      (transaction.writableEngineData.nodeInstances as MutableHandleMap).remove(node)
    }
  }

  override fun preClear(transaction: ProcessDBTransaction) {
    throw UnsupportedOperationException("Clearing the instance database is not supported at this point")
  }

  override fun getPrimaryKeyCondition(where: Database._Where,
                                      instance: SecureObject<ProcessInstance>): Database.WhereClause? {
    return getHandleCondition(where, instance.withPermission().handle)
  }

  override fun asInstance(obj: Any): ProcessInstance? {
    @Suppress("UNCHECKED_CAST")
    return obj as? ProcessInstance
  }

  override fun insertStatement(value: SecureObject<ProcessInstance>): Database.Insert {
    return value.withPermission().let { value -> ProcessEngineDB
          .INSERT(pi.pmhandle, pi.parentActivity, pi.name, pi.owner, pi.state, pi.uuid)
          .VALUES(value.processModel.rootModel.handle, value.parentActivity, value.name, value.owner.name,
                  value.state, value.uuid) }
  }

  override val keyColumn get() = pi.pihandle

  override fun store(update: Database._UpdateBuilder, value: SecureObject<ProcessInstance>) {
    update.run { value.withPermission().let { value ->
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

  override fun isEqualForStorage(oldValue: SecureObject<ProcessInstance>?, newValue: SecureObject<ProcessInstance>): Boolean {
    if (oldValue==null) { return false; }
    if (oldValue === newValue) { return true; }
    return isEqualForStorage(oldValue.withPermission(), newValue.withPermission())
  }

  fun isEqualForStorage(oldValue: ProcessInstance, newValue: ProcessInstance): Boolean {
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
