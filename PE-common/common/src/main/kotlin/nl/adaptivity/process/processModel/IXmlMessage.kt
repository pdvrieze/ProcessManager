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

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.xml.QName


interface IXmlMessage {

    /**
     * Gets the value of the service property.
     *
     * @return possible object is [QName]
     */
    var serviceName: String?

    var serviceNS: String?

    /**
     * Sets the value of the service property.
     *
     * @param value allowed object is [QName]
     */
    var service: QName?

    /**
     * Gets the value of the endpoint property.
     *
     * @return possible object is [String]
     */
    /**
     * Sets the value of the endpoint property.
     *
     * @param value allowed object is [String]
     */
    var endpoint: String

    val endpointDescriptor: EndpointDescriptor?

    /**
     * Gets the value of the operation property.
     *
     * @return possible object is [String]
     */
    /**
     * Sets the value of the operation property.
     *
     * @param value allowed object is [String]
     */
    var operation: String

    val messageBody: ICompactFragment

    /**
     * Gets the value of the url property.
     *
     * @return possible object is [String]
     */
    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is [String]
     */
    var url: String

    /**
     * Gets the value of the method property.
     *
     * @return possible object is [String]
     */
    /**
     * Sets the value of the method property.
     *
     * @param value allowed object is [String]
     */
    var method: String

    val contentType: String

    fun setType(type: String)

    override fun toString(): String

}