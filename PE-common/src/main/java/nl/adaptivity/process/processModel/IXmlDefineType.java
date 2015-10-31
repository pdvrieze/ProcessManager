package nl.adaptivity.process.processModel;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.exec.IProcessNodeInstance;

import javax.xml.namespace.NamespaceContext;

import java.sql.SQLException;
import java.util.List;


public interface IXmlDefineType {

  <T extends IProcessNodeInstance<T>> ProcessData apply(Transaction pTransaction, IProcessNodeInstance<T> pNode) throws
          SQLException;

  public char[] getContent();

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

  /**
   * Get the namespace context that defines the "missing" namespaces in the content.
   * @return
   */
  public NamespaceContext getOriginalNSContext();

}