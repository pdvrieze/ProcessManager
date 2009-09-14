package nl.adaptivity.process.userMessageHandler.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.exec.Task;


public class InternalEndpoint implements GenericEndpoint {

  private UserMessageService aService;

  @Override
  public QName getService() {
    return new QName("http:://adaptivity.nl/userMessageHandler", "userMessageHandler");
  }

  @Override
  public String getEndpoint() {
    return "internal";
  }

  @WebMethod
  boolean postTask(@WebParam(name="task", mode=Mode.IN) Task pTask) {
    return aService.postTask(pTask);
  }
}
