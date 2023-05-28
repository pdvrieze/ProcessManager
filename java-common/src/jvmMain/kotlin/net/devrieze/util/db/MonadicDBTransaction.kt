/*
 * Copyright (c) 2021.
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

package net.devrieze.util.db

import io.github.pdvrieze.kotlinsql.UnmanagedSql
import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.monadic.*
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import net.devrieze.util.Transaction
import nl.adaptivity.util.multiplatform.Runnable
import java.sql.SQLWarning
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

open class MonadicDBTransaction<DB: Database>(
    delegateSource: () -> DBTransactionContext<DB>
): DBTransactionContext<DB>, Transaction {

    constructor(delegate: DBTransactionContext<DB>): this({ delegate })

    @UnmanagedSql
    constructor(connection: MonadicDBConnection<DB>): this({ TransactionBuilder(connection) })

    private val finishHandler: Continuation<Any?> = object : Continuation<Any?> {
        override val context: CoroutineContext get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any?>) {
            check(_result == null) { "Result already set" }
            if (result.isFailure) rollback()
            _result = result
        }
    }
    private var _result: Result<Any?>? = null
    private val result: Any?
        get() {
            return checkNotNull(_result) { "Result not set" }.getOrThrow()
        }

    private val delegate: DBTransactionContext<DB> by lazy(delegateSource)

    override val connectionMetadata: MonadicMetadata<DB> get() = delegate.connectionMetadata
    override val db: DB get() = delegate.db

    override fun getConnectionWarnings(): List<SQLWarning> {
        return delegate.getConnectionWarnings()
    }

    override fun <O> DBAction<DB, O>.commit(): O {
        return with(delegate) { this@commit.commit() }
    }

    override fun <O> DBAction<DB, O>.evaluateNow(): O {
        return with(delegate) { evaluateNow() }
    }

    override fun close() {
        // Does do anything
    }

    override fun commit() {
        delegate.commit()
    }

    override fun rollback() {
        delegate.rollback()
    }

    override fun <T> commit(value: T): T {
        return value.also { commit() }
    }

    override fun addRollbackHandler(runnable: Runnable) {
        delegate.addRollbackHandler{ runnable.run() }
    }

    override fun addRollbackHandler(onRollback: DBConnectionContext<DB>.() -> Unit) {
        delegate.addRollbackHandler(onRollback)
    }

    companion object {
        fun <TR: MonadicDBTransaction<*>, R> inTransaction(trFactory: () -> TR, action: suspend TR.() -> R): R {
            val tr = trFactory()
            action.startCoroutine(tr, tr.finishHandler)
            return tr.result as R
        }
    }

}
