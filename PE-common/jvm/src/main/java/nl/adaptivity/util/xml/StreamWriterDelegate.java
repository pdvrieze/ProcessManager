/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * Delegate that simply forwards to the wrapped streamwriter
 * Created by pdvrieze on 24/08/15.
 */
public class StreamWriterDelegate implements XMLStreamWriter {

  final XMLStreamWriter mDelegate;

  public StreamWriterDelegate(final XMLStreamWriter delegate) {
    mDelegate = delegate;
  }

  public XMLStreamWriter getDelegate() {
    return mDelegate;
  }

  @Override
  public void writeStartElement(final String localName) throws XMLStreamException {
    mDelegate.writeStartElement(localName);
  }

  @Override
  public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
    mDelegate.writeStartElement(namespaceURI, localName);
  }

  @Override
  public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws
          XMLStreamException {
    mDelegate.writeStartElement(prefix, localName, namespaceURI);
  }

  @Override
  public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
    mDelegate.writeEmptyElement(namespaceURI, localName);
  }

  @Override
  public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI) throws
          XMLStreamException {
    mDelegate.writeEmptyElement(prefix, localName, namespaceURI);
  }

  @Override
  public void writeEmptyElement(final String localName) throws XMLStreamException {
    mDelegate.writeEmptyElement(localName);
  }

  @Override
  public void writeEndElement() throws XMLStreamException {
    mDelegate.writeEndElement();
  }

  @Override
  public void writeEndDocument() throws XMLStreamException {
    mDelegate.writeEndDocument();
  }

  @Override
  public void close() throws XMLStreamException {
    mDelegate.close();
  }

  @Override
  public void flush() throws XMLStreamException {
    mDelegate.flush();
  }

  @Override
  public void writeAttribute(final String localName, final String value) throws XMLStreamException {
    mDelegate.writeAttribute(localName, value);
  }

  @Override
  public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws
          XMLStreamException {
    mDelegate.writeAttribute(prefix, namespaceURI, localName, value);
  }

  @Override
  public void writeAttribute(final String namespaceURI, final String localName, final String value) throws
          XMLStreamException {
    mDelegate.writeAttribute(namespaceURI, localName, value);
  }

  @Override
  public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
    mDelegate.writeNamespace(prefix, namespaceURI);
  }

  @Override
  public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
    mDelegate.writeDefaultNamespace(namespaceURI);
  }

  @Override
  public void writeComment(final String data) throws XMLStreamException {
    mDelegate.writeComment(data);
  }

  @Override
  public void writeProcessingInstruction(final String target) throws XMLStreamException {
    mDelegate.writeProcessingInstruction(target);
  }

  @Override
  public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
    mDelegate.writeProcessingInstruction(target, data);
  }

  @Override
  public void writeCData(final String data) throws XMLStreamException {
    mDelegate.writeCData(data);
  }

  @Override
  public void writeDTD(final String dtd) throws XMLStreamException {
    mDelegate.writeDTD(dtd);
  }

  @Override
  public void writeEntityRef(final String name) throws XMLStreamException {
    mDelegate.writeEntityRef(name);
  }

  @Override
  public void writeStartDocument() throws XMLStreamException {
    mDelegate.writeStartDocument();
  }

  @Override
  public void writeStartDocument(final String version) throws XMLStreamException {
    mDelegate.writeStartDocument(version);
  }

  @Override
  public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
    mDelegate.writeStartDocument(encoding, version);
  }

  @Override
  public void writeCharacters(final String text) throws XMLStreamException {
    mDelegate.writeCharacters(text);
  }

  @Override
  public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
    mDelegate.writeCharacters(text, start, len);
  }

  @Override
  public String getPrefix(final String uri) throws XMLStreamException {
    return mDelegate.getPrefix(uri);
  }

  @Override
  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    mDelegate.setPrefix(prefix, uri);
  }

  @Override
  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    mDelegate.setDefaultNamespace(uri);
  }

  @Override
  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    mDelegate.setNamespaceContext(context);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return mDelegate.getNamespaceContext();
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return mDelegate.getProperty(name);
  }
}
