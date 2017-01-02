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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.dom.DOMSource;

import java.util.Map.Entry;


/**
 * Created by pdvrieze on 11/04/16.
 */
public class JAXBUnmarshallingAdapter<T extends XmlSerializable> extends JAXBAdapter {

  @NotNull private final XmlDeserializerFactory<T> mFactory;

  public JAXBUnmarshallingAdapter(@NotNull final Class<T> targetType) {
    final XmlDeserializer factoryTypeAnn = targetType.getAnnotation(XmlDeserializer.class);
    if (factoryTypeAnn == null || factoryTypeAnn.value() == null) {
      throw new IllegalArgumentException("For unmarshalling with this adapter to work, the type " + targetType.getName() + " must have the " + XmlDeserializer.class
                                                                                                                                                       .getName() + " annotation");
    }
    try {
      @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = (XmlDeserializerFactory<T>) factoryTypeAnn.value().newInstance();
      mFactory = factory;
    } catch (@NotNull InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException("The factory must have a visible no-arg constructor", e);
    }
  }

  @Override
  public T unmarshal(@NotNull final SimpleAdapter v) throws Exception {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final Document document = dbf.newDocumentBuilder().newDocument();

      final QName   outerName = v.name == null ? new QName("value") : v.name;
      final Element root;
      root = DomUtil.createElement(document, outerName);


      final nl.adaptivity.xml.SimpleNamespaceContext sourceNamespaceContext = v.getNamespaceContext();


      for (int i = ((nl.adaptivity.xml.SimpleNamespaceContext) sourceNamespaceContext).size() - 1; i >= 0; --i) {
        final String prefix    = sourceNamespaceContext.getPrefix(i);
        final String namespace = sourceNamespaceContext.getNamespaceURI(i);
        if (!(XMLConstants.NULL_NS_URI.equals(namespace) || // Not null namespace
              XMLConstants.XML_NS_PREFIX.equals(prefix) || // or xml mPrefix
              XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))) { // or xmlns mPrefix

        }

        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) { // Set the default namespace, unless it is the null namespace
          if (!XMLConstants.NULL_NS_URI.equals(namespace)) {
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", namespace);
          }
        } else if (!XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) { // Bind the mPrefix, except for xmlns itself
          root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespace);
        }
      }


      for (final Entry<QName, Object> attr : v.attributes.entrySet()) {
        DomUtil.setAttribute(root, attr.getKey(), (String) attr.getValue());
      }
      for (final Object child : v.children) {
        if (child instanceof Node) {
          root.appendChild(document.importNode((Node) child, true));
        }
      }
      final XMLInputFactory xif    = XMLInputFactory.newFactory();
      final XmlReader       reader = XmlStreaming.newReader(new DOMSource(root));
      reader.nextTag();

      return mFactory.deserialize(reader);
    } catch (@NotNull final Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
