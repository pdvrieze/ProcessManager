package nl.adaptivity.util.activation;

import java.io.*;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


public final class Sources {

  private Sources() {}

  public static void writeToStream(final Source pSource, final OutputStream pOutputStream) throws TransformerException {
    final StreamResult outResult = new StreamResult(pOutputStream);
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer identityTransformer = factory.newTransformer();
    identityTransformer.transform(pSource, outResult);
  }

  public static void writeToWriter(final Source pSource, final Writer pWriter) throws TransformerException {
    final StreamResult outResult = new StreamResult(pWriter);
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer identityTransformer = factory.newTransformer();
    identityTransformer.transform(pSource, outResult);
  }

  public static InputStream toInputStream(final Source pSource) {
    if (pSource instanceof StreamSource) {
      return ((StreamSource) pSource).getInputStream();
    }
    if (pSource instanceof SAXSource) {
      return ((SAXSource) pSource).getInputSource().getByteStream();
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
    if (pSource instanceof StreamSource) {
      return ((StreamSource) pSource).getReader();
    }
    if (pSource instanceof SAXSource) {
      return new InputStreamReader(((SAXSource) pSource).getInputSource().getByteStream());
    }
    final CharArrayWriter caw = new CharArrayWriter();
    try {
      Sources.writeToWriter(pSource, caw);
    } catch (final TransformerException e) {
      throw new RuntimeException(e);
    }
    return new CharArrayReader(caw.toCharArray());
  }

}
