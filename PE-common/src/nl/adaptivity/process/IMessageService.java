package nl.adaptivity.process;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IXmlMessage;


/**
 * Interface signifying that the object can be used to send messages.
 *
 * @author Paul de Vrieze
 * @param <T> The type signifying a message that can then be sent.
 * @param <U> The task that the message corresponds to. This allows for messages
 *          to be linked to tasks.
 */
public interface IMessageService<T, U extends IProcessNodeInstance<U>> {

  /**
   * Create a message.
   *
   * @param pMessage The message to create (for later sending)
   * @return The sendable message that can be sent.
   */
  T createMessage(IXmlMessage pMessage);

  /**
   * Send a message.
   *
   * @param pMessage The message to send. (Created by
   *          {@link #createMessage(IXmlMessage)}).
   * @param pInstance The task instance to link the sending to.
   * @return <code>true</code> on success, <code>false</code> on failure.
   */
  boolean sendMessage(T pMessage, U pInstance);

  /**
   * Get the endpoint belonging to the messenger. (Where can replies go)
   * @return The descriptor of the local endpoint.
   */
  EndpointDescriptor getLocalEndpoint();
}
