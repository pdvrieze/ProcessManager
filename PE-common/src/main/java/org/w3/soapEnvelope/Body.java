/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.09.24 at 08:12:58 PM CEST
//


package org.w3.soapEnvelope;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.namespace.QName;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * <p>
 * Java class for Body complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="Body">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class Body<T extends XmlSerializable> implements XmlSerializable {

  public static final String ELEMENTLOCALNAME = "Body";
  public static final QName ELEMENTNAME = new QName(Envelope.NAMESPACE, ELEMENTLOCALNAME, Envelope.PREFIX);

  @XmlAnyAttribute
  private final Map<QName, String> otherAttributes = new HashMap<>();
  private T mContent;

  public static <T extends XmlSerializable> Body<T> deserialize(final XmlReader in, final XmlDeserializerFactory<T> bodyFactory) throws XmlException {
    final Body<T> result = new Body<T>();
    XmlUtil.skipPreamble(in);
    assert XmlUtil.isElement(in, result.getElementName()): "Expected " + result.getElementName() + " but found " + in.getLocalName();
    for(int i = in.getAttributeCount() - 1; i >= 0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    result.deserializeChildren(in, bodyFactory);
    if (XmlUtil.class.desiredAssertionStatus()) {
      in.require(XmlStreaming.END_ELEMENT, result.getElementName().getNamespaceURI(), result.getElementName().getLocalPart());
    }
    return result;
  }

  public void deserializeChildren(final XmlReader in, XmlDeserializerFactory<T> bodyFactory) throws XmlException {
    if( in.next() != EventType.END_ELEMENT) { // first child
      if (in.hasNext()) mContent = bodyFactory.deserialize(in);
    }
  }

  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    QName qname = new QName(StringUtil.toString(attributeNamespace), StringUtil.toString(attributeLocalName));
    otherAttributes.put(qname, StringUtil.toString(attributeValue));
    return true;
  }

  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, getElementName());
    for(Entry<QName, String> attr:otherAttributes.entrySet()) {
      XmlUtil.writeAttribute(out, attr.getKey(), attr.getValue());
    }
    if (mContent!=null) {
      mContent.serialize(out);
    }
    XmlUtil.writeEndElement(out, getElementName());
  }

  /**
   * Gets the value of the any property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the any property.
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getAny().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link Object }
   * {@link Element }
   */
  public T getBodyContent() {
    return mContent;
  }

  /**
   * Gets a map that contains attributes that aren't bound to any typed property
   * on this class.
   * <p>
   * the map is keyed by the name of the attribute and the value is the string
   * value of the attribute. the map returned by this method is live, and you
   * can add new attribute by updating the map directly. Because of this design,
   * there's no setter.
   *
   * @return always non-null
   */
  @NotNull
  public Map<QName, String> getOtherAttributes() {
    return otherAttributes;
  }

}