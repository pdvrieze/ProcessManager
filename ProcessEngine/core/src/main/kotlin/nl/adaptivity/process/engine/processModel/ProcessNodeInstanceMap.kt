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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.*
import net.devrieze.util.db.*
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.nodedata
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.pnipredecessors
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.processNodeInstances

import java.sql.*
import java.sql.Types


class ProcessNodeInstanceMap(transactionFactory: TransactionFactory<out DBTransaction>, processEngine: ProcessEngine<DBTransaction>) :
      DBHandleMap<ProcessNodeInstance<DBTransaction>>(transactionFactory, ProcessEngineDB, ProcessNodeInstanceFactory(processEngine))
