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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.engine.processModel

import nl.adaptivity.process.engine.PETransformer
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.contentFragment
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.processModel.refName
import nl.adaptivity.process.processModel.refNode
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.siblingsToFragment
import org.w3c.dom.NodeList
import java.sql.SQLException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

@Throws(SQLException::class)
fun XmlDefineType.applyData(engineData: ProcessEngineDataAccess, node: ProcessNodeInstance<*>): ProcessData {
    val processData: ProcessData
    val refNode = refNode
    val refName = refName
    if (refNode != null && refName != null) {
        val predecessor = node.resolvePredecessor(engineData, refNode)
        val origpair = predecessor!!.getResult(engineData, refName)
        if (origpair == null) {
            // TODO on missing data do something else than an empty value
            processData = ProcessData.missingData(name)
        } else {
            try {
                val xPath = this.xPath
                if (xPath == null) {
                    processData = ProcessData(name, origpair.content)
                } else {
                    processData = ProcessData(name,
                                              DomUtil.nodeListToFragment(
                                                  xPath.evaluate(origpair.contentFragment,
                                                                 XPathConstants.NODESET) as NodeList))
                }
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            } catch (e: XmlException) {
                throw RuntimeException(e)
            }

        }
    } else {
        processData = ProcessData(name, CompactFragment(""))
    }
    val content = content
    if (content != null && content.isNotEmpty()) {
        try {
            val transformer = PETransformer.create(SimpleNamespaceContext.from(originalNSContext), processData)

            val reader = transformer.createFilter(bodyStreamReader)
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
