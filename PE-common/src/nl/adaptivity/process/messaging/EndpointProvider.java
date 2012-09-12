package nl.adaptivity.process.messaging;

import java.util.Collection;


public interface EndpointProvider {

  Collection<GenericEndpoint> getEndpoints();

  void setContext(AsyncMessenger pContext);

}
