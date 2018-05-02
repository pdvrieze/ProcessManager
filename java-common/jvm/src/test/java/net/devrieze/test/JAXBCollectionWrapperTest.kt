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

package net.devrieze.test

import net.devrieze.util.JAXBCollectionWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlValue
import javax.xml.namespace.QName

import java.io.StringWriter
import java.util.ArrayList


class JAXBCollectionWrapperTest {

    private lateinit var collection: ArrayList<Any>

    private val expectedMarshalling = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><list xmlns=\"http://example.org/test\"><xItem>x</xItem>blabla<yItem>y</yItem></list>"

    @XmlRootElement(name = "xItem", namespace = "http://example.org/test")
    internal class XItem {

        @XmlValue
        var value: String? = null

        constructor() {
            value = null
        }

        constructor(string: String) {
            value = string
        }
    }

    @XmlRootElement(name = "yItem", namespace = "http://example.org/test")
    internal class YItem {

        @XmlValue
        var value: String? = null

        constructor(string: String) {
            value = string
        }

        constructor() {
            value = null
        }
    }

    @BeforeEach
    fun setUp() {
        collection = ArrayList()
        collection.add(XItem("x"))
        collection.add("blabla")
        collection.add(YItem("y"))

    }

    @Test
    @Throws(JAXBException::class)
    fun testGetJAXBElement() {
        val context = JAXBContext.newInstance(XItem::class.java, YItem::class.java, JAXBCollectionWrapper::class.java)
        val wrapper = JAXBCollectionWrapper(collection, Any::class.java)
        val element = wrapper.getJAXBElement(QName(NS, "list"))
        val out = StringWriter()
        context.createMarshaller().marshal(element, out)
        assertEquals(expectedMarshalling, out.toString())
    }

    companion object {


        private val NS = "http://example.org/test"
    }

}

