package nl.adaptivity.process.engine;

import javax.servlet.http.HttpServletResponse;

import nl.adaptivity.messaging.HttpResponseException;


/**
 * Exception signalling an error in the kind of message body.
 *
 * @author Paul de Vrieze
 */
public class MessagingFormatException extends HttpResponseException {

  private static final long serialVersionUID = 7931145565871734159L;

  public MessagingFormatException(final String message) {
    super(HttpServletResponse.SC_BAD_REQUEST, message);
  }

  public MessagingFormatException(final Throwable cause) {
    super(HttpServletResponse.SC_BAD_REQUEST, cause);
  }

  public MessagingFormatException(final String message, final Throwable cause) {
    super(HttpServletResponse.SC_BAD_REQUEST, message, cause);
  }

}
