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

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.writeSimpleElement


/**
 * Created by pdvrieze on 10/12/15.
 */
abstract class XmlHandle<T>(handleValue: Long) : Handle<T>, XmlSerializable, SimpleXmlDeserializable {

    override final var handleValue = handleValue
        private set

    constructor(handle: Handle<T>): this(handle.handleValue)

    override fun deserializeChild(reader: XmlReader): Boolean {
        return false
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
        handleValue = elementText.toString().toLong()
        return true
    }

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean {
        return false
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
        // ignore
    }

    override fun serialize(out: XmlWriter) {
        out.writeSimpleElement(elementName, handleValue.toString())
    }

    fun setHandle(handle: Long) {
        handleValue = handle
    }

    override fun toString(): String {
        return "{$handleValue}"
    }
}
