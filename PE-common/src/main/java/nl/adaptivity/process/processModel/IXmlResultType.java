package nl.adaptivity.process.processModel;

import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;


public interface IXmlResultType {

  char[] getContent();

  /**
   * Gets the value of the name property.
   *
   * @return possible object is {@link String }
   */
  String getName();

  /**
   * Sets the value of the name property.
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

  @NotNull
  ProcessData apply(Node payload);

  /**
   * Get the namespace context for evaluating the xpath expression.
   * @return the context
   */
  @Nullable
  Iterable<Namespace> getOriginalNSContext();
}