package nl.adaptivity.process.processModel;


public class IllegalProcessModelException extends RuntimeException {

  private static final long serialVersionUID = 4026941365873864428L;

  public IllegalProcessModelException() {
    super();
  }

  public IllegalProcessModelException(String pMessage, Throwable pCause) {
    super(pMessage, pCause);
  }

  public IllegalProcessModelException(String pMessage) {
    super(pMessage);
  }

  public IllegalProcessModelException(Throwable pCause) {
    super(pCause);
  }

}
