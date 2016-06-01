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

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import net.devrieze.util.StringCache
import net.devrieze.util.TransactionFactory
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.db.DBTransaction
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.processModels
import java.util.*


internal class ProcessModelMap(transactionFactory: TransactionFactory<DBTransaction>, stringCache: StringCache) : DBHandleMap<ProcessModelImpl>(
      transactionFactory, ProcessEngineDB, ProcessModelFactory(stringCache)), IProcessModelMap<DBTransaction> {

  override fun getModelWithUuid(transaction: DBTransaction, uuid: UUID): Handle<ProcessModelImpl>? {
    val candidates = ProcessEngineDB
          .SELECT(processModels.pmhandle)
          .WHERE { processModels.model LIKE "%${uuid.toString()}%" }
          .getList(transaction.connection)

    return candidates.asSequence()
          .filterNotNull()
          .map { Handles.handle<ProcessModelImpl>(it) }
          .firstOrNull {
      val candidate:ProcessModelImpl? = get(transaction, it)
      uuid == candidate?.uuid
    }
  }

}
