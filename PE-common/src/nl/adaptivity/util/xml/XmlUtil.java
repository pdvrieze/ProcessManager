package nl.adaptivity.util.xml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nl.adaptivity.util.activation.Sources;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.devrieze.util.StringUtil;


public class XmlUtil {

  private static class NamespaceInfo {
    String prefix;
    String url;

    public NamespaceInfo(String prefix, String url) {
      this.prefix = prefix;
      this.url = url;
    }
  }

  private XmlUtil() {}

  public static Document tryParseXml(final InputStream pInputStream) throws IOException {
    return tryParseXml(new InputSource(pInputStream));
  }

  public static Document tryParseXml(final Reader pReader) throws IOException {
    return tryParseXml(new InputSource(pReader));
  }

  public static Document tryParseXml(final String pString) throws IOException {
    return tryParseXml(new StringReader(pString));
  }

  public static Document tryParseXml(final InputSource s) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();

      final Document d = db.parse(s);
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

  public static boolean isXmlWhitespace(String pData) {
    for(int i=pData.length()-1; i>=0; --i) {
      final char c = pData.charAt(i);
      if (!(c==0xA || c==0x9 || c==0xd || c==' ')) {
        return false;
      }
    }
    return true;
  }

  public static void optimizeNamespaces(final Node pNode) {
    if (pNode instanceof DocumentFragment) {
      DocumentFragment df = (DocumentFragment) pNode;
      for(Node child = df.getFirstChild(); child!=null; child=child.getNextSibling()) {
        optimizeNamespaces(child);
      }
    } else if (pNode instanceof Element) {
      Element e = (Element) pNode;
      List<NamespaceInfo> namespaces = findUsedNamespaces(e);
      stripUnusedNamespaces(e, namespaces);
    }
  }

  private static void stripUnusedNamespaces(final Element pE, final List<NamespaceInfo> pNamespaces) {
    NamedNodeMap attributes = pE.getAttributes();
    for(int i= attributes.getLength()-1; i>=0; --i) {
      Attr attr = (Attr) attributes.item(i);
      if (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getName())) {
        if (! containsNamespace(pNamespaces, attr.getValue(), null)) {
          attributes.removeNamedItem(attr.getName());
        }
      } else if ()
    }

  }

  private static List<NamespaceInfo> findUsedNamespaces(final Element pE) {
    ArrayList<NamespaceInfo> found = new ArrayList<>();
    findUsedNamespaces(pE, found);

    return found;
  }

  private static void findUsedNamespaces(final Element pE, final List<NamespaceInfo> pFound) {
    if (! (pE.getNamespaceURI()==null || XMLConstants.NULL_NS_URI.equals(pE.getNamespaceURI()))) {
      if (!containsNamespace(pFound, pE.getNamespaceURI(), pE.getPrefix())) {
        pFound.add(new NamespaceInfo(pE.getPrefix(), pE.getNamespaceURI()));
      }
    }
    NamedNodeMap attributes = pE.getAttributes();
    for(int i = 0; i<attributes.getLength(); ++i) {
      Attr attr = (Attr) attributes.item(i);
      if (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix())) {
        if(!containsNamespace(pFound, attr.getValue(), attr.getLocalName())) {
          pFound.add(new NamespaceInfo(attr.getPrefix(), attr.getNamespaceURI()));
        }
      } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(attr.getPrefix())&& XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getLocalName())) {
        if(!containsNamespace(pFound, attr.getValue(), null)) {
          pFound.add(new NamespaceInfo(attr.getPrefix(), attr.getNamespaceURI()));
        }
      }
    }
    for(Node child = pE.getFirstChild(); child!=null; child=child.getNextSibling()) {
      if (child instanceof Element) {
        findUsedNamespaces((Element) child, pFound);
      }
    }
  }

  private static boolean containsNamespace(final List<NamespaceInfo> pFound, final String pNamespaceURI, String prefix) {
    for(NamespaceInfo info: pFound) {
      if (Objects.equals(info.url, pNamespaceURI)) {
        if (prefix!=null && (info.prefix==null||XMLConstants.DEFAULT_NS_PREFIX.equals(info.prefix))) {
          info.prefix=prefix;
        }
        return true;
      }
    }
    return false;
  }
}
