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

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.xml.XmlStreaming;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;


/**
 * Created by pdvrieze on 11/04/16.
 */
public class JAXBAdapter extends XmlAdapter<SimpleAdapter, XmlSerializable> {

  @Override
  public XmlSerializable unmarshal(final SimpleAdapter v) throws Exception {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public SimpleAdapter marshal(@NotNull final XmlSerializable v) throws Exception {


    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document         document = dbf.newDocumentBuilder().newDocument();
    final DocumentFragment content  = document.createDocumentFragment();
    final XMLOutputFactory xof      = XMLOutputFactory.newFactory();
    final XMLStreamWriter  out      = xof.createXMLStreamWriter(new DOMResult(content));

    v.serialize(XmlStreaming.newWriter(new DOMResult(content)));
    final int childCount = content.getChildNodes().getLength();
    if (childCount == 0) {
      return new SimpleAdapter();
    } else if (childCount == 1) {
      final SimpleAdapter result = new SimpleAdapter();
      final Node          child  = content.getFirstChild();
      if (child instanceof Element) {
        result.setAttributes(child.getAttributes());
        for (Node child2 = child.getFirstChild(); child2 != null; child2 = child2.getNextSibling()) {
          result.children.add(child2);
        }
      } else {
        result.children.add(child);
      }
      return result;
    } else { // More than one child
      final SimpleAdapter result = new SimpleAdapter();
      for (Node child = content.getFirstChild(); child != null; child = child.getNextSibling()) {
        result.children.add(child);
      }
      return result;
    }
  }
}
