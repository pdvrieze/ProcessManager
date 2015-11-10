package nl.adaptivity.messaging;


public class Header implements nl.adaptivity.messaging.ISendableMessage.IHeader {

  private final String mName;

  private final String mValue;

  public Header(final String name, final String value) {
    mName = name;
    mValue = value;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public String getValue() {
    return mValue;
  }

}
