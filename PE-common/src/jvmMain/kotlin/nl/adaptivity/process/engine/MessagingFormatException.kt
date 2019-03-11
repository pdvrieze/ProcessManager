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

package nl.adaptivity.process.engine

import nl.adaptivity.messaging.HttpResponseException

import javax.servlet.http.HttpServletResponse


/**
 * Exception signalling an error in the kind of message body.
 *
 * @author Paul de Vrieze
 */
class MessagingFormatException : HttpResponseException {

    constructor(message: String) : super(HttpServletResponse.SC_BAD_REQUEST, message)

    constructor(cause: Throwable) : super(HttpServletResponse.SC_BAD_REQUEST, cause)

    constructor(message: String, cause: Throwable) : super(HttpServletResponse.SC_BAD_REQUEST, message, cause)

    companion object {

        private const val serialVersionUID = 7931145565871734159L
    }

}
