package nl.adaptivity.process.engine;

import java.io.Serializable;


public class Payload implements Serializable {
  
  private static final long serialVersionUID = -772414281618180765L;

  private final String aPayload;

  private Payload(String pPayload) {
    aPayload = pPayload;
  }

  public Payload(Object pPayload) {
    aPayload = pPayload.toString();
  }

  public static Payload create(String pPayload) {
    return new Payload(pPayload);
  }
  
  @Override
  public String toString() {
    return aPayload;
  }

}
