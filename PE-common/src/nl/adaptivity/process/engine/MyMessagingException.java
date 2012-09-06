package nl.adaptivity.process.engine;


public class MyMessagingException extends RuntimeException {

  private static final long serialVersionUID = -2048040838045843869L;

  public MyMessagingException(String pMessage) {
    super(pMessage);
  }

  public MyMessagingException(Throwable pCause) {
    super(pCause);
  }

  public MyMessagingException(String pMessage, Throwable pCause) {
    super(pMessage, pCause);
  }

}
