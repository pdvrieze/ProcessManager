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

import nl.adaptivity.xmlutil.*


expect abstract class XPathHolder : XMLContainer {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    var _name: String?

    constructor()

    constructor(name: String?,
                path: String?,
                content: CharArray?,
                originalNSContext: Iterable<Namespace>)

    fun getName(): String

    fun setName(value: String)

    fun getPath(): String?

    @Deprecated("This should be immutable")
    fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

    open fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean

    override fun deserializeChildren(reader: XmlReader)

    override fun serializeAttributes(out: XmlWriter)

    override fun visitNamespaces(baseContext: NamespaceContext)

    override fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                            owner: QName,
                                            attributeName: QName,
                                            attributeValue: CharSequence)
}



