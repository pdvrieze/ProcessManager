package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by pdvrieze on 31/10/15.
 */
public class NamespaceAddingStreamReader implements XMLStreamReader {

  private final XMLStreamReader mSource;
  private final NamespaceContext mLookupSource;

  public NamespaceAddingStreamReader(final NamespaceContext lookupSource, final XMLStreamReader source) {
    mSource = source;
    mLookupSource = lookupSource;
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return mSource.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    return mSource.next();
  }

  @Override
  public void require(final int type, @Nullable final String namespaceURI, @Nullable final String localName) throws XMLStreamException {
    if (type != getEventType() ||
            (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) ||
            (localName != null && !localName.equals(getLocalName()))) {
      mSource.require(type, namespaceURI, localName);
    } {
      throw new XMLStreamException("Require failed");
    }
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return mSource.getElementText();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return mSource.nextTag();
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
    final String namespaceURI = mSource.getNamespaceURI(prefix);
    return namespaceURI != null ? namespaceURI : mLookupSource.getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return mSource.isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return mSource.isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return mSource.isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return mSource.isWhiteSpace();
  }

  @Nullable
  @Override
  public String getAttributeValue(@Nullable final String namespaceURI, @NotNull final String localName) {
    for(int i=getAttributeCount()-1; i>=0; --i) {
      if ((namespaceURI==null || namespaceURI.equals(getAttributeNamespace(i))) && localName.equals(getAttributeLocalName(i))) {
        return getAttributeValue(i);
      }
    }
    return null;
  }

  @Override
  public int getAttributeCount() {
    return mSource.getAttributeCount();
  }

  @NotNull
  @Override
  public QName getAttributeName(final int index) {
    return new QName(getAttributeNamespace(index),getAttributeLocalName(index), getAttributePrefix(index));
  }

  @Override
  public String getAttributeNamespace(final int index) {
    final String attributeNamespace = mSource.getAttributeNamespace(index);
    return attributeNamespace!=null ? attributeNamespace : mLookupSource.getNamespaceURI(mSource.getAttributePrefix(index));
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return mSource.getAttributeLocalName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return mSource.getAttributePrefix(index);
  }

  @Override
  public String getAttributeType(final int index) {
    return mSource.getAttributeType(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return mSource.getAttributeValue(index);
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    return mSource.isAttributeSpecified(index);
  }

  @Override
  public int getNamespaceCount() {
    return mSource.getNamespaceCount();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    return mSource.getNamespacePrefix(index);
  }

  @Override
  public String getNamespaceURI(final int index) {
    return mSource.getNamespaceURI(index);
  }

  @NotNull
  @Override
  public NamespaceContext getNamespaceContext() {
    return new CombiningNamespaceContext(mSource.getNamespaceContext(), mLookupSource);
  }

  @Override
  public int getEventType() {
    return mSource.getEventType();
  }

  @Override
  public String getText() {
    return mSource.getText();
  }

  @Override
  public char[] getTextCharacters() {
    return mSource.getTextCharacters();
  }

  @Override
  public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    return mSource.getTextCharacters(sourceStart, target, targetStart, length);
  }

  @Override
  public int getTextStart() {
    return mSource.getTextStart();
  }

  @Override
  public int getTextLength() {
    return mSource.getTextLength();
  }

  @Override
  public String getEncoding() {
    return mSource.getEncoding();
  }

  @Override
  public boolean hasText() {
    return mSource.hasText();
  }

  @Override
  public Location getLocation() {
    return mSource.getLocation();
  }

  @NotNull
  @Override
  public QName getName() {
    return new QName(getNamespaceURI(), getLocalName(), getPrefix());
  }

  @Override
  public String getLocalName() {
    return mSource.getLocalName();
  }

  @Override
  public boolean hasName() {
    return mSource.hasName();
  }

  @Override
  public String getNamespaceURI() {
    final String namespaceURI = mSource.getNamespaceURI();
    return namespaceURI !=null ? namespaceURI : mLookupSource.getNamespaceURI(mSource.getPrefix());
  }

  @Override
  public String getPrefix() {
    return mSource.getPrefix();
  }

  @Override
  public String getVersion() {
    return mSource.getVersion();
  }

  @Override
  public boolean isStandalone() {
    return mSource.isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return mSource.standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return mSource.getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return mSource.getPITarget();
  }

  @Override
  public String getPIData() {
    return mSource.getPIData();
  }
}
