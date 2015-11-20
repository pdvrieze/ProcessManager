package nl.adaptivity.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.Namespace;
import nl.adaptivity.xml.XmlStreaming.EventType;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by pdvrieze on 16/11/15.
 */
public abstract class XmlEvent {

  public static class TextEvent extends XmlEvent {

    public final EventType eventType;
    public final CharSequence text;

    public TextEvent(final String locationInfo, final EventType eventType, final CharSequence text) {
      super(locationInfo);
      this.eventType = eventType;
      this.text = text;
    }

    @Override
    public EventType getEventType() {
      return eventType;
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      switch (eventType) {
        case CDSECT: writer.cdsect(text); break;
        case COMMENT: writer.comment(text); break;
        case DOCDECL: writer.docdecl(text); break;
        case ENTITY_REF: writer.entityRef(text); break;
        case IGNORABLE_WHITESPACE: writer.ignorableWhitespace(text); break;
        case PROCESSING_INSTRUCTION: writer.processingInstruction(text); break;
        case TEXT: writer.text(text);
      }
    }
  }

  public static class EndDocumentEvent extends XmlEvent {

    public EndDocumentEvent(final String locationInfo) {
      super(locationInfo);
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      writer.endDocument();
    }

    @Override
    public EventType getEventType() {
      return EventType.END_DOCUMENT;
    }
  }

  public static class EndElementEvent extends NamedEvent {

    public EndElementEvent(final String locationInfo, final CharSequence namespaceUri, final CharSequence localName, final CharSequence prefix) {
      super(locationInfo, namespaceUri, localName, prefix);
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      writer.endTag(namespaceUri, localName, prefix);
    }

    @Override
    public EventType getEventType() {
      return EventType.END_ELEMENT;
    }
  }

  public static class StartDocumentEvent extends XmlEvent {

    public CharSequence version;
    public CharSequence encoding;
    public Boolean standalone;

    public StartDocumentEvent(final String locationInfo, final CharSequence version, final CharSequence encoding, final Boolean standalone) {
      super(locationInfo);
      this.version = version;
      this.encoding = encoding;
      this.standalone = standalone;
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      writer.startDocument(version, encoding, standalone);
    }

    @Override
    public EventType getEventType() {
      return EventType.START_DOCUMENT;
    }
  }

  private abstract static class NamedEvent extends XmlEvent {
    public final CharSequence namespaceUri;
    public final CharSequence localName;
    public final CharSequence prefix;

    public NamedEvent(final String locationInfo, final CharSequence namespaceUri, final CharSequence localName, final CharSequence prefix) {
      super(locationInfo);
      this.namespaceUri = namespaceUri;
      this.localName = localName;
      this.prefix = prefix;
    }

    public boolean isEqualNames(final NamedEvent ev) {
      return StringUtil.isEqual(namespaceUri, ev.namespaceUri) &&
              StringUtil.isEqual(localName, ev.localName) &&
              StringUtil.isEqual(prefix, ev.prefix);
    }

  }

  public static class StartElementEvent extends NamedEvent implements NamespaceContext {

    public final Attribute[] attributes;
    public final Namespace[] namespaceDecls;

    public StartElementEvent(final String locationInfo, final CharSequence namespaceUri, final CharSequence localName, final CharSequence prefix, final Attribute[] attributes, final Namespace[] namespaceDecls) {
      super(locationInfo, namespaceUri, localName, prefix);
      this.attributes = attributes;
      this.namespaceDecls = namespaceDecls;
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      writer.startTag(namespaceUri, localName, prefix);
      for(Attribute attr: attributes) {
        writer.attribute(attr.namespace, attr.localName, attr.prefix, attr.value);
      }
      for(Namespace ns: namespaceDecls) {
        writer.namespaceAttr(ns.getPrefix(), ns.getNamespaceURI());
      }
    }

    @Override
    public EventType getEventType() {
      return EventType.START_ELEMENT;
    }

    @Override
    public String getPrefix(final String namespaceURI) {
      return getPrefix((CharSequence) namespaceURI);
    }

    public String getPrefix(final CharSequence namespaceUri) {
      for(Namespace ns: namespaceDecls) {
        if (StringUtil.isEqual(ns.getNamespaceURI(), namespaceUri)) {
          return ns.getPrefix();
        }
      }
      return null;
    }

