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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import java.util.*


/**
 * Created by pdvrieze on 07/05/16.
 */
class MemProcessModelMap : MemTransactionedHandleMap<SecureObject<ExecutableProcessModel>, StubProcessTransaction>(), IMutableProcessModelMap<StubProcessTransaction> {

  override fun getModelWithUuid(transaction: StubProcessTransaction,
                                uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>? {
    for (c in this) {
      val candidate = c.withPermission()
      if (uuid == candidate.uuid) {
        return candidate.getHandle()
      }
    }
    return null
  }


  override fun withTransaction(transaction: StubProcessTransaction): IMutableProcessModelMapAccess {
    return defaultWithTransaction(this, transaction)
  }

}
