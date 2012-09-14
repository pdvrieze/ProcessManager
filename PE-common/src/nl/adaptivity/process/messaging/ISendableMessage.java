package nl.adaptivity.process.messaging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import net.devrieze.util.Tupple;


public interface ISendableMessage {

  /**
   * What should be the destination of the message.
   * @return the url to open. Can be partial!
   */
  String getDestination();

  /**
   * What method should be used for the message
   * @return <code>null</code> if default, otherwise the method (in uppercase)
   */
  String getMethod();

  /**
   * Determine whether there is a body in the message.
   * @return <code>true</code> if the message has a body, <code>false</code> if not.
   */
  boolean hasBody();

  /** Get the headers needing to be set on the request. */
  Collection<Tupple<String, String>> getHeaders();

  /**
   * Write the body to the outputstream
   * @param pOutputStream
   * @throws IOException When writing somehow fails
   */
  void writeBody(OutputStream pOutputStream) throws IOException;

}
