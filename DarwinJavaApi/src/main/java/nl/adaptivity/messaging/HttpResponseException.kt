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
package nl.adaptivity.messaging

/**
 * A messaging exception that responds to an http status code.
 *
 * @author Paul de Vrieze
 */
class HttpResponseException : MessagingException {
    val responseCode: Int

    constructor(code: Int, message: String?) : super(message) {
        responseCode = code
    }

    constructor(code: Int, cause: Throwable?) : super(cause) {
        responseCode = code
    }

    constructor(code: Int, message: String?, cause: Throwable?) : super(message, cause) {
        responseCode = code
    }

    companion object {
        private const val serialVersionUID = -1958369502963081324L
    }
}
