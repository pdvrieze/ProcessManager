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

import net.devrieze.util.CachingHandleMap
import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import java.sql.SQLException
import java.util.*


/**
 * Extension to cachingHandleMap that handles the uuids needed for process models.
 * Created by pdvrieze on 20/05/16.
 */
class CachingProcessModelMap<T : ContextProcessTransaction>(base: IMutableProcessModelMap<T>, cacheSize: Int) : CachingHandleMap<SecureObject<ExecutableProcessModel>, T>(
      base,
      cacheSize), IMutableProcessModelMap<T> {

  override val delegate: IMutableProcessModelMap<T>
    get() = super.delegate as IMutableProcessModelMap<T>

  @Throws(SQLException::class)
  override fun getModelWithUuid(transaction: T, uuid: UUID): Handle<SecureObject<ExecutableProcessModel>>? {
    return delegate.inReadonlyTransaction(transaction) { getModelWithUuid(uuid) }
  }
}
