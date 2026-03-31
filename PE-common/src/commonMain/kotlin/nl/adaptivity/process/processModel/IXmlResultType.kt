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

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

@Serializable(with = IXmlResultType.Serializer::class)
interface IXmlResultType {

    val content: CompactFragment

    val name: String

    val path: String?

    /**
     * A reader for the underlying body stream.
     */
    val bodyStreamReader: XmlReader

    /**
     * Get the namespace context for evaluating the xpath expression.
     * @return the context
     */
    val originalNSContext: Iterable<Namespace> get() = content.namespaces

    fun copy(name: String = this.name, path: String? = this.path, content: ICompactFragment = this.content): IXmlResultType

    fun copy(name: String = this.name, path: String? = this.path, content: CharArray? = this.content.content, originalNSContext: Iterable<Namespace> = this.content?.namespaces ?: emptyList()): IXmlResultType

    private class Serializer : DelegatingSerializer<IXmlResultType, XmlResultType>("nl.adaptivity.process.processModel.IXmlResultType", XmlResultType.serializer()) {

        override fun fromDelegate(delegate: XmlResultType): IXmlResultType = delegate

        override fun IXmlResultType.toDelegate(): XmlResultType {
            return this as? XmlResultType ?: XmlResultType(this)
        }
    }

}
