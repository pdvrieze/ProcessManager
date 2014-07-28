package nl.adaptivity.process.util;

import java.net.URI;

import javax.xml.namespace.QName;


public final class Constants {

  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";

  public static final QName PESERVICE = new QName(PROCESS_ENGINE_NS, "ProcessEngine");

  public static final URI WSDL_MEP_ROBUST_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");

  public static final URI WSDL_MEP_IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");

  public static final URI WSDL_MEP_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/in-only");

  public static final URI WEBMETHOD_NS = URI.create("http://www.w3.org/2003/05/soap/features/web-method/Method");

  public static final String PROTOCOL_HEADERS = "javax.jbi.messaging.protocol.headers";

  public static final String USER_MESSAGE_HANDLER_NS = "http://adaptivity.nl/userMessageHandler";

  public static final String DARWIN_NS = "http://darwin.bournemouth.ac.uk/services";

  private Constants() {

  }

}
