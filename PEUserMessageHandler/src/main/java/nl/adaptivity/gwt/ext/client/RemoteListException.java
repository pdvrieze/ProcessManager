package nl.adaptivity.gwt.ext.client;

import com.google.gwt.http.client.RequestException;


public class RemoteListException extends RequestException {

  private static final long serialVersionUID = -4607613016180815639L;

  private final int aStatusCode;

  public RemoteListException(final int statusCode, final String statusText) {
    super("Error (" + statusCode + "): " + statusText);
    aStatusCode = statusCode;
  }

  public int getStatusCode() {
    return aStatusCode;
  }

}
