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
import nl.adaptivity.xml.XmlEvent.TextEvent;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

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
    public void startDocument(@Nullable final CharSequence version, @Nullable final CharSequence encoding, @Nullable final Boolean standalone) throws XmlException {
      /* ignore */
    }
  }

  /** Flag to indicate that the xml declaration should be omitted, when possible. */
  public static final int FLAG_OMIT_XMLDECL = 1;
  private static final int DEFAULT_FLAGS = FLAG_OMIT_XMLDECL;


  private XmlUtil() { /* Utility class is not constructible. */ }

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

  public static String getQualifiedName(@NotNull final QName name) {
    final String prefix = name.getPrefix();
    if ((prefix == null) || XMLConstants.NULL_NS_URI.equals(prefix)) {
      return name.getLocalPart();
    }
    return prefix + ':' + name.getLocalPart();
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
    AbstractXmlReader.skipPreamble(in);
    final QName elementName = result.getElementName();
    assert AbstractXmlReader.isElement(in, elementName) : "Expected " + elementName + " but found " + in.getLocalName();
    for(int i=in.getAttributeCount()-1; i>=0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    result.onBeforeDeserializeChildren(in);
    EventType event = null;
    if (result instanceof SimpleXmlDeserializable) {
      loop: while (in.hasNext() && event != XmlStreamingKt.END_ELEMENT) {
        switch ((event = in.next())) {
          case START_ELEMENT:
            if (((SimpleXmlDeserializable)result).deserializeChild(in)) {
              continue loop;
            }
            AbstractXmlReader.unhandledEvent(in);
            break;
          case TEXT:
          case CDSECT:
            if (((SimpleXmlDeserializable)result).deserializeChildText(in.getText())) {
              continue loop;
            }
            // If the text was not deserialized, then just fall through
          default:
            AbstractXmlReader.unhandledEvent(in);
        }
      }
    } else if (result instanceof ExtXmlDeserializable){
      ((ExtXmlDeserializable)result).deserializeChildren(in);
      if (XmlUtil.class.desiredAssertionStatus()) {
        in.require(XmlStreamingKt.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
      }
    } else {// Neither, means ignore children
      if(! isXmlWhitespace(AbstractXmlReader.siblingsToFragment(in).getContent())) {
        throw new XmlException("Unexpected child content in element");
      }
    }
    return result;
  }

  public static <T> T deSerialize(final InputStream in, @NotNull final Class<T> type) throws XmlException {
    return AbstractXmlReader.deSerialize(XmlStreaming.newReader(in, "UTF-8"), type);
  }

  public static <T> T deSerialize(final Reader in, @NotNull final Class<T> type) throws XmlException {
    return AbstractXmlReader.deSerialize(XmlStreaming.newReader(in), type);
  }

  public static <T> T deSerialize(final String in, @NotNull final Class<T> type) throws XmlException {
    return AbstractXmlReader.deSerialize(XmlStreaming.newReader(new StringReader(in)), type);
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
    final XmlDeserializerFactory<T> factory;
    try {
      //noinspection unchecked
      factory = (XmlDeserializerFactory<T>) deserializer.value().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    for (String string : in) {
      result.add(factory.deserialize(XmlStreaming.newReader(new StringReader(string))));
    }
    return result;
  }

  public static <T> T deSerialize(final Source in, @NotNull final Class<T> type) throws XmlException {
    return AbstractXmlReader.deSerialize(XmlStreaming.newReader(in), type);
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
    return AbstractXmlReader.toCharArrayWriter(XmlStreaming.newReader(source));
  }

  @Deprecated
  @NotNull
  public static XmlWriter stripMetatags(final XmlWriter out) {
    return new MetaStripper(out);
  }


  public static char[] siblingsToCharArray(final XmlReader in) throws XmlException {
    return AbstractXmlReader.siblingsToFragment(in).getContent();
  }

  public static void addUndeclaredNamespaces(final XmlReader in, final XmlWriter out, final Map<String, String> missingNamespaces) throws XmlException {
    undeclaredPrefixes(in, out, missingNamespaces);
  }

  private static void undeclaredPrefixes(final XmlReader in, final XmlWriter reference, final Map<String, String> missingNamespaces) throws XmlException {
    assert in.getEventType()==XmlStreamingKt.START_ELEMENT;
    final String prefix = StringUtil.toString(in.getPrefix());
    if (prefix!=null) {
      if (!missingNamespaces.containsKey(prefix)) {
        final CharSequence uri = in.getNamespaceUri();
        if (StringUtil.isEqual(reference.getNamespaceUri(prefix), uri) && AbstractXmlReader.isPrefixDeclaredInElement(in, prefix)) {
          return;
        } else if (uri.length()>0) {
          if (! StringUtil.isEqual(reference.getNamespaceUri(prefix), uri)) {
            missingNamespaces.put(prefix, uri.toString());
          }
        }
      }
    }
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
        return isXmlWhitespace(((TextEvent) event).getText());
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


  private static void writeElementContent(@NotNull final XmlReader in, @NotNull final XmlWriter out) throws
                                                                                                                                                            XmlException {
    writeElementContent(new HashMap<String, String>(), in, out);
  }

  /**
   * TODO make it package protected once XmlUtil is moved.
   * @deprecated should be more private
   */
  @Deprecated
  public static void writeElementContent(@Nullable final Map<String, String> missingNamespaces, @NotNull final XmlReader in, @NotNull final XmlWriter out) throws
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

  static void configure(@NotNull final Transformer transformer, final int flags) {
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
