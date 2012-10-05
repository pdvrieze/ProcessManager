package nl.adaptivity.messaging;


public class HttpResponseException extends MessagingException {

  private static final long serialVersionUID = -1958369502963081324L;

  private final int aCode;

  public HttpResponseException(final int pCode, final String pMessage) {
    super(pMessage);
    aCode = pCode;
  }

  public int getResponseCode() {
    return aCode;
  }

}
