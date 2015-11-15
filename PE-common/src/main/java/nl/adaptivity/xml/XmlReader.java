package nl.adaptivity.xml;

/**
 * Created by pdvrieze on 15/11/15.
 */
public interface XmlReader {

  int nextTag() throws XmlException;

  boolean isWhitespace() throws XmlException;

  int next() throws XmlException;

  CharSequence getNamespace() throws XmlException;

  CharSequence getLocalName() throws XmlException;

  CharSequence getPrefix() throws XmlException;

  void require(int type, CharSequence namespace, CharSequence name) throws XmlException;

  int getDepth() throws XmlException;

  CharSequence getText() throws XmlException;

  int getAttributeCount() throws XmlException;

  CharSequence getAttributeNamespace(int i) throws XmlException;

  CharSequence getAttributePrefix(int i) throws XmlException;

  CharSequence getAttributeLocalName(int i) throws XmlException;

  CharSequence getAttributeValue(int i) throws XmlException;

  int getEventType() throws XmlException;

  CharSequence getAttributeValue(CharSequence nsUri, CharSequence localName) throws XmlException;

  int getNamespaceStart() throws XmlException;

  int getNamespaceEnd() throws XmlException;

  CharSequence getNamespacePrefix(int i) throws XmlException;

  CharSequence getNamespaceUri(int i) throws XmlException;
}
