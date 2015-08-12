package nl.adaptivity.util.activation;

import net.devrieze.util.Streams;

import java.io.*;

import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


public final class Sources {

  private Sources() {}

  public static void writeToStream(final Source pSource, final OutputStream pOutputStream) throws TransformerException {
    writeToResult(pSource, new StreamResult(pOutputStream), false);
  }

  public static void writeToStream(final Source pSource, final OutputStream pOutputStream, boolean pIndent) throws TransformerException {
    writeToResult(pSource, new StreamResult(pOutputStream), pIndent);
  }

  public static void writeToWriter(final Source pSource, final Writer pWriter) throws TransformerException {
    writeToWriter(pSource, pWriter, false);
  }

  public static void writeToWriter(final Source pSource, final Writer pWriter, final boolean pIndent) throws TransformerException {
    writeToResult(pSource, new StreamResult(pWriter), pIndent);
  }

  public static void writeToResult(final Source pSource, final Result pResult) throws TransformerException{
    writeToResult(pSource, pResult, false);
  }

  public static void writeToResult(final Source pSource, final Result pResult, final boolean pIndent)
      throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer identityTransformer = factory.newTransformer();
    if (pIndent) {
      identityTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      identityTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    identityTransformer.transform(pSource, pResult);
  }

  public static InputStream toInputStream(final Source pSource) {
    if (pSource instanceof StreamSource) {
      final InputStream result = ((StreamSource) pSource).getInputStream();
      if (result!=null) { return result; }
    }
    if (pSource instanceof SAXSource && (! (pSource instanceof JAXBSource))) {
      final InputStream result = ((SAXSource) pSource).getInputSource().getByteStream();
      if (result!=null) { return result; }
    }
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(pSource, baos);
    } catch (final TransformerException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());

  }

  public static Reader toReader(final Source pSource) {
    {
      if (pSource instanceof StreamSource) {
        Reader result = ((StreamSource) pSource).getReader();
        if (result!=null) { return result; }
      }
      if (pSource instanceof SAXSource && (! (pSource instanceof JAXBSource))) {
        final InputStream byteStream = ((SAXSource) pSource).getInputSource().getByteStream();
        if (byteStream!=null) { return new InputStreamReader(byteStream); }
      }
    }
    final CharArrayWriter caw = new CharArrayWriter();
    try {
      Sources.writeToWriter(pSource, caw);
    } catch (final TransformerException e) {
      throw new RuntimeException(e);
    }
    return new CharArrayReader(caw.toCharArray());
  }

  public static String toString(final Source pSource) {
    Reader in = null;
    try {
      if (pSource instanceof StreamSource) {
        Reader result = ((StreamSource) pSource).getReader();
        if (result!=null) { return Streams.toString(result); }
      }
      if (pSource instanceof SAXSource && (! (pSource instanceof JAXBSource))) {
        final InputStream byteStream = ((SAXSource) pSource).getInputSource().getByteStream();
        if (byteStream!=null) { return Streams.toString(new InputStreamReader(byteStream)); }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final StringWriter sw = new StringWriter();
    try {
      Sources.writeToWriter(pSource, sw);
    } catch (final TransformerException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }
}
