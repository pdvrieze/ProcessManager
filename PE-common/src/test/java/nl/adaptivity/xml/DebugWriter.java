package nl.adaptivity.xml;

import nl.adaptivity.util.xml.XmlDelegatingWriter;


/**
 * A writer for debugging that writes all events to stdout as well
 * Created by pdvrieze on 05/12/15.
 */
public class DebugWriter extends XmlDelegatingWriter {

  private static final String TAG = "DEBUGWRITER: ";

  public DebugWriter(final XmlWriter delegate) {
    super(delegate);
  }

  @Override
  public void startTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws XmlException {
    System.out.println(TAG + "startTag(namespace='"+namespace+"', localName='"+localName+"', prefix='"+prefix+"')");
    super.startTag(namespace, localName, prefix);
  }

  @Override
  public void endTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws XmlException {
    System.out.println(TAG + "endTag(namespace='"+namespace+"', localName='"+localName+"', prefix='"+prefix+"')");
    super.endTag(namespace, localName, prefix);
  }

  @Override
  public void attribute(final CharSequence namespace, final CharSequence name, final CharSequence prefix, final CharSequence value) throws XmlException {
    System.out.println(TAG + "  attribute(namespace='"+namespace+"', name='"+name+"', prefix='"+prefix+"', value='"+value+"')");
    super.attribute(namespace, name, prefix, value);
  }

  @Override
  public void namespaceAttr(final CharSequence namespacePrefix, final CharSequence namespaceUri) throws XmlException {
    System.out.println(TAG + "  namespaceAttr(namespacePrefix='"+namespacePrefix+"', namespaceUri='"+namespaceUri+"')");
    super.namespaceAttr(namespacePrefix, namespaceUri);
  }

  @Override
  public void text(final CharSequence text) throws XmlException {
    System.out.println(TAG + "--text('"+text+"')");
    super.text(text);
  }

  @Override
  public void ignorableWhitespace(final CharSequence text) throws XmlException {
    System.out.println(TAG + "  ignorableWhitespace()");
    super.ignorableWhitespace(text);
  }

  @Override
  public void startDocument(final CharSequence version, final CharSequence encoding, final Boolean standalone) throws XmlException {
    System.out.println(TAG + "startDocument()");
    super.startDocument(version, encoding, standalone);
  }

  @Override
  public void comment(final CharSequence text) throws XmlException {
    System.out.println(TAG + "comment('"+text+"')");
    super.comment(text);
  }

  @Override
  public void processingInstruction(final CharSequence text) throws XmlException {
    System.out.println(TAG + "processingInstruction('"+text+"')");
    super.processingInstruction(text);
  }

  @Override
  public void close() throws XmlException {
    System.out.println(TAG + "close()");
    super.close();
  }

  @Override
  public void flush() throws XmlException {
    System.out.println(TAG + "flush()");
    super.flush();
  }

  @Override
  public void endDocument() throws XmlException {
    System.out.println(TAG + "endDocument()");
    super.endDocument();
  }
}
