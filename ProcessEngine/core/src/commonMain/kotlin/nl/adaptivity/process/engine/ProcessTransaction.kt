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

import net.devrieze.util.Transaction

/**
 * A transaction interface for processes. This allow access to the process data without having to pass the transaction in
 * as parameter
 */
interface ContextProcessTransaction<C: ActivityInstanceContext> : Transaction {
  val readableEngineData: ProcessEngineDataAccess<C>
  val writableEngineData: MutableProcessEngineDataAccess<C>
}

interface ProcessTransactionFactory<T: ContextProcessTransaction<C>, C: ActivityInstanceContext> {
  fun startTransaction(engineData: IProcessEngineData<T, C>): T
}
