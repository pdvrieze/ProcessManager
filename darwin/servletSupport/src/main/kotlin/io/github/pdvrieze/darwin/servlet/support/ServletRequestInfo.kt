/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.darwin.servlet.support

import uk.ac.bournemouth.darwin.html.RequestInfo
import java.security.Principal
import jakarta.servlet.http.HttpServletRequest

public class ServletRequestInfo(public val request: HttpServletRequest) : RequestInfo {
    override fun getHeader(name: String): String? = request.getHeader(name)

    override fun isUserInRole(role: String): Boolean = request.isUserInRole(role)

    override val userPrincipal: Principal?
        get() = request.userPrincipal

    override val contextPath: String
        get() = request.contextPath

}

