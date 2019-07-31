/*
 * Copyright (c) 2018.
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
import nl.adaptivity.process.processModel.engine.ConditionResult.*
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.XmlSerializable


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
abstract class ExecutableCondition : Condition, Function2<ProcessEngineDataAccess, IProcessNodeInstance, ConditionResult> {
    open val isAlternate: Boolean get()= false

    /**
     * Evaluate the condition.
     *
     * @param engineData The transaction to use for reading state
     * @param instance The instance to use to evaluate against.
     * @return `true` if the condition holds, `false` if not
     */
    abstract fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult

    final override operator fun invoke(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult =
        eval(engineData, instance)

    override val condition: String get() = "class:${this::class.name}"

    object TRUE: ExecutableCondition() {
        override fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult = ConditionResult.TRUE

        override val condition: String get() = "true()"
    }

    object FALSE: ExecutableCondition() {
        override fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult = NEVER

        override val condition: String get() = "false()"
    }

    object OTHERWISE: ExecutableCondition() {
        override val isAlternate: Boolean get() = true

        override fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult = MAYBE

        override val condition: String get() = "otherwise"
    }
}

fun Condition.toExecutableCondition(): ExecutableCondition = when (this) {
    is ExecutableCondition -> this
    else                   -> ExecutableXSLTCondition(condition)
}

enum class ConditionResult {
    /** The result is true now */
    TRUE,
    /** The result may be true in the future but is not now */
    MAYBE,
    /** The result is not going to be true. No timers. */
    NEVER
}

fun ConditionResult(boolean: Boolean): ConditionResult {
    return if (boolean) TRUE else NEVER
}
