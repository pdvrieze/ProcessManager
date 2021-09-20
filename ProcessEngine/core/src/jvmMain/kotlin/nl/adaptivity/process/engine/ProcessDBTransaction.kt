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

import io.github.pdvrieze.kotlinsql.monadic.DBTransactionContext
import net.devrieze.util.Handle
import net.devrieze.util.db.MonadicDBTransaction
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.db.ProcessEngineDB

/**
 * A process transaction that uses the database to store the data.
 */
class ProcessDBTransaction(
    dbTransactionContext: DBTransactionContext<ProcessEngineDB>,
    private val engineData: IProcessEngineData<ProcessDBTransaction>
) : MonadicDBTransaction<ProcessEngineDB>(dbTransactionContext), ProcessTransaction {
    private val pendingProcessInstances =
        mutableMapOf<Handle<SecureObject<ProcessInstance>>, ProcessInstance.ExtBuilder>()

    fun pendingProcessInstance(pihandle: Handle<SecureObject<ProcessInstance>>): ProcessInstance.ExtBuilder? {
        return pendingProcessInstances[pihandle]
    }

    override val readableEngineData: ProcessEngineDataAccess
        get() = engineData.createReadDelegate(this)
    override val writableEngineData: MutableProcessEngineDataAccess
        get() = engineData.createWriteDelegate(this)
}
