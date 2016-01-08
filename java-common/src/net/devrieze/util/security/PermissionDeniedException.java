package net.devrieze.util.security;


public class PermissionDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1782673055725449807L;

  public PermissionDeniedException(final String pMessage, final Throwable pCause, final boolean pEnableSuppression, final boolean pWritableStackTrace) {
    super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
  }

  public PermissionDeniedException(final String pMessage, final Throwable pCause) {
    super(pMessage, pCause);
  }

  public PermissionDeniedException(final String pMessage) {
    super(pMessage);
  }

  public PermissionDeniedException(final Throwable pCause) {
    super(pCause);
  }

}
