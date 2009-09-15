package nl.adaptivity.process.userMessageHandler.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.exec.Task;


public class InternalEndpoint implements GenericEndpoint {

  private static final String ENDPOINT = "internal";
  public static final QName SERVICENAME = new QName("http:://adaptivity.nl/userMessageHandler", "userMessageHandler");
  private UserMessageService aService;

  public InternalEndpoint(UserMessageService pService) {
    aService = pService;
  }

  @Override
  public QName getService() {
    return SERVICENAME;
  }

  @Override
  public String getEndpoint() {
    return ENDPOINT;
  }

  @WebMethod
  boolean postTask(@WebParam(name="task", mode=Mode.IN) Task pTask) {
    return aService.postTask(pTask);
  }
}
