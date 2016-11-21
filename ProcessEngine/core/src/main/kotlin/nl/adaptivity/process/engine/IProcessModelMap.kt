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
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.processModel.engine.ProcessModelImpl

import java.sql.SQLException
import java.util.UUID


/**
 * Created by pdvrieze on 07/05/16.
 */
interface IProcessModelMap<T : ProcessTransaction<T>> : TransactionedHandleMap<SecureObject<ProcessModelImpl>, T> {

  fun getModelWithUuid(transaction: T, uuid: UUID): Handle<out SecureObject<ProcessModelImpl>>?

  override fun withTransaction(transaction: T): IProcessModelMapAccess {
    return ProcessModelMapForwarder(transaction, this as IMutableProcessModelMap<T>)
  }
}

interface IMutableProcessModelMap<T : ProcessTransaction<T>> : MutableTransactionedHandleMap<SecureObject<ProcessModelImpl>, T>, IProcessModelMap<T> {

  override fun withTransaction(transaction: T) = defaultWithTransaction(this, transaction)

}

fun <T:ProcessTransaction<T>> defaultWithTransaction(map: IMutableProcessModelMap<T>, transaction: T):IMutableProcessModelMapAccess {
  return MutableProcessModelMapForwarder(transaction, map)
}

interface IProcessModelMapAccess : HandleMap<SecureObject<ProcessModelImpl>> {
  fun getModelWithUuid(uuid: UUID): Handle<out SecureObject<ProcessModelImpl>>?

  operator fun get(uuid:UUID) = getModelWithUuid(uuid)
}

interface IMutableProcessModelMapAccess : MutableHandleMap<SecureObject<ProcessModelImpl>>, IProcessModelMapAccess

inline fun <T:ProcessTransaction<T>, R> IProcessModelMap<T>.inReadonlyTransaction(transaction: T, body: IProcessModelMapAccess.()->R):R {
  return withTransaction(transaction).body()
}

inline fun <T:ProcessTransaction<T>, R> IMutableProcessModelMap<T>.inWriteTransaction(transaction: T, body: IMutableProcessModelMapAccess.()->R):R {
  return withTransaction(transaction).body()
}

private class ProcessModelMapForwarder<T:ProcessTransaction<T>>(transaction: T, override val delegate:IProcessModelMap<T>)
  : HandleMapForwarder<SecureObject<ProcessModelImpl>, T>(transaction, delegate), IProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<out SecureObject<ProcessModelImpl>>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}

private class MutableProcessModelMapForwarder<T:ProcessTransaction<T>>(transaction: T, override val delegate:IMutableProcessModelMap<T>)
  : MutableHandleMapForwarder<SecureObject<ProcessModelImpl>, T>(transaction, delegate), IMutableProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<out SecureObject<ProcessModelImpl>>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}
