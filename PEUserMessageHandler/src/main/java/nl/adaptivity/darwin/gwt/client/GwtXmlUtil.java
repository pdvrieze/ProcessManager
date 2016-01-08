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

package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;


public final class GwtXmlUtil {
  private GwtXmlUtil() {}

  public static SafeHtml getTextContent(Element element) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    getTextContent(builder, element);
    return builder.toSafeHtml();
  }

  private static void getTextContent(SafeHtmlBuilder builder, Element element) {
    for(Node n=element.getFirstChild(); n!=null; n=n.getNextSibling()) {
      switch (n.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
        case Node.TEXT_NODE:
          builder.appendEscaped(n.getNodeValue());
          break;
        case Node.ELEMENT_NODE:
          getTextContent(builder, (Element)n);
      }
    }
  }

  public static SafeHtml serialize(NodeList childNodes) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    for(int i=0; i<childNodes.getLength(); ++i) {
      Node child = childNodes.item(i);
      serialize(builder, child);
    }
    return builder.toSafeHtml();
  }

  private static void serialize(SafeHtmlBuilder builder, Node child) {
    builder.appendHtmlConstant(child.toString());
  }

}
