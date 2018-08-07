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

package nl.adaptivity.process.processModel

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable
import org.w3c.dom.Node

actual interface IXmlResultType : XmlSerializable {

    actual val content: CharArray?

    /**
     * The value of the name property.
     */
    actual fun getName(): String

    actual fun setName(value: String)

    /**
     * Gets the value of the path property.
     *
     * @return possible object is [String]
     */
    actual fun getPath(): String?

    actual val bodyStreamReader: XmlReader
    /**
     * Sets the value of the path property.
     *
     * @param namespaceContext
     *
     * @param value allowed object is [String]
     */
    actual fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

    fun applyData(payload: Node?): ProcessData

    /**
     * Get the namespace context for evaluating the xpath expression.
     * @return the context
     */
    actual val originalNSContext: Iterable<Namespace>


    actual companion object serializer: KSerializer<IXmlResultType> {
        override val serialClassDesc: KSerialClassDesc
            get() = XmlResultType.serializer().serialClassDesc

        override fun load(input: KInput): IXmlResultType {
            return XmlResultType.serializer().load(input)
        }

        override fun save(output: KOutput, obj: IXmlResultType) {
            XmlResultType.serializer().save(output, XmlResultType(obj))
        }
    }

}