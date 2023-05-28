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
import net.devrieze.util.TransactionFactory
import kotlin.coroutines.startCoroutine


/**
 * Created by pdvrieze on 09/12/15.
 */
class StubTransactionFactory : TransactionFactory<StubTransaction> {

    private val transaction = StubTransaction()

    override fun startTransaction() = transaction

    override fun <R> inTransaction(action: suspend StubTransaction.() -> R): R {
        return StubTransaction.inTransaction({ transaction }, action)
    }

    override fun isValidTransaction(transaction: Transaction): Boolean {
        return this.transaction === transaction
    }
}
