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

  public MessagingFormatException(final String pMessage) {
    super(HttpServletResponse.SC_BAD_REQUEST, pMessage);
  }

  public MessagingFormatException(final Throwable pCause) {
    super(HttpServletResponse.SC_BAD_REQUEST, pCause);
  }

  public MessagingFormatException(final String pMessage, final Throwable pCause) {
    super(HttpServletResponse.SC_BAD_REQUEST, pMessage, pCause);
  }

}
