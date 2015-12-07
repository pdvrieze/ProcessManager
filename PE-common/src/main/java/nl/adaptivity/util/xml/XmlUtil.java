package nl.adaptivity.util.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.CombiningReader;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.codehaus.stax2.XMLOutputFactory2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public final class XmlUtil {

  private static class NamespaceInfo {

    final String mPrefix;
    final String mUrl;

    public NamespaceInfo(final String prefix, final String url) {
      this.mPrefix = prefix;
      this.mUrl = url;

    }
  }

  private static class MetaStripper extends XmlDelegatingWriter {

    public MetaStripper(final XmlWriter delegate) {
      super(delegate);
    }

    @Override
    public void processingInstruction(final CharSequence text) throws XmlException {
      /* ignore */
    }

    @Override
    public void endDocument() throws XmlException {
      /* ignore */
    }

    @Override
    public void docdecl(final CharSequence text) throws XmlException {
      /* ignore */
    }

    @Override
    public void startDocument(final CharSequence version, final CharSequence encoding, final Boolean standalone) throws
            XmlException {
      /* ignore */
    }
  }

  private static class StAXMetaStripper extends StreamWriterDelegate {

    public StAXMetaStripper(final XMLStreamWriter wrapped) {
      super(wrapped);
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

  private static class SubstreamFilter extends XmlBufferedReader {

    public SubstreamFilter(final XmlReader delegate) {
      super(delegate);
    }

    @NotNull
    @Override
    protected List<XmlEvent> doPeek() throws XmlException {
      List<XmlEvent> events = super.doPeek();
      for (Iterator<XmlEvent> it = events.iterator(); it.hasNext();) {
        XmlEvent event = it.next();
        EventType eventType = event.getEventType();
        switch (eventType) {
          case START_DOCUMENT:
          case PROCESSING_INSTRUCTION:
          case DOCDECL:
          case END_DOCUMENT:
            it.remove();
            break;
        }
      }
      return events;
    }
  }

  private static class SubstreamStreamFilter implements StreamFilter {

    private static final StreamFilter SUBSTREAM_FILTER = new SubstreamStreamFilter();

    @Override
    public boolean accept(@NotNull final XMLStreamReader reader) {
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
  }

  private static class SubstreamEventFilter implements EventFilter {

    private static final EventFilter SUBEVENTS_FILTER = new SubstreamEventFilter();

    @Override
    public boolean accept(@NotNull final XMLEvent event) {
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
  }

  public static final int OMIT_XMLDECL = 1;
  private static final int DEFAULT_FLAGS = OMIT_XMLDECL;


  private XmlUtil() {
  }

  public static Element createElement(@NotNull final Document document, @NotNull final QName outerName) {
    final Element root;
    if (XMLConstants.NULL_NS_URI.equals(outerName.getNamespaceURI()) || null == outerName.getNamespaceURI()) {
      root = document.createElement(outerName.getLocalPart());
    } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(outerName.getPrefix())) {
      root = document.createElementNS(outerName.getNamespaceURI(), outerName.getLocalPart());
    } else {
      root = document.createElementNS(outerName.getNamespaceURI(), outerName.getPrefix() + ':' + outerName.getLocalPart());
    }
    return root;
  }

  public static void skipElement(final XmlReader in) throws XmlException {
    in.require(EventType.START_ELEMENT, null, null);
    while (in.hasNext() && in.next()!=EventType.END_ELEMENT) {
      if (in.getEventType()==EventType.START_ELEMENT) {
        skipElement(in);
      }
    }
  }

  public static String getPrefix(final Node node, final String namespaceURI) {
    if (node==null) { return null; }
    if (node instanceof Element) {
      NamedNodeMap attrs = node.getAttributes();
      for (int i=0; i<attrs.getLength(); ++i) {
        Attr attr = (Attr) attrs.item(i);
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()) && attr.getValue().equals(namespaceURI)) {
          return attr.getName();
        }
      }
    }
    String prefix = getPrefix(node.getParentNode(), namespaceURI);
    if (node.hasAttributes()&& prefix!=null) {
      if (prefix.isEmpty()) {
        if (node.getAttributes().getNamedItem(XMLConstants.XMLNS_ATTRIBUTE) != null) {
          return null;
        }
      } else {
        if (node.getAttributes().getNamedItemNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) != null) {
          return null;
        }
      }
    }
    return prefix;
  }

  public static Reader toReader(final XmlSerializable serializable) throws XmlException {
    CharArrayWriter buffer = new CharArrayWriter();
    XmlWriter writer = XmlStreaming.newWriter(buffer);
    serializable.serialize(writer);
    writer.close();
    return new CharArrayReader(buffer.toCharArray());
  }

  public static void writeEndElement(final XmlWriter out, final QName predelemname) throws XmlException {
    out.endTag(predelemname.getNamespaceURI(), predelemname.getLocalPart(), predelemname.getPrefix());
  }

  @Nullable
  public static Document tryParseXml(final InputStream inputStream) throws IOException {
    return tryParseXml(new InputSource(inputStream));
  }

  @Nullable
  public static Document tryParseXml(final Reader reader) throws IOException {
    return tryParseXml(new InputSource(reader));
  }

  @Nullable
  public static Document tryParseXml(@NotNull final String string) throws IOException {
    return tryParseXml(new StringReader(string));
  }

  public static Document tryParseXml(final InputSource s) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();

      return db.parse(s);
    } catch (@NotNull final SAXException e) {
      return null;
    } catch (@NotNull final ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static DocumentFragment tryParseXmlFragment(final Reader reader) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document doc = db.parse(new InputSource(new CombiningReader(new StringReader("<elem>"), reader, new StringReader("</elem>"))));
      final DocumentFragment frag = doc.createDocumentFragment();
      final Element docelem = doc.getDocumentElement();
      for (Node child = docelem.getFirstChild(); child != null; child = docelem.getFirstChild()) {
        frag.appendChild(child);
      }
      doc.removeChild(docelem);
      return frag;
    } catch (@NotNull ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }

  }

  @NotNull
  public static QName asQName(@NotNull final Node reference, @NotNull final String name) {
    final int colPos = name.indexOf(':');
    if (colPos >= 0) {
      final String prefix = name.substring(0, colPos);
      return new QName(reference.lookupNamespaceURI(prefix), name.substring(colPos + 1), prefix);
    } else {
      return new QName(reference.lookupNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), name, XMLConstants.NULL_NS_URI);
    }

  }

  @NotNull
  public static QName asQName(@NotNull final NamespaceContext reference, @NotNull final String name) {
    final int colPos = name.indexOf(':');
    if (colPos >= 0) {
      final String prefix = name.substring(0, colPos);
      return new QName(reference.getNamespaceURI(prefix), name.substring(colPos + 1), prefix);
    } else {
      return new QName(reference.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), name, XMLConstants.NULL_NS_URI);
    }

  }

  @Nullable
  public static Element getChild(@NotNull final Element parent, @NotNull final QName name) {
    return getFirstChild(parent, name.getNamespaceURI(), name.getLocalPart());
  }

  public static Element getFirstChild(@NotNull final Element parent, @Nullable final String namespaceURI, final String localName) {
    for (Element child = getFirstChildElement(parent); child != null; child = getNextSiblingElement(child)) {
      if ((namespaceURI == null) || (namespaceURI.length() == 0)) {
        if (((child.getNamespaceURI() == null) || (child.getNamespaceURI().length() == 0))
                && StringUtil.isEqual(localName, child.getLocalName())) {
          return child;
        }
      } else {
        if (StringUtil.isEqual(namespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(localName, child.getLocalName())) {
          return child;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Element getNextSibling(@NotNull final Element sibling, @NotNull final QName name) {
    return getNextSibling(sibling, name.getNamespaceURI(), name.getLocalPart());
  }

  public static Element getNextSibling(@NotNull final Element sibling, final String namespaceURI, final String localName) {
    for (Element child = getNextSiblingElement(sibling); child != null; child = getNextSiblingElement(child)) {
      if (StringUtil.isEqual(namespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(localName, child.getLocalName())) {
        return child;
      }
    }
    return null;
  }

  public static String getQualifiedName(@NotNull final QName name) {
    final String prefix = name.getPrefix();
    if ((prefix == null) || (prefix == XMLConstants.NULL_NS_URI)) {
      return name.getLocalPart();
    }
    return prefix + ':' + name.getLocalPart();
  }

  /**
   * Return the first child that is an element.
   *
   * @param parent The parent element.
   * @return The first element child, or <code>null</code> if there is none.
   */
  public static Element getFirstChildElement(@NotNull final Element parent) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }

  /**
   * Return the next sibling that is an element.
   *
   * @param sibling The reference element.
   * @return The next element sibling, or <code>null</code> if there is none.
   */
  public static Element getNextSiblingElement(@NotNull final Element sibling) {
    for (Node child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }

  public static void serialize(final XmlSerializable serializable, final Writer writer) throws XmlException {
    XmlWriter out = XmlStreaming.newWriter(writer, true);
    serializable.serialize(out);
    out.close();
  }

  public static void serialize(final Node in, @NotNull final XMLStreamWriter out) throws XMLStreamException {
    serialize(new StAXSource(createXMLStreamReader(XMLInputFactory.newFactory(), new DOMSource(in))), out);
  }

  public static void serialize(@NotNull final StAXSource in, @NotNull final XMLStreamWriter out) throws
          XMLStreamException {
    serialize(createXMLEventReader(XMLInputFactory.newFactory(), in), createXMLEventWriter(XMLOutputFactory.newFactory(), out));
  }

  public static void serialize(final Node in, @NotNull final XmlWriter out) throws XmlException {
    serialize(XmlStreaming.newReader(new DOMSource(in)), out);
  }

  /**
   * Serialize the inputstream to the outputstream. Not that it will ignore document level events if the outputstream is not at depth 0
   * @param in The inputstream
   * @param out the outputsream
   * @throws XmlException
   */
  public static void serialize(@NotNull final XmlReader in, @NotNull final XmlWriter out) throws XmlException {
    while (in.hasNext()) {
      EventType eventType = in.next();
      if (eventType==null) { break; }
      switch (eventType) {
        case START_DOCUMENT:
        case PROCESSING_INSTRUCTION:
        case DOCDECL:
        case END_DOCUMENT:
          if (out.getDepth()>0) {
            break; // ignore
          } // otherwise fallthrough
        default:
          writeCurrentEvent(in, out);
      }
    }
  }

  public static void serialize(final XMLEventReader in, @NotNull final XMLEventWriter out) throws XMLStreamException {
    out.add(in);
  }

  /**
   * @deprecated  This goes throught transformer and DOM, as such avoid it.
   */
  @Deprecated
  public static void serialize(@NotNull final XMLStreamWriter out, final Source source) throws TransformerFactoryConfigurationError,
      XMLStreamException {
    if (source instanceof StAXSource) {
      serialize((StAXSource) source, out);
      return;
    }
    try {
      final Transformer transformer = TransformerFactory
              .newInstance()
              .newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(source, new StAXResult(out));

    } catch (@NotNull final TransformerException e) {
      throw new XMLStreamException(e);
    }
  }

  @NotNull
  public static <T extends XmlDeserializable> T deserializeHelper(@NotNull final T result, @NotNull final XmlReader in) throws
          XmlException {
    XmlUtil.skipPreamble(in);
    final QName elementName = result.getElementName();
    assert XmlUtil.isElement(in, elementName): "Expected "+elementName+" but found "+in.getLocalName();
    for(int i=in.getAttributeCount()-1; i>=0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    result.onBeforeDeserializeChildren(in);
    EventType event = null;
    if (result instanceof SimpleXmlDeserializable) {
      loop: while (in.hasNext() && event != XmlStreaming.END_ELEMENT) {
        switch ((event = in.next())) {
          case START_ELEMENT:
            if (((SimpleXmlDeserializable)result).deserializeChild(in)) {
              continue loop;
            }
            XmlUtil.unhandledEvent(in);
            break;
          case TEXT:
          case CDSECT:
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
        in.require(XmlStreaming.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
      }
    } else {// Neither, means ignore children
      if(! isXmlWhitespace(siblingsToFragment(in).getContent())) {
        throw new XmlException("Unexpected child content in element");
      }
    }
    return result;
  }

  public static <T> T deSerialize(final InputStream in, @NotNull final Class<T> type) throws XmlException {
    return deSerialize(XmlStreaming.newReader(in, "UTF-8"), type);
  }

  public static <T> T deSerialize(final Reader in, @NotNull final Class<T> type) throws XmlException {
    return deSerialize(XmlStreaming.newReader(in), type);
  }

  public static <T> T deSerialize(final Source in, @NotNull final Class<T> type) throws XmlException {
    return deSerialize(XmlStreaming.newReader(in), type);
  }

  public static <T> T deSerialize(final XmlReader in, @NotNull final Class<T> type) throws XmlException {
    final XmlDeserializer deserializer = type.getAnnotation(XmlDeserializer.class);
    if (deserializer==null) { throw new IllegalArgumentException("Types must be annotated with "+XmlDeserializer.class.getName()+" to be deserialized automatically"); }
    try {
      @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = deserializer.value().newInstance();
      return factory.deserialize((StAXReader) in);
    } catch (@NotNull InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
  public static String toString(final Node value) {
    return toString(value, DEFAULT_FLAGS);
  }

  public static String toString(final Node value, final int flags) {
    final StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      configure(t, flags);
      t.transform(new DOMSource(value), new StreamResult(out));
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(@NotNull final NodeList nodeList) {
    return toString(nodeList, DEFAULT_FLAGS);
  }

  public static String toString(@NotNull final NodeList nodeList, final int flags) {
    final StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      configure(t, flags);
      for(int i=0; i<nodeList.getLength(); ++i) {
        t.transform(new DOMSource(nodeList.item(i)), new StreamResult(out));
      }
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(@NotNull final XmlSerializable serializable) {
    final int flags = DEFAULT_FLAGS;
    return toString(serializable, flags);
  }

  public static CharSequence readSimpleElement(@NotNull final XmlReader in) throws XmlException {
    in.require(EventType.START_ELEMENT, null, null);
    EventType type;
    StringBuilder result = new StringBuilder();
    while ((type = in.next())!=EventType.END_ELEMENT) {
      switch (type) {
        case COMMENT:
        case PROCESSING_INSTRUCTION:
          /* Ignore */
          break;
        case TEXT:
        case CDSECT:
          result.append(in.getText());
          break;
        default:
          throw new XmlException("Expected text content or end tag, found: "+type);
      }
    }
    return result.toString(); // create a string to ensure some lifetime for the buffer
  }

  /**
   * Filter the stream such that is a valid substream. This basically strips start document, end document, processing
   * instructions and dtd declarations.
   * @param streamReader The original stream reader.
   * @return A filtered stream
   */
  public static XMLStreamReader filterSubstream(final XMLStreamReader streamReader) throws
          XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(streamReader, SubstreamStreamFilter.SUBSTREAM_FILTER);
  }

  /**
   * Filter the stream such that is a valid substream. This basically strips start document, end document, processing
   * instructions and dtd declarations.
   * @param streamReader The original stream reader.
   * @return A filtered stream
   */
  public static XmlReader filterSubstream(final XmlReader streamReader) {
    return new SubstreamFilter(streamReader);
  }

  public static XMLEventReader filterSubstream(final XMLEventReader xMLEventReader) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(xMLEventReader, SubstreamEventFilter.SUBEVENTS_FILTER);
  }

  public static XmlWriter filterSubstream(final XmlWriter streamWriter) {
    return stripMetatags(streamWriter);
  }

  @NotNull
  public static String xmlEncode(@NotNull final String unEncoded) {
    StringBuilder result = null;
    int last=0;
    for(int i=0; i<unEncoded.length(); ++i) {
      switch (unEncoded.charAt(i)) {
        case '<':
          if (result==null) { result = new StringBuilder(unEncoded.length()); }
          result.append(unEncoded,last, i).append("&lt;");
          last = i+1;
          break;
        case '&':
          if (result==null) { result = new StringBuilder(unEncoded.length()); }
          result.append(unEncoded,last, i).append("&amp;");
          last = i+1;
          break;
        default:
          break;
      }

    }
    if (result==null) { return unEncoded; }
    result.append(unEncoded, last, unEncoded.length());
    return result.toString();
  }

  public static void safeAdd(@NotNull final XMLEventWriter xew, @NotNull final XMLEventReader xer) throws XMLStreamException {
    while(xer.hasNext()) {
      safeAdd(xew, xer.nextEvent());
    }
  }

  public static void safeAdd(@NotNull final XMLEventWriter xew, @NotNull final XMLEvent event) throws XMLStreamException {
    if (event.isStartElement()) {
      final StartElement startElement = event.asStartElement();
      final Map<String,String> prefixes = new HashMap<>();
      prefixes.put(startElement.getName().getPrefix(), startElement.getName().getNamespaceURI());
      for(@SuppressWarnings("unchecked") final Iterator<Attribute> attrs = startElement.getAttributes(); attrs.hasNext();) {
        final Attribute attr = attrs.next();
        final String prefix = attr.getName().getPrefix();
        if(!(prefixes.containsKey(prefix)|| prefix.isEmpty())) {
          prefixes.put(prefix, attr.getName().getNamespaceURI());
        }
      }
      final List<javax.xml.stream.events.Namespace> namespaceDecls = new ArrayList<>();
      for(@SuppressWarnings("unchecked") final
          Iterator<javax.xml.stream.events.Namespace> namespaces = startElement.getNamespaces(); namespaces.hasNext(); ) {
        final javax.xml.stream.events.Namespace namespace = namespaces.next();
        namespaceDecls.add(namespace);
        prefixes.remove(namespace.getPrefix()); // Remove the prefixes locally declared
      }
      if (! prefixes.isEmpty()) {
        for (final Iterator<Entry<String, String>> prefixit = prefixes.entrySet().iterator(); prefixit.hasNext(); ) {
          final Entry<String, String> prefix = prefixit.next();
          if (Objects.equals(prefix.getValue(), xew.getNamespaceContext().getNamespaceURI(prefix.getKey()))) {
            prefixit.remove(); // No need to add the namespace
          }
        }
      }
      if (prefixes.isEmpty()) {
        xew.add(event);
      } else {
        final XMLEventFactory xef = XMLEventFactory.newFactory();
        for(final Entry<String, String> prefix:prefixes.entrySet()) {
          namespaceDecls.add(xef.createNamespace(prefix.getKey(), prefix.getValue()));
        }
        final StartElement newEvent = xef.createStartElement(startElement.getName(), startElement.getAttributes(), namespaceDecls.iterator());
        xew.add(newEvent);
      }
    } else {
      xew.add(event);
    }
  }

  @Nullable
  public static DocumentFragment toDocFragment(@Nullable final NodeList value) {
    if (value==null || value.getLength()==0) { return null; }
    final Document document = value.item(0).getOwnerDocument();
    final DocumentFragment fragment = document.createDocumentFragment();
    for(int i=0; i<value.getLength(); ++i) {
      final Node n = value.item(i);
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }

  private static String toString(@NotNull final XmlSerializable serializable, final int flags) {
    final StringWriter out =new StringWriter();
    final XMLOutputFactory factory = XMLOutputFactory.newInstance();
    configure(factory, flags);
    try {
      final XmlWriter serializer = XmlStreaming.newWriter(out);
      serializable.serialize(serializer);
      serializer.close();
    } catch (@NotNull final XmlException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static char[] toCharArray(final Source content) throws XMLStreamException {
    return toCharArrayWriter(content).toCharArray();
  }

  public static String toString(final Source source) throws XMLStreamException {
    return toCharArrayWriter(source).toString();
  }

  @NotNull
  private static CharArrayWriter toCharArrayWriter(final Source source) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    final XMLEventReader in = createXMLEventReader(xif, source);
    return toCharArrayWriter(in);
  }

  public static XMLEventReader createXMLEventReader(final XMLInputFactory xif, @NotNull final StAXSource source) throws
          XMLStreamException {
    final XMLEventReader eventReader = source.getXMLEventReader();
    if (eventReader !=null) { return eventReader; }
    return xif.createXMLEventReader(source.getXMLStreamReader());
  }

  public static XMLEventReader createXMLEventReader(@NotNull final XMLInputFactory xif, final Source source) throws
          XMLStreamException {
    if (source instanceof StAXSource) {
      return createXMLEventReader(xif, (StAXSource) source);
    }
    return xif.createXMLEventReader(source);
  }

  public static XMLStreamReader createXMLStreamReader(final XMLInputFactory xif, @NotNull final StAXSource source) throws
          XMLStreamException {
    final XMLStreamReader streamReader = source.getXMLStreamReader();
    if (streamReader !=null) { return streamReader; }
    return new XMLEventStreamReader(source.getXMLEventReader());
  }

  public static XMLStreamReader createXMLStreamReader(@NotNull final XMLInputFactory xif, final Source source) throws
          XMLStreamException {
    if (source instanceof StAXSource) {
      return createXMLStreamReader(xif, (StAXSource) source);
    }
    return xif.createXMLStreamReader(source);
  }

  public static XMLEventWriter createXMLEventWriter(@NotNull final XMLOutputFactory xof, final Result result) throws
          XMLStreamException {
    if (result instanceof StAXResult) {
      return createXMLEventWriter(xof, (StAXResult) result);
    }
    return xof.createXMLEventWriter(result);
  }

  public static XMLEventWriter createXMLEventWriter(final XMLOutputFactory xof, @NotNull final StAXResult result) throws
          XMLStreamException {
    final XMLEventWriter eventWriter = result.getXMLEventWriter();
    if (eventWriter!=null) { return eventWriter; }
    if (xof instanceof XMLOutputFactory2) {
      return ((XMLOutputFactory2) xof).createXMLEventWriter(result.getXMLStreamWriter());
    }
    return xof.createXMLEventWriter(result);
  }

  public static XMLEventWriter createXMLEventWriter(final XMLOutputFactory xof, @NotNull final XMLStreamWriter streamWriter) throws
          XMLStreamException {
    if (xof instanceof XMLOutputFactory2) {
      return ((XMLOutputFactory2) xof).createXMLEventWriter(streamWriter);
    }
    return xof.createXMLEventWriter(new StAXResult(streamWriter));
  }

  @NotNull
  private static CharArrayWriter toCharArrayWriter(final XMLEventReader in) throws XMLStreamException {
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    final CharArrayWriter caw = new CharArrayWriter();
    final XMLEventWriter xew = xof.createXMLEventWriter(caw);
    try {
      xew.add(in);
    } finally {
      xew.close();
    }
    return caw;
  }

  @NotNull
  public static XMLStreamWriter stripMetatags(final XMLStreamWriter out) {
    return new StAXMetaStripper(out);
  }

  @Deprecated
  @NotNull
  public static XmlWriter stripMetatags(final XmlWriter out) {
    return new MetaStripper(out);
  }

  public static void setAttribute(@NotNull final Element element, @NotNull final QName name, final String value) {
    if (name.getNamespaceURI()==null || XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI())) {
      element.setAttribute(name.getLocalPart(), value);
    } else if (name.getPrefix()==null || XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
      element.setAttributeNS(name.getNamespaceURI(),name.getLocalPart(), value);
    } else {
      element.setAttributeNS(name.getNamespaceURI(),name.getPrefix()+':'+name.getLocalPart(), value);
    }
  }

  public static DocumentFragment childrenToDocumentFragment(final XMLStreamReader in) throws XMLStreamException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XMLStreamException(e);
    }
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    final XMLEventReader xer = xif.createXMLEventReader(in);
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    final XMLEventWriter out = xof.createXMLEventWriter(new DOMResult(documentFragment));
    final XMLEventFactory xef = XMLEventFactory.newFactory();
    while (xer.hasNext() && (! xer.peek().isEndElement())) {
      final XMLEvent event = xer.nextEvent();
      out.add(event);
      if (event.isStartElement()) {
        writeElementContent(xer, out);
      }
    }
    return documentFragment;
  }

  public static DocumentFragment childrenToDocumentFragment(final XmlReader in) throws XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    XmlWriter out = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
    while (in.hasNext() && (in.next()!= EventType.END_ELEMENT)) {
      writeCurrentEvent(in, out);
      if (in.getEventType()== EventType.START_ELEMENT) {
        writeElementContent(null, in, out);
      }
    }
    return documentFragment;
  }


  public static Node childToNode(final XmlReader in) throws XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    XmlWriter out = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
    writeCurrentEvent(in, out);
    if (in.getEventType()== EventType.START_ELEMENT) {
      writeElementContent(null, in, out);
    }
    return documentFragment.getFirstChild();
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
  @NotNull
  public static CompactFragment siblingsToFragment(final XMLStreamReader in) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    final XMLEventReader xer = xif.createXMLEventReader(in);
    return siblingsToFragment(xer);
  }

  /**
   * Differs from {@link #siblingsToFragment(XmlReader)} in that it skips the current event.
   * @param in
   * @return
   * @throws XmlException
   */
  public static CompactFragment readerToFragment(final XmlReader in) throws XmlException {
    XmlUtil.skipPreamble(in);
    if (in.hasNext()) {
      in.require(EventType.START_ELEMENT, null, null);
      in.next();
      return siblingsToFragment(in);
    }
    return new CompactFragment("");
  }

  /**
   * Read the current element (and content) and all its siblings into a fragment.
   * @param in The source stream.
   * @return the fragment
   * @throws XmlException parsing failed
   */
  @NotNull
  public static CompactFragment siblingsToFragment(final XmlReader in) throws XmlException {
    CharArrayWriter caw = new CharArrayWriter();
    if (in.getEventType()==null && in.hasNext()) { in.next(); }

    final String startLocation = in.getLocationInfo();
    try {

      final TreeMap<String, String> missingNamespaces = new TreeMap<>();
      GatheringNamespaceContext gatheringContext = null;
      // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
      int initialDepth = in.getDepth() - (in.getEventType()==EventType.START_ELEMENT ? 1 : 0);
      for(EventType type = in.getEventType(); type!=XmlStreaming.END_DOCUMENT && type!=XmlStreaming.END_ELEMENT && in.getDepth()>=initialDepth; type = (in.hasNext()? in.next(): null)) {
        if (type==XmlStreaming.START_ELEMENT) {
          XmlWriter out = XmlStreaming.newWriter(caw);
          writeCurrentEvent(in, out); // writes the start tag
          for(String prefix: undeclaredPrefixes(in, out)) {
            if (! missingNamespaces.containsKey(prefix)) {
              missingNamespaces.put(prefix, in.getNamespaceUri(prefix));
            }
          }
          writeElementContent(missingNamespaces, in, out); // writes the children and end tag
          out.close();
        } else if (type==XmlStreaming.TEXT || type==XmlStreaming.IGNORABLE_WHITESPACE || type==XmlStreaming.CDATA) {
          caw.append(xmlEncode(in.getText().toString()));
        }
      }
      return new CompactFragment(new SimpleNamespaceContext(missingNamespaces), caw.toCharArray());
    } catch (@NotNull XmlException | RuntimeException e) {
      throw new XmlException("Failure to parse children into string at "+startLocation, e);
    }

  }

  private static List<String> undeclaredPrefixes(final XmlReader in, final XmlWriter reference) throws XmlException {
    assert in.getEventType()==XmlStreaming.START_ELEMENT;
    List<String> result = new ArrayList<>(2);
    String prefix = StringUtil.toString(in.getPrefix());
    if (prefix!=null) {
      CharSequence uri;
      if ((! prefix.isEmpty()) && ((uri=reference.getNamespaceUri(prefix))==null || (uri.length()==0 && prefix.length()>0))) {
        result.add(prefix);
      }
    }
    return result;
  }

  public static void writeCurrentEvent(final XmlReader in, final XmlWriter out) throws XmlException {
    switch (in.getEventType()) {
      case START_DOCUMENT:
        out.startDocument(null, in.getEncoding(), in.getStandalone());break;
      case START_ELEMENT: {
        out.startTag(in.getNamespaceUri(), in.getLocalName(), in.getPrefix());
        {
          int nsStart = in.getNamespaceStart();
          int nsEnd = in.getNamespaceEnd();
          for(int i=nsStart; i<nsEnd; ++i) {
            out.namespaceAttr(in.getNamespacePrefix(i), in.getNamespaceUri(i));
          }
        }
        {
          int attrCount = in.getAttributeCount();
          for(int i=0; i<attrCount; ++i) {
            out.attribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), null, in.getAttributeValue(i));
          }
        }
        break;
      }
      case END_ELEMENT:
        out.endTag(in.getNamespaceUri(), in.getLocalName(), in.getPrefix()); break;
      case COMMENT:
        out.comment(in.getText()); break;
      case TEXT:
        out.text(in.getText()); break;
      case ATTRIBUTE:
        out.attribute(in.getNamespaceUri(), in.getLocalName(),in.getPrefix(), in.getText()); break;
      case CDSECT:
        out.cdsect(in.getText()); break;
      case DOCDECL:
        out.docdecl(in.getText()); break;
      case END_DOCUMENT:
        out.endDocument(); break;
      case ENTITY_REF:
        out.entityRef(in.getText()); break;
      case IGNORABLE_WHITESPACE:
        out.ignorableWhitespace(in.getText()); break;
      case PROCESSING_INSTRUCTION:
        out.processingInstruction(in.getText()); break;
      default:
        throw new XmlException("Unsupported element found");
    }
  }

  @NotNull
  public static CompactFragment siblingsToFragment(@NotNull final XMLEventReader in) throws
          XMLStreamException {
    final Location startLocation = in.peek().getLocation();
    try {

      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
      final CharArrayWriter caw = new CharArrayWriter();

      final TreeMap<String, String> missingNamespaces = new TreeMap<>();
      GatheringNamespaceContext gatheringContext = null;
      while (in.hasNext() && (! in.peek().isEndElement())) {
        final XMLEvent event = in.nextEvent();
        if (event.isStartElement()) {
          if (gatheringContext==null) {
            gatheringContext = new GatheringNamespaceContext(event.asStartElement().getNamespaceContext(), missingNamespaces);
          }
          final XMLEventWriter out = xof.createXMLEventWriter(caw);
          out.setNamespaceContext(gatheringContext);
          out.add(event);
          out.getNamespaceContext().getNamespaceURI(event.asStartElement().getName().getPrefix());
          writeElementContent(in, out);
          out.close();
        } else if (event.isCharacters()) {
          event.writeAsEncodedUnicode(caw);
        }
      }
      return new CompactFragment(new SimpleNamespaceContext(missingNamespaces), caw.toCharArray());
    } catch (@NotNull XMLStreamException | RuntimeException e) {
      throw new XMLStreamException("Failure to parse children into string at "+startLocation, e);
    }
  }

  @NotNull
  public static CompactFragment nodeListToFragment(@NotNull final NodeList nodeList) throws XmlException {
    switch(nodeList.getLength()) {
      case 0:
        return new CompactFragment("");
      case 1:
        final Node node = nodeList.item(0);
        return nodeToFragment(node);
      default:
        return nodeToFragment(toDocFragment(nodeList));
    }
  }

  @NotNull
  public static CompactFragment nodeListToFragment(@NotNull final XMLInputFactory xif, @NotNull final NodeList nodeList) throws XMLStreamException {
    try {
      return nodeListToFragment(nodeList);
    } catch (XmlException e) {
      if (e.getCause() instanceof XMLStreamException) {
        throw (XMLStreamException) e.getCause();
      }
      throw new XMLStreamException(e);
    }
  }

  @NotNull
  public static CompactFragment nodeToFragment(final Node node) throws XmlException {
    if (node instanceof Text) {
      return new CompactFragment(((Text) node).getData());
    }
    return siblingsToFragment(XmlStreaming.newReader(new DOMSource(node)));
  }

  public static void unhandledEvent(@NotNull final XMLStreamReader in) throws XMLStreamException {
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

  public static void unhandledEvent(@NotNull final XmlReader in) throws XmlException {
    switch (in.getEventType()) {
      case CDSECT:
      case TEXT:
        if (!in.isWhitespace()) {
          throw new XmlException("Content found where not expected ["+in.getLocationInfo()+"] Text:'"+in.getText()+"'");
        }
        break;
      case COMMENT:
        break; // ignore
      case START_ELEMENT:
        throw new XmlException("Element found where not expected ["+in.getLocationInfo()+"]: "+in.getName());
      case END_DOCUMENT:
        throw new XmlException("End of document found where not expected");
    }
  }

  /**
   * Skil the preamble events in the stream reader
   * @param in The stream reader to skip
   */
  public static void skipPreamble(@NotNull final XmlReader in) throws XmlException {
    EventType type = in.getEventType();
    while (isIgnorable(type) && in.hasNext()) {
      type = in.next();
    }
  }

  @Deprecated
  public static boolean isStAXPreamble(final int type) {
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

  public static boolean isIgnorable(final EventType type) {
    if (type==null) { return true; } // Before start, means ignore the "current event"
    switch (type) {
      case COMMENT:
      case START_DOCUMENT:
      case END_DOCUMENT:
      case PROCESSING_INSTRUCTION:
      case DOCDECL:
      case IGNORABLE_WHITESPACE:
        return true;
      default:
        return false;
    }
  }

  public static void writeChild(final XmlWriter out, @Nullable final XmlSerializable child) throws XmlException {
    if (child!=null) {
      child.serialize(out);
    }
  }

  public static void writeChild(XmlWriter out, final Node in) throws XmlException {
    serialize(in, out);
  }

  public static void writeChildren(final XmlWriter out, @Nullable final Iterable<? extends XmlSerializable> children) throws
          XmlException {
    if (children!=null) {
      for (final XmlSerializable child : children) {
        writeChild(out, child);
      }
    }
  }

  public static void writeStartElement(@NotNull final XmlWriter out, @NotNull final QName qName) throws XmlException {
    boolean writeNs = false;
    String namespace = qName.getNamespaceURI();
    CharSequence prefix;
    if (namespace==null) {
      namespace = out.getNamespaceContext().getNamespaceURI(qName.getPrefix());
      prefix = qName.getPrefix();
    } else {
      prefix = out.getPrefix(namespace);
      if (prefix==null) { // The namespace is not know in the output context, so add an attribute
        writeNs = true;
        prefix = qName.getPrefix();
      }
    }
    out.startTag(namespace, qName.getLocalPart(), prefix);
    if (writeNs) {
      out.namespaceAttr(prefix, namespace);
    }
  }

/* XXX These can't work because they don't allow for attributes
  public static void writeEmptyElement(@NotNull final StAXWriter out, @NotNull final QName qName) throws XMLStreamException {
    String namespace = qName.getNamespaceURI();
    String prefix;
    if (namespace==null) {
      namespace = out.getNamespaceContext().getNamespaceURI(qName.getPrefix());
      prefix = qName.getPrefix();
    } else {
      prefix = out.getPrefix(namespace);
      if (prefix==null) { prefix = qName.getPrefix(); }
    }
    out.writeEmptyElement(prefix, qName.getLocalPart(), namespace);
  }
*/

  public static void writeSimpleElement(@NotNull final XmlWriter out, @NotNull final QName qName, @Nullable final CharSequence value) throws
        XmlException {
    writeStartElement(out, qName);
    if (value!=null && value.length()!=0) {
      out.text(value);
    }
    writeEndElement(out, qName);
  }

  public static void writeAttribute(@NotNull final XmlWriter out, final String name, @Nullable final String value) throws XmlException {
    if (value!=null) {
      out.attribute(null, name, null, value);
    }
  }

  public static void writeAttribute(@NotNull final XmlWriter out, final QName name, @Nullable final String value) throws XmlException {
    if (value!=null) {
      out.attribute(name.getNamespaceURI(), name.getLocalPart(), name.getPrefix(), value);
    }
  }

  public static void writeAttribute(@NotNull final XmlWriter out, final String name, final double value) throws XmlException {
    if (! Double.isNaN(value)) {
      out.attribute(null, name, null, Double.toString(value));
    }
  }

  public static void writeAttribute(@NotNull final XmlWriter out, final String name, final long value) throws XmlException {
    if (! Double.isNaN(value)) {
      out.attribute(null, name, null, Long.toString(value));
    }
  }

  public static void writeAttribute(@NotNull final XmlWriter out, final String name, @Nullable final QName value) throws
          XMLStreamException, XmlException {
    if (value!=null) {
      String prefix;
      if (value.getNamespaceURI()!=null) {
        if (value.getPrefix()!=null && value.getNamespaceURI().equals(out.getNamespaceContext().getNamespaceURI(value.getPrefix()))) {
          prefix = value.getPrefix();
        } else {
          prefix = out.getNamespaceContext().getPrefix(value.getNamespaceURI());
          if (prefix == null) {
            prefix = value.getPrefix();
            out.namespaceAttr(prefix, value.getNamespaceURI());
          }
        }
      } else {
        prefix = value.getPrefix();
        final String ns = out.getNamespaceContext().getNamespaceURI(prefix);
        if (ns==null) { throw new IllegalArgumentException("Cannot determine namespace of qname"); }
      }
      out.attribute(null, name, null, prefix+':'+value.getLocalPart());
    }
  }

  private static boolean nullOrEmpty(@Nullable final CharSequence s) {
    return s==null || s.length()==0;
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementname The name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */

  public static boolean isElement(@NotNull final XMLStreamReader in, @NotNull final QName elementname) {
    return isElement(in, elementname.getNamespaceURI(), elementname.getLocalPart(), elementname.getPrefix());
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementname The name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */

  public static boolean isElement(@NotNull final XmlReader in, @NotNull final QName elementname) throws XmlException {
    return isElement(in, elementname.getNamespaceURI(), elementname.getLocalPart(), elementname.getPrefix());
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XMLStreamReader in, final String elementNamespace, final String elementName) {
    final String elementPrefix = null;
    return isElement(in, elementNamespace, elementName, elementPrefix);
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against   @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XmlReader in, final CharSequence elementNamespace, final CharSequence elementName) throws
          XmlException {
    final String elementPrefix = null;
    return isElement(in, elementNamespace, elementName, elementPrefix);
  }


  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against
   * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XMLStreamReader in, final String elementNamespace, final String elementName, @NotNull final String elementPrefix) {
    if (in.getEventType()!= XMLStreamConstants.START_ELEMENT) { return false; }
    String expNs =  elementNamespace;
    if ("".equals(expNs)) { expNs = null; }
    if (! in.getLocalName().equals(elementName)) { return false; }

    if (nullOrEmpty(elementNamespace)) {
      if (nullOrEmpty(elementPrefix)) {
        return nullOrEmpty(in.getPrefix());
      } else {
        return elementPrefix.equals(in.getPrefix());
      }
    } else {
      return expNs.equals(in.getNamespaceURI());
    }
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against
   * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XmlReader in, final CharSequence elementNamespace, final CharSequence elementName, @NotNull final CharSequence elementPrefix) throws
          XmlException {
    if (in.getEventType()!= XmlStreaming.START_ELEMENT) { return false; }
    CharSequence expNs =  elementNamespace;
    if (expNs!=null && expNs.length()==0) { expNs = null; }
    if (! in.getLocalName().equals(elementName)) { return false; }

    if (nullOrEmpty(elementNamespace)) {
      if (nullOrEmpty(elementPrefix)) {
        return nullOrEmpty(in.getPrefix());
      } else {
        return elementPrefix.equals(in.getPrefix());
      }
    } else {
      return StringUtil.isEqual(expNs,in.getNamespaceUri());
    }
  }

  private static void writeElementContent(@NotNull final XMLEventReader in, @NotNull final XMLEventWriter out) throws XMLStreamException {
    while (in.hasNext()) {
      final XMLEvent event = in.nextEvent();
      out.add(event);
      if (event.isStartElement()) {
        out.getNamespaceContext().getNamespaceURI(event.asStartElement().getName().getPrefix());
        writeElementContent(in, out);
      }
      if (event.isEndElement()) {
        break;
      }
    }
  }

  private static void writeElementContent(@NotNull final XmlReader in, @NotNull final XmlWriter out) throws
                                                                                                                                                            XmlException {
    writeElementContent(new HashMap<String, String>(), in, out);
  }

  private static void writeElementContent(@Nullable final Map<String, String> missingNamespaces, @NotNull final XmlReader in, @NotNull final XmlWriter out) throws
          XmlException {
    while (in.hasNext()) {
      EventType type = in.next();
      writeCurrentEvent(in, out);
      if (type== EventType.START_ELEMENT) {
        if (missingNamespaces!=null) {
          for (String prefix : undeclaredPrefixes(in, out)) {
            if (!missingNamespaces.containsKey(prefix)) {
              missingNamespaces.put(prefix, in.getNamespaceUri(prefix));
            }
          }
        }
        writeElementContent(missingNamespaces, in, out);
      } else if (type == EventType.END_ELEMENT) {
        break;
      }
    }
  }

  private static void configure(@NotNull final Transformer transformer, final int flags) {
    if ((flags & OMIT_XMLDECL)!=0) {
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
  }

  private static void configure(final XMLOutputFactory factory, final int flags) {
    // Nothing to configure for now
  }

  public static boolean isXmlWhitespace(@NotNull final char[] data) {
    for(int i=data.length-1; i>=0; --i) {
      final char c = data[i];
      if (!(c==0xA || c==0x9 || c==0xd || c==' ')) {
        return false;
      }
    }
    return true;
  }


  public static boolean isXmlWhitespace(@NotNull final CharSequence data) {
    for(int i=data.length()-1; i>=0; --i) {
      final char c = data.charAt(i);
      if (!(c==0xA || c==0x9 || c==0xd || c==' ')) {
        return false;
      }
    }
    return true;
  }

  public static void cannonicallize(final Source in, final Result out) throws XMLStreamException {
    // TODO add wrapper methods that get stream readers and writers analogous to the event writers and readers
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLStreamReader xsr = xif.createXMLStreamReader(in);
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    final XMLStreamWriter xsw = xof.createXMLStreamWriter(out);
    final Map<String, NamespaceInfo> collectedNS = new HashMap<>();

    while (xsr.hasNext()) {
      final int type=xsr.next();
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
      final int type = xsr.next();
      switch (type) { // TODO extract the default elements to a separate method that is also used to copy StreamReader to StreamWriter without events.
        case XMLStreamConstants.START_ELEMENT:
          {
            if (first) {
              NamespaceInfo namespaceInfo = collectedNS.get(xsr.getNamespaceURI());
              if (namespaceInfo != null) {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(xsr.getPrefix())) {
                  namespaceInfo = new NamespaceInfo("", namespaceInfo.mUrl);
                }
                xsw.setPrefix(namespaceInfo.mPrefix, namespaceInfo.mUrl);
                xsw.writeStartElement(namespaceInfo.mPrefix, xsr.getLocalName(), namespaceInfo.mUrl);
              } else { // no namespace info (probably no namespace at all)
                xsw.writeStartElement(xsr.getPrefix(), xsr.getLocalName(), xsr.getNamespaceURI());
              }
              first = false;
              for (final NamespaceInfo ns : collectedNS.values()) {
                xsw.setPrefix(ns.mPrefix, ns.mUrl);
                xsw.writeNamespace(ns.mPrefix, ns.mUrl);
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

  public static Node cannonicallize(final Node content) throws ParserConfigurationException,
          XMLStreamException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();

    if (content instanceof DocumentFragment) {
      final DocumentFragment df = (DocumentFragment) content;
      final DocumentFragment result = db.newDocument().createDocumentFragment();
      final DOMResult dr = new DOMResult(result);
      for(Node child=df.getFirstChild(); child!=null; child=child.getNextSibling()) {
        cannonicallize(new DOMSource(child), dr);
      }
      return result;
    } else {
      final Document result = db.newDocument();
      cannonicallize(new DOMSource(content), new DOMResult(result));
      return result.getDocumentElement();
    }
  }

  private static void addNamespace(@NotNull final Map<String, NamespaceInfo> collectedNS, final String prefix, @Nullable final String namespaceURI) {
    if (! (namespaceURI==null || XMLConstants.NULL_NS_URI.equals(namespaceURI))) {
      NamespaceInfo nsInfo = collectedNS.get(namespaceURI);
      if (nsInfo==null) {
        collectedNS.put(namespaceURI, new NamespaceInfo(prefix, namespaceURI));
      } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(nsInfo.mPrefix)) {
        nsInfo=new NamespaceInfo(prefix, nsInfo.mUrl);
      }
    }
  }

  @Nullable
  public static DocumentFragment toDocFragment(@Nullable final List<Node> value) {
    if (value==null || value.size()==0) { return null; }
    final Document document = value.get(0).getOwnerDocument();
    final DocumentFragment fragment = document.createDocumentFragment();
    for(final Node n: value) {
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }
}
