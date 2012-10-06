package nl.adaptivity.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.devrieze.util.StringUtil;


public class XmlUtil {

  private XmlUtil() {}

  public static Document tryParseXml(final InputStream pInputStream) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();

      final Document d = db.parse(pInputStream);
      return d;
    } catch (final SAXException e) {
      return null;
    } catch (final ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static QName asQName(final Node pReference, final String pName) {
    final int colPos = pName.indexOf(':');
    if (colPos >= 0) {
      final String prefix = pName.substring(0, colPos);
      return new QName(pReference.lookupNamespaceURI(prefix), pName.substring(colPos + 1), prefix);
    } else {
      return new QName(pReference.lookupNamespaceURI(null), pName, XMLConstants.NULL_NS_URI);
    }

  }

  public static Element getChild(final Element pParent, final QName pName) {
    return getFirstChild(pParent, pName.getNamespaceURI(), pName.getLocalPart());
  }

  public static Element getFirstChild(final Element pParent, final String pNamespaceURI, final String pLocalName) {
    for (Element child = getFirstChildElement(pParent); child != null; child = getNextSiblingElement(child)) {
      if ((pNamespaceURI == null) || (pNamespaceURI.length() == 0)) {
        if (((child.getNamespaceURI() == null) || (child.getNamespaceURI().length() == 0))
            && StringUtil.isEqual(pLocalName, child.getLocalName())) {
          return child;
        }
      } else {
        if (StringUtil.isEqual(pNamespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(pLocalName, child.getLocalName())) {
          return child;
        }
      }
    }
    return null;
  }

  public static Element getNextSibling(final Element pSibling, final QName pName) {
    return getNextSibling(pSibling, pName.getNamespaceURI(), pName.getLocalPart());
  }

  public static Element getNextSibling(final Element pSibling, final String pNamespaceURI, final String pLocalName) {
    for (Element child = getNextSiblingElement(pSibling); child != null; child = getNextSiblingElement(child)) {
      if (StringUtil.isEqual(pNamespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(pLocalName, child.getLocalName())) {
        return child;
      }
    }
    return null;
  }

  public static String getQualifiedName(final QName pName) {
    final String prefix = pName.getPrefix();
    if ((prefix == null) || (prefix == XMLConstants.NULL_NS_URI)) {
      return pName.getLocalPart();
    }
    return prefix + ':' + pName.getLocalPart();
  }

  /**
   * Return the first child that is an element.
   * 
   * @param pParent The parent element.
   * @return The first element child, or <code>null</code> if there is none.
   */
  public static Element getFirstChildElement(final Element pParent) {
    for (Node child = pParent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }

  /**
   * Return the next sibling that is an element.
   * 
   * @param pParent The reference element.
   * @return The next element sibling, or <code>null</code> if there is none.
   */
  public static Element getNextSiblingElement(final Element pSibling) {
    for (Node child = pSibling.getNextSibling(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }
}
