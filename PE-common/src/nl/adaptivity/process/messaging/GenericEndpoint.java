package nl.adaptivity.process.messaging;

import javax.xml.namespace.QName;


public interface GenericEndpoint {

  QName getService();

  String getEndpoint();

}
