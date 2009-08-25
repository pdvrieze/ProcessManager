package nl.adaptivity.process.engine.processModel;


public class IllegalProcessModelException extends RuntimeException {

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
