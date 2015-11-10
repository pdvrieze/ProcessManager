package nl.adaptivity.messaging;

/**
 * A messaging exception that responds to an http status code.
 *
 * @author Paul de Vrieze
 */
public class HttpResponseException extends MessagingException {

  private static final long serialVersionUID = -1958369502963081324L;

  private final int aCode;

  public HttpResponseException(final int code, final String message) {
    super(message);
    aCode = code;
  }

  public HttpResponseException(int code, Throwable cause) {
    super(cause);
    aCode = code;
  }

  public HttpResponseException(int code, String message, Throwable cause) {
    super(message, cause);
    aCode = code;
  }

  public int getResponseCode() {
    return aCode;
  }

}
