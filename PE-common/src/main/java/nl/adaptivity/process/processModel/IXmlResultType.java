/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel;

import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.util.xml.Namespace;
import nl.adaptivity.util.xml.XmlSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;


public interface IXmlResultType extends XmlSerializable{

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