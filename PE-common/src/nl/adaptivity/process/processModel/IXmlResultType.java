package nl.adaptivity.process.processModel;

import java.util.List;

public interface IXmlResultType {

  List<Object> getContent();

  /**
   * Gets the value of the name property.
   *
   * @return possible object is {@link String }
   */
  public String getName();

  /**
   * Sets the value of the name property.
   *
   * @param value allowed object is {@link String }
   */
  public void setName(String value);

  /**
   * Gets the value of the path property.
   *
   * @return possible object is {@link String }
   */
  public String getPath();

  /**
   * Sets the value of the path property.
   *
   * @param value allowed object is {@link String }
   */
  public void setPath(String value);

}