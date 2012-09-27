package nl.adaptivity.messaging;


public class MessagingException extends RuntimeException {

  public MessagingException(String pMessage, Throwable pCause) {
    super(pMessage, pCause);
  }

  public MessagingException(String pMessage) {
    super(pMessage);
  }

  public MessagingException(Throwable pCause) {
    super(pCause);
  }

}
