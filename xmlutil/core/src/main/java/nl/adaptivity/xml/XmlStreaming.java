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

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;


/**
 * Utility class with factories and constants for the {@link XmlReader} and {@link XmlWriter} interfaces.
 * Created by pdvrieze on 15/11/15.
 */
public final class XmlStreaming {

  private static class DefaultFactory implements XmlStreamingFactory {

    private static final XmlStreamingFactory DEFAULTFACTORY = new DefaultFactory();

    @Override
    public XmlWriter newWriter(final Writer writer, final boolean repairNamespaces) throws XmlException {
      try {
        return new StAXWriter(writer, repairNamespaces);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }

    @Override
    public XmlWriter newWriter(final OutputStream outputStream, final String encoding, final boolean repairNamespaces) throws XmlException {
      try {
        return new StAXWriter(outputStream, encoding, repairNamespaces);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }

    @Override
    public XmlWriter newWriter(final Result result, final boolean repairNamespaces) throws XmlException {
      try {
        return new StAXWriter(result, repairNamespaces);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }

    @Override
    public XmlReader newReader(final Reader reader) throws XmlException {
      try {
        return new StAXReader(reader);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }

    @Override
    public XmlReader newReader(final InputStream inputStream, final String encoding) throws XmlException {
      try {
        return new StAXReader(inputStream, encoding);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }

    @Override
    public XmlReader newReader(final Source source) throws XmlException {
      try {
        return new StAXReader(source);
      } catch (XMLStreamException e) {
        throw new XmlException(e);
      }
    }
  }

  public interface XmlStreamingFactory {

    XmlWriter newWriter(Writer writer, final boolean repairNamespaces) throws XmlException;
    XmlWriter newWriter(OutputStream outputStream, String encoding, final boolean repairNamespaces) throws XmlException;
    XmlWriter newWriter(Result result, final boolean repairNamespaces) throws XmlException;

    XmlReader newReader(Source source) throws XmlException;
    XmlReader newReader(Reader reader) throws XmlException;
    XmlReader newReader(InputStream inputStream, String encoding) throws XmlException;
  }

  public enum EventType {
    START_DOCUMENT,
    START_ELEMENT,
    END_ELEMENT,
    COMMENT,
    TEXT,
    CDSECT,
    DOCDECL,
    END_DOCUMENT,
    ENTITY_REF,
    IGNORABLE_WHITESPACE,
    ATTRIBUTE,
    PROCESSING_INSTRUCTION
  }
/* XXX this is for initial correctness
  public static final int START_DOCUMENT = 0;
  public static final int START_ELEMENT = 1;
  public static final int END_TAG = 2;
  public static final int COMMENT = 3;
  public static final int TEXT = 4;
  public static final int CDSECT = 5;
  public static final int DOCDECL = 6;
  public static final int END_DOCUMENT = 7;
  public static final int ENTITY_REF = 8;
  public static final int IGNORABLE_WHITESPACE = 9;
  public static final int PROCESSING_INSTRUCTION = 10;

  public static final int CDATA = CDSECT;
  public static final int CHARACTERS = TEXT;
*/
  public static final EventType START_DOCUMENT = EventType.START_DOCUMENT;
  public static final EventType START_ELEMENT = EventType.START_ELEMENT;
  public static final EventType END_ELEMENT = EventType.END_ELEMENT;
  public static final EventType COMMENT = EventType.COMMENT;
  public static final EventType TEXT = EventType.TEXT;
  public static final EventType CDSECT = EventType.CDSECT;
  public static final EventType DOCDECL = EventType.DOCDECL;
  public static final EventType ATTRIBUTE = EventType.ATTRIBUTE;
  public static final EventType END_DOCUMENT = EventType.END_DOCUMENT;
  public static final EventType ENTITY_REF = EventType.ENTITY_REF;
  public static final EventType IGNORABLE_WHITESPACE = EventType.IGNORABLE_WHITESPACE;
  public static final EventType PROCESSING_INSTRUCTION = EventType.PROCESSING_INSTRUCTION;

  public static final EventType CDATA = CDSECT;
  public static final EventType CHARACTERS = TEXT;

  private static XmlStreamingFactory _factory;

  private XmlStreaming() {}

  public static XmlWriter newWriter(final Result result) throws XmlException {
    return newWriter(result, false);
  }

  public static XmlWriter newWriter(final Result result, final boolean repairNamespaces) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newWriter(result, repairNamespaces);
  }

  public static XmlWriter newWriter(final OutputStream outputStream, final String encoding) throws XmlException {
    return newWriter(outputStream, encoding, false);
  }

  public static XmlWriter newWriter(final OutputStream outputStream, final String encoding, final boolean repairNamespaces) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newWriter(outputStream, encoding, repairNamespaces);
  }

  public static XmlWriter newWriter(final Writer writer) throws XmlException {
    return newWriter(writer, false);
  }

  public static XmlWriter newWriter(final Writer writer, final boolean repairNamespaces) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newWriter(writer, repairNamespaces);
  }

  public static XmlReader newReader(final InputStream inputStream, final String encoding) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newReader(inputStream, encoding);
  }

  public static XmlReader newReader(final Reader reader) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newReader(reader);
  }

  public static XmlReader newReader(final Source source) throws XmlException {
    return (_factory !=null ? _factory : DefaultFactory.DEFAULTFACTORY).newReader(source);
  }

  public static void setFactory(XmlStreamingFactory factory) {
    _factory = factory;
  }
}
