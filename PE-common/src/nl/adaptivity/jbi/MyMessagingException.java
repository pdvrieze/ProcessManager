package nl.adaptivity.jbi;


public class MyMessagingException extends RuntimeException {

  private static final long serialVersionUID = -169484940652046250L;

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
