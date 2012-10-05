package nl.adaptivity.messaging;


public class MessagingException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = -5272386729911111109L;

  public MessagingException(final String pMessage, final Throwable pCause) {
    super(pMessage, pCause);
  }

  public MessagingException(final String pMessage) {
    super(pMessage);
  }

  public MessagingException(final Throwable pCause) {
    super(pCause);
  }

}
