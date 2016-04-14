/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml;

import org.jetbrains.annotations.NotNull;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * An implementation of {@link XmlWriter} that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
public class StAXWriter extends AbstractXmlWriter {

  final static Class<? extends XMLStreamWriter> _XMLStreamWriter2;
  final static Method                           _writeStartDocument;

  static {
    Class<?> clazz;
    Method m = null;
    try {
      clazz = StAXWriter.class.getClassLoader().loadClass("org.codehaus.stax2.XMLStreamWriter");
      m = clazz.getMethod("writeStartDocument", String.class, String.class, boolean.class);
    } catch (ClassNotFoundException|NoSuchMethodException e) {
      clazz = null;
    }
    //noinspection unchecked
    _XMLStreamWriter2 = clazz.asSubclass(XMLStreamWriter.class);
    _writeStartDocument = m;
  }

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
        mDelegate.writeAttribute(StAXReader.toString(name), StAXReader.toString(value));
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
    final String textStr = text.toString();
    final int    split   = textStr.indexOf(' ');
    try {
      if (split>0) {
        mDelegate.writeProcessingInstruction(textStr.substring(0,split), textStr.substring(split, text.length()));
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
      if (standalone!=null && _XMLStreamWriter2.isInstance(mDelegate)) {
        try {
          _writeStartDocument.invoke(mDelegate, toString(version), toString(encoding), standalone);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
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
