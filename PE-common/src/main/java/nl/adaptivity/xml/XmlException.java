package nl.adaptivity.xml;

/**
 * Created by pdvrieze on 15/11/15.
 */
public class XmlException extends Exception {

  public XmlException() {
  }

  public XmlException(final String message) {
    super(message);
  }

  public XmlException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public XmlException(final Throwable cause) {
    super(cause);
  }
}
