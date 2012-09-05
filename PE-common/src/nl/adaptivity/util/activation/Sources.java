package nl.adaptivity.util.activation;

import java.io.OutputStream;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;


public final class Sources {

  private Sources() {}

  public static void writeToStream(Source pSource, OutputStream pOutputStream) throws TransformerException {
    StreamResult outResult = new StreamResult(pOutputStream);
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer identityTransformer = factory.newTransformer();
    identityTransformer.transform(pSource, outResult);
  }
  
}
