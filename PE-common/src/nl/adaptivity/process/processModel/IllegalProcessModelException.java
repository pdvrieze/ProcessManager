package nl.adaptivity.process.processModel;


public class IllegalProcessModelException extends RuntimeException {

  private static final long serialVersionUID = 4026941365873864428L;

  public IllegalProcessModelException() {
    super();
  }

  public IllegalProcessModelException(final String pMessage, final Throwable pCause) {
    super(pMessage, pCause);
  }

  public IllegalProcessModelException(final String pMessage) {
    super(pMessage);
  }

  public IllegalProcessModelException(final Throwable pCause) {
    super(pCause);
  }

}
