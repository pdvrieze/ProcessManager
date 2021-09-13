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

@file:Suppress("DEPRECATION")

package nl.adaptivity.messaging

import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.xml.QName

/**
 * Interface describing service endpoints.
 *
 * @author Paul de Vrieze
 */

interface EndpointDescriptor {

    /**
     * The QName of the service
     * @return The qname.
     */
    val serviceName: QName?

    /**
     * The actual endpoint in the service provided.
     * @return The name of the endpoint
     */
    val endpointName: String?

    /**
     * Get the URI for this endpoint. If this returns null, that means messages
     * may not be able to be delivered in all cases.
     * @return The uri through which the service can be accessed.
     */
    val endpointLocation: URI?

    /**
     * Determine whether the service is the same. In other words, whether it has the same name and endpoint.
     * @param other The service to compare with
     * @return `true` if it is, `false` if it isn't.
     */
    fun isSameService(other: EndpointDescriptor?): Boolean

}
