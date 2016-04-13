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

package nl.adaptivity.util.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.xml.*;
import org.codehaus.stax2.XMLOutputFactory2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;


/**
 * Utility class that contains a lot of functionality to handle xml.
 */
public final class StaxXmlUtil {

  private static class NamespaceInfo {

    final String mPrefix;
    final String mUrl;

    public NamespaceInfo(final String prefix, final String url) {
      this.mPrefix = prefix;
      this.mUrl = url;

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

  /** Flag to indicate that the xml declaration should be omitted, when possible. */
  public static final int FLAG_OMIT_XMLDECL = 1;
  private static final int DEFAULT_FLAGS = FLAG_OMIT_XMLDECL;


  private StaxXmlUtil() { /* Utility class is not constructible. */ }

// Object Initialization
  // Object Initialization end

  public static void serialize(final Node in, @NotNull final XMLStreamWriter out) throws XMLStreamException {
    serialize(new StAXSource(createXMLStreamReader(XMLInputFactory.newFactory(), new DOMSource(in))), out);
  }

  public static void serialize(@NotNull final StAXSource in, @NotNull final XMLStreamWriter out) throws
          XMLStreamException {
    serialize(createXMLEventReader(XMLInputFactory.newFactory(), in), createXMLEventWriter(XMLOutputFactory.newFactory(), out));
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

  /**
   * Filter the stream such that is a valid substream. This basically strips start document, end document, processing
   * instructions and dtd declarations.
   *
   * @param streamReader The original stream reader.
   * @return A filtered stream
   * @throws XMLStreamReader
   * @deprecated Usage of {@link XMLStreamReader} is deprecated over {@link XmlReader}
   */
  public static XMLStreamReader filterSubstream(final XMLStreamReader streamReader) throws
          XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(streamReader, SubstreamStreamFilter.SUBSTREAM_FILTER);
  }

  /**
   * Filter out all meta events like start doc, end doc, document declratations and processing instructions.
   * @param xMLEventReader
   * @return
   * @throws XMLStreamException
   * @deprecated Usage of {@link XMLStreamReader} is deprecated over {@link XmlReader}
   */
  public static XMLEventReader filterSubstream(final XMLEventReader xMLEventReader) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createFilteredReader(xMLEventReader, SubstreamEventFilter.SUBEVENTS_FILTER);
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

  public static XMLStreamReader createXMLStreamReader(final XMLInputFactory xif, @NotNull final StAXSource source) {
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

  public static DocumentFragment childrenToDocumentFragment(final XMLStreamReader in) throws XMLStreamException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document doc;
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
      return new CompactFragment(new nl.adaptivity.xml.SimpleNamespaceContext(missingNamespaces), caw.toCharArray());
    } catch (@NotNull XMLStreamException | RuntimeException e) {
      throw new XMLStreamException("Failure to parse children into string at "+startLocation, e);
    }
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
   * @param elementName The local name to check against
   * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined    @return <code>true</code> if it matches, otherwise <code>false</code>
   * */
  public static boolean isElement(@NotNull final XMLStreamReader in, final String elementNamespace, final String elementName, @NotNull final String elementPrefix) {
    if (in.getEventType()!= XMLStreamConstants.START_ELEMENT) { return false; }
    String expNs =  elementNamespace;
    if ("".equals(expNs)) { expNs = null; }
    if (! in.getLocalName().equals(elementName)) { return false; }

    if (StringUtil.isNullOrEmpty(elementNamespace)) {
      if (StringUtil.isNullOrEmpty(elementPrefix)) {
        return StringUtil.isNullOrEmpty(in.getPrefix());
      } else {
        return elementPrefix.equals(in.getPrefix());
      }
    } else {
      return expNs.equals(in.getNamespaceURI());
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

  private static void configure(@NotNull final Transformer transformer, final int flags) {
    if ((flags & FLAG_OMIT_XMLDECL) != 0) {
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
  }

  private static void configure(final XMLOutputFactory factory, final int flags) {
    // Nothing to configure for now
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

}
