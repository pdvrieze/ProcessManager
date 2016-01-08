package nl.adaptivity.process.engine;


public class ProcessException extends RuntimeException {
  private static final long serialVersionUID = 4924991215321938319L;

  public ProcessException() {
    super();
  }

  public ProcessException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public ProcessException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ProcessException(final String message) {
    super(message);
  }

  public ProcessException(final Throwable cause) {
    super(cause);
  }

}
