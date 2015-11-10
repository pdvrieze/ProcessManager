package nl.adaptivity.process.processModel;


public class IllegalProcessModelException extends RuntimeException {

  private static final long serialVersionUID = 4026941365873864428L;

  public IllegalProcessModelException() {
    super();
  }

  public IllegalProcessModelException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public IllegalProcessModelException(final String message) {
    super(message);
  }

  public IllegalProcessModelException(final Throwable cause) {
    super(cause);
  }

}
