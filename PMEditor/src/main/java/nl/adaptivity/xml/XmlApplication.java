package nl.adaptivity.xml;

import android.app.Application;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;


/**
 * Simple application that takes care to register the correct streaming factory.
 */
public class XmlApplication extends Application {

  public XmlApplication() {
    // Don't use standard stax as it is not available on android.
    XmlStreaming.setFactory(new AndroidStreamingFactory());
    // Use the default preference database to store the account name (to enable settings)
    AuthenticatedWebClient.setSharedPreferenceName(null);
  }
}
