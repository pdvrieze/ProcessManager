/*
 * Copyright (c) 2019.
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

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.dom.Node
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.xmlutil.util.ICompactFragment

expect fun IXmlResultType.applyData(payload: ICompactFragment?): ProcessData
expect fun IXmlDefineType.applyData(engineData: ProcessEngineDataAccess,context: ActivityInstanceContext): ProcessData
expect fun IXmlDefineType.applyFromProcessInstance(engineData: ProcessEngineDataAccess, processInstance: ProcessInstance): ProcessData
expect fun IXmlDefineType.applyFromProcessInstance(engineData: ProcessEngineDataAccess, processInstance: ProcessInstance.Builder): ProcessData
