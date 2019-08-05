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

import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.impl.dom.Node
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.XmlDefineType

expect fun IXmlResultType.applyData(payload: Node?): ProcessData
expect fun XmlDefineType.applyData(engineData: ProcessEngineDataAccess, node: ProcessNodeInstance<*>): ProcessData
