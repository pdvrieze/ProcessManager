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

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.process.processModel.engine.ConditionResult.TRUE
import nl.adaptivity.util.multiplatform.Locales
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.toLowercase
import nl.adaptivity.xmlutil.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
actual class ExecutableXSLTCondition actual constructor(condition: String) : ExecutableCondition(), XmlSerializable {

    actual constructor(condition: Condition): this(condition.condition)

    override val isAlternate: Boolean = condition.trim().toLowercase(Locales.ENGLISH) == "otherwise"
    actual override val condition: String = if (isAlternate) "" else condition

    @Throws(XmlException::class)
    override actual fun serialize(out: XmlWriter) {
        out.writeSimpleElement(QName(Engine.NAMESPACE, Condition.ELEMENTLOCALNAME, Engine.NSPREFIX), condition)
    }

    /**
     * Evaluate the condition.

     * @param engineData The transaction to use for reading state
     *
     * @param instance The instance to use to evaluate against.
     *
     * @return `true` if the condition holds, `false` if not
     */
    actual override fun eval(engineData: ProcessEngineDataAccess, instance: IProcessNodeInstance): ConditionResult {
        if (condition.isBlank()) return ConditionResult.TRUE
        // TODO process the condition as xpath, expose the node's defines as variables
        val factory = XPathFactory.newInstance()
        val resolver = ConditionResolver(engineData, instance)
        factory.setXPathFunctionResolver(resolver)
        factory.setXPathVariableResolver(resolver)

        val doc =
            DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()

        val xpath = factory.newXPath()
        val expression = xpath.compile(condition)
        return (expression.evaluate(doc.createDocumentFragment(), XPathConstants.BOOLEAN) as Boolean).toResult(resolver)
    }

}

private fun Boolean.toResult(resolver: ConditionResolver) = ConditionResult(this)

private class ConditionResolver(val engineData: ProcessEngineDataAccess, val instance: IProcessNodeInstance) :
    XPathFunctionResolver, XPathVariableResolver {
    override fun resolveVariable(variableName: QName): Any? {
        // Actually resolve variables
        return null
    }

    override fun resolveFunction(functionName: QName, arity: Int): XPathFunction? {
        // TODO Actually resolve functions
        return null
    }

}
