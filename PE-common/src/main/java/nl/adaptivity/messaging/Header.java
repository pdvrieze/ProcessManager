package nl.adaptivity.messaging;


public class Header implements nl.adaptivity.messaging.ISendableMessage.IHeader {

  private final String mName;

  private final String aValue;

  public Header(final String name, final String value) {
    mName = name;
    aValue = value;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public String getValue() {
    return aValue;
  }

}
