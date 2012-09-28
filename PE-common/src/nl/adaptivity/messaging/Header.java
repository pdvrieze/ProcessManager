package nl.adaptivity.messaging;


public class Header implements nl.adaptivity.messaging.ISendableMessage.IHeader {

  private String aName;
  private String aValue;

  public Header(String pName, String pValue) {
    aName = pName;
    aValue = pValue;
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public String getValue() {
    return aValue;
  }

}
