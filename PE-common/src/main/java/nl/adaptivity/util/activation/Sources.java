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

package nl.adaptivity.util.activation;

import net.devrieze.util.Streams;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.*;


public final class Sources {

  private Sources() {}

  public static void writeToStream(final Source source, final OutputStream outputStream) throws TransformerException {
    writeToResult(source, new StreamResult(outputStream), false);
  }

  public static void writeToStream(final Source source, final OutputStream outputStream, final boolean indent) throws TransformerException {
    writeToResult(source, new StreamResult(outputStream), indent);
  }

  public static void writeToWriter(final Source source, final Writer writer) throws TransformerException {
    writeToWriter(source, writer, false);
  }

  public static void writeToWriter(final Source source, final Writer writer, final boolean indent) throws TransformerException {
    writeToResult(source, new StreamResult(writer), indent);
  }

  public static void writeToResult(final Source source, final Result result) throws TransformerException{
    writeToResult(source, result, false);
  }

  public static void writeToResult(final Source source, final Result result, final boolean indent)
      throws TransformerFactoryConfigurationError, TransformerException {
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer identityTransformer = factory.newTransformer();
    if (indent) {
      identityTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      identityTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    identityTransformer.transform(source, result);
  }

  public static InputStream toInputStream(final Source source) {
    if (source instanceof StreamSource) {
      final InputStream result = ((StreamSource) source).getInputStream();
      if (result!=null) { return result; }
    }
    if (source instanceof SAXSource && (! (source instanceof JAXBSource))) {
      final InputStream result = ((SAXSource) source).getInputSource().getByteStream();
      if (result!=null) { return result; }
    }
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(source, baos);
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());

  }

  public static Reader toReader(final Source source) {
    {
      if (source instanceof StreamSource) {
        final Reader result = ((StreamSource) source).getReader();
        if (result!=null) { return result; }
      }
      if (source instanceof SAXSource && (! (source instanceof JAXBSource))) {
        final InputStream byteStream = ((SAXSource) source).getInputSource().getByteStream();
        if (byteStream!=null) { return new InputStreamReader(byteStream); }
      }
    }
    final CharArrayWriter caw = new CharArrayWriter();
    try {
      Sources.writeToWriter(source, caw);
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return new CharArrayReader(caw.toCharArray());
  }

  public static String toString(final Source source) {
    return toString(source, false);
  }

  public static String toString(final Source source, final boolean indent) {
    final Reader in = null;
    try {
      if (source instanceof StreamSource) {
        final Reader result = ((StreamSource) source).getReader();
        if (result!=null) { return Streams.toString(result); }
      }
      if (source instanceof SAXSource && (! (source instanceof JAXBSource))) {
        final InputStream byteStream = ((SAXSource) source).getInputSource().getByteStream();
        if (byteStream!=null) { return Streams.toString(new InputStreamReader(byteStream)); }
      }
    } catch (@NotNull final IOException e) {
      throw new RuntimeException(e);
    }
    final StringWriter sw = new StringWriter();
    try {
      Sources.writeToWriter(source, sw, indent);
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }
}
