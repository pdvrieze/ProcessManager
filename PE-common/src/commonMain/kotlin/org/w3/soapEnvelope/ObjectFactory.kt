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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlSerializable


/**
 * This object contains factory methods for each Java content interface and Java
 * element interface generated in the org.w3.soapEnvelope package.
 *
 *
 * An ObjectFactory allows you to programatically construct new instances of the
 * Java representation for XML content. The Java representation of XML content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for each of these are provided in this class.
 */
class ObjectFactory {

    /**
     * Create an instance of [Reasontext]
     */
    fun createReasontext(): Reasontext {
        return Reasontext()
    }

    /**
     * Create an instance of [Faultreason]
     */
    fun createFaultreason(): Faultreason {
        return Faultreason()
    }

    /**
     * Create an instance of [Faultcode]
     */
    fun createFaultcode(): Faultcode {
        return Faultcode()
    }

    /**
     * Create an instance of [Fault]
     */
    fun createFault(): Fault {
        return Fault()
    }

    /**
     * Create an instance of [Detail]
     */
    fun createDetail(): Detail {
        return Detail()
    }

    /**
     * Create an instance of [SupportedEnvType]
     */
    fun createSupportedEnvType(): SupportedEnvType {
        return SupportedEnvType()
    }

    /**
     * Create an instance of [Body]
     */
    fun <T : XmlSerializable> createBody(): Body<T> {
        return Body()
    }

    /**
     * Create an instance of [Subcode]
     */
    fun createSubcode(): Subcode {
        return Subcode()
    }

    /**
     * Create an instance of [Header]
     */
    fun createHeader(): Header {
        return Header()
    }

    /**
     * Create an instance of [Envelope]
     */
    fun <T : XmlSerializable> createEnvelope(): Envelope<T> {
        return Envelope()
    }

    /**
     * Create an instance of [UpgradeType]
     */
    fun createUpgradeType(): UpgradeType {
        return UpgradeType()
    }

    /**
     * Create an instance of [NotUnderstoodType]
     */
    fun createNotUnderstoodType(): NotUnderstoodType {
        return NotUnderstoodType()
    }

    companion object {

        private val _Envelope_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "Envelope")

        private val _Upgrade_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "Upgrade")

        private val _Header_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "Header")

        private val _Body_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "Body")

        private val _NotUnderstood_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "NotUnderstood")

        private val _Fault_QNAME = QName("http://www.w3.org/2003/05/soap-envelope", "Fault")
    }

}
