package nl.adaptivity.messaging;


/**
 * General exception signifying that something went wrong while processing/sending messages.
 *
 * @author Paul de Vrieze
 */
public class MessagingException extends RuntimeException {

  private static final long serialVersionUID = -5272386729911111109L;

  public MessagingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public MessagingException(final String message) {
    super(message);
  }

  public MessagingException(final Throwable cause) {
    super(cause);
  }

}
