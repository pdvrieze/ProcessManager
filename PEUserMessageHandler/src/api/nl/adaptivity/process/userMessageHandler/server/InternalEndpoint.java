package nl.adaptivity.process.userMessageHandler.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import nl.adaptivity.messaging.*;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.ws.soap.SoapSeeAlso;


/*@XmlSeeAlso(XmlTask.class)*/
@XmlAccessorType(XmlAccessType.NONE)
@Descriptor(InternalEndpoint.Descriptor.class)
public interface InternalEndpoint extends GenericEndpoint {

  @WebMethod
  public ActivityResponse<Boolean> postTask(@WebParam(name = "repliesParam", mode = Mode.IN) final EndpointDescriptorImpl pEndPoint, @WebParam(name = "taskParam", mode = Mode.IN) /*@SoapSeeAlso(XmlTask.class)*/ final UserTask<?> pTask);

  public static class Descriptor implements EndpointDescriptor {
    @Override
    public QName getServiceName() {
      return SERVICENAME;
    }

    public static final String ENDPOINT = "internal";

    public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler");

    @Override
    public String getEndpointName() {
      return ENDPOINT;
    }

    @Override
    public URI getEndpointLocation() {
      return null;
    }
  }
}
