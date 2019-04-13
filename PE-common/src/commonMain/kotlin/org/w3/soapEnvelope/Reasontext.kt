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

import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 *
 *
 * Java class for reasontext complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="reasontext">
 * <simpleContent>
 * <extension base="<http://www.w3.org/2001/XMLSchema>string">
 * <attribute ref="{http://www.w3.org/XML/1998/namespace}lang use="required""/>
 * </extension>
 * </simpleContent>
 * </complexType>
 * ```
 */
class Reasontext {

    var value: String? = null

    @XmlSerialName("lang", "http://www.w3.org/XML/1998/namespace", "")
    var lang: String? = null

}