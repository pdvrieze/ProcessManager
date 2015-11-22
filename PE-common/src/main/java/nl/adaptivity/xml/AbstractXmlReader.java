package nl.adaptivity.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.namespace.QName;

import static nl.adaptivity.xml.XmlStreaming.*;


/**
 * Created by pdvrieze on 16/11/15.
 */
public abstract class AbstractXmlReader implements XmlReader {

  @Override
  public void require(final EventType type, final CharSequence namespace, final CharSequence name) throws XmlException {
    if (getEventType()!= type) {
      throw new XmlException("Unexpected event type");
    }
    if (namespace!=null) {
      if (! StringUtil.isEqual(namespace, getNamespaceUri())) {
        throw new XmlException("Namespace uri's don't match: expected="+namespace+" found="+getNamespaceUri());
      }
    }
    if (name!=null) {
      if (! StringUtil.isEqual(name, getLocalName())) {
        throw new XmlException("Local names don't match: expected="+name+" found="+getLocalName());
      }
    }
  }

  @Override
  public boolean isEndElement() throws XmlException {
    return getEventType()== END_ELEMENT;
  }

  @Override
  public boolean isCharacters() throws XmlException {
    return getEventType()== CHARACTERS;
  }

  @Override
  public boolean isStartElement() throws XmlException {
    return getEventType()== CHARACTERS;
  }

  @Override
  public boolean isWhitespace() throws XmlException {
    return (getEventType() == IGNORABLE_WHITESPACE) || ((getEventType() == TEXT) && XmlUtil.isXmlWhitespace(getText()));
  }

  @Override
  public QName getName() throws XmlException {
    CharSequence prefix = getPrefix();
    return new QName(StringUtil.toString(getNamespaceUri()), getLocalName().toString(), prefix ==null ? "" : prefix
            .toString());
  }

  @Override
  public QName getAttributeName(final int i) throws XmlException {
    CharSequence attributePrefix = getAttributePrefix(i);
    return new QName(StringUtil.toString(getAttributeNamespace(i)), getAttributeLocalName(i).toString(), attributePrefix ==null ? "" : attributePrefix
            .toString());
  }
}
