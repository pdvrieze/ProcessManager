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

@file:JvmName("ExecutableConditionKt")
@file:JvmMultifileClass
package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.process.processModel.engine.ConditionResult.NEVER
import nl.adaptivity.process.processModel.engine.ConditionResult.TRUE
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlWriter
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
expect class ExecutableCondition(condition: String) : XmlSerializable, Condition {
  val isAlternate: Boolean
  override val condition: String

  override fun serialize(out: XmlWriter)

  /**
   * Evaluate the condition.
   *
   * @param engineData The transaction to use for reading state
   * @param instance The instance to use to evaluate against.
   * @return `true` if the condition holds, `false` if not
   */
  fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult

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
  return if(boolean) TRUE else NEVER
}
