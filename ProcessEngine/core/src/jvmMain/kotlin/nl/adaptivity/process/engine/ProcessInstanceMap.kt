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
import net.devrieze.util.DBTransactionFactory
import net.devrieze.util.Handle
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.db.ProcessEngineDB


internal class ProcessInstanceMap <C: ActivityInstanceContext>(
    transactionFactory: DBTransactionFactory<ProcessDBTransaction<C>, ProcessEngineDB>,
    processEngine: ProcessEngine<ProcessDBTransaction<C>, C>
) : DBHandleMap<ProcessInstance.BaseBuilder<C>, SecureObject<ProcessInstance<C>>, ProcessDBTransaction<C>, ProcessEngineDB>(
    transactionFactory,
    ProcessInstanceElementFactory(processEngine)
) {

    class Cache<T : ContextProcessTransaction<C>, C : ActivityInstanceContext>(
        delegate: ProcessInstanceMap<C>,
        cacheSize: Int
    ) : CachingHandleMap<SecureObject<ProcessInstance<C>>, T>(
        delegate as MutableTransactionedHandleMap<SecureObject<ProcessInstance<C>>, T>,
        cacheSize
    ) {
        fun pendingValue(piHandle: Handle<SecureObject<ProcessInstance<C>>>): ProcessInstance.BaseBuilder<C>? {
            return (delegate as ProcessInstanceMap).pendingValue(piHandle)
        }
    }
}
