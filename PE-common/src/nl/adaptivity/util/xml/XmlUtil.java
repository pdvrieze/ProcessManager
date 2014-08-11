package nl.adaptivity.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

  public static void serialize(XMLStreamWriter pOut, Node pNode) throws XMLStreamException {
    serialize(pOut, new DOMSource(pNode));
  }

  public static void serialize(XMLStreamWriter pOut, final Source source) throws TransformerFactoryConfigurationError,
      XMLStreamException {
    try {
      TransformerFactory
          .newInstance()
          .newTransformer()
          .transform(source, new StAXResult(pOut));
    } catch (TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

  public static String toString(Node pValue) {
    StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.transform(new DOMSource(pValue), new StreamResult(out));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(NodeList pNodeList) {
    StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      for(int i=0; i<pNodeList.getLength(); ++i) {
        t.transform(new DOMSource(pNodeList.item(i)), new StreamResult(out));
      }
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(XmlSerializable pSerializable) {
    StringWriter out =new StringWriter();
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    try {
      XMLStreamWriter serializer = factory.createXMLStreamWriter(out);
      pSerializable.serialize(serializer);
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }
}
