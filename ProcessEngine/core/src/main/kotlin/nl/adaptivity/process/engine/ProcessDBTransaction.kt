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

import net.devrieze.util.db.DBTransaction
import uk.ac.bournemouth.kotlinsql.Database
import javax.sql.DataSource

/**
 * Created by pdvrieze on 20/11/16.
 */
class ProcessDBTransaction(dataSource: DataSource,
                           db: Database, private val engineData: IProcessEngineData<ProcessDBTransaction>)
  : DBTransaction(dataSource, db), ProcessTransaction {
  override val readableEngineData: ProcessEngineDataAccess
    get() = engineData.createReadDelegate(this)
  override val writableEngineData: MutableProcessEngineDataAccess
    get() = engineData.createWriteDelegate(this)
}