package nl.adaptivity.process.engine;


public class InvalidMessageException extends Exception {

  private static final long serialVersionUID = -2375058242935530666L;
  
  private final IMessage aMessage;

  public InvalidMessageException(IMessage pMessage) {
    super();
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, IMessage pMessage, Throwable pCause) {
    super(pErrorMessage, pCause);
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, IMessage pMessage) {
    super(pErrorMessage);
    aMessage = pMessage;
  }

  public InvalidMessageException(IMessage pMessage, Throwable pCause) {
    super(pCause);
    aMessage = pMessage;
  }
  
  public IMessage getMsgCause() {
    return aMessage;
  }

}
