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

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import java.util.*


/**
 * Map interface that has some additional features/optimizations for process models.
 */
interface IProcessModelMap<T : ProcessTransaction> : TransactionedHandleMap<SecureObject<ExecutableProcessModel>, T> {

  fun getModelWithUuid(transaction: T, uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>?

  override fun withTransaction(transaction: T): IProcessModelMapAccess {
    return ProcessModelMapForwarder(transaction, this as IMutableProcessModelMap<T>)
  }
}

interface IMutableProcessModelMap<T : ProcessTransaction> : MutableTransactionedHandleMap<SecureObject<ExecutableProcessModel>, T>, IProcessModelMap<T> {

  override fun withTransaction(transaction: T): IMutableProcessModelMapAccess = defaultWithTransaction(this, transaction)

}

fun <T:ProcessTransaction> defaultWithTransaction(map: IMutableProcessModelMap<T>, transaction: T):IMutableProcessModelMapAccess {
  return MutableProcessModelMapForwarder(transaction, map)
}

interface IProcessModelMapAccess : HandleMap<SecureObject<ExecutableProcessModel>> {
  fun getModelWithUuid(uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>?

  operator fun get(uuid:UUID) = getModelWithUuid(uuid)
}

interface IMutableProcessModelMapAccess : MutableHandleMap<SecureObject<ExecutableProcessModel>>, IProcessModelMapAccess

inline fun <T:ProcessTransaction, R> IProcessModelMap<T>.inReadonlyTransaction(transaction: T, body: IProcessModelMapAccess.()->R):R {
  return withTransaction(transaction).body()
}

inline fun <T:ProcessTransaction, R> IMutableProcessModelMap<T>.inWriteTransaction(transaction: T, body: IMutableProcessModelMapAccess.()->R):R {
  return withTransaction(transaction).body()
}

private class ProcessModelMapForwarder<T:ProcessTransaction>(transaction: T, override val delegate:IProcessModelMap<T>)
  : HandleMapForwarder<SecureObject<ExecutableProcessModel>, T>(transaction, delegate), IProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}

private class MutableProcessModelMapForwarder<T:ProcessTransaction>(transaction: T, override val delegate:IMutableProcessModelMap<T>)
  : MutableHandleMapForwarder<SecureObject<ExecutableProcessModel>, T>(transaction, delegate), IMutableProcessModelMapAccess {

  override fun getModelWithUuid(uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>? {
    return delegate.getModelWithUuid(transaction, uuid)
  }
}
