package nl.adaptivity.xml;

import android.app.Application;


/**
 * Simple application that takes care to register the correct streaming factory.
 */
public class XmlApplication extends Application {

  public XmlApplication() {
    XmlStreaming.setFactory(new AndroidStreamingFactory());
  }
}
