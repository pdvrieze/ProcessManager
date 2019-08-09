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


package nl.adaptivity.process.engine.processModel

import nl.adaptivity.process.engine.*
import nl.adaptivity.process.processModel.*
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReader
import org.w3c.dom.NodeList
import java.io.CharArrayReader
import java.sql.SQLException
import javax.xml.xpath.XPathConstants

@Throws(SQLException::class)
actual fun IXmlDefineType.applyData(engineData: ProcessEngineDataAccess, node: ProcessNodeInstance<*>): ProcessData {
    return applyDataImpl(engineData, refNode?.let { node.resolvePredecessor(engineData, it)})
}


@Throws(SQLException::class)
actual fun IXmlDefineType.applyFromProcessInstance(engineData: ProcessEngineDataAccess, processInstance: ProcessInstance): ProcessData {
    val predecessor: ProcessNodeInstance<*>? = refNode?.let { refNode -> processInstance.childNodes
        .map { it.withPermission() }
        .filter { it.node.id == refNode }
        .lastOrNull()
    }
    return applyDataImpl(engineData, predecessor)
}

@Throws(SQLException::class)
actual fun IXmlDefineType.applyFromProcessInstance(engineData: ProcessEngineDataAccess, processInstance: ProcessInstance.Builder): ProcessData {
    val predecessor: IProcessNodeInstance? = refNode?.let { refNode -> processInstance
        .allChildren { it.node.id == refNode }
        .lastOrNull()
    }
    return applyDataImpl(engineData, predecessor?.build(processInstance))
}


private fun IXmlDefineType.applyDataImpl(engineData: ProcessEngineDataAccess, predecessor: ProcessNodeInstance<*>?): ProcessData {
    val processData: ProcessData

    val refName = predecessor?.node?.effectiveRefName(refName)
    if (predecessor != null && refName != null) {
        val origpair = predecessor.getResult(engineData, refName)
        if (origpair == null) {
            // TODO on missing data do something else than an empty value
            processData = ProcessData.missingData(name)
        } else {
            val xPath = when (this) {
                is XPathHolder -> xPath
                else           -> null
            }
            processData = when (xPath) {
                null -> ProcessData(name, origpair.content)
                else -> ProcessData(
                    name,
                    DomUtil.nodeListToFragment(
                        xPath.evaluate(
                            origpair.contentFragment,
                            XPathConstants.NODESET
                                      ) as NodeList
                                              )
                                   )
            }
        }
    } else {
        processData = ProcessData(name, CompactFragment(""))
    }
    val content = content
    if (content != null && content.isNotEmpty()) {
        try {
            val transformer = PETransformer.create(SimpleNamespaceContext.from(originalNSContext), processData)
            val fragmentReader =
                when (this) {
                    is ICompactFragment -> XMLFragmentStreamReader.from(this)
                    else                -> XMLFragmentStreamReader.from(CharArrayReader(content), originalNSContext)
                }

            val reader = transformer.createFilter(fragmentReader)
            if (reader.hasNext()) reader.next() // Initialise the reader
            val transformed = reader.siblingsToFragment()
            return ProcessData(name, transformed)

        } catch (e: XmlException) {
            throw RuntimeException(e)
        }

    } else {
        return processData
    }
}

private fun ProcessNode.effectiveRefName(refName: String?): String? = when {
    ! refName.isNullOrEmpty() -> refName
    else -> results.singleOrNull()?.getName()
}
