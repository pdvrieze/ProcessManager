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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.09.24 at 08:12:58 PM CEST 
//


package org.w3.soapEnvelope

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 *
 *
 * Java class for faultcode complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="faultcode">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <element name="Value" type="{http://www.w3.org/2003/05/soap-envelope}faultcodeEnum"/>
 * <element name="Subcode" type="{http://www.w3.org/2003/05/soap-envelope}subcode" minOccurs="0"/>
 * </sequence>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 */
@Serializable
class Faultcode(
    @XmlSerialName("Value", Envelope.NAMESPACE, Envelope.PREFIX)
    @Serializable(QNameSerializer::class)
    val qNameValue: QName,
    @XmlSerialName("Subcode", Envelope.NAMESPACE, Envelope.PREFIX)
    val subcode: Subcode? = null
) {

    val value: Values?
        get() {
            val qNameValue = this.qNameValue

            return Values.values().firstOrNull { c ->
                c.qName.namespaceURI == qNameValue.namespaceURI && c.qName.localPart == qNameValue.namespaceURI
            }
        }

    enum class Values {
        VersionMismatch,
        MustUnderstand,
        DataEncodingUnknown,
        Sender,
        Receiver;

        val qName = QName(Envelope.NAMESPACE, name, Envelope.PREFIX)
    }

}
