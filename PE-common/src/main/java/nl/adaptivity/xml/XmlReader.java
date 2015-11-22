package nl.adaptivity.xml;

import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 15/11/15.
 */
public interface XmlReader {

  /** Get the next tag. This must call next, not use the underlying stream. */
  EventType nextTag() throws XmlException;

  boolean hasNext() throws XmlException;

  EventType next() throws XmlException;

  @Nullable CharSequence getNamespaceUri() throws XmlException;

  @NotNull CharSequence getLocalName() throws XmlException;

  @Nullable CharSequence getPrefix() throws XmlException;

  QName getName() throws XmlException;

  void require(EventType type, CharSequence namespace, CharSequence name) throws XmlException;

  int getDepth() throws XmlException;

  CharSequence getText() throws XmlException;

  int getAttributeCount() throws XmlException;

  @Nullable CharSequence getAttributeNamespace(int i) throws XmlException;

  @Nullable CharSequence getAttributePrefix(int i) throws XmlException;

  @NotNull CharSequence getAttributeLocalName(int i) throws XmlException;

  @NotNull QName getAttributeName(int i) throws XmlException;

  CharSequence getAttributeValue(int i) throws XmlException;

  EventType getEventType() throws XmlException;

  @Nullable CharSequence getAttributeValue(CharSequence nsUri, CharSequence localName) throws XmlException;

  int getNamespaceStart() throws XmlException;

  int getNamespaceEnd() throws XmlException;

  CharSequence getNamespacePrefix(int i) throws XmlException;

  void close() throws XmlException;

  CharSequence getNamespaceUri(int i) throws XmlException;

  @Nullable CharSequence getNamespacePrefix(CharSequence namespaceUri) throws XmlException;

  boolean isWhitespace() throws XmlException;

  boolean isEndElement() throws XmlException;

  boolean isCharacters() throws XmlException;

  boolean isStartElement() throws XmlException;

  @Nullable String getNamespaceUri(CharSequence prefix) throws XmlException;

  /** Get some information on the current location in the file. This is implementation dependent. */
  String getLocationInfo();

  NamespaceContext getNamespaceContext() throws XmlException;

  CharSequence getEncoding();

  Boolean getStandalone();

  CharSequence getVersion();
}
