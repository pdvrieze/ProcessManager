package nl.adaptivity.process.engine;


public class MyMessagingException extends RuntimeException {

  private static final long serialVersionUID = -2048040838045843869L;

  public MyMessagingException(final String pMessage) {
    super(pMessage);
  }

  public MyMessagingException(final Throwable pCause) {
    super(pCause);
  }

  public MyMessagingException(final String pMessage, final Throwable pCause) {
    super(pMessage, pCause);
  }

}
