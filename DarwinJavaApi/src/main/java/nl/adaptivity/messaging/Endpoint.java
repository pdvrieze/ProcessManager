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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.messaging;

import javax.jws.WebMethod;


/**
 * Interface for classes that can function as service endpoints. The class
 * should have some method annotated with <code>RestMethod</code> or
 * {@link WebMethod} or both, as these annotations are used by
 * <code>EndpointServlet</code> to actually link up these services.
 *
 * @author Paul de Vrieze
 */
public interface Endpoint extends EndpointDescriptor {
  // This is a marker interface that shares the methods with descriptor
  // and can be self-describing, but can actually handle being an endpoint
}
