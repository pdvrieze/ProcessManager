package nl.adaptivity.process.messaging;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;


/**
 * Marker interface for classes that can provide a set of endpoints.
 * 
 * @author Paul de Vrieze
 * @deprecated Does not work currently in servlet context as they are
 *             declarative.
 */
@Deprecated
public interface EndpointProvider {

  /**
   * Get the endpoints provided.
   * 
   * @return The collection
   */
  @NotNull
  Collection<GenericEndpoint> getEndpoints();

}
