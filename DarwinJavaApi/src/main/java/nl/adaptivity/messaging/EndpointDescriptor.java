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

package nl.adaptivity.messaging;

import javax.xml.namespace.QName;

import java.net.URI;

/**
 * Interface describing service endpoints.
 *
 * @author Paul de Vrieze
 *
 */

public interface EndpointDescriptor {

  /**
   * The QName of the service
   * @return The qname.
   */
  QName getServiceName();

  /**
   * The actual endpoint in the service provided.
   * @return The name of the endpoint
   */
  String getEndpointName();

  /**
   * Get the URI for this endpoint. If this returns null, that means messages
   * may not be able to be delivered in all cases.
   * @return The uri through which the service can be accessed.
   */
  URI getEndpointLocation();

}
