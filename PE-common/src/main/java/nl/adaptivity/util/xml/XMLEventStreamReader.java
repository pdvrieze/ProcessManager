package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private final XMLEventReader mSource;
  @Nullable private XMLEvent mEvent;
  @Nullable private List<Attribute> mAttributes;
  @Nullable private List<Namespace> mNamespaces;

  public XMLEventStreamReader(final XMLEventReader source) {
    mSource = source;
    mEvent = null;
    mAttributes = null;
    mNamespaces = null;
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return mSource.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    if (! mSource.hasNext()) { throw new IllegalStateException("Beyond end of underlying stream"); }
    mEvent = mSource.nextEvent();
    mAttributes = null;
    mNamespaces = null;
    return getEvent().getEventType();
  }

  @Override
  public void require(final int type, @Nullable final String namespaceURI, @Nullable final String localName) throws XMLStreamException {
    XMLEvent event = getEvent();
    boolean valid = true;
    if (event.getEventType()!= type) {
      valid = false;
    } else {
      if (namespaceURI!=null || localName!=null) {
        final QName eventName = getEventName(event);
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

  private static QName getEventName(@NotNull final XMLEvent event) {
    if (event.isStartElement()) { return event.asStartElement().getName(); }
    else if (event.isAttribute()) { return ((Attribute) event).getName(); }
    else if (event.isEndElement()) { return event.asEndElement().getName(); }
    throw new IllegalStateException("The given mEvent does not have a name");
  }

  @NotNull
  @Override
  public String getElementText() throws XMLStreamException {
    XMLEvent event = getEvent();
    if (event==null) { throw new IllegalStateException("Not in a valid state of the stream"); }
    if(! event.isStartElement()) {
      throw new XMLStreamException(
              "parser must be on START_ELEMENT to read next text", getLocation());
    }
    final int eventType = next();
    final StringBuilder content = new StringBuilder();
    while (mSource.hasNext() && !(event = mSource.nextEvent()).isEndElement()) {
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
        throw new XMLStreamException("Unexpected mEvent type "+eventType, getLocation());
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
      throw new XMLStreamException("Unexpected mEvent while searching next tag", getEvent() ==null ? null : getEvent().getLocation());
    }
    return result;
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return mSource.hasNext();
  }

  @Override
  public void close() throws XMLStreamException {
    mSource.close();
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    return getEvent().asStartElement().getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return getEvent().isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return getEvent().isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return getEvent().isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return getEvent().isCharacters() && getEvent().asCharacters().isWhiteSpace();
  }

  private void cacheAttrs() {
    if (mAttributes ==null) {
      final ArrayList<Attribute> attrs = new ArrayList<>();
      for (@SuppressWarnings("unchecked") final Iterator<Attribute> attributeIterator = getEvent().asStartElement().getAttributes(); attributeIterator.hasNext(); ) {
        attrs.add(attributeIterator.next());
      }
      mAttributes = attrs;
    }
  }

  @Nullable
  @Override
  public String getAttributeValue(@Nullable final String namespaceURI, @NotNull final String localName) {
    cacheAttrs();
    for(final Attribute candidate: mAttributes) {
      final QName name = candidate.getName();
      if ((namespaceURI==null || namespaceURI.equals(name.getNamespaceURI())) && localName.equals(name.getLocalPart())) {
        return candidate.getValue();
      }
    }
    return null;
  }

  @Override
  public int getAttributeCount() {
    cacheAttrs();
    return mAttributes.size();
  }

  @Override
  public QName getAttributeName(final int index) {
    cacheAttrs();
    return mAttributes.get(index).getName();
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
    return mAttributes.get(index).getDTDType();
  }

  @Override
  public String getAttributeValue(final int index) {
    cacheAttrs();
    return mAttributes.get(index).getValue();
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    cacheAttrs();
    return mAttributes.get(index).isSpecified();
  }

  private void cacheNamespaces() {
    final List<javax.xml.stream.events.Namespace> collector = new ArrayList<>();
    for(@SuppressWarnings("unchecked") final Iterator<Namespace> namespaceIterator = getEvent().asStartElement().getNamespaces(); namespaceIterator.hasNext();) {
      collector.add(namespaceIterator.next());
    }
    mNamespaces = collector;
  }

  @Override
  public int getNamespaceCount() {
    cacheNamespaces();
    return mNamespaces.size();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    cacheNamespaces();
    return mNamespaces.get(index).getPrefix();
  }

  @Override
  public String getNamespaceURI(final int index) {
    cacheNamespaces();
    return mNamespaces.get(index).getNamespaceURI();
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return getEvent().asStartElement().getNamespaceContext();
  }

  @Override
  public int getEventType() {
    return getEvent().getEventType();
  }

  @Override
  public String getText() {
    return getEvent().asCharacters().getData();
  }

  @NotNull
  @Override
  public char[] getTextCharacters() {
    return getEvent().asCharacters().getData().toCharArray();
  }

  @Override
  public int getTextCharacters(final int sourceStart, @NotNull final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    final String text = getText();
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
    return getEvent().asCharacters().getData().length();
  }

  @Nullable
  @Override
  public String getEncoding() {
    return null;
  }

  @Override
  public boolean hasText() {
    return getEvent().isCharacters() || getEvent().getEventType()==XMLStreamConstants.DTD || getEvent().isEntityReference();
  }

  @Override
  public Location getLocation() {
    return getEvent().getLocation();
  }

  @Nullable
  @Override
  public QName getName() {
    final XMLEvent event= getEvent();
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
    return getEvent().isStartElement()|| getEvent().isEndElement();
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
    return ((StartDocument) getEvent()).getVersion();
  }

  @Override
  public boolean isStandalone() {
    return ((StartDocument) getEvent()).isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return ((StartDocument) getEvent()).standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return ((StartDocument) getEvent()).getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return ((ProcessingInstruction) getEvent()).getTarget();
  }

  @Override
  public String getPIData() {
    return ((ProcessingInstruction) getEvent()).getData();
  }

  @NotNull
  public XMLEvent getEvent() {
    if (mEvent==null) { throw new IllegalStateException("Not in a valid state of the stream"); }
    return mEvent;
  }
}
