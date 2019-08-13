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


internal class ProcessInstanceMap(transactionFactory: TransactionFactory<ProcessDBTransaction>, processEngine: ProcessEngine<*,*>) :
      DBHandleMap<ProcessInstance.BaseBuilder, SecureObject<ProcessInstance>, ProcessDBTransaction>(transactionFactory, ProcessEngineDB, ProcessInstanceElementFactory(processEngine)) {

  class Cache<T: ProcessTransaction>(delegate: ProcessInstanceMap,
              cacheSize: Int) : CachingHandleMap<SecureObject<ProcessInstance>, T>(delegate as MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>, cacheSize) {
    fun  pendingValue(piHandle: ComparableHandle<SecureObject<ProcessInstance>>): ProcessInstance.BaseBuilder? {
      return (delegate as ProcessInstanceMap).pendingValue(piHandle)
    }
  }
}
