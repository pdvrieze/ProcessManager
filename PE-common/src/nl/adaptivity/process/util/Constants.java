package nl.adaptivity.process.util;

import java.net.URI;

import javax.xml.namespace.QName;


public final class Constants {
  
  public static final QName PESERVICE = new QName("http://adaptivity.nl/ProcessEngine/","ProcessEngine");
  public static final URI WSDL_MEP_IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");

  private Constants() {
    
  }
  
  public static final String PROTOCOL_HEADERS = "javax.jbi.messaging.protocol.headers";

}
