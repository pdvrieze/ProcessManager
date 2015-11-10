package nl.adaptivity.messaging;

/**
 * A messaging exception that responds to an http status code.
 *
 * @author Paul de Vrieze
 */
public class HttpResponseException extends MessagingException {

  private static final long serialVersionUID = -1958369502963081324L;

  private final int mCode;

  public HttpResponseException(final int code, final String message) {
    super(message);
    mCode = code;
  }

  public HttpResponseException(int code, Throwable cause) {
    super(cause);
    mCode = code;
  }

  public HttpResponseException(int code, String message, Throwable cause) {
    super(message, cause);
    mCode = code;
  }

  public int getResponseCode() {
    return mCode;
  }

}
