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

package net.devrieze.util

import java.sql.SQLException


/**
 * Created by pdvrieze on 19/05/16.
 */
abstract class AbstractTransactionedHandleMap<V, T : Transaction> : MutableTransactionedHandleMap<V, T> {

  @Throws(SQLException::class)
  override fun castOrGet(transaction: T, handle: Handle<out V>): V? {
    return get(transaction, handle)
  }

  override fun containsAll(transaction: T, c: Collection<*>): Boolean {
    for (o in c) {
      if (o==null) return false
      if (!contains(transaction, o)) {
        return false
      }
    }
    return true
  }

}
