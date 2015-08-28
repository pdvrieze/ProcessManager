package nl.adaptivity.process.processModel;

import java.util.List;

import nl.adaptivity.process.engine.ProcessData;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;


public interface IXmlResultType {

  char[] getContent();

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

  ProcessData apply(Node pPayload);

  /**
   * Get the namespace context for evaluating the xpath expression.
   * @return the context
   */
  NamespaceContext getNamespaceContext();
}