    @Override
    public String getNamespaceURI(final String prefix) {
      return getNamespaceUri(prefix);
    }

    public String getNamespaceUri(final CharSequence prefix) {
      for(Namespace ns: namespaceDecls) {
        if (StringUtil.isEqual(ns.getPrefix(), prefix)) {
          return ns.getNamespaceURI();
        }
      }
      return null;
    }

    public NamespaceContext getNamespaceContext() {
      return this;
    }

    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
      ArrayList<String> result = new ArrayList<>(namespaceDecls.length);
      for(Namespace ns: namespaceDecls) {
        if (StringUtil.isEqual(ns.getNamespaceURI(), namespaceUri)) {
          result.add(ns.getPrefix());
        }
      }
      return result.iterator();
    }
  }

  public static class Attribute extends XmlEvent {

    public final CharSequence namespace;
    public final CharSequence localName;
    public final CharSequence prefix;
    public final CharSequence value;

    public Attribute(final String locationInfo, final CharSequence namespace, final CharSequence localName, final CharSequence prefix, final CharSequence value) {
      super(locationInfo);
      this.namespace = namespace;
      this.localName = localName;
      this.prefix = prefix;
      this.value = value;
    }

    @Override
    public EventType getEventType() {
      return EventType.ATTRIBUTE;
    }

    @Override
    public void writeTo(final XmlWriter writer) throws XmlException {
      writer.attribute(namespace, localName, prefix, value);
    }

    public boolean isNamespace() {
      return StringUtil.isEqual(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, namespace) ||
              (prefix.length()==0 && StringUtil.isEqual(XMLConstants.XMLNS_ATTRIBUTE, localName)) ;
    }
  }

  private static class NamespaceImpl implements Namespace {

    private final String mPrefix;
    private final String mNamespaceUri;

    public NamespaceImpl(final CharSequence namespacePrefix, final CharSequence namespaceUri) {
      mPrefix = namespacePrefix.toString();
      mNamespaceUri = namespaceUri.toString();
    }

    @Override
    public String getPrefix() {
      return mPrefix;
    }

    @Override
    public String getNamespaceURI() {
      return mNamespaceUri;
    }
  }

  private final String mLocationInfo;

  private XmlEvent(String locationInfo) {
    mLocationInfo = locationInfo;
  }

  public abstract EventType getEventType();

  public String getLocationInfo() {
    return mLocationInfo;
  }

  public static XmlEvent from(final XmlReader reader) throws XmlException {
    EventType eventType = reader.getEventType();

    final String locationInfo = reader.getLocationInfo();
    switch (eventType) {
      case CDSECT:
      case COMMENT:
      case DOCDECL:
      case ENTITY_REF:
      case IGNORABLE_WHITESPACE:
      case PROCESSING_INSTRUCTION:
      case TEXT:
        return new TextEvent(locationInfo, eventType, reader.getText());
      case END_DOCUMENT:
        return new EndDocumentEvent(locationInfo);
      case END_ELEMENT:
        return new EndElementEvent(locationInfo, reader.getNamespaceUri(), reader.getLocalName(), reader.getPrefix());
      case START_DOCUMENT:
        return new StartDocumentEvent(locationInfo, reader.getVersion(), reader.getEncoding(), reader.getStandalone());
      case START_ELEMENT:
        return new StartElementEvent(locationInfo, reader.getNamespaceUri(), reader.getLocalName(), reader.getPrefix(), getAttributes(reader), getNamespaceDecls(reader));
    }
    throw new IllegalStateException("This should not be reachable");
  }

  private static Namespace[] getNamespaceDecls(final XmlReader reader) throws XmlException {
    Namespace[] namespaces = new Namespace[reader.getNamespaceEnd()-reader.getNamespaceStart()];
    for(int i=namespaces.length-1; i>=0; --i) {
      namespaces[i] = new NamespaceImpl(reader.getNamespacePrefix(i), reader.getNamespaceUri(i));
    }
    return namespaces;
  }

  private static Attribute[] getAttributes(final XmlReader reader) throws XmlException {
    Attribute[] result = new Attribute[reader.getAttributeCount()];
    for(int i=0; i<result.length; ++i) {
      result[i] = new Attribute(reader.getLocationInfo(), reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributePrefix(i), reader.getAttributeValue(i));
    }
    return result;
  }

  public abstract void writeTo(XmlWriter writer) throws XmlException;
}
