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

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.util.xml.CompactFragment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;


public interface IXmlMessage {

  /**
   * Gets the value of the service property.
   *
   * @return possible object is {@link QName }
   */
  @Nullable
  String getServiceName();

  void setServiceName(String name);

  @Nullable
  String getServiceNS();

  void setServiceNS(String namespace);

  QName getService();

  /**
   * Sets the value of the service property.
   *
   * @param value allowed object is {@link QName }
   */
  void setService(QName value);

  /**
   * Gets the value of the endpoint property.
   *
   * @return possible object is {@link String }
   */
  String getEndpoint();

  /**
   * Sets the value of the endpoint property.
   *
   * @param value allowed object is {@link String }
   */
  void setEndpoint(String value);

  @Nullable
  EndpointDescriptor getEndpointDescriptor();

  /**
   * Gets the value of the operation property.
   *
   * @return possible object is {@link String }
   */
  String getOperation();

  @NotNull
  CompactFragment getMessageBody();

  /**
   * Sets the value of the operation property.
   *
   * @param value allowed object is {@link String }
   */
  void setOperation(String value);

  /**
   * Gets the value of the url property.
   *
   * @return possible object is {@link String }
   */
  String getUrl();

  /**
   * Sets the value of the url property.
   *
   * @param value allowed object is {@link String }
   */
  void setUrl(String value);

  /**
   * Gets the value of the method property.
   *
   * @return possible object is {@link String }
   */
  String getMethod();

  /**
   * Sets the value of the method property.
   *
   * @param value allowed object is {@link String }
   */
  void setMethod(String value);

  String getContentType();

  void setType(String type);

  @Override
  String toString();

}