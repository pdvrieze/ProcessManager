package nl.adaptivity.messaging;

import java.util.Collection;

import javax.activation.DataSource;


/**
 * Interface signalling that a message can be sent by {@link AsyncMessenger}
 *
 * @author Paul de Vrieze
 */
public interface ISendableMessage {


  /**
   * Class representing a simple HTTP header.
   *
   * @author Paul de Vrieze
   */
  public interface IHeader {

    /**
     * Get the name of the header.
     *
     * @return The header name
     */
    public String getName();

    /**
     * Get the value of the header.
     *
     * @return The header value
     */
    public String getValue();

  }

  /**
   * What should be the destination of the message.
   *
   * @return the url to open. Can be partial!
   */
  Endpoint getDestination();

  /**
   * What method should be used for the message.
   *
   * @return <code>null</code> if default, otherwise the method (in uppercase)
   */
  String getMethod();

  /** Get the headers needing to be set on the request. */
  Collection<? extends IHeader> getHeaders();

  /**
   * Get the source that represents the body of the message.
   *
   * @return The body of the message. Returns <code>null</code> if there is no
   *         body.
   */
  DataSource getBodySource();

}
