/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.messaging;

import nl.adaptivity.messaging.Endpoint;
import nl.adaptivity.rest.annotations.RestMethod;

import javax.jws.WebMethod;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.xml.namespace.QName;


/**
 * Marker interface for classes that can function as service endpoints. The
 * class should have some method annotated with {@link RestMethod} or
 * {@link WebMethod} or both, as these annotations are used by
 * {@link EndpointServlet} to actually link up these services.
 * 
 * @author Paul de Vrieze
 */
public interface GenericEndpoint extends Endpoint {

  /**
   * The QName of the service
   * 
   * @return The qname.
   */
  @Override
  QName getServiceName();

  /**
   * The actual endpoint in the service provided.
   * 
   * @return
   */
  @Override
  String getEndpointName();

  /**
   * Called when the endpoint needs to clean up. This is called when the
   * {@link EndpointServlet Servlet} needs to clean up.
   */
  void destroy();

  /**
   * Called to initialize the endpoint to a given context. This can not be
   * called init as that would conflict with {@link Servlet#init(ServletConfig)}
   * 
   * @param config The configuration information to use
   */
  void initEndpoint(ServletConfig config);

}
