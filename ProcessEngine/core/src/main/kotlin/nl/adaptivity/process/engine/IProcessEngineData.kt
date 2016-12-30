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
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import java.security.Principal

abstract class IProcessEngineData<T:ProcessTransaction>() : TransactionFactory<T> {
  protected abstract val processModels: IMutableProcessModelMap<T>
  protected abstract val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>
  protected abstract val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, T>


  fun invalidateCachePM(handle: Handle<out SecureObject<ExecutableProcessModel>>) {
    (processModels as? CachingProcessModelMap<T>)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }

  fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance>>) {
    (processInstances as? CachingHandleMap)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }

  fun invalidateCachePNI(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>) {
    (processNodeInstances as? CachingHandleMap)?.apply {
      if (handle.valid) invalidateCache(handle) else invalidateCache()
    }
  }


  inline fun <R> inReadonlyTransaction(transaction: T, body: ProcessEngineDataAccess.() -> R): R {
    return body(createReadDelegate(transaction))
  }

  open fun createReadDelegate(transaction: T): ProcessEngineDataAccess = createWriteDelegate(transaction)

  inline fun <R> inWriteTransaction(transaction: T, body: MutableProcessEngineDataAccess.() -> R): R {
    return body(createWriteDelegate(transaction))
  }

  abstract fun  createWriteDelegate(transaction: T): MutableProcessEngineDataAccess


  inline fun <R> inReadonlyTransaction(principal: Principal, permissionResult: SecurityProvider.PermissionResult, body: ProcessEngineDataAccess.() -> R): R {
    startTransaction().use { tr ->
      return body(createReadDelegate(tr))
    }
  }

  inline fun <R> inWriteTransaction(principal: Principal,
                                      permissionResult: SecurityProvider.PermissionResult,
                                      body: MutableProcessEngineDataAccess.() -> R): R {
    startTransaction().use { tr ->
      return body(createWriteDelegate(tr)).apply { tr.commit() }
    }
  }

}

interface ProcessEngineDataAccess {
  val instances: HandleMap<SecureObject<ProcessInstance>>

  fun  instance(handle: Handle<out SecureObject<ProcessInstance>>)
        = instances[handle].mustExist(handle)

  val nodeInstances: HandleMap<SecureObject<ProcessNodeInstance>>

  fun nodeInstance(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>)
        = nodeInstances[handle].mustExist(handle)

  val processModels: IProcessModelMapAccess

  fun processModel(handle: Handle<out SecureObject<ExecutableProcessModel>>)
        = processModels[handle].mustExist(handle)
}

interface MutableProcessEngineDataAccess : ProcessEngineDataAccess {

  fun messageService(): IMessageService<*>

  override val instances: MutableHandleMap<SecureObject<ProcessInstance>>

  override val processModels: IMutableProcessModelMapAccess

  fun invalidateCachePM(handle: Handle<out SecureObject<ExecutableProcessModel>>)

  fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance>>)

  fun invalidateCachePNI(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>)

  fun commit()

  fun rollback()

  /** Handle a process instance completing. This allows the policy of deleting or not to be delegated here. */
  fun  handleFinishedInstance(handle: ComparableHandle<out SecureObject<ProcessInstance>>)
}
