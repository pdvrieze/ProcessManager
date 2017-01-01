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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.process.processModel.engine.ConditionResult.NEVER
import nl.adaptivity.process.processModel.engine.ConditionResult.TRUE
import nl.adaptivity.xml.*
import java.util.*
import javax.xml.namespace.QName
import javax.xml.xpath.*


/**
 * Class encapsulating a condition.

 * @author Paul de Vrieze
 */
class ExecutableCondition(condition: String) : XmlSerializable, Condition {
  val isAlternate: Boolean = condition.trim().toLowerCase(Locale.ENGLISH)=="otherwise"
  private val condition: String = if(isAlternate) "" else condition

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.writeSimpleElement(QName(Engine.NAMESPACE, Condition.ELEMENTLOCALNAME, Engine.NSPREFIX), condition)
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.engine.Condition#getCondition()
   */
  override fun getCondition(): String {
    return condition
  }

  /**
   * Evaluate the condition.

   * @param transaction The transaction to use for reading state
   * *
   * @param instance The instance to use to evaluate against.
   * *
   * @return `true` if the condition holds, `false` if not
   */
  fun eval(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance): ConditionResult {
    // TODO process the condition as xpath, expose the node's defines as variables
    val factory = XPathFactory.newInstance()
    val resolver = ConditionResolver(engineData, instance)
    factory.setXPathFunctionResolver(resolver)
    factory.setXPathVariableResolver(resolver)

    val xpath = factory.newXPath()
    val expression = xpath.compile(condition)
    return (expression.evaluate(null, XPathConstants.BOOLEAN) as Boolean).toResult(resolver)
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): ExecutableCondition {
      val condition = reader.readSimpleElement()
      return ExecutableCondition(condition.toString())
    }
  }

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

private fun Boolean.toResult(resolver: ConditionResolver) = ConditionResult(this)

class ConditionResolver(val engineData: ProcessEngineDataAccess, val instance: ProcessNodeInstance) : XPathFunctionResolver, XPathVariableResolver {
  override fun resolveVariable(variableName: QName): Any? {
    // Actually resolve variables
    return null
  }

  override fun resolveFunction(functionName: QName, arity: Int): XPathFunction? {
    // TODO Actually resolve functions
    return null
  }

}