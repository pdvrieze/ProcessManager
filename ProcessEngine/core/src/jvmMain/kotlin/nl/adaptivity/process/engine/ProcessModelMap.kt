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
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.engine.db.ProcessEngineDB.processModels
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import java.util.*


internal class ProcessModelMap(transactionFactory: TransactionFactory<ProcessDBTransaction>, stringCache: StringCache = StringCache.NOPCACHE) : DBHandleMap<XmlProcessModel.Builder, SecureObject<ExecutableProcessModel>, ProcessDBTransaction>(
      transactionFactory, ProcessEngineDB, ProcessModelFactory(stringCache)), IMutableProcessModelMap<ProcessDBTransaction> {

  override fun getModelWithUuid(transaction: ProcessDBTransaction, uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>? {
    val candidates = ProcessEngineDB
          .SELECT(processModels.pmhandle)
          .WHERE { processModels.model LIKE "%$uuid%" }
          .getList(transaction.connection)

    return candidates.asSequence()
          .filterNotNull()
          .map { handle(it) }
          .firstOrNull {
      val candidate:ExecutableProcessModel? = get(transaction, it)?.withPermission()
      uuid == candidate?.uuid
    }
  }

  override val elementFactory: ProcessModelFactory
    get() = super.elementFactory as ProcessModelFactory
}
