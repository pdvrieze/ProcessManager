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
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//

@XmlSchema(
    namespace = Engine.NAMESPACE,
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns={
           @XmlNs(prefix= Soap.SOAP_ENCODING_PREFIX, namespaceURI= Soap.SOAP_ENCODING_NS),
           @XmlNs(prefix=XMLConstants.DEFAULT_NS_PREFIX, namespaceURI= Engine.NAMESPACE)})

@XmlJavaTypeAdapter(
    type=java.util.UUID.class, value=nl.adaptivity.xml.UUIDAdapter.class)
package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.ProcessConsts.Soap;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;