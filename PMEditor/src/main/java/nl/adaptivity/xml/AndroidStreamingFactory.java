package nl.adaptivity.xml;

import nl.adaptivity.xml.XmlStreaming.XmlStreamingFactory;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import java.io.*;


/**
 * Created by pdvrieze on 21/11/15.
 */
public class AndroidStreamingFactory implements XmlStreamingFactory {

  @Override
  public XmlWriter newWriter(final Writer writer, final boolean repairNamespaces) throws XmlException {
    try {
      return new AndroidXmlWriter(writer, repairNamespaces);
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public XmlWriter newWriter(final OutputStream outputStream, final String encoding, final boolean repairNamespaces) throws XmlException {
    try {
      return new AndroidXmlWriter(outputStream, encoding, repairNamespaces);
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public XmlWriter newWriter(final Result result, final boolean repairNamespaces) throws XmlException {
    throw new UnsupportedOperationException("Results are not supported");
  }

  @Override
  public XmlReader newReader(final Source source) throws XmlException {
    throw new UnsupportedOperationException("Sources are not supported");
  }

  @Override
  public XmlReader newReader(final Reader reader) throws XmlException {
    try {
      return new AndroidXmlReader(reader);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public XmlReader newReader(final InputStream inputStream, final String encoding) throws XmlException {
    try {
      return new AndroidXmlReader(inputStream, encoding);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }
}
