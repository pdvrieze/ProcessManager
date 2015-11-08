package nl.adaptivity.util.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.events.Namespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by pdvrieze on 08/11/15.
 */
public class XMLEventStreamReader implements XMLStreamReader {

  private final XMLEventReader source;
  private XMLEvent event;
  private List<Attribute> attributes;
  private List<Namespace> namespaces;

  public XMLEventStreamReader(final XMLEventReader pSource) throws XMLStreamException {
    source = pSource;
    event = null;
    attributes = null;
    namespaces = null;
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return source.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    event = source.nextEvent();
    attributes = null;
    namespaces = null;
    return event.getEventType();
  }

  @Override
  public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
    boolean valid = true;
    if (event.getEventType()!= type) {
      valid = false;
    } else {
      if (namespaceURI!=null || localName!=null) {
        QName eventName = getEventName(event);
        if (namespaceURI!=null && (! namespaceURI.equals(eventName.getNamespaceURI()))) {
          valid = false;
        }
        if (valid && localName!=null && (! localName.equals(eventName.getLocalPart()))) {
          valid = false;
        }
      }
    }
    if (!valid) {
      throw new XMLStreamException("Requirements not met");
    }
  }

  private static QName getEventName(final XMLEvent pEvent) {
    if (pEvent.isStartElement()) { return pEvent.asStartElement().getName(); }
    else if (pEvent.isAttribute()) { return ((Attribute) pEvent).getName(); }
    else if (pEvent.isEndElement()) { return pEvent.asEndElement().getName(); }
    throw new IllegalStateException("The given event does not have a name");
  }

  @Override
  public String getElementText() throws XMLStreamException {
    if(! event.isStartElement()) {
      throw new XMLStreamException(
              "parser must be on START_ELEMENT to read next text", getLocation());
    }
    int eventType = next();
    StringBuilder content = new StringBuilder();
    while (source.hasNext() && !(event=source.nextEvent()).isEndElement()) {
      if (event.isCharacters()) {
        content.append(event.asCharacters().getData());
      } else if (event.isEntityReference()) {
        content.append(((EntityReference) event).getDeclaration().getReplacementText());
      } else //noinspection StatementWithEmptyBody
        if (event.isProcessingInstruction() || event.getEventType()== XMLStreamConstants.COMMENT) {
        //skip
      } else if (event.isEndDocument()) {
        throw new XMLStreamException("unexpected end of document when reading element text content", event.getLocation());
      } else if(event.isStartElement()) {
        throw new XMLStreamException("element text content may not contain START_ELEMENT", event.getLocation());
      } else {
        throw new XMLStreamException("Unexpected event type "+eventType, getLocation());
      }
    }
    return content.toString();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    int result;
    do {
      result = next();
    } while (result==XMLStreamConstants.COMMENT || (result==CHARACTERS && isWhiteSpace())|| (result==PROCESSING_INSTRUCTION));
    if (result!=START_ELEMENT && result!=END_ELEMENT) {
      throw new XMLStreamException("Unexpected event while searching next tag",event.getLocation());
    }
    return result;
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return source.hasNext();
  }

  @Override
  public void close() throws XMLStreamException {
    source.close();
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    return event.asStartElement().getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return event.isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return event.isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return event.isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return event.isCharacters() && event.asCharacters().isWhiteSpace();
  }

  private void cacheAttrs() {
    if (attributes==null) {
      ArrayList<Attribute> attrs = new ArrayList<>();
      for (@SuppressWarnings("unchecked") Iterator<Attribute> attributeIterator = event.asStartElement().getAttributes(); attributeIterator.hasNext(); ) {
        attrs.add(attributeIterator.next());
      }
      attributes = attrs;
    }
  }

  @Override
  public String getAttributeValue(final String namespaceURI, final String localName) {
    cacheAttrs();
    for(Attribute candidate: attributes) {
      QName name = candidate.getName();
      if ((namespaceURI==null || namespaceURI.equals(name.getNamespaceURI())) && localName.equals(name.getLocalPart())) {
        return candidate.getValue();
      }
    }
    return null;
  }

  @Override
  public int getAttributeCount() {
    cacheAttrs();
    return attributes.size();
  }

  @Override
  public QName getAttributeName(final int index) {
    cacheAttrs();
    return attributes.get(index).getName();
  }

  @Override
  public String getAttributeNamespace(final int index) {
    return getAttributeName(index).getNamespaceURI();
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return getAttributeName(index).getLocalPart();
  }

  @Override
  public String getAttributePrefix(final int index) {
    return getAttributeName(index).getPrefix();
  }

  @Override
  public String getAttributeType(final int index) {
    cacheAttrs();
    return attributes.get(index).getDTDType();
  }

  @Override
  public String getAttributeValue(final int index) {
    cacheAttrs();
    return attributes.get(index).getValue();
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    cacheAttrs();
    return attributes.get(index).isSpecified();
  }

  private void cacheNamespaces() {
    List<javax.xml.stream.events.Namespace> collector = new ArrayList<>();
    for(@SuppressWarnings("unchecked") Iterator<Namespace> namespaceIterator = event.asStartElement().getNamespaces(); namespaceIterator.hasNext();) {
      collector.add(namespaceIterator.next());
    }
    namespaces = collector;
  }

  @Override
  public int getNamespaceCount() {
    cacheNamespaces();
    return namespaces.size();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    cacheNamespaces();
    return namespaces.get(index).getPrefix();
  }

  @Override
  public String getNamespaceURI(final int index) {
    cacheNamespaces();
    return namespaces.get(index).getNamespaceURI();
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return event.asStartElement().getNamespaceContext();
  }

  @Override
  public int getEventType() {
    return event.getEventType();
  }

  @Override
  public String getText() {
    return event.asCharacters().getData();
  }

  @Override
  public char[] getTextCharacters() {
    return event.asCharacters().getData().toCharArray();
  }

  @Override
  public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    String text = getText();
    // TODO be more flexible in fitting in the target buffer
    text.getChars(sourceStart, sourceStart+length, target, targetStart);
    return length;
  }

  @Override
  public int getTextStart() {
    return 0;
  }

  @Override
  public int getTextLength() {
    return event.asCharacters().getData().length();
  }

  @Override
  public String getEncoding() {
    return null;
  }

  @Override
  public boolean hasText() {
    return event.isCharacters() || event.getEventType()==XMLStreamConstants.DTD || event.isEntityReference();
  }

  @Override
  public Location getLocation() {
    return event.getLocation();
  }

  @Override
  public QName getName() {
    if (event.isStartElement()) {
      return event.asStartElement().getName();
    }
    if (event.isEndElement()) {
      return event.asEndElement().getName();
    }
    return null;
  }

  @Override
  public String getLocalName() {
    return getName().getLocalPart();
  }

  @Override
  public boolean hasName() {
    return event.isStartElement()||event.isEndElement();
  }

  @Override
  public String getNamespaceURI() {
    return getName().getNamespaceURI();
  }

  @Override
  public String getPrefix() {
    return getName().getPrefix();
  }

  @Override
  public String getVersion() {
    return ((StartDocument) event).getVersion();
  }

  @Override
  public boolean isStandalone() {
    return ((StartDocument) event).isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return ((StartDocument) event).standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return ((StartDocument) event).getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return ((ProcessingInstruction) event).getTarget();
  }

  @Override
  public String getPIData() {
    return ((ProcessingInstruction) event).getData();
  }
}
