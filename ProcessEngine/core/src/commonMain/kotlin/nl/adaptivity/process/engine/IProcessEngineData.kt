/*
 * Copyright (c) 2019.
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
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.TransactionFactory
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.impl.Logger
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.security.Principal
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class IProcessEngineData<T : ProcessTransaction> : TransactionFactory<T> {
    protected abstract val processModels: IMutableProcessModelMap<T>
    protected abstract val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>
    protected abstract val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T>

    abstract val logger: Logger

    fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
        processModels.apply {
            if (handle.isValid) invalidateCache(handle) else invalidateCache()
        }
    }

    fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance>>) {
        processInstances.apply {
            if (handle.isValid) invalidateCache(handle) else invalidateCache()
        }
    }

    fun invalidateCachePNI(handle: Handle<SecureObject<ProcessNodeInstance<*>>>) {
        processNodeInstances.apply {
            if (handle.isValid) invalidateCache(handle) else invalidateCache()
        }
    }


    inline fun <R> inReadonlyTransaction(transaction: T, body: ProcessEngineDataAccess.() -> R): R {
        return body(createReadDelegate(transaction))
    }

    open fun createReadDelegate(transaction: T): ProcessEngineDataAccess = createWriteDelegate(transaction)

    abstract fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess

    abstract fun queueTickle(instanceHandle: Handle<SecureObject<ProcessInstance>>)

    @Suppress("UNUSED_PARAMETER")
    inline fun <R> inReadonlyTransaction(
        principal: Principal,
        permissionResult: SecurityProvider.PermissionResult,
        body: ProcessEngineDataAccess.() -> R
    ): R {
        val tr = startTransaction()
        try {
            return body(createReadDelegate(tr))
        } finally {
            tr.close()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun <R> inWriteTransaction(
        principal: Principal,
        permissionResult: SecurityProvider.PermissionResult,
        body: MutableProcessEngineDataAccess.() -> R
    ): R {
        val tr = startTransaction()
        try {
            return body(createWriteDelegate(tr)).apply { tr.commit() }
        } finally {
            tr.close()
        }
    }

}


@OptIn(ExperimentalContracts::class)
inline fun <T : ProcessTransaction, R> IProcessEngineData<T>.inWriteTransaction(
    transaction: T,
    body: MutableProcessEngineDataAccess.() -> R
): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return body(createWriteDelegate(transaction))
}
