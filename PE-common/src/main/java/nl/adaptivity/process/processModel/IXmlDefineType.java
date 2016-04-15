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

import net.devrieze.util.Transaction;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.xml.Namespace;
import nl.adaptivity.xml.XmlSerializable;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;


public interface IXmlDefineType extends XmlSerializable {

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