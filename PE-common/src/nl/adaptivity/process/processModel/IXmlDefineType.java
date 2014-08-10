package nl.adaptivity.process.processModel;

import java.util.List;


public interface IXmlDefineType {

  /**
   * May contain literal elements as content. In that case only the paramName
   * attribute is used.Gets the value of the content property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the content property.
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getContent().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link Object }
   * {@link String }
   */
  public List<Object> getContent();

  /**
   * Gets the value of the node property.
   *
   * @return possible object is {@link String }
   */
  public String getRefNode();

  /**
   * Sets the value of the node property.
   *
   * @param value allowed object is {@link String }
   */
  public void setRefNode(String value);

  /**
   * Gets the value of the name property.
   *
   * @return possible object is {@link String }
   */
  public String getRefName();

  /**
   * Sets the value of the name property.
   *
   * @param value allowed object is {@link String }
   */
  public void setRefName(String value);

  /**
   * Gets the value of the paramName property.
   *
   * @return possible object is {@link String }
   */
  public String getName();

  /**
   * Sets the value of the paramName property.
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