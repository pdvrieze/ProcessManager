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

  public static void writeToStream(Source pSource, OutputStream pOutputStream) throws TransformerException {
    StreamResult outResult = new StreamResult(pOutputStream);
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer identityTransformer = factory.newTransformer();
    identityTransformer.transform(pSource, outResult);
  }

  public static void writeToWriter(Source pSource, Writer pWriter) throws TransformerException {
    StreamResult outResult = new StreamResult(pWriter);
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer identityTransformer = factory.newTransformer();
    identityTransformer.transform(pSource, outResult);
  }

  public static InputStream toInputStream(Source pSource) {
    if (pSource instanceof StreamSource) {
      return ((StreamSource) pSource).getInputStream();
    }
    if (pSource instanceof SAXSource) {
      return ((SAXSource) pSource).getInputSource().getByteStream();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(pSource, baos);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());

  }

  public static Reader toReader(Source pSource) {
    if (pSource instanceof StreamSource) {
      return ((StreamSource) pSource).getReader();
    }
    if (pSource instanceof SAXSource) {
      return new InputStreamReader(((SAXSource) pSource).getInputSource().getByteStream());
    }
    CharArrayWriter caw = new CharArrayWriter();
    try {
      Sources.writeToWriter(pSource, caw);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return new CharArrayReader(caw.toCharArray());
  }

}
