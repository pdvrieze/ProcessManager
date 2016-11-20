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

import nl.adaptivity.process.StubTransaction

/**
 * Created by pdvrieze on 20/11/16.
 */
class StubProcessTransaction(private val engineData: IProcessEngineData<StubProcessTransaction>) : StubTransaction(), ProcessTransaction<StubProcessTransaction> {
  override val readableEngineData: ProcessEngineDataAccess<StubProcessTransaction>
    get() = engineData.createReadDelegate(this)
  override val writableEngineData: MutableProcessEngineDataAccess<StubProcessTransaction>
    get() = engineData.createWriteDelegate(this)
}