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

import io.github.pdvrieze.kotlinsql.monadic.actions.mapSeq
import net.devrieze.util.*
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.engine.db.ProcessEngineDB.processModels
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.PMHandle
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.util.multiplatform.UUID


internal class ProcessModelMap(
    transactionFactory: DBTransactionFactory<ProcessDBTransaction, ProcessEngineDB>
) : DBHandleMap<XmlProcessModel.Builder, SecureObject<ExecutableProcessModel>, ProcessDBTransaction, ProcessEngineDB>(
    transactionFactory,
    ProcessModelFactory()
), IMutableProcessModelMap<ProcessDBTransaction> {

    override fun getModelWithUuid(
        transaction: ProcessDBTransaction,
        uuid: UUID
    ): PMHandle? {
        return with(transaction) {
            SELECT(processModels.pmhandle)
                .WHERE { processModels.model LIKE "%$uuid%" }
                .mapSeq {
                    it.filterNotNull()
                        .firstOrNull { handle ->
                            val candidate: ExecutableProcessModel? = get(transaction, handle)?.withPermission()
                            uuid == candidate?.uuid
                        }
                }.evaluateNow()
        }
    }

    override val elementFactory: ProcessModelFactory
        get() = super.elementFactory as ProcessModelFactory
}
