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
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class IProcessEngineData<T : ContextProcessTransaction<C>, C: ActivityInstanceContext> : TransactionFactory<T> {
    protected abstract val processModels: IMutableProcessModelMap<T>
    protected abstract val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance<C>>, T>
    protected abstract val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, C>>, T>

    abstract val logger: LoggerCompat

    fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
        processModels.apply {
            if (handle.isValid) invalidateCache(handle) else invalidateCache()
        }
    }

    fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance<*>>>) {
        processInstances.apply {
            if (handle.isValid) invalidateCache(handle.coerce()) else invalidateCache()
        }
    }

    fun invalidateCachePNI(handle: Handle<SecureObject<ProcessNodeInstance<*, *>>>) {
        processNodeInstances.apply {
            if (handle.isValid) invalidateCache(handle.coerce()) else invalidateCache()
        }
    }


    inline fun <R> inReadonlyTransaction(transaction: T, body: ProcessEngineDataAccess<C>.() -> R): R {
        return body(createReadDelegate(transaction))
    }

    open fun createReadDelegate(transaction: T): ProcessEngineDataAccess<C> = createWriteDelegate(transaction)

    abstract fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess<C>

    abstract fun queueTickle(instanceHandle: Handle<SecureObject<ProcessInstance<*>>>)

    @Suppress("UNUSED_PARAMETER")
    inline fun <R> inReadonlyTransaction(
        principal: PrincipalCompat,
        permissionResult: SecurityProvider.PermissionResult,
        body: ProcessEngineDataAccess<*>.() -> R
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
        principal: PrincipalCompat,
        permissionResult: SecurityProvider.PermissionResult,
        body: MutableProcessEngineDataAccess<C>.() -> R
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
inline fun <T : ContextProcessTransaction<C>, R, C: ActivityInstanceContext> IProcessEngineData<T, C>.inWriteTransaction(
    transaction: T,
    body: MutableProcessEngineDataAccess<C>.() -> R
): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return body(createWriteDelegate(transaction))
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <C : ActivityInstanceContext> Handle<SecureObject<ProcessNodeInstance<*, *>>>.coerce() =
    this as Handle<SecureObject<ProcessNodeInstance<*, C>>>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@JvmName("coerceInstance")
inline fun <C : ActivityInstanceContext> Handle<SecureObject<ProcessInstance<*>>>.coerce() =
    this as Handle<SecureObject<ProcessInstance<C>>>
