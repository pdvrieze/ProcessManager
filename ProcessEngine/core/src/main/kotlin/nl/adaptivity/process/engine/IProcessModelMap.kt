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

import net.devrieze.util.*
import nl.adaptivity.process.processModel.engine.ProcessModelImpl

import java.sql.SQLException
import java.util.UUID


/**
 * Created by pdvrieze on 07/05/16.
 */
interface IProcessModelMap<T : Transaction> : TransactionedHandleMap<ProcessModelImpl, T> {

  fun getModelWithUuid(transaction: T, uuid: UUID): Handle<ProcessModelImpl>?

  override fun withTransaction(transaction: T): IProcessModelMapAccess {
    return ProcessModelMapForwarder(transaction, this as IMutableProcessModelMap<T>)
  }
}

interface IMutableProcessModelMap<T : Transaction> : MutableTransactionedHandleMap<ProcessModelImpl, T>, IProcessModelMap<T> {

  override fun withTransaction(transaction: T) = defaultWithTransaction(this, transaction)

}

fun <T:Transaction> defaultWithTransaction(map: IMutableProcessModelMap<T>, transaction: T):IMutableProcessModelMapAccess {
  return MutableProcessModelMapForwarder(transaction, map)
}

interface IProcessModelMapAccess : HandleMap<ProcessModelImpl> {
  fun getModelWithUuid(uuid: UUID): Handle<ProcessModelImpl>?

  operator fun get(uuid:UUID) = getModelWithUuid(uuid)
}

interface IMutableProcessModelMapAccess : MutableHandleMap<ProcessModelImpl>, IProcessModelMapAccess

inline fun <T:Transaction, R> IProcessModelMap<T>.inTransaction(transaction: T, body: IProcessModelMapAccess.()->R):R {
  return withTransaction(transaction).body()
}

private class ProcessModelMapForwarder<T:Transaction>(transaction: T, override val delegate:IProcessModelMap<T>)
  : HandleMapForwarder<ProcessModelImpl, T>(transaction, delegate), IProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<ProcessModelImpl>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}

private class MutableProcessModelMapForwarder<T:Transaction>(transaction: T, override val delegate:IMutableProcessModelMap<T>)
  : MutableHandleMapForwarder<ProcessModelImpl, T>(transaction, delegate), IMutableProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<ProcessModelImpl>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}
