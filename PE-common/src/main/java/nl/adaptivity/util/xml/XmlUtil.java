package nl.adaptivity.util.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.CombiningReader;
import nl.adaptivity.xml.GatheringNamespaceContext;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


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
  private static final StreamFilter SUBSTREAM_FILTER = new StreamFilter() {
    @Override
    public boolean accept(final XMLStreamReader reader) {
      switch (reader.getEventType()) {
        case XMLStreamConstants.START_DOCUMENT:
        case XMLStreamConstants.END_DOCUMENT:
        case XMLStreamConstants.DTD:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          return false;
        default:
          return true;
      }
    }
  };
  private static final EventFilter SUBEVENTS_FILTER = new EventFilter() {
    @Override
    public boolean accept(final XMLEvent event) {
      switch (event.getEventType()) {
        case XMLStreamConstants.START_DOCUMENT:
        case XMLStreamConstants.END_DOCUMENT:
        case XMLStreamConstants.DTD:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          return false;
        default:
          return true;
      }
    }
  };;

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

  public static DocumentFragment tryParseXmlFragment(final Reader pReader) throws IOException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new InputSource(new CombiningReader(new StringReader("<elem>"), pReader, new StringReader("</elem>"))));
      DocumentFragment frag = doc.createDocumentFragment();
      Element docelem = doc.getDocumentElement();
      for(Node child=docelem.getFirstChild(); child!=null; child=child.getNextSibling()) {
        frag.appendChild(child);
      }
      doc.removeChild(docelem);
      return frag;
    } catch (ParserConfigurationException |SAXException pE) {
      throw new IOException(pE);
    }

  }

  public static QName asQName(final Node pReference, final String pName) {
    final int colPos = pName.indexOf(':');
    if (colPos >= 0) {
      final String prefix = pName.substring(0, colPos);
      return new QName(pReference.lookupNamespaceURI(prefix), pName.substring(colPos + 1), prefix);
    } else {
      return new QName(pReference.lookupNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), pName, XMLConstants.NULL_NS_URI);
    }

  }

  public static QName asQName(final NamespaceContext pReference, final String pName) {
    final int colPos = pName.indexOf(':');
    if (colPos >= 0) {
      final String prefix = pName.substring(0, colPos);
      return new QName(pReference.getNamespaceURI(prefix), pName.substring(colPos + 1), prefix);
    } else {
      return new QName(pReference.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), pName, XMLConstants.NULL_NS_URI);
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

  public static <T extends XmlDeserializable> T deserializeHelper(T result, final XMLStreamReader in) throws XMLStreamException {
    XmlUtil.skipPreamble(in);
    QName elementName = result.getElementName();
    assert XmlUtil.isElement(in, elementName);
    for(int i=in.getAttributeCount()-1; i>=0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    result.onBeforeDeserializeChildren(in);
    int event = -1;
    if (result instanceof SimpleXmlDeserializable) {
      loop: while (in.hasNext() && event != XMLStreamConstants.END_ELEMENT) {
        switch ((event = in.next())) {
          case XMLStreamConstants.START_ELEMENT:
            if (((SimpleXmlDeserializable)result).deserializeChild(in)) {
              continue loop;
            }
            XmlUtil.unhandledEvent(in);
            break;
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
            if (((SimpleXmlDeserializable)result).deserializeChildText(in.getText())) {
              continue loop;
            }
          default:
            XmlUtil.unhandledEvent(in);
        }
      }
    } else if (result instanceof ExtXmlDeserializable){
      ((ExtXmlDeserializable)result).deserializeChildren(in);
      if (XmlUtil.class.desiredAssertionStatus()) {
        in.require(XMLStreamConstants.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
      }
    } else {// Neither, means ignore children
      if(! isXmlWhitespace(siblingsToFragment(in).getContent())) {
        throw new XMLStreamException("Unexpected child content in element");
      }
    }
    return result;
  }

  public static <T> T deSerialize(InputStream pIn, Class<T> type) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return deSerialize(xif.createXMLStreamReader(pIn), type);
  }

  public static <T> T deSerialize(Reader pIn, Class<T> type) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return deSerialize(xif.createXMLStreamReader(pIn), type);
  }

  public static <T> T deSerialize(Source pIn, Class<T> type) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return deSerialize(xif.createXMLStreamReader(pIn), type);
  }

  public static <T> T deSerialize(XMLStreamReader pIn, Class<T> type) throws XMLStreamException {
    XmlDeserializer deserializer = type.getAnnotation(XmlDeserializer.class);
    if (deserializer==null) { throw new IllegalArgumentException("Types must be annotated with "+XmlDeserializer.class.getName()+" to be deserialized automatically"); }
    try {
      XmlDeserializerFactory<T> factory = deserializer.value().newInstance();
      return factory.deserialize(pIn);
    } catch (InstantiationException | IllegalAccessException pE) {
      throw new RuntimeException(pE);
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

  public static String readSimpleElement(final XMLStreamReader pIn) throws XMLStreamException {
    return pIn.getElementText();
  }

  /**
   * Filter the stream such that is a valid substream. This basically strips start document, end document, processing
   * instructions and dtd declarations.
   * @param pStreamReader The original stream reader.
   * @return A filtered stream
   */
  public static XMLStreamReader filterSubstream(final NamespaceAddingStreamReader pStreamReader) throws
          XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(pStreamReader, SUBSTREAM_FILTER);
  }

  public static XMLEventReader filterSubstream(final XMLEventReader pXMLEventReader) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(pXMLEventReader, SUBEVENTS_FILTER);
  }

  public static String xmlEncode(final String pUnEncoded) {
    StringBuilder result = null;
    int last=0;
    for(int i=0; i<pUnEncoded.length(); ++i) {
      switch (pUnEncoded.charAt(i)) {
        case '<':
          if (result==null) { result = new StringBuilder(pUnEncoded.length()); }
          result.append(pUnEncoded,last, i).append("&lt;");
          last = i+1;
          break;
        case '&':
          if (result==null) { result = new StringBuilder(pUnEncoded.length()); }
          result.append(pUnEncoded,last, i).append("&amp;");
          last = i+1;
          break;
        default:
          break;
      }

    }
    if (result==null) { return pUnEncoded; }
    result.append(pUnEncoded, last, pUnEncoded.length());
    return result.toString();
  }

  private static String toString(final XmlSerializable pSerializable, final int pFlags) {
    StringWriter out =new StringWriter();
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    configure(factory, pFlags);
    try {
      XMLStreamWriter serializer = factory.createXMLStreamWriter(out);
      pSerializable.serialize(serializer);
      serializer.close();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static char[] toCharArray(final Source pContent) throws XMLStreamException {
    return toCharArrayWriter(pContent).toCharArray();
  }

  public static String toString(final Source pSource) throws XMLStreamException {
    return toCharArrayWriter(pSource).toString();
  }

  private static CharArrayWriter toCharArrayWriter(final Source pSource) throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    XMLEventReader in = xif.createXMLEventReader(pSource);
    CharArrayWriter caw = new CharArrayWriter();
    XMLEventWriter xew = xof.createXMLEventWriter(caw);
    try {
      xew.add(in);
    } finally {
      xew.close();
    }
    return caw;
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

  public static DocumentFragment childrenToDocumentFragment(final XMLStreamReader in) throws XMLStreamException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = null;
      try {
        doc = dbf.newDocumentBuilder().newDocument();
      } catch (ParserConfigurationException e) {
        throw new XMLStreamException(e);
      }

      XMLInputFactory xif = XMLInputFactory.newFactory();
      XMLEventReader xer = xif.createXMLEventReader(in);

      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
      DocumentFragment documentFragment = doc.createDocumentFragment();
      XMLEventWriter out = xof.createXMLEventWriter(new DOMResult(documentFragment));
      XMLEventFactory xef = XMLEventFactory.newFactory();

      while (xer.hasNext() && (! xer.peek().isEndElement())) {
        XMLEvent event = xer.nextEvent();
        out.add(event);
        if (event.isStartElement()) {
          writeElementContent(xef, xer, out);
        }
      }
      return documentFragment;
    } catch (XMLStreamException | RuntimeException e) {
      throw e;
    }
  }

  public static char[] siblingsToCharArray(final XMLStreamReader in) throws XMLStreamException {
    return siblingsToFragment(in).getContent();
  }
    /**
     * Get a character array containing the current node and all it's following siblings.
     * @param in
     * @return
     * @throws XMLStreamException
     */
  public static CompactFragment siblingsToFragment(final XMLStreamReader in) throws XMLStreamException {
    Location startLocation = in.getLocation();
    try {
      XMLInputFactory xif = XMLInputFactory.newFactory();
      XMLEventReader xer = xif.createXMLEventReader(in);

      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
      CharArrayWriter caw = new CharArrayWriter();
      XMLEventFactory xef = XMLEventFactory.newFactory();

      TreeMap<String, String> missingNamespaces = new TreeMap<>();
      GatheringNamespaceContext gatheringContext = new GatheringNamespaceContext(in.getNamespaceContext(), missingNamespaces);
      while (xer.hasNext() && (! xer.peek().isEndElement())) {
        XMLEvent event = xer.nextEvent();
        if (event.isStartElement()) {
          XMLEventWriter out = xof.createXMLEventWriter(caw);
          out.setNamespaceContext(gatheringContext);
          out.add(event);
          out.getNamespaceContext().getNamespaceURI(event.asStartElement().getName().getPrefix());
          writeElementContent(xef, xer, out);
          out.close();
        } else if (event.isCharacters()) {
          event.writeAsEncodedUnicode(caw);
        }
      }
      return new CompactFragment(new SimpleNamespaceContext(missingNamespaces),caw.toCharArray());
    } catch (XMLStreamException | RuntimeException e) {
      throw new XMLStreamException("Failure to parse children into string at "+startLocation, e);
    }
  }

  public static void unhandledEvent(final XMLStreamReader in) throws XMLStreamException {
    switch (in.getEventType()) {
      case XMLStreamConstants.CDATA:
      case XMLStreamConstants.CHARACTERS:
        if (!in.isWhiteSpace()) {
          throw new XMLStreamException("Content found where not expected ["+in.getLocation()+"]");
        }
        break;
      case XMLStreamConstants.COMMENT:
        break; // ignore
      case XMLStreamConstants.START_ELEMENT:
        throw new XMLStreamException("Element found where not expected ["+in.getLocation()+"]: "+in.getName());
      case XMLStreamConstants.END_DOCUMENT:
        throw new XMLStreamException("End of document found where not expected");
    }
  }

  /**
   * Skil the preamble events in the stream reader
   * @param pIn The stream reader to skip
   */
  public static void skipPreamble(final XMLStreamReader pIn) throws XMLStreamException {
    int type = pIn.getEventType();
    while (isPreamble(type) && pIn.hasNext()) {
      type = pIn.next();
    }
  }

  public static boolean isPreamble(int type) {
    switch (type) {
      case XMLStreamConstants.COMMENT:
      case XMLStreamConstants.START_DOCUMENT:
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
      case XMLStreamConstants.DTD:
      case XMLStreamConstants.SPACE:
        return true;
      default:
        return false;
    }
  }

  public static void writeChild(final XMLStreamWriter pOut, final XmlSerializable pChild) throws XMLStreamException {
    if (pChild!=null) {
      pChild.serialize(pOut);
    }
  }

  public static void writeChildren(final XMLStreamWriter pOut, final Iterable<? extends XmlSerializable> pChildren) throws
          XMLStreamException {
    if (pChildren!=null) {
      for (XmlSerializable child : pChildren) {
        writeChild(pOut, child);
      }
    }
  }

  public static void writeStartElement(final XMLStreamWriter pOut, final QName pQName) throws XMLStreamException {
    boolean writeNs = false;
    String namespace = pQName.getNamespaceURI();
    String prefix;
    if (namespace==null) {
      namespace = pOut.getNamespaceContext().getNamespaceURI(pQName.getPrefix());
      prefix = pQName.getPrefix();
    } else {
      prefix = pOut.getPrefix(namespace);
      if (prefix==null) { // The namespace is not know in the output context, so add an attribute
        writeNs = true;
        prefix = pQName.getPrefix();
      }
    }
    pOut.writeStartElement(prefix, pQName.getLocalPart(), namespace);
    if (writeNs) {
      pOut.writeNamespace(prefix, namespace);
    }
  }

  public static void writeEmptyElement(final XMLStreamWriter pOut, final QName pQName) throws XMLStreamException {
    String namespace = pQName.getNamespaceURI();
    String prefix;
    if (namespace==null) {
      namespace = pOut.getNamespaceContext().getNamespaceURI(pQName.getPrefix());
      prefix = pQName.getPrefix();
    } else {
      prefix = pOut.getPrefix(namespace);
      if (prefix==null) { prefix = pQName.getPrefix(); }
    }
    pOut.writeEmptyElement(prefix, pQName.getLocalPart(), namespace);
  }


  public static void writeSimpleElement(final XMLStreamWriter pOut, final QName pQName, final String pValue) throws
  XMLStreamException {
    if (pValue!=null) {
      if (pValue.isEmpty()) {
        writeEmptyElement(pOut, pQName);
      } else {
        writeStartElement(pOut, pQName);
        pOut.writeCharacters(pValue);
        pOut.writeEndElement();
      }
    }
  }

  public static void writeAttribute(final XMLStreamWriter pOut, final String pName, final String pValue) throws
  XMLStreamException {
    if (pValue!=null) {
      pOut.writeAttribute(pName, pValue);
    }
  }

  public static void writeAttribute(final XMLStreamWriter pOut, final String pName, final double pValue) throws
  XMLStreamException {
    if (! Double.isNaN(pValue)) {
      pOut.writeAttribute(pName, Double.toString(pValue));
    }
  }

  public static void writeAttribute(final XMLStreamWriter pOut, final String pName, final QName pValue) throws
          XMLStreamException {
    if (pValue!=null) {
      String prefix;
      if (pValue.getNamespaceURI()!=null) {
        if (pValue.getPrefix()!=null && pValue.getNamespaceURI().equals(pOut.getNamespaceContext().getNamespaceURI(pValue.getPrefix()))) {
          prefix = pValue.getPrefix();
        } else {
          prefix = pOut.getNamespaceContext().getPrefix(pValue.getNamespaceURI());
          if (prefix == null) {
            prefix = pValue.getPrefix();
            pOut.writeNamespace(prefix, pValue.getNamespaceURI());
          }
        }
      } else {
        prefix = pValue.getPrefix();
        String ns = pOut.getNamespaceContext().getNamespaceURI(prefix);
        if (ns==null) { throw new IllegalArgumentException("Cannot determine namespace of qname"); }
      }
      pOut.writeAttribute(pName, prefix+':'+pValue.getLocalPart());
    }
  }

  private static boolean nullOrEmpty(String s) {
    return s==null || s.isEmpty();
  }

  /**
   * Check that the current state is a start element for the given name. The prefix is ignored.
   * @param pIn The stream reader to check
   * @param pElementname The name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */

  public static boolean isElement(final XMLStreamReader pIn, final QName pElementname) {
    return isElement(pIn, pElementname.getNamespaceURI(), pElementname.getLocalPart(), pElementname.getPrefix());
  }

  /**
   * Check that the current state is a start element for the given name. The prefix is ignored.
   * @param pIn The stream reader to check
   * @param pElementNamespace  The namespace to check against.
   * @param pElementName The local name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(final XMLStreamReader pIn, final String pElementNamespace, final String pElementName) {
    String pElementPrefix = null;
    return isElement(pIn, pElementNamespace, pElementName, pElementPrefix);
  }


  /**
   * Check that the current state is a start element for the given name. The prefix is ignored.
   * @param pIn The stream reader to check
   * @param pElementNamespace  The namespace to check against.
   * @param pElementName The local name to check against
   * @param pElementPrefix The prefix to fall back on if the namespace can't be determined
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(final XMLStreamReader pIn, final String pElementNamespace, final String pElementName, final String pElementPrefix) {
    if (pIn.getEventType()!= XMLStreamConstants.START_ELEMENT) { return false; }
    String expNs =  pElementNamespace;
    if ("".equals(expNs)) { expNs = null; }
    if (! pIn.getLocalName().equals(pElementName)) { return false; }

    if (nullOrEmpty(pElementNamespace)) {
      if (nullOrEmpty(pElementPrefix)) {
        return nullOrEmpty(pIn.getPrefix());
      } else {
        return pElementPrefix.equals(pIn.getPrefix());
      }
    } else {
      return pElementNamespace.equals(pIn.getNamespaceURI());
    }
  }

  private static void writeElementContent(final XMLEventFactory pXef, final XMLEventReader pIn, final XMLEventWriter pOut) throws XMLStreamException {
    while (pIn.hasNext()) {
      XMLEvent event = pIn.nextEvent();
      pOut.add(event);
      if (event.isStartElement()) {
        pOut.getNamespaceContext().getNamespaceURI(event.asStartElement().getName().getPrefix());
        writeElementContent(pXef, pIn, pOut);
      }
      if (event.isEndElement()) {
        break;
      }
    }
  }

  private static void configure(final Transformer pTransformer, final int pFlags) {
    if ((pFlags & OMIT_XMLDECL)!=0) {
      pTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
  }

  private static void configure(final XMLOutputFactory pFactory, final int pDEFAULT_FLAGS) {
    // Nothing to configure for now
  }

  private static boolean isXmlWhitespace(final char[] pData) {
    for(int i=pData.length-1; i>=0; --i) {
      final char c = pData[i];
      if (!(c==0xA || c==0x9 || c==0xd || c==' ')) {
        return false;
      }
    }
    return true;
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
