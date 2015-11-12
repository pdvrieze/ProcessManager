package nl.adaptivity.gwt.ext.client;

import com.google.gwt.http.client.RequestException;


public class RemoteListException extends RequestException {

  private static final long serialVersionUID = -4607613016180815639L;

  private final int mStatusCode;

  public RemoteListException(){mStatusCode=-1;}

  public RemoteListException(final int statusCode, final String statusText) {
    super("Error (" + statusCode + "): " + statusText);
    mStatusCode = statusCode;
  }

  public int getStatusCode() {
    return mStatusCode;
  }

}
