/*
 * Copyright (c) 2021.
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

package nl.adaptivity.process.processModel.test

import kotlinx.serialization.serializer
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerializeXmlResultType {

    @Test
    fun testSerializeIXmlResultType() {
        if (isLegacyJs || isJsNode) return

        val data: IXmlResultType = XmlResultType("myName", "/ns1:myPath", originalNSContext = listOf(XmlEvent.NamespaceImpl("ns1", "http://example.org/ns1")))
        val expected = """<result xmlns="http://adaptivity.nl/ProcessEngine/" xmlns:ns1="http://example.org/ns1" name="myName" xpath="/ns1:myPath"/>"""
        val serialized = XML.encodeToString(IXmlResultType.serializer(), data, "")
        assertEquals(expected, serialized.replace(" />", "/>"))
    }

    @Test
    fun testSerializeXmlResultType() {
        if (isLegacyJs || isJsNode) return

        val data = XmlResultType("myName", "/ns1:myPath", originalNSContext = listOf(XmlEvent.NamespaceImpl("ns1", "http://example.org/ns1")))
        val expected = """<result xmlns="http://adaptivity.nl/ProcessEngine/" xmlns:ns1="http://example.org/ns1" name="myName" xpath="/ns1:myPath"/>"""
        val serialized = XML.encodeToString(data)
        assertEquals(expected, serialized.replace(" />", "/>"))
    }
}
