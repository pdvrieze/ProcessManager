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

import nl.adaptivity.xml.XmlEvent.*;
import org.jetbrains.annotations.NotNull;

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

/*
        CDSECT                 -> writer.cdsect(text)
        COMMENT                -> writer.comment(text)
        DOCDECL                -> writer.docdecl(text)
        ENTITY_REF             -> writer.entityRef(text)
        IGNORABLE_WHITESPACE   -> writer.ignorableWhitespace(text)
        PROCESSING_INSTRUCTION -> writer.processingInstruction(text)
        TEXT                   -> writer.text(text)
*/
  public enum EventType {
    START_DOCUMENT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new StartDocumentEvent(reader.getLocationInfo(), reader.getVersion(), reader.getEncoding(), reader.getStandalone());
      }
    },
    START_ELEMENT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new StartElementEvent(reader.getLocationInfo(), reader.getNamespaceUri(), reader.getLocalName(),
                                     reader.getPrefix(), XmlEvent.getAttributes(reader), XmlEvent.getNamespaceDecls(reader));
      }
    },
    END_ELEMENT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new EndElementEvent(reader.getLocationInfo(), reader.getNamespaceUri(), reader.getLocalName(), reader.getPrefix());
      }
    },
    COMMENT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.comment(textEvent.getText());
      }
    },
    TEXT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.text(textEvent.getText());
      }
    },
    CDSECT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.cdsect(textEvent.getText());
      }
    },
    DOCDECL {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.docdecl(textEvent.getText());
      }
    },
    END_DOCUMENT {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new EndDocumentEvent(reader.getLocationInfo());
      }
    },
    ENTITY_REF {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.entityRef(textEvent.getText());
      }
    },
    IGNORABLE_WHITESPACE{
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.ignorableWhitespace(textEvent.getText());
      }
    },
    ATTRIBUTE {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new Attribute(reader.getLocationInfo(), reader.getNamespaceUri(), reader.getLocalName(), reader.getPrefix(), reader.getText());
      }
    },
    PROCESSING_INSTRUCTION {
      @Override
      public XmlEvent createEvent(@NotNull final XmlReader reader) {
        return new TextEvent(reader.getLocationInfo(), this, reader.getText());
      }

      @Override
      public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
        writer.processingInstruction(textEvent.getText());
      }
    };

  public void writeEvent(@NotNull final XmlWriter writer, @NotNull final TextEvent textEvent) throws XmlException {
      throw new UnsupportedOperationException("This is not generally supported, only by text types");
    }

  public abstract XmlEvent createEvent(@NotNull final XmlReader reader);
}

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
