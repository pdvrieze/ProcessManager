package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.NamespaceContext;


/**
 * <p>Simple delegating writer that passes all calls on to the delegate. This class is abstract for the only reason that any
 * direct instances of this class make little sense.
 * </p><p>
 * Created by pdvrieze on 17/11/15.
 * </p>
 */
public abstract class XmlDelegatingWriter implements XmlWriter{

  private final XmlWriter mDelegate;

  public XmlDelegatingWriter(final XmlWriter delegate) {
    mDelegate = delegate;
  }

  public void setPrefix(final CharSequence prefix, final CharSequence namespaceUri) throws XmlException {
    mDelegate.setPrefix(prefix, namespaceUri);
  }

  public void startDocument(final CharSequence version, final CharSequence encoding, final Boolean standalone) throws
          XmlException {
    mDelegate.startDocument(version, encoding, standalone);
  }

  public void attribute(final CharSequence namespace, final CharSequence name, final CharSequence prefix, final CharSequence value) throws
          XmlException {
    mDelegate.attribute(namespace, name, prefix, value);
  }

  public void text(final CharSequence text) throws XmlException {
    mDelegate.text(text);
  }

  public NamespaceContext getNamespaceContext() {
    return mDelegate.getNamespaceContext();
  }

  public void close() throws XmlException {
    mDelegate.close();
  }

  public void namespaceAttr(final CharSequence namespacePrefix, final CharSequence namespaceUri) throws XmlException {
    mDelegate.namespaceAttr(namespacePrefix, namespaceUri);
  }

  public void endTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws
          XmlException {
    mDelegate.endTag(namespace, localName, prefix);
  }

  public int getDepth() {
    return mDelegate.getDepth();
  }

  public void processingInstruction(final CharSequence text) throws XmlException {
    mDelegate.processingInstruction(text);
  }

  public void docdecl(final CharSequence text) throws XmlException {
    mDelegate.docdecl(text);
  }

  public void comment(final CharSequence text) throws XmlException {
    mDelegate.comment(text);
  }

  public void flush() throws XmlException {
    mDelegate.flush();
  }

  public void entityRef(final CharSequence text) throws XmlException {
    mDelegate.entityRef(text);
  }

  public void cdsect(final CharSequence text) throws XmlException {
    mDelegate.cdsect(text);
  }

  public void ignorableWhitespace(final CharSequence text) throws XmlException {
    mDelegate.ignorableWhitespace(text);
  }

  public void startTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws
          XmlException {
    mDelegate.startTag(namespace, localName, prefix);
  }

  public CharSequence getNamespaceUri(final CharSequence prefix) throws XmlException {
    return mDelegate.getNamespaceUri(prefix);
  }

  public void endDocument() throws XmlException {
    mDelegate.endDocument();
  }

  public CharSequence getPrefix(final CharSequence namespaceUri) throws XmlException {
    return mDelegate.getPrefix(namespaceUri);
  }
}
