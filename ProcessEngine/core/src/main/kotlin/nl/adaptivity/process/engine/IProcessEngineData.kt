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
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import java.security.Principal

abstract class IProcessEngineData<T:ProcessTransaction<T>> : TransactionFactory<T> {
  protected abstract val processModels: IMutableProcessModelMap<T>
  protected abstract val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance<T>>, T>
  protected abstract val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<T>>, T>


  fun invalidateCachePM(handle: Handle<out SecureObject<ProcessModelImpl>>) {
    (processModels as? CachingProcessModelMap<T>)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }

  fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance<T>>>) {
    (processInstances as? CachingHandleMap)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }

  fun invalidateCachePNI(handle: Handle<out SecureObject<ProcessNodeInstance<T>>>) {
    (processNodeInstances as? CachingHandleMap)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }


  inline fun <R> inReadonlyTransaction(transaction: T, body: ProcessEngineDataAccess<T>.() -> R): R {
    return body(createReadDelegate(transaction))
  }

  open fun createReadDelegate(transaction: T): ProcessEngineDataAccess<T> = createWriteDelegate(transaction)

  inline fun <R> inWriteTransaction(transaction: T, body: MutableProcessEngineDataAccess<T>.() -> R): R {
    return body(createWriteDelegate(transaction))
  }

  abstract fun  createWriteDelegate(transaction: T): MutableProcessEngineDataAccess<T>


  inline fun <R> inReadonlyTransaction(principal: Principal, permissionResult: SecurityProvider.PermissionResult, body: ProcessEngineDataAccess<T>.() -> R): R {
    startTransaction().use { tr ->
      return body(createReadDelegate(tr))
    }
  }

  inline fun <R> inWriteTransaction(principal: Principal,
                                      permissionResult: SecurityProvider.PermissionResult,
                                      body: MutableProcessEngineDataAccess<T>.() -> R): R {
    startTransaction().use { tr ->
      return body(createWriteDelegate(tr)).apply { tr.commit() }
    }
  }

}

interface ProcessEngineDataAccess<T:ProcessTransaction<T>> {
  val instances: HandleMap<SecureObject<ProcessInstance<T>>>

  val nodeInstances: HandleMap<SecureObject<ProcessNodeInstance<T>>>

  val processModels: IProcessModelMapAccess
}

interface MutableProcessEngineDataAccess<T:ProcessTransaction<T>> : ProcessEngineDataAccess<T> {
  override val instances: MutableHandleMap<SecureObject<ProcessInstance<T>>>

  override val nodeInstances: MutableHandleMap<SecureObject<ProcessNodeInstance<T>>>

  override val processModels: IMutableProcessModelMapAccess

  fun invalidateCachePM(handle: Handle<out SecureObject<ProcessModelImpl>>)

  fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance<T>>>)

  fun invalidateCachePNI(handle: Handle<out SecureObject<ProcessNodeInstance<T>>>)

  fun commit()

  fun rollback()

  /** Handle a process instance completing. This allows the policy of deleting or not to be delegated here. */
  fun  handleFinishedInstance(handle: ComparableHandle<out ProcessInstance<T>>)
}
