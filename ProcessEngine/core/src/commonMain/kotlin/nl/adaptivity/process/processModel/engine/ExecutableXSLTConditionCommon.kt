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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
expect class ExecutableXSLTCondition(condition: String) : ExecutableCondition, XmlSerializable {

    constructor(condition: Condition)

    override val condition: String

    override fun serialize(out: XmlWriter)

    /**
     * Evaluate the condition.
     *
     * @param engineData The transaction to use for reading state
     * @param instance The instance to use to evaluate against.
     * @return `true` if the condition holds, `false` if not
     */
    override fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult

}
