package nl.adaptivity.xml;

import net.devrieze.util.StringUtil;
import org.codehaus.stax2.XMLStreamWriter2;
import org.jetbrains.annotations.NotNull;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

import java.io.OutputStream;
import java.io.Writer;

/**
 * An implementation of {@link XmlWriter} that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
public class StAXWriter extends AbstractXmlWriter {

  private final XMLStreamWriter mDelegate;
  private int mDepth;

  public StAXWriter(final Writer writer, final boolean repairNamespaces) throws XMLStreamException {
    this(newFactory(repairNamespaces).createXMLStreamWriter(writer));
  }

  private static XMLOutputFactory newFactory(final boolean repairNamespaces) {
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    if (repairNamespaces) xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    return xmlOutputFactory;
  }

  public StAXWriter(final OutputStream outputStream, final String encoding, final boolean repairNamespaces) throws XMLStreamException {
    this(newFactory(repairNamespaces).createXMLStreamWriter(outputStream, encoding));
  }

  public StAXWriter(final Result result, final boolean repairNamespaces) throws XMLStreamException {
    this(newFactory(repairNamespaces).createXMLStreamWriter(result));
  }

  public StAXWriter(final XMLStreamWriter out) {
    mDelegate = out;
  }

  @Override
  public void startTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws
          XmlException {
    mDepth++;
    try {
      mDelegate.writeStartElement(toString(prefix), toString(localName), toString(namespace));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeStartElement(final String localName) throws XmlException {
    startTag(null, localName, null);
  }

  @Deprecated
  public void writeStartElement(final String namespaceURI, final String localName) throws XmlException {
    startTag(namespaceURI, null, localName);
  }

  @Deprecated
  public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws
          XmlException {
    startTag(null, localName, prefix);
  }

  @Override
  public void endTag(final CharSequence namespace, final CharSequence localName, final CharSequence prefix) throws
          XmlException {
    // TODO add verifying assertions
    try {
      mDelegate.writeEndElement();
      mDepth--;
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeEndElement() throws XmlException {
    endTag(null, null, null);
  }

  @Override
  public void endDocument() throws XmlException {
    assert getDepth()==0; // Don't write this until really the end of the document
    try {
      mDelegate.writeEndDocument();
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeEndDocument() throws XmlException {
    endDocument();
  }

  @Override
  public void close() throws XmlException {
    try {
      mDelegate.close();
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  public void flush() throws XmlException {
    try {
      mDelegate.flush();
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void attribute(final CharSequence namespace, final CharSequence name, final CharSequence prefix, final CharSequence value) throws
          XmlException {
    try {
      if (namespace==null || prefix==null || prefix.length()==0 ||namespace.length()==0) {
        mDelegate.writeAttribute(StringUtil.toString(name), StringUtil.toString(value));
      } else {
        mDelegate.writeAttribute(toString(namespace), toString(name), toString(value));
      }
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeAttribute(final String localName, final String value) throws XmlException {
    attribute(null, localName, null, value);
  }

  @Deprecated
  public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws
          XmlException {
    attribute(namespaceURI, localName, prefix, value);
  }

  @Deprecated
  public void writeAttribute(final String namespaceURI, final String localName, final String value) throws
          XmlException {
    attribute(namespaceURI, localName, null, value);
  }

  @Override
  public void namespaceAttr(final CharSequence namespacePrefix, final CharSequence namespaceUri) throws XmlException {
    try {
      mDelegate.writeNamespace(toString(namespacePrefix), toString(namespaceUri));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }

  }

  @Deprecated
  public void writeNamespace(final CharSequence prefix, final CharSequence namespaceURI) throws XmlException {
    namespaceAttr(prefix, namespaceURI);
  }

  @Deprecated
  public void writeDefaultNamespace(final CharSequence namespaceURI) throws XmlException {
    namespaceAttr(null, namespaceURI);
  }

  @Override
  public void comment(final CharSequence text) throws XmlException {
    try {
      mDelegate.writeComment(toString(text));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeComment(final String data) throws XmlException {
    comment(data);
  }

  @Override
  public void processingInstruction(final CharSequence text) throws XmlException {
    int split = StringUtil.indexOf(text, ' ');
    try {
      if (split>0) {
        mDelegate.writeProcessingInstruction(text.subSequence(0,split).toString(), text.subSequence(split, text.length()).toString());
      } else {
        mDelegate.writeProcessingInstruction(toString(text));
      }
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeProcessingInstruction(final String target) throws XmlException {
    processingInstruction(target);
  }

  @Deprecated
  public void writeProcessingInstruction(final String target, final String data) throws XmlException {
    processingInstruction(target+" "+data);
  }

  @Override
  public void cdsect(final CharSequence text) throws XmlException {
    try {
      mDelegate.writeCData(toString(text));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeCData(final String data) throws XmlException {
    cdsect(data);
  }

  @Override
  public void docdecl(final CharSequence dtd) throws XmlException {
    try {
      mDelegate.writeDTD(toString(dtd));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeDTD(final String dtd) throws XmlException {
    docdecl(dtd);
  }

  @Override
  public void entityRef(final CharSequence name) throws XmlException {
    try {
      mDelegate.writeEntityRef(toString(name));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeEntityRef(final String name) throws XmlException {
    entityRef(name);
  }

  @Override
  public void startDocument(final CharSequence version, final CharSequence encoding, final Boolean standalone) throws XmlException {
    try {
      if (standalone!=null && mDelegate instanceof XMLStreamWriter2) {
        ((XMLStreamWriter2) mDelegate).writeStartDocument(toString(version), toString(encoding), standalone);
      } else {
        mDelegate.writeStartDocument(toString(encoding), toString(version)); // standalone doesn't work
      }
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public void writeStartDocument() throws XmlException {
    startDocument(null, null, null);
  }

  @Deprecated
  public void writeStartDocument(final String version) throws XmlException {
    startDocument(version, null, null);
  }

  @Deprecated
  public void writeStartDocument(final String encoding, final String version) throws XmlException {
    startDocument(version, encoding, null);
  }

  @Override
  public void ignorableWhitespace(final CharSequence text) throws XmlException {
    text(toString(text));
  }

  @Override
  public void text(final CharSequence text) throws XmlException {
    try {
      mDelegate.writeCharacters(toString(text));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public final void writeCharacters(final String text) throws XmlException {
    text(text);
  }

  @Deprecated
  public void writeCharacters(final char[] text, final int start, final int len) throws XmlException {
    text(new String(text, start, len));
  }

  @Override
  public CharSequence getPrefix(final CharSequence namespaceUri) throws XmlException {
    try {
      return mDelegate.getPrefix(toString(namespaceUri));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void setPrefix(final CharSequence prefix, final CharSequence namespaceUri) throws XmlException {
    try {
      mDelegate.setPrefix(toString(prefix), toString(namespaceUri));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public CharSequence getNamespaceUri(@NotNull final CharSequence prefix) throws XmlException {
    return mDelegate.getNamespaceContext().getNamespaceURI(prefix.toString());
  }

  public void setDefaultNamespace(final String uri) throws XmlException {
    setPrefix(XMLConstants.DEFAULT_NS_PREFIX, uri);
  }

  public void setNamespaceContext(final NamespaceContext context) throws XmlException {
    if (getDepth()==0) {
      try {
        mDelegate.setNamespaceContext(context);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    } else {
      throw new XmlException("Modifying the namespace context halfway in a document");
    }
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return mDelegate.getNamespaceContext();
  }

  private static String toString(CharSequence charSequence) {
    return charSequence==null ? null : charSequence.toString();
  }

  @Override
  public int getDepth() {
    return mDepth;
  }
}
