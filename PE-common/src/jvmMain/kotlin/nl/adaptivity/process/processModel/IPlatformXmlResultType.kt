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

package nl.adaptivity.process.processModel

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.serialutil.DelegatingSerializer
import javax.xml.xpath.XPathExpression

@Serializable(with = IPlatformXmlResultType.Serializer::class)
actual interface IPlatformXmlResultType: IXmlResultType {

    val xPath: XPathExpression?

    object Serializer : DelegatingSerializer<IPlatformXmlResultType, XmlResultType>("nl.adaptivity.process.processModel.IPlatformXmlResultType", XmlResultType.serializer()) {
        override val descriptor: SerialDescriptor =
            SerialDescriptor("nl.adaptivity.process.processModel.IPlatformXmlResultType", delegateSerializer.descriptor)

        override fun fromDelegate(delegate: XmlResultType): IPlatformXmlResultType = delegate

        override fun IPlatformXmlResultType.toDelegate(): XmlResultType {
            return this as? XmlResultType ?: XmlResultType(this)
        }
    }

}
