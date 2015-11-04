package nl.adaptivity.util.xml;

import nl.adaptivity.util.CombiningReader;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import java.io.Reader;
import java.io.StringReader;


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.
 *
 * Created by pdvrieze on 04/11/15.
 */
public class XMLFragmentStreamReader implements XMLStreamReader {

  private static final String WRAPPERPPREFIX = "SDFKLJDSF";
  private static final String WRAPPERNAMESPACE = "http://wrapperns";
  private final XMLStreamReader delegate;

  public XMLFragmentStreamReader(final XMLInputFactory pXif, final Reader pIn) throws XMLStreamException {
    Reader actualInput = new CombiningReader(new StringReader("<" + WRAPPERPPREFIX + ":wrapper xmlns:"+WRAPPERPPREFIX+ "=\"" + WRAPPERNAMESPACE + "\">"), pIn, new StringReader("</" + WRAPPERPPREFIX + ":wrapper>"));
    delegate = pXif.createXMLStreamReader(actualInput);
  }

  public static XMLFragmentStreamReader from (XMLInputFactory xif, Reader in) throws XMLStreamException {
    return new XMLFragmentStreamReader(xif, in);
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return delegate.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    int result = delegate.next();
    switch (result) {
      case XMLStreamConstants.START_DOCUMENT:
      case XMLStreamConstants.END_DOCUMENT:
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
      case XMLStreamConstants.DTD:
        return next();
      case XMLStreamConstants.START_ELEMENT:
      case XMLStreamConstants.END_ELEMENT:
        if (WRAPPERNAMESPACE.equals(delegate.getNamespaceURI())) { return delegate.next(); }
    }
    return result;
  }

  @Override
  public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
    delegate.require(type, namespaceURI, localName);
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return delegate.getElementText();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return delegate.nextTag();
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return delegate.hasNext();
  }

  @Override
  public void close() throws XMLStreamException {
    delegate.close();
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    return delegate.getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return delegate.isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return delegate.isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return delegate.isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return delegate.isWhiteSpace();
  }

  @Override
  public String getAttributeValue(final String namespaceURI, final String localName) {
    return delegate.getAttributeValue(namespaceURI, localName);
  }

  @Override
  public int getAttributeCount() {
    return delegate.getAttributeCount();
  }

  @Override
  public QName getAttributeName(final int index) {
    return delegate.getAttributeName(index);
  }

  @Override
  public String getAttributeNamespace(final int index) {
    return delegate.getAttributeNamespace(index);
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return delegate.getAttributeLocalName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return delegate.getAttributePrefix(index);
  }

  @Override
  public String getAttributeType(final int index) {
    return delegate.getAttributeType(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return delegate.getAttributeValue(index);
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    return delegate.isAttributeSpecified(index);
  }

  @Override
  public int getNamespaceCount() {
    return delegate.getNamespaceCount();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    return delegate.getNamespacePrefix(index);
  }

  @Override
  public String getNamespaceURI(final int index) {
    return delegate.getNamespaceURI(index);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return delegate.getNamespaceContext();
  }

  @Override
  public int getEventType() {
    return delegate.getEventType();
  }

  @Override
  public String getText() {
    return delegate.getText();
  }

  @Override
  public char[] getTextCharacters() {
    return delegate.getTextCharacters();
  }

  @Override
  public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    return delegate.getTextCharacters(sourceStart, target, targetStart, length);
  }

  @Override
  public int getTextStart() {
    return delegate.getTextStart();
  }

  @Override
  public int getTextLength() {
    return delegate.getTextLength();
  }

  @Override
  public String getEncoding() {
    return delegate.getEncoding();
  }

  @Override
  public boolean hasText() {
    return delegate.hasText();
  }

  @Override
  public Location getLocation() {
    return delegate.getLocation();
  }

  @Override
  public QName getName() {
    return delegate.getName();
  }

  @Override
  public String getLocalName() {
    return delegate.getLocalName();
  }

  @Override
  public boolean hasName() {
    return delegate.hasName();
  }

  @Override
  public String getNamespaceURI() {
    return delegate.getNamespaceURI();
  }

  @Override
  public String getPrefix() {
    return delegate.getPrefix();
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  @Override
  public boolean isStandalone() {
    return delegate.isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return delegate.standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return delegate.getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return delegate.getPITarget();
  }

  @Override
  public String getPIData() {
    return delegate.getPIData();
  }
}
