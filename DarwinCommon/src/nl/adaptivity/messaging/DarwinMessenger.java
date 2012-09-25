package nl.adaptivity.messaging;

import java.net.URI;
import java.util.concurrent.Future;

import javax.activation.DataSource;
import javax.xml.namespace.QName;


public class DarwinMessenger implements IMessenger {

  @Override
  public void registerEndpoint(QName pService, String pEndPoint, URI pTarget) {
    // TODO Auto-generated method stub

  }

  @Override
  public void registerEndpoint(Endpoint pEndpoint) {
    // TODO Auto-generated method stub

  }

  @Override
  public <T> Future<T> sendMessage(Endpoint pDestination, DataSource pMessage, CompletionListener pCompletionListener) {
    // TODO Auto-generated method stub
    return null;
  }

}
