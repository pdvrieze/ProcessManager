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
import nl.adaptivity.util.CombiningReader;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlEvent.TextEvent;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.Contract;
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
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.*;


/**
 * Utility class that contains a lot of functionality to handle xml.
 */
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

  /**
   * A class that filters an xml stream such that it will only
   */
  private static class SubstreamFilter extends XmlBufferedReader {

    public SubstreamFilter(final XmlReader delegate) {
      super(delegate);
    }

    @NotNull
    @Override
    protected List<XmlEvent> doPeek() throws XmlException {
      final List<XmlEvent> events = super.doPeek();
      for (final Iterator<XmlEvent> it = events.iterator(); it.hasNext();) {
        final XmlEvent event = it.next();
        final EventType eventType = event.getEventType();
        switch (eventType) {
          case START_DOCUMENT:
          case PROCESSING_INSTRUCTION:
          case DOCDECL:
          case END_DOCUMENT:
            it.remove();
            break;
          default:
            // other events should remain
        }
      }
      return events;
    }
  }

  /** Flag to indicate that the xml declaration should be omitted, when possible. */
  public static final int FLAG_OMIT_XMLDECL = 1;
  private static final int DEFAULT_FLAGS = FLAG_OMIT_XMLDECL;


  private XmlUtil() { /* Utility class is not constructible. */ }

// Object Initialization
  /**
   * Create an {@link Element} with the given name. Depending on the prefix, and namespace it uses the "correct"
   * approach, with null namespace or prefix, or specified namespace and prefix.
   *
   * @param document  The owning document.
   * @param qName The name of the element.
   */
  public static Element createElement(@NotNull final Document document, @NotNull final QName qName) {
    final Element root;
    if (XMLConstants.NULL_NS_URI.equals(qName.getNamespaceURI()) || null == qName.getNamespaceURI()) {
      root = document.createElement(qName.getLocalPart());
    } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(qName.getPrefix())) {
      root = document.createElementNS(qName.getNamespaceURI(), qName.getLocalPart());
    } else {
      root = document.createElementNS(qName.getNamespaceURI(), qName.getPrefix() + ':' + qName.getLocalPart());
    }
    return root;
  }
