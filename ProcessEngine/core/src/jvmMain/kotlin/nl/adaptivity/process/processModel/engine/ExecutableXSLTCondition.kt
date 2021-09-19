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

import kotlinx.serialization.Serializable
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.NodeInstanceSource
import nl.adaptivity.process.engine.impl.dom.toDocumentFragment
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.util.multiplatform.Locales
import nl.adaptivity.util.multiplatform.toLowercase
import nl.adaptivity.xmlutil.*
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 *
 * TODO: Add namespace inheritance support
 */
@Serializable(ExecutableXSLTConditionSerializer::class)
actual class ExecutableXSLTCondition actual constructor(condition: String, override val label: String?) : ExecutableCondition() {

    actual constructor(condition: Condition): this(condition.condition, condition.label)

    override val isAlternate: Boolean = condition.trim().toLowercase(Locales.ENGLISH) == "otherwise"
    actual override val condition: String = if (isAlternate) "" else condition

    /**
     * Evaluate the condition.

     * @param engineData The transaction to use for reading state
     *
     * @param nodeInstance The instance to use to evaluate against.
     *
     * @return `true` if the condition holds, `false` if not
     */
    @OptIn(XmlUtilInternal::class)
    actual override fun eval(nodeInstanceSource: NodeInstanceSource, nodeInstance: IProcessNodeInstance): ConditionResult {
        if (condition.isBlank()) return ConditionResult.TRUE

        val documentBuilder =
            DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()

        // TODO process the condition as xpath, expose the node's defines as variables
        val factory = XPathFactory.newInstance()
        val resolver = ConditionResolver(nodeInstanceSource, nodeInstance, documentBuilder.newDocument())
        factory.setXPathFunctionResolver(resolver)
        factory.setXPathVariableResolver(resolver)

        val doc = documentBuilder.newDocument()

        val xpath = factory.newXPath()

        xpath.namespaceContext = SimpleNamespaceContext(Engine.NSPREFIX, Engine.NAMESPACE)

        val expression = xpath.compile(condition)
        return (expression.evaluate(doc.createDocumentFragment(), XPathConstants.BOOLEAN) as Boolean).toResult(resolver)
    }

    override fun toString(): String {
        return "ExecutableXSLTCondition(label=$label, condition='$condition')"
    }

}

private fun Boolean.toResult(resolver: ConditionResolver) = ConditionResult(this)

private class ConditionResolver(val nodeSource: NodeInstanceSource, val nodeInstance: IProcessNodeInstance, val document: Document) :
    XPathFunctionResolver, XPathVariableResolver {
    override fun resolveVariable(variableName: QName): Any? {
        // Actually resolve variables
        return null
    }

    override fun resolveFunction(functionName: QName, arity: Int): XPathFunction? {
        if (functionName.namespaceURI!=Engine.NAMESPACE) return null
        when (functionName.localPart) {
            "node" -> when (arity) {
                1 -> return defaultNodeFunction
                2 -> return resultNodeFunction
            }
        }
        return null
    }

    private val defaultNodeFunction: XPathFunction = object : XPathFunction {
        override fun evaluate(args: List<*>): Any? {
            val pred = nodeInstance.resolvePredecessor(nodeSource, args.single().toString())
            return when {
                pred == null -> null
                pred.results.size==1 -> pred.results.single().content.toDocumentFragment()
                else -> pred.results.firstOrNull { it.name=="result" }?.content?.toDocumentFragment()
            }
        }
    }

    private val resultNodeFunction: XPathFunction = object : XPathFunction {
        override fun evaluate(args: List<*>): Any? {
            val pred = nodeInstance.resolvePredecessor(nodeSource, args[0].toString())
            val resultName = args[1].toString()
            return when {
                pred == null -> null
                else -> pred.results.firstOrNull { it.name==resultName }?.content?.toDocumentFragment()
            }
        }
    }

    companion object {
        const val DEFAULT_PREFIX="pe"
    }

}
