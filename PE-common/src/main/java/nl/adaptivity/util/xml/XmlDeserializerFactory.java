package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;


/**
 * Created by pdvrieze on 27/08/15.
 */
public interface XmlDeserializerFactory<T> {

  T deserialize(XmlReader in) throws XmlException;

}
