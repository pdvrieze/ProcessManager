package nl.adaptivity.util.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Created by pdvrieze on 27/08/15.
 */
public interface XmlDeserializerFactory<T> {

  public T deserialize(XMLStreamReader in) throws XMLStreamException;

}
