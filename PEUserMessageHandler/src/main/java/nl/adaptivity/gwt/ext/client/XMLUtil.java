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

package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.CDATASection;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Text;


public class XMLUtil {

  private XMLUtil() {}

  public static String getParamText(final Node node, final String spec) {
    if (spec.startsWith("=")) {
      return parseParam(node, spec.substring(1));
    } else if (spec.startsWith("@")) {
      return parseParam(node, "@{" + spec.substring(1) + "}");
    } else {
      return parseParam(node, "${" + spec + "}");
    }
  }

  public static String getParamText(final com.google.gwt.dom.client.Node node, final String spec) {
    if (spec.startsWith("=")) {
      return parseParam(node, spec.substring(1));
    } else if (spec.startsWith("@")) {
      return parseParam(node, "@{" + spec.substring(1) + "}");
    } else {
      return parseParam(node, "${" + spec + "}");
    }
  }

  private static String parseParam(final Node node, final String spec) {
    final StringBuilder result = new StringBuilder(spec.length() * 2);
    int i = 0;
    int j = 0;
    while (j < spec.length()) {
      final char c = spec.charAt(j);
      if ((c == '\\') && ((j + 1) < spec.length())) {
        result.append(spec.substring(i, j));
        ++j;
        i = j;
      } else if (c == '$') {
        result.append(spec.substring(i, j));
        if (((j + 3) < spec.length()) && (spec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < spec.length()) && (spec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getSubNodeValue(node, spec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < spec.length()) && isChar(spec.charAt(j))) {
            ++j;
          }
          result.append(getSubNodeValue(node, spec.substring(i, j)));
        }

        i = j;
      } else if (c == '@') {
        result.append(spec.substring(i, j));
        if (((j + 3) < spec.length()) && (spec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < spec.length()) && (spec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getAttributeValue((Element) node, spec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < spec.length()) && isChar(spec.charAt(j))) {
            ++j;
          }
          result.append(getAttributeValue((Element) node, spec.substring(i, j)));
        }

        i = j;
      } else {
        ++j;
      }
    }
    result.append(spec.substring(i, j));
    return result.toString();
  }

  private static String parseParam(final com.google.gwt.dom.client.Node node, final String spec) {
    final StringBuilder result = new StringBuilder(spec.length() * 2);
    int i = 0;
    int j = 0;
    while (j < spec.length()) {
      final char c = spec.charAt(j);
      if ((c == '\\') && ((j + 1) < spec.length())) {
        result.append(spec.substring(i, j));
        ++j;
        i = j;
      } else if (c == '$') {
        result.append(spec.substring(i, j));
        if (((j + 3) < spec.length()) && (spec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < spec.length()) && (spec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getSubNodeValue(node, spec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < spec.length()) && isChar(spec.charAt(j))) {
            ++j;
          }
          result.append(getSubNodeValue(node, spec.substring(i, j)));
        }

        i = j;
      } else if (c == '@') {
        result.append(spec.substring(i, j));
        if (((j + 3) < spec.length()) && (spec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < spec.length()) && (spec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getAttributeValue(com.google.gwt.dom.client.Element.as(node), spec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < spec.length()) && isChar(spec.charAt(j))) {
            ++j;
          }
          result.append(getAttributeValue(com.google.gwt.dom.client.Element.as(node), spec.substring(i, j)));
        }

        i = j;
      } else {
        ++j;
      }
    }
    result.append(spec.substring(i, j));
    return result.toString();
  }

  public static String getSubNodeValue(final Node node, final String name) {
    final String value;
    Node candidate = node.getFirstChild();
    while ((candidate != null) && (!name.equals(candidate.getNodeName()))) {
      candidate = candidate.getNextSibling();
    }
    value = candidate == null ? null : candidate.getNodeValue();
    if (value == null) {
      GWT.log("subnode " + name + " could not be resolved", null);
    }
    return value;
  }

  public static Object getSubNodeValue(final com.google.gwt.dom.client.Node node, final String name) {
    final String value;
    com.google.gwt.dom.client.Node candidate = node.getFirstChild();
    while ((candidate != null) && (!name.equals(candidate.getNodeName()))) {
      candidate = candidate.getNextSibling();
    }
    value = candidate == null ? null : candidate.getNodeValue();
    if (value == null) {
      GWT.log("subnode " + name + " could not be resolved", null);
    }
    return value;
  }

  public static String getAttributeValue(final Element node, final String name) {
    final Node val = node.getAttributes().getNamedItem(name);
    if (val == null) {
      GWT.log("Attribute " + name + " could not be resolved", null);
    }
    return val == null ? null : val.toString();
  }

  public static String getAttributeValue(final com.google.gwt.dom.client.Element node, final String name) {
    final String val = node.getAttribute(name);

    if (val == null) {
      GWT.log("Attribute " + name + " could not be resolved", null);
    }
    return val;
  }

  private static boolean isChar(final char c) {
    return Character.isLetterOrDigit(c);
  }

  public static boolean isTag(final String namespace, final String localName, final Node element) {
    return isNS(namespace, element) && isLocalPart(localName, element);
  }

  public static boolean isLocalPart(final String localPart, final Node element) {
    if (element.getNodeType() == Node.ELEMENT_NODE) {
      return localPart.equals(((Element) element).getTagName());
    }
    return false;
  }

  public static boolean isNS(final String nameSpace, final Node element) {
    if (nameSpace == null) {
      return (element.getNamespaceURI() == null) || "".equals(element.getNamespaceURI());
    } else {
      return nameSpace.equals(element.getNamespaceURI());
    }
  }

  public static String getTextChildren(final Node node) {
    final StringBuilder result = new StringBuilder();
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
        result.append(((CDATASection) child).getData());
      } else if (child.getNodeType() == Node.TEXT_NODE) {
        result.append(((Text) child).getData());
      } else {
        GWT.log("Unexpected node: " + child, null);
      }
    }
    result.trimToSize();
    return result.toString();
  }

  public static com.google.gwt.dom.client.Element descendentWithAttribute(final com.google.gwt.dom.client.Element base, final String attributeName, final String value) {
    for (com.google.gwt.dom.client.Element elem = base.getFirstChildElement(); elem != null; elem = elem.getNextSiblingElement()) {
      if (value.equals(elem.getAttribute(attributeName))) {
        return elem;
      }
    }

    for (com.google.gwt.dom.client.Element elem = base.getFirstChildElement(); elem != null; elem = elem.getNextSiblingElement()) {
      final com.google.gwt.dom.client.Element descendent = descendentWithAttribute(elem, attributeName, value);
      if (descendent != null) {
        return descendent;
      }
    }
    return null;
  }

  public static String localName(String nodeName) {
    final int i = nodeName.indexOf(':');
    if (i < 0) {
      return nodeName;
    }
    return nodeName.substring(i + 1);
  }

  public static long getLongAttr(Element owner, String attribute, long defaultVal) {
    String a = owner.getAttribute(attribute);
    if (a!=null) {
      try {
        return Long.parseLong(a);
      } catch (NumberFormatException e) {
        return defaultVal;
      }
    }
    return defaultVal;
  }

}
