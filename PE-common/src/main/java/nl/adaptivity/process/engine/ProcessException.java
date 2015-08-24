package nl.adaptivity.process.engine;


public class ProcessException extends RuntimeException {
  private static final long serialVersionUID = 4924991215321938319L;

  public ProcessException() {
    super();
  }

  public ProcessException(String pMessage, Throwable pCause, boolean pEnableSuppression, boolean pWritableStackTrace) {
    super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
  }

  public ProcessException(String pMessage, Throwable pCause) {
    super(pMessage, pCause);
  }

  public ProcessException(String pMessage) {
    super(pMessage);
  }

  public ProcessException(Throwable pCause) {
    super(pCause);
  }

}
