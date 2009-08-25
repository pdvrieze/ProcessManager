package nl.adaptivity.process.engine;


public class InvalidMessageException extends Exception {

  private static final long serialVersionUID = -2375058242935530666L;
  
  private final ExtMessage aMessage;

  public InvalidMessageException(ExtMessage pMessage) {
    super();
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, ExtMessage pMessage, Throwable pCause) {
    super(pErrorMessage, pCause);
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, ExtMessage pMessage) {
    super(pErrorMessage);
    aMessage = pMessage;
  }

  public InvalidMessageException(ExtMessage pMessage, Throwable pCause) {
    super(pCause);
    aMessage = pMessage;
  }
  
  public ExtMessage getMsgCause() {
    return aMessage;
  }

}
