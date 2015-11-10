package nl.adaptivity.process.processModel;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.util.xml.Namespace;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;


public interface IXmlDefineType {

  @Nullable
  <T extends IProcessNodeInstance<T>> ProcessData apply(Transaction transaction, IProcessNodeInstance<T> node) throws
          SQLException;

  char[] getContent();

  /**
   * Gets the value of the node property.
   *
   * @return possible object is {@link String }
   */
  String getRefNode();

  /**
   * Sets the value of the node property.
   *
   * @param value allowed object is {@link String }
   */
  void setRefNode(String value);

  /**
   * Gets the value of the name property.
   *
   * @return possible object is {@link String }
   */
  String getRefName();

  /**
   * Sets the value of the name property.
   *
   * @param value allowed object is {@link String }
   */
  void setRefName(String value);

  /**
   * Gets the value of the paramName property.
   *
   * @return possible object is {@link String }
   */
  String getName();

  /**
   * Sets the value of the paramName property.
   *
   * @param value allowed object is {@link String }
   */
  void setName(String value);

  /**
   * Gets the value of the path property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  String getPath();

  /**
   * Sets the value of the path property.
   *
   * @param namespaceContext
   * @param value allowed object is {@link String }
   */
  void setPath(final Iterable<Namespace> namespaceContext, String value);

  /**
   * Get the namespace context that defines the "missing" namespaces in the content.
   * @return
   */
  @Nullable
  Iterable<Namespace> getOriginalNSContext();

}