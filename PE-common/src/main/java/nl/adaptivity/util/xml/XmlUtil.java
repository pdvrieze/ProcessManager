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

  private static class MetaStripper extends StreamWriterDelegate {

    public MetaStripper(final XMLStreamWriter pWrapped) {
      super(pWrapped);
    }

    @Override
    public void writeProcessingInstruction(final String target) throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
      /* Ignore */
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
      /* Ignore */
    }
  }

  public static final int OMIT_XMLDECL = 1;
  private static final int DEFAULT_FLAGS = OMIT_XMLDECL;

  private XmlUtil() {}

  public static Element createElement(final Document pDocument, final QName pOuterName) {
    Element root;
    if (XMLConstants.NULL_NS_URI.equals(pOuterName.getNamespaceURI()) || null== pOuterName.getNamespaceURI()) {
      root = pDocument.createElement(pOuterName.getLocalPart());
    } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(pOuterName.getPrefix())) {
      root = pDocument.createElementNS(pOuterName.getNamespaceURI(), pOuterName.getLocalPart());
    } else {
      root = pDocument.createElementNS(pOuterName.getNamespaceURI(), pOuterName.getPrefix()+':'+ pOuterName.getLocalPart());
    }
    return root;
  }

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
      Transformer transformer = TransformerFactory
              .newInstance()
              .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(source, new StAXResult(pOut));

    } catch (TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

  public static String toString(Node pValue) {
    return toString(pValue, DEFAULT_FLAGS);
  }
  public static String toString(Node pValue, int flags) {
    StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      configure(t, flags);
      t.transform(new DOMSource(pValue), new StreamResult(out));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(NodeList pNodeList) {
    return toString(pNodeList, DEFAULT_FLAGS);
  }

  public static String toString(final NodeList pNodeList, final int pFlags) {
    StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      configure(t, pFlags);
      for(int i=0; i<pNodeList.getLength(); ++i) {
        t.transform(new DOMSource(pNodeList.item(i)), new StreamResult(out));
      }
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(XmlSerializable pSerializable) {
    int flags = DEFAULT_FLAGS;
    return toString(pSerializable, flags);
  }

  public static XMLStreamWriter stripMetatags(final XMLStreamWriter pOut) {
    return new MetaStripper(pOut);
  }

  public static void setAttribute(final Element pElement, final QName pName, final String pValue) {
    if (pName.getNamespaceURI()==null || XMLConstants.NULL_NS_URI.equals(pName.getNamespaceURI())) {
      pElement.setAttribute(pName.getLocalPart(), pValue);
    } else if (pName.getPrefix()==null || XMLConstants.DEFAULT_NS_PREFIX.equals(pName.getPrefix())) {
      pElement.setAttributeNS(pName.getNamespaceURI(),pName.getLocalPart(), pValue);
    } else {
      pElement.setAttributeNS(pName.getNamespaceURI(),pName.getPrefix()+':'+pName.getLocalPart(), pValue);
    }
  }

  private static String toString(final XmlSerializable pSerializable, final int pFlags) {
    StringWriter out =new StringWriter();
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    configure(factory, pFlags);
    try {
      XMLStreamWriter serializer = factory.createXMLStreamWriter(out);
      pSerializable.serialize(serializer);
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  private static void configure(final Transformer pTransformer, final int pFlags) {
    if ((pFlags & OMIT_XMLDECL)!=0) {
      pTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
  }

  private static void configure(final XMLOutputFactory pFactory, final int pDEFAULT_FLAGS) {
    // Nothing to configure for now
  }

  public static boolean isXmlWhitespace(CharSequence pData) {
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
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
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
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(xsr.getPrefix())) {
                  namespaceInfo.prefix="";
                }
                xsw.setPrefix(namespaceInfo.prefix, namespaceInfo.url);
                xsw.writeStartElement(namespaceInfo.prefix, xsr.getLocalName(), namespaceInfo.url);
              } else { // no namespace info (probably no namespace at all)
                xsw.writeStartElement(xsr.getPrefix(), xsr.getLocalName(), xsr.getNamespaceURI());
              }
              first = false;
              for (NamespaceInfo ns : collectedNS.values()) {
                xsw.setPrefix(ns.prefix, ns.url);
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
