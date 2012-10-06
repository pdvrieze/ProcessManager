package nl.adaptivity.messaging;


public class Header implements nl.adaptivity.messaging.ISendableMessage.IHeader {

  private final String aName;

  private final String aValue;

  public Header(final String pName, final String pValue) {
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
