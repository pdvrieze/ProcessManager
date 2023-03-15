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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.DBTransactionFactory
import net.devrieze.util.db.DBHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessDBTransaction
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode


class ProcessNodeInstanceMap<C : ActivityInstanceContext>(
    transactionFactory: DBTransactionFactory<ProcessDBTransaction<C>, ProcessEngineDB>,
    processEngine: ProcessEngine<ProcessDBTransaction<C>, C>
) : DBHandleMap<ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*, C>, *>,
    SecureObject<ProcessNodeInstance<*, C>>,
    ProcessDBTransaction<C>,
    ProcessEngineDB>(
    transactionFactory,
    ProcessNodeInstanceFactory(processEngine)
)
