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

import kotlinx.serialization.*
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlSerializable


interface IXmlDefineType : XmlSerializable {

    val content: CharArray?

    /**
     * Gets the value of the node property.
     *
     * @return possible object is [String]
     */
    fun getRefNode(): String?

    /**
     * Sets the value of the node property.
     *
     * @param value allowed object is [String]
     */
    fun setRefNode(value: String?)

    /**
     * Gets the value of the name property.
     *
     * @return possible object is [String]
     */
    fun getRefName(): String?

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is [String]
     */
    fun setRefName(value: String?)

    /**
     * Gets the value of the paramName property.
     *
     * @return possible object is [String]
     */
    fun getName(): String

    /**
     * Sets the value of the paramName property.
     *
     * @param value allowed object is [String]
     */
    fun setName(value:String)

    /**
     * Gets the value of the path property.
     *
     * @return possible object is [String]
     */
    fun getPath(): String?

    /**
     * Sets the value of the path property.

     * @param namespaceContext
     *
     * @param value allowed object is [String]
     */
    fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

    /**
     * Get the namespace context that defines the "missing" namespaces in the content.
     * @return
     */
    val originalNSContext: Iterable<Namespace>

    companion object serializer: KSerializer<IXmlDefineType> {
        override val serialClassDesc: KSerialClassDesc
            get() = XmlResultType.serializer().serialClassDesc

        override fun load(input: KInput): IXmlDefineType {
            return XmlDefineType.serializer().load(input)
        }

        override fun save(output: KOutput, obj: IXmlDefineType) {
            XmlDefineType.serializer().save(output, XmlDefineType(obj))
        }
    }

}

var IXmlDefineType.refNode:String?
    inline get() = getRefNode()
    inline set(value) {setRefNode(value)}

var IXmlDefineType.refName:String?
    inline get() = getRefName()
    inline set(value) {setRefName(value)}

var IXmlDefineType.name:String
    inline get() = getName()
    inline set(value) { setName(value) }

val IXmlDefineType.originalNSContext: Iterable<Namespace>
    inline get() = originalNSContext ?: emptyList()


object IXmlDefineTypeListSerializer: KSerializer<List<IXmlDefineType>> {

    val delegate = XmlDefineType.list

    override val serialClassDesc: KSerialClassDesc = delegate.serialClassDesc

    override fun load(input: KInput): List<IXmlDefineType> {
        return delegate.load(input)
    }

    override fun update(input: KInput, old: List<IXmlDefineType>): List<IXmlDefineType> {
        @Suppress("UNCHECKED_CAST")
        return delegate.update(input, old as List<XmlDefineType>)
    }

    override fun save(output: KOutput, obj: List<IXmlDefineType>) {
        delegate.save(output, obj.map(::XmlDefineType))
    }
}