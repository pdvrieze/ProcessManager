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

package nl.adaptivity.rest.annotations


/**
 *
 * @property post Expressions that put conditions on post parameters
 * @property get Expressions that put conditions on get parameters
 * @property query Expressions that put conditions on any request parameters (post or get)
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RestMethod(val method: HttpMethod,
                            val path: String,
                            val post: Array<String> = arrayOf(),
                            val get: Array<String> = arrayOf(),
                            val query: Array<String> = arrayOf(),
                            val contentType: String = "")

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD
}
