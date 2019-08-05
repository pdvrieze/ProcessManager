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

package nl.adaptivity.process.engine.impl

import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter
import java.io.CharArrayWriter
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement

actual typealias TimeUnit = java.util.concurrent.TimeUnit
actual typealias Future<V> = java.util.concurrent.Future<V>
actual typealias CancellationException = java.util.concurrent.CancellationException

actual typealias Logger = java.util.logging.Logger
actual typealias Level = java.util.logging.Level
actual object LogLevel {
    actual val WARNING: Level = java.util.logging.Level.WARNING
}

actual typealias JAXBElement<T> = JAXBElement<T>
actual typealias Source = javax.xml.transform.Source
actual typealias Result = javax.xml.transform.Result

actual fun JAXBmarshal(jaxbObject: Any, xml: Result) = JAXB.marshal(jaxbObject, xml)
actual inline fun <T:Any> T.getClass(): Class<T> = this.javaClass

actual inline fun generateXmlString(generator: (XmlWriter) -> Unit): CharArray {
    val caw = CharArrayWriter()
    caw.use {
        XmlStreaming.newWriter(caw, repairNamespaces = true).use { writer ->
            generator(writer)
        }
    }
    return caw.toCharArray()
}
