package nl.adaptivity.util.xml;

import java.io.*;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

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
   * @param pSibling The reference element.
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

  public static void cannonicallize(Source in, Result out) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLStreamReader xsr = xif.createXMLStreamReader(in);
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    XMLStreamWriter xsw = xof.createXMLStreamWriter(out);
    Map<String, NamespaceInfo> collectedNS = new HashMap<>();

    while (xsr.hasNext()) {
      int type=xsr.next();
      switch (type) {
        case XMLStreamConstants.START_ELEMENT:
//          if (xsr.getNamespaceCount()>0) {
//            for(int i=0; i<xsr.getNamespaceCount(); ++i) {
//              addNamespace(collectedNS, xsr.getNamespacePrefix(i), xsr.getNamespaceURI(i));
//            }
//          }
          addNamespace(collectedNS, xsr.getPrefix(), xsr.getNamespaceURI());
          for(int i=xsr.getAttributeCount()-1; i>=0; --i) {
            addNamespace(collectedNS, xsr.getAttributePrefix(i), xsr.getAttributeNamespace(i));
          }
        default:
          // ignore
      }
    }

    xsr = xif.createXMLStreamReader(in);

    boolean first = true;
    while (xsr.hasNext()) {
      int type = xsr.next();
      switch (type) {
        case XMLStreamConstants.START_ELEMENT:
          {
            if (first) {
              NamespaceInfo namespaceInfo = collectedNS.get(xsr.getNamespaceURI());
              if (namespaceInfo != null) {
                xsw.writeStartElement(namespaceInfo.prefix, namespaceInfo.url, xsr.getLocalName());
              }
              first = false;
              for (NamespaceInfo ns : collectedNS.values()) {
                xsw.writeNamespace(ns.prefix, ns.url);
              }
            } else {
              xsw.writeStartElement(xsr.getNamespaceURI(), xsr.getLocalName());
            }
            final int ac = xsr.getAttributeCount();
            for (int i = 0; i<ac; ++i) {
              xsw.writeAttribute(xsr.getAttributeNamespace(i),xsr.getAttributeLocalName(i), xsr.getAttributeValue(i));
            }
            break;
          }
        case XMLStreamConstants.ATTRIBUTE:
          xsw.writeAttribute(xsr.getNamespaceURI(),xsr.getLocalName(), xsr.getText());
          break;
        case XMLStreamConstants.NAMESPACE:
          break;
        case XMLStreamConstants.END_ELEMENT:
          xsw.writeEndElement();
          break;
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.SPACE:
          xsw.writeCharacters(xsr.getTextCharacters(), xsr.getTextStart(), xsr.getTextLength());
          break;
        case XMLStreamConstants.CDATA:
          xsw.writeCData(xsr.getText());
          break;
        case XMLStreamConstants.COMMENT:
          xsw.writeComment(xsr.getText());
          break;
        case XMLStreamConstants.START_DOCUMENT:
          xsw.writeStartDocument(xsr.getCharacterEncodingScheme(), xsr.getVersion());
          break;
        case XMLStreamConstants.END_DOCUMENT:
          xsw.writeEndDocument();
          break;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          xsw.writeProcessingInstruction(xsr.getPITarget(), xsr.getPIData());
          break;
        case XMLStreamConstants.ENTITY_REFERENCE:
          xsw.writeEntityRef(xsr.getLocalName());
          break;
        case XMLStreamConstants.DTD:
          xsw.writeDTD(xsr.getText());
          break;
      }
    }
    xsw.close();
    xsr.close();
  }

  public static Node cannonicallize(final Node pContent) throws ParserConfigurationException,
          XMLStreamException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();

    if (pContent instanceof DocumentFragment) {
      DocumentFragment df = (DocumentFragment) pContent;
      DocumentFragment result = db.newDocument().createDocumentFragment();
      DOMResult dr = new DOMResult(result);
      for(Node child=df.getFirstChild(); child!=null; child=child.getNextSibling()) {
        cannonicallize(new DOMSource(child), dr);
      }
      return result;
    } else {
      Document result = db.newDocument();
      cannonicallize(new DOMSource(pContent), new DOMResult(result));
      return result.getDocumentElement();
    }
  }

  private static void addNamespace(final Map<String, NamespaceInfo> pCollectedNS, final String pPrefix, final String pNamespaceURI) {
    if (! (pNamespaceURI==null || XMLConstants.NULL_NS_URI.equals(pNamespaceURI))) {
      NamespaceInfo nsInfo = pCollectedNS.get(pNamespaceURI);
      if (nsInfo==null) {
        pCollectedNS.put(pNamespaceURI, new NamespaceInfo(pPrefix, pNamespaceURI));
      } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(nsInfo.prefix)) {
        nsInfo.prefix=pPrefix;
      }
    }
  }

}
