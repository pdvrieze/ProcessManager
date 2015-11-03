package nl.adaptivity.util.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by pdvrieze on 31/10/15.
 */
public class NamespaceAddingStreamReader implements XMLStreamReader {

  private final XMLStreamReader source;
  private final NamespaceContext lookupSource;

  public NamespaceAddingStreamReader(final NamespaceContext pLookupSource, final XMLStreamReader pSource) {
    source = pSource;
    lookupSource = pLookupSource;
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return source.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    return source.next();
  }

  @Override
  public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
    if (type != getEventType() ||
            (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) ||
            (localName != null && !localName.equals(getLocalName()))) {
      source.require(type, namespaceURI, localName);
    } {
      throw new XMLStreamException("Require failed");
    }
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return source.getElementText();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return source.nextTag();
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
    String namespaceURI = source.getNamespaceURI(prefix);
    return namespaceURI != null ? namespaceURI : lookupSource.getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return source.isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return source.isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return source.isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return source.isWhiteSpace();
  }

  @Override
  public String getAttributeValue(final String namespaceURI, final String localName) {
    for(int i=getAttributeCount()-1; i>=0; --i) {
      if ((namespaceURI==null || namespaceURI.equals(getAttributeNamespace(i))) && localName.equals(getAttributeLocalName(i))) {
        return getAttributeValue(i);
      }
    }
    return null;
  }

  @Override
  public int getAttributeCount() {
    return source.getAttributeCount();
  }

  @Override
  public QName getAttributeName(final int index) {
    return new QName(getAttributeNamespace(index),getAttributeLocalName(index), getAttributePrefix(index));
  }

  @Override
  public String getAttributeNamespace(final int index) {
    String attributeNamespace = source.getAttributeNamespace(index);
    return attributeNamespace!=null ? attributeNamespace :lookupSource.getNamespaceURI(source.getAttributePrefix(index));
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return source.getAttributeLocalName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return source.getAttributePrefix(index);
  }

  @Override
  public String getAttributeType(final int index) {
    return source.getAttributeType(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return source.getAttributeValue(index);
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    return source.isAttributeSpecified(index);
  }

  @Override
  public int getNamespaceCount() {
    return source.getNamespaceCount();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    return source.getNamespacePrefix(index);
  }

  @Override
  public String getNamespaceURI(final int index) {
    return source.getNamespaceURI(index);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return new CombiningNamespaceContext(source.getNamespaceContext(), lookupSource);
  }

  @Override
  public int getEventType() {
    return source.getEventType();
  }

  @Override
  public String getText() {
    return source.getText();
  }

  @Override
  public char[] getTextCharacters() {
    return source.getTextCharacters();
  }

  @Override
  public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    return source.getTextCharacters(sourceStart, target, targetStart, length);
  }

  @Override
  public int getTextStart() {
    return source.getTextStart();
  }

  @Override
  public int getTextLength() {
    return source.getTextLength();
  }

  @Override
  public String getEncoding() {
    return source.getEncoding();
  }

  @Override
  public boolean hasText() {
    return source.hasText();
  }

  @Override
  public Location getLocation() {
    return source.getLocation();
  }

  @Override
  public QName getName() {
    return new QName(getNamespaceURI(), getLocalName(), getPrefix());
  }

  @Override
  public String getLocalName() {
    return source.getLocalName();
  }

  @Override
  public boolean hasName() {
    return source.hasName();
  }

  @Override
  public String getNamespaceURI() {
    String namespaceURI = source.getNamespaceURI();
    return namespaceURI !=null ? namespaceURI : lookupSource.getNamespaceURI(source.getPrefix());
  }

  @Override
  public String getPrefix() {
    return source.getPrefix();
  }

  @Override
  public String getVersion() {
    return source.getVersion();
  }

  @Override
  public boolean isStandalone() {
    return source.isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return source.standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return source.getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return source.getPITarget();
  }

  @Override
  public String getPIData() {
    return source.getPIData();
  }
}
