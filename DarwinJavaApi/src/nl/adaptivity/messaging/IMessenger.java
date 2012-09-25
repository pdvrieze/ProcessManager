package nl.adaptivity.messaging;

import java.net.URI;
import java.util.concurrent.Future;

import javax.activation.DataSource;
import javax.xml.namespace.QName;


public interface IMessenger {

  public void registerEndpoint(QName pService, String endPoint, URI pTarget);

  public void registerEndpoint(Endpoint pEndpoint);

  public <T> Future<T> sendMessage(Endpoint pDestination, DataSource pMessage);
}
