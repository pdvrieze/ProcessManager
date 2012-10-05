package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.CDATASection;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Text;


public class XMLUtil {

  private XMLUtil() {}

  public static String getParamText(final Node pNode, final String pSpec) {
    if (pSpec.startsWith("=")) {
      return parseParam(pNode, pSpec.substring(1));
    } else if (pSpec.startsWith("@")) {
      return parseParam(pNode, "@{" + pSpec.substring(1) + "}");
    } else {
      return parseParam(pNode, "${" + pSpec + "}");
    }
  }

  public static String getParamText(final com.google.gwt.dom.client.Node pNode, final String pSpec) {
    if (pSpec.startsWith("=")) {
      return parseParam(pNode, pSpec.substring(1));
    } else if (pSpec.startsWith("@")) {
      return parseParam(pNode, "@{" + pSpec.substring(1) + "}");
    } else {
      return parseParam(pNode, "${" + pSpec + "}");
    }
  }

  private static String parseParam(final Node pNode, final String pSpec) {
    final StringBuilder result = new StringBuilder(pSpec.length() * 2);
    int i = 0;
    int j = 0;
    while (j < pSpec.length()) {
      final char c = pSpec.charAt(j);
      if ((c == '\\') && ((j + 1) < pSpec.length())) {
        result.append(pSpec.substring(i, j));
        ++j;
        i = j;
      } else if (c == '$') {
        result.append(pSpec.substring(i, j));
        if (((j + 3) < pSpec.length()) && (pSpec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < pSpec.length()) && (pSpec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < pSpec.length()) && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
        }

        i = j;
      } else if (c == '@') {
        result.append(pSpec.substring(i, j));
        if (((j + 3) < pSpec.length()) && (pSpec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < pSpec.length()) && (pSpec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getAttributeValue((Element) pNode, pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < pSpec.length()) && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getAttributeValue((Element) pNode, pSpec.substring(i, j)));
        }

        i = j;
      } else {
        ++j;
      }
    }
    result.append(pSpec.substring(i, j));
    return result.toString();
  }

  private static String parseParam(final com.google.gwt.dom.client.Node pNode, final String pSpec) {
    final StringBuilder result = new StringBuilder(pSpec.length() * 2);
    int i = 0;
    int j = 0;
    while (j < pSpec.length()) {
      final char c = pSpec.charAt(j);
      if ((c == '\\') && ((j + 1) < pSpec.length())) {
        result.append(pSpec.substring(i, j));
        ++j;
        i = j;
      } else if (c == '$') {
        result.append(pSpec.substring(i, j));
        if (((j + 3) < pSpec.length()) && (pSpec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < pSpec.length()) && (pSpec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < pSpec.length()) && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
        }

        i = j;
      } else if (c == '@') {
        result.append(pSpec.substring(i, j));
        if (((j + 3) < pSpec.length()) && (pSpec.charAt(j + 1) == '{')) {
          j += 2;
          i = j;
          while ((j < pSpec.length()) && (pSpec.charAt(j) != '}')) {
            ++j;
          }
          result.append(getAttributeValue(com.google.gwt.dom.client.Element.as(pNode), pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i = j;
          while ((j < pSpec.length()) && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getAttributeValue(com.google.gwt.dom.client.Element.as(pNode), pSpec.substring(i, j)));
        }

        i = j;
      } else {
        ++j;
      }
    }
    result.append(pSpec.substring(i, j));
    return result.toString();
  }

  public static String getSubNodeValue(final Node pNode, final String pName) {
    final String value;
    Node candidate = pNode.getFirstChild();
    while ((candidate != null) && (!pName.equals(candidate.getNodeName()))) {
      candidate = candidate.getNextSibling();
    }
    value = candidate == null ? null : candidate.getNodeValue();
    if (value == null) {
      GWT.log("subnode " + pName + " could not be resolved", null);
    }
    return value;
  }

  public static Object getSubNodeValue(final com.google.gwt.dom.client.Node pNode, final String pName) {
    final String value;
    com.google.gwt.dom.client.Node candidate = pNode.getFirstChild();
    while ((candidate != null) && (!pName.equals(candidate.getNodeName()))) {
      candidate = candidate.getNextSibling();
    }
    value = candidate == null ? null : candidate.getNodeValue();
    if (value == null) {
      GWT.log("subnode " + pName + " could not be resolved", null);
    }
    return value;
  }

  public static String getAttributeValue(final Element pNode, final String pName) {
    final Node val = pNode.getAttributes().getNamedItem(pName);
    if (val == null) {
      GWT.log("Attribute " + pName + " could not be resolved", null);
    }
    return val == null ? null : val.toString();
  }

  public static String getAttributeValue(final com.google.gwt.dom.client.Element pNode, final String pName) {
    final String val = pNode.getAttribute(pName);

    if (val == null) {
      GWT.log("Attribute " + pName + " could not be resolved", null);
    }
    return val;
  }

  private static boolean isChar(final char c) {
    return Character.isLetterOrDigit(c);
  }

  public static boolean isTag(final String pNamespace, final String pLocalName, final Node pElement) {
    return isNS(pNamespace, pElement) && isLocalPart(pLocalName, pElement);
  }

  public static boolean isLocalPart(final String pLocalPart, final Node pElement) {
    if (pElement.getNodeType() == Node.ELEMENT_NODE) {
      return pLocalPart.equals(((Element) pElement).getTagName());
    }
    return false;
  }

  public static boolean isNS(final String pNameSpace, final Node pElement) {
    if (pNameSpace == null) {
      return (pElement.getNamespaceURI() == null) || "".equals(pElement.getNamespaceURI());
    } else {
      return pNameSpace.equals(pElement.getNamespaceURI());
    }
  }

  public static String getTextChildren(final Node pNode) {
    final StringBuilder result = new StringBuilder();
    for (Node child = pNode.getFirstChild(); child != null; child = child.getNextSibling()) {
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

  public static com.google.gwt.dom.client.Element descendentWithAttribute(final com.google.gwt.dom.client.Element pBase, final String pAttributeName, final String pValue) {
    for (com.google.gwt.dom.client.Element elem = pBase.getFirstChildElement(); elem != null; elem = elem.getNextSiblingElement()) {
      if (pValue.equals(elem.getAttribute(pAttributeName))) {
        return elem;
      }
    }

    for (com.google.gwt.dom.client.Element elem = pBase.getFirstChildElement(); elem != null; elem = elem.getNextSiblingElement()) {
      final com.google.gwt.dom.client.Element descendent = descendentWithAttribute(elem, pAttributeName, pValue);
      if (descendent != null) {
        return descendent;
      }
    }
    return null;
  }

}
