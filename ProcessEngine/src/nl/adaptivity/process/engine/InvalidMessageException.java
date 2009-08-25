package nl.adaptivity.process.engine;


public class InvalidMessageException extends Exception {

  private static final long serialVersionUID = -2375058242935530666L;
  
  private final IExtMessage aMessage;

  public InvalidMessageException(IExtMessage pMessage) {
    super();
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, IExtMessage pMessage, Throwable pCause) {
    super(pErrorMessage, pCause);
    aMessage = pMessage;
  }

  public InvalidMessageException(String pErrorMessage, IExtMessage pMessage) {
    super(pErrorMessage);
    aMessage = pMessage;
  }

  public InvalidMessageException(IExtMessage pMessage, Throwable pCause) {
    super(pCause);
    aMessage = pMessage;
  }
  
  public IExtMessage getMsgCause() {
    return aMessage;
  }

}