// Object Initialization end

  /**
   * Get the next text sequence in the reader. This will skip over comments and ignorable whitespace, but not tags.
   *
   * @param in The reader to read from.
   * @return   The text found
   * @throws XmlException If reading breaks, or an unexpected element was found.
   */
  public static CharSequence nextText(final XmlReader in) throws XmlException {
    EventType type;
    final StringBuilder result = new StringBuilder();
    while ((type=in.next())!=EventType.END_ELEMENT) {
      switch (type) {
        case COMMENT: break; //ignore
        case IGNORABLE_WHITESPACE:
          if (result.length()==0) { break; } // ignore whitespace starting the element.
          //noinspection fallthrough
        case TEXT:
        case CDSECT:
          result.append(in.getText());
          break;
        default:
          throw new XmlException("Found unexpected child tag");
      }

    }
    return result;
  }

  /**
   * XPath processing does require either a document or a fragment to actually work. This method will
   * make this work. If the node is either that will be returned, otherwise, if it is the root node of the owner document,
   * the owner document is returned. Otherwise, a fragment will be created with a clone of the node.
   * @param node The node to attach if needed.
   * @return A document or documentfragment representing the given node (it may be a clone though)
   */
  public static Node ensureAttached(final Node node) {
    if (node==null) { return null; }
    if (node instanceof Document || node instanceof DocumentFragment) {
      return node;
    }
    if (node.isSameNode(node.getOwnerDocument().getDocumentElement())) {
      return node.getOwnerDocument();
    }
    final DocumentFragment frag = node.getOwnerDocument().createDocumentFragment();
    frag.appendChild(node.cloneNode(true));
    return frag;
  }

  public static boolean isAttached(final Node node) {
    if (node instanceof Document || node instanceof DocumentFragment) {
      return true;
    }
    final Node docElem = node.getOwnerDocument().getDocumentElement();
    if (docElem!=null) {
      for (Node curNode = node; curNode != null; curNode = curNode.getParentNode()) {
        if (docElem.isSameNode(curNode)) { return true; }
      }
    }
    return false;
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
      final NamedNodeMap attrs = node.getAttributes();
      for (int i=0; i<attrs.getLength(); ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()) && attr.getValue().equals(namespaceURI)) {
          return attr.getName();
        }
      }
    }
    final String prefix = getPrefix(node.getParentNode(), namespaceURI);
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
    final CharArrayWriter buffer = new CharArrayWriter();
    final XmlWriter writer = XmlStreaming.newWriter(buffer);
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
  public static Document tryParseXml(@NotNull final String xmlString) throws IOException {
    return tryParseXml(new StringReader(xmlString));
  }

  public static Document tryParseXml(final InputSource xmlSource) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();

      return db.parse(xmlSource);
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

  /**
   * Make a QName for the given parameters.
   * @param reference The node to use to look up the namespace that corresponds to the prefix.
   * @param name This is the full name of the element. That includes the prefix (or if no colon present) the default prefix.
   * @return The QName.
   */
  @NotNull
  @Contract(pure = true)
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
    if ((prefix == null) || XMLConstants.NULL_NS_URI.equals(prefix)) {
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
    final XmlWriter out = XmlStreaming.newWriter(writer, true);
    serializable.serialize(out);
    out.close();
  }

  public static void serialize(final Node in, @NotNull final XmlWriter out) throws XmlException {
    serialize(XmlStreaming.newReader(new DOMSource(in)), out);
  }

  /**
   * Serialize the inputstream to the outputstream. Not that it will ignore document level events if the outputstream is not at depth 0
   * @param in The inputstream
   * @param out the outputsream
   * @throws XmlException When serialization fails.
   */
  public static void serialize(@NotNull final XmlReader in, @NotNull final XmlWriter out) throws XmlException {
    while (in.hasNext()) {
      final EventType eventType = in.next();
      if (eventType==null) { break; }
      switch (eventType) {
        case START_DOCUMENT:
        case PROCESSING_INSTRUCTION:
        case DOCDECL:
        case END_DOCUMENT:
          if (out.getDepth()>0) {
            break; // ignore
          }
          // otherwise fall through
        default:
          writeCurrentEvent(in, out);
      }
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
            // If the text was not deserialized, then just fall through
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

  public static <T> T deSerialize(final String in, @NotNull final Class<T> type) throws XmlException {
    return deSerialize(XmlStreaming.newReader(new StringReader(in)), type);
  }

  /**
   * Utility method to deserialize a list of xml containing strings
   * @param in The strings to deserialize
   * @param type The type that contains the factory to deserialize
   * @param <T> The type
   * @return A list of deserialized objects.
   * @throws XmlException If deserialization fails anywhere.
   */
  public static <T> List<T> deSerialize(final Iterable<String> in, @NotNull final Class<T> type) throws XmlException {
    ArrayList<T> result = (in instanceof Collection) ? new ArrayList<T>(((Collection) in).size()): new ArrayList<T>();
    final XmlDeserializer deserializer = type.getAnnotation(XmlDeserializer.class);
    if (deserializer==null) { throw new IllegalArgumentException("Types must be annotated with "+XmlDeserializer.class.getName()+" to be deserialized automatically"); }
    @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory;
    try {
      factory = deserializer.value().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    for (String string : in) {
      result.add(factory.deserialize(XmlStreaming.newReader(new StringReader(string))));
    }
    return result;
  }

  public static <T> T deSerialize(final Source in, @NotNull final Class<T> type) throws XmlException {
    return deSerialize(XmlStreaming.newReader(in), type);
  }

  public static <T> T deSerialize(final XmlReader in, @NotNull final Class<T> type) throws XmlException {
    final XmlDeserializer deserializer = type.getAnnotation(XmlDeserializer.class);
    if (deserializer==null) { throw new IllegalArgumentException("Types must be annotated with "+XmlDeserializer.class.getName()+" to be deserialized automatically"); }
    try {
      @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = deserializer.value().newInstance();
      return factory.deserialize(in);
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

  /**
   * Do bulk toString conversion of a list. Note that this is serialization, not dropping tags.
   * @param serializables The source list.
   * @return A result list
   */
  @Contract(pure=true)
  public static @NotNull ArrayList<String> toString(@NotNull final Iterable<? extends XmlSerializable> serializables) {
    final int flags = DEFAULT_FLAGS;

    final ArrayList<String> result;
    if (serializables instanceof Collection) {
      result = new ArrayList<>(((Collection)serializables).size());
    } else {
      result = new ArrayList<>();
    }
    for (final XmlSerializable serializable : serializables) {
      result.add(toString(serializable));
    }
    return result;
  }

  public static CharSequence readSimpleElement(@NotNull final XmlReader in) throws XmlException {
    in.require(EventType.START_ELEMENT, null, null);
    EventType type;
    final StringBuilder result = new StringBuilder();
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
  public static XmlReader filterSubstream(final XmlReader streamReader) {
    return new SubstreamFilter(streamReader);
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
    try {
      final XmlWriter serializer = XmlStreaming.newWriter(out);
      serializable.serialize(serializer);
      serializer.close();
    } catch (@NotNull final XmlException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static char[] toCharArray(final Source content) throws XmlException {
    return toCharArrayWriter(content).toCharArray();
  }

  public static String toString(final Source source) throws XmlException {
    return toCharArrayWriter(source).toString();
  }

  @NotNull
  private static CharArrayWriter toCharArrayWriter(final Source source) throws XmlException {
    return toCharArrayWriter(XmlStreaming.newReader(source));
  }

  @NotNull
  private static CharArrayWriter toCharArrayWriter(final XmlReader in) throws XmlException {
    final CharArrayWriter caw = new CharArrayWriter();
    XmlWriter out = XmlStreaming.newWriter(caw);
    try {
      while (in.hasNext()) {
        XmlUtil.writeCurrentEvent(in, out);
      }
    } finally {
      out.close();
    }
    return caw;
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

  public static DocumentFragment childrenToDocumentFragment(final XmlReader in) throws XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    final XmlWriter out = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
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
    final Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    final XmlWriter out = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
    writeCurrentEvent(in, out);
    if (in.getEventType()== EventType.START_ELEMENT) {
      writeElementContent(null, in, out);
    }
    return documentFragment.getFirstChild();
  }


  public static char[] siblingsToCharArray(final XmlReader in) throws XmlException {
    return siblingsToFragment(in).getContent();
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
    final CharArrayWriter caw = new CharArrayWriter();
    if (in.getEventType()==null && in.hasNext()) { in.next(); }

    final String startLocation = in.getLocationInfo();
    try {

      final TreeMap<String, String> missingNamespaces = new TreeMap<>();
      final GatheringNamespaceContext gatheringContext = null;
      // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
      final int initialDepth = in.getDepth() - (in.getEventType() == EventType.START_ELEMENT ? 1 : 0);
      for(EventType type = in.getEventType(); type!=XmlStreaming.END_DOCUMENT && type!=XmlStreaming.END_ELEMENT && in.getDepth()>=initialDepth; type = (in.hasNext()? in.next(): null)) {
        if (type==XmlStreaming.START_ELEMENT) {
          final XmlWriter out = XmlStreaming.newWriter(caw);
          writeCurrentEvent(in, out); // writes the start tag
          addUndeclaredNamespaces(in, out, missingNamespaces);
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

  public static void addUndeclaredNamespaces(final XmlReader in, final XmlWriter out, final Map<String, String> missingNamespaces) throws XmlException {
    undeclaredPrefixes(in, out, missingNamespaces);
  }

  private static void undeclaredPrefixes(final XmlReader in, final XmlWriter reference, final Map<String, String> missingNamespaces) throws XmlException {
    assert in.getEventType()==XmlStreaming.START_ELEMENT;
    final String prefix = StringUtil.toString(in.getPrefix());
    if (prefix!=null) {
      if (!missingNamespaces.containsKey(prefix)) {
        final CharSequence uri = in.getNamespaceUri();
        if (StringUtil.isEqual(reference.getNamespaceUri(prefix), uri) && isPrefixDeclaredInElement(in, prefix)) {
          return;
        } else if (uri.length()>0) {
          if (! StringUtil.isEqual(reference.getNamespaceUri(prefix), uri)) {
            missingNamespaces.put(prefix, uri.toString());
          }
        }
      }
    }
  }

  private static boolean isPrefixDeclaredInElement(final XmlReader in, final String prefix) throws XmlException {
    for (int i = in.getNamespaceStart(); i < in.getNamespaceEnd(); i++) {
      if (StringUtil.isEqual(in.getNamespacePrefix(i), prefix)) { return true; }
    }
    return false;
  }

  public static void writeCurrentEvent(final XmlReader in, final XmlWriter out) throws XmlException {
    switch (in.getEventType()) {
      case START_DOCUMENT:
        out.startDocument(null, in.getEncoding(), in.getStandalone());break;
      case START_ELEMENT: {
        out.startTag(in.getNamespaceUri(), in.getLocalName(), in.getPrefix());
        {
          final int nsStart = in.getNamespaceStart();
          final int nsEnd = in.getNamespaceEnd();
          for(int i=nsStart; i<nsEnd; ++i) {
            out.namespaceAttr(in.getNamespacePrefix(i), in.getNamespaceUri(i));
          }
        }
        {
          final int attrCount = in.getAttributeCount();
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
  public static CompactFragment nodeToFragment(final Node node) throws XmlException {
    if (node instanceof Text) {
      return new CompactFragment(((Text) node).getData());
    }
    return siblingsToFragment(XmlStreaming.newReader(new DOMSource(node)));
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
    while (isIgnorable(in) && in.hasNext()) {
      in.next();
    }
  }

  public static boolean isIgnorable(final XmlReader in) throws XmlException {
    final EventType type = in.getEventType();
    if (type==null) { return true; } // Before start, means ignore the "current event"
    switch (type) {
      case COMMENT:
      case START_DOCUMENT:
      case END_DOCUMENT:
      case PROCESSING_INSTRUCTION:
      case DOCDECL:
      case IGNORABLE_WHITESPACE:
        return true;
      case TEXT:
        return isXmlWhitespace(in.getText());
      default:
        return false;
    }
  }

  public static boolean isIgnorable(final XmlEvent event) {
    final EventType type = event.getEventType();
    if (type==null) { return true; } // Before start, means ignore the "current event"
    switch (type) {
      case COMMENT:
      case START_DOCUMENT:
      case END_DOCUMENT:
      case PROCESSING_INSTRUCTION:
      case DOCDECL:
      case IGNORABLE_WHITESPACE:
        return true;
      case TEXT:
        return isXmlWhitespace(((TextEvent) event).text);
      default:
        return false;
    }
  }

  public static void writeChild(final XmlWriter out, @Nullable final XmlSerializable child) throws XmlException {
    if (child!=null) {
      child.serialize(out);
    }
  }

  public static void writeChild(final XmlWriter out, final Node in) throws XmlException {
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

  public static void writeAttribute(final XmlWriter out, final String name, @Nullable final Object value) throws XmlException {
    if (value!=null) {
      out.attribute(null, name, null, value.toString());
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
          XmlException {
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

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementname The name to check against
   * @return <code>true</code> if it matches, otherwise <code>false</code>
   */

  public static boolean isElement(@NotNull final XmlReader in, @NotNull final QName elementname) throws XmlException {
    return isElement(in, EventType.START_ELEMENT, elementname.getNamespaceURI(), elementname.getLocalPart(), elementname
            .getPrefix());
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param type
   *@param elementname The name to check against  @return <code>true</code> if it matches, otherwise <code>false</code>
   */

  public static boolean isElement(@NotNull final XmlReader in, final EventType type, @NotNull final QName elementname) throws XmlException {
    return isElement(in, type, elementname.getNamespaceURI(), elementname.getLocalPart(), elementname.getPrefix());
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against   @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XmlReader in, final CharSequence elementNamespace, final CharSequence elementName) throws
          XmlException {
    return isElement(in, EventType.START_ELEMENT, elementNamespace, elementName, null);
  }

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param type The type to verify. Should be named so start or end element
   * @param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against   @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XmlReader in, final EventType type, final CharSequence elementNamespace, final CharSequence elementName) throws
          XmlException {
    return isElement(in, type, elementNamespace, elementName, null);
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
          XmlException {return isElement(in, EventType.START_ELEMENT, elementNamespace, elementName, elementPrefix);}

  /**
   * Check that the current state is a start element for the given name. The mPrefix is ignored.
   * @param in The stream reader to check
   * @param type The type to verify. Should be named so start or end element
   *@param elementNamespace  The namespace to check against.
   * @param elementName The local name to check against
   * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined    @return <code>true</code> if it matches, otherwise <code>false</code>
   */
  public static boolean isElement(@NotNull final XmlReader in, final EventType type, final CharSequence elementNamespace, final CharSequence elementName, @NotNull final CharSequence elementPrefix) throws
          XmlException {
    if (in.getEventType()!= type) { return false; }
    CharSequence expNs =  elementNamespace;
    if (expNs!=null && expNs.length()==0) { expNs = null; }
    if (! in.getLocalName().equals(elementName)) { return false; }

    if (StringUtil.isNullOrEmpty(elementNamespace)) {
      if (StringUtil.isNullOrEmpty(elementPrefix)) {
        return StringUtil.isNullOrEmpty(in.getPrefix());
      } else {
        return elementPrefix.equals(in.getPrefix());
      }
    } else {
      return StringUtil.isEqual(expNs,in.getNamespaceUri());
    }
  }

  private static void writeElementContent(@NotNull final XmlReader in, @NotNull final XmlWriter out) throws
                                                                                                                                                            XmlException {
    writeElementContent(new HashMap<String, String>(), in, out);
  }

  private static void writeElementContent(@Nullable final Map<String, String> missingNamespaces, @NotNull final XmlReader in, @NotNull final XmlWriter out) throws
          XmlException {
    while (in.hasNext()) {
      final EventType type = in.next();
      writeCurrentEvent(in, out);
      if (type== EventType.START_ELEMENT) {
        if (missingNamespaces!=null) {
          addUndeclaredNamespaces(in, out, missingNamespaces);
        }
        writeElementContent(missingNamespaces, in, out);
      } else if (type == EventType.END_ELEMENT) {
        break;
      }
    }
  }

  private static void configure(@NotNull final Transformer transformer, final int flags) {
    if ((flags & FLAG_OMIT_XMLDECL) != 0) {
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
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

  public static void cannonicallize(final Source in, final Result out) throws XmlException {
    // TODO add wrapper methods that get stream readers and writers analogous to the event writers and readers
    XmlReader xsr = XmlStreaming.newReader(in);
    final XmlWriter xsw = XmlStreaming.newWriter(out, true);
    final Map<String, NamespaceInfo> collectedNS = new HashMap<>();

    while (xsr.hasNext()) {
      final EventType type=xsr.next();
      switch (type) {
        case START_ELEMENT:
//          if (xsr.getNamespaceCount()>0) {
//            for(int i=0; i<xsr.getNamespaceCount(); ++i) {
//              addNamespace(collectedNS, xsr.getNamespacePrefix(i), xsr.getNamespaceURI(i));
//            }
//          }
          addNamespace(collectedNS, xsr.getPrefix().toString(), xsr.getNamespaceUri().toString());
          for(int i=xsr.getAttributeCount()-1; i>=0; --i) {
            addNamespace(collectedNS, xsr.getAttributePrefix(i).toString(), xsr.getAttributeNamespace(i).toString());
          }
        default:
          // ignore
      }
    }

    xsr = XmlStreaming.newReader(in);

    boolean first = true;
    while (xsr.hasNext()) {
      final EventType type = xsr.next();
      switch (type) { // TODO extract the default elements to a separate method that is also used to copy StreamReader to StreamWriter without events.
        case START_ELEMENT:
          {
            if (first) {
              NamespaceInfo namespaceInfo = collectedNS.get(xsr.getNamespaceUri());
              if (namespaceInfo != null) {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(xsr.getPrefix())) {
                  namespaceInfo = new NamespaceInfo("", namespaceInfo.mUrl);
                }
                xsw.setPrefix(namespaceInfo.mPrefix, namespaceInfo.mUrl);
                xsw.startTag(namespaceInfo.mPrefix, xsr.getLocalName().toString(), namespaceInfo.mUrl);
              } else { // no namespace info (probably no namespace at all)
                xsw.startTag(xsr.getPrefix(), xsr.getLocalName(), xsr.getNamespaceUri());
              }
              first = false;
              for (final NamespaceInfo ns : collectedNS.values()) {
                xsw.setPrefix(ns.mPrefix, ns.mUrl);
                xsw.namespaceAttr(ns.mPrefix, ns.mUrl);
              }
            } else {
              xsw.startTag(xsr.getNamespaceUri(), xsr.getLocalName(), null);
            }
            final int ac = xsr.getAttributeCount();
            for (int i = 0; i<ac; ++i) {
              xsw.attribute(xsr.getAttributeNamespace(i),xsr.getAttributeLocalName(i), null, xsr.getAttributeValue(i));
            }
            break;
          }
        case ATTRIBUTE:
          xsw.attribute(xsr.getNamespaceUri(),xsr.getLocalName(), null, xsr.getText());
          break;
        case END_ELEMENT:
          xsw.endTag(null, null, null);
          break;
        case TEXT:
          xsw.text(xsr.getText());
          break;
        case IGNORABLE_WHITESPACE:
          xsw.ignorableWhitespace(xsr.getText());
          break;
        case CDSECT:
          xsw.cdsect(xsr.getText());
          break;
        case COMMENT:
          xsw.comment(xsr.getText());
          break;
        case START_DOCUMENT:
          xsw.startDocument(xsr.getEncoding(), xsr.getVersion(), xsr.getStandalone());
          break;
        case END_DOCUMENT:
          xsw.endDocument();
          break;
        case PROCESSING_INSTRUCTION:
          xsw.processingInstruction(xsr.getText());
          break;
        case ENTITY_REF:
          xsw.entityRef(xsr.getText());
          break;
        case DOCDECL:
          xsw.docdecl(xsr.getText());
          break;
      }
    }
    xsw.close();
    xsr.close();
  }

  public static Node cannonicallize(final Node content) throws ParserConfigurationException,
          XmlException {
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
