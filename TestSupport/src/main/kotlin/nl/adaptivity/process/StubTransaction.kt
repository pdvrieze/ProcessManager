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

package nl.adaptivity.process

import net.devrieze.util.Transaction

import java.sql.SQLException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine


/**
 * Created by pdvrieze on 09/12/15.
 */
open class StubTransaction : Transaction {

    private var _result: Result<Any?>? = null

    private val result: Any?
        get() {
            return requireNotNull(_result) { "Result not set" }.getOrThrow()
        }

    private val finishHandler: Continuation<Any?> = object : Continuation<Any?> {

        override val context: CoroutineContext get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any?>) {
            require(_result == null) { "The result of a transaction can only be set once" }
            _result = result
        }
    }

    override fun close() = Unit

    @Throws(SQLException::class)
    override fun commit() = Unit

    @Throws(SQLException::class)
    override fun rollback() {
        System.err.println("Rollback needed (but not supported on the stub")
    }

    @Throws(SQLException::class)
    override fun <T> commit(value: T) = value

    override fun addRollbackHandler(runnable: Runnable) = // Do nothing
        Unit

    companion object {
        fun <TR: StubTransaction, R> inTransaction(trFactory: () -> TR, action: suspend TR.()->R): R {
            val tr = trFactory()
            action.startCoroutine(tr, tr.finishHandler)
            @Suppress("UNCHECKED_CAST")
            return tr.result as R
        }
    }
}
