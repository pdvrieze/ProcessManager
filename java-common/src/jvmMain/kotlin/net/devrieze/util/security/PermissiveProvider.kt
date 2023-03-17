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

package net.devrieze.util.security

import java.security.Principal


/**
 * Security provider that allows everything.
 *
 * @author Paul de Vrieze
 */
class PermissiveProvider : BaseSecurityProvider() {

    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: Principal?,
        secureObject: SecuredObject<*>
    ): SecurityProvider.PermissionResult {
        return SecurityProvider.PermissionResult.GRANTED
    }

    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: Principal?
    ): SecurityProvider.PermissionResult {
        return SecurityProvider.PermissionResult.GRANTED
    }

    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: Principal?,
        objectPrincipal: Principal
    ): SecurityProvider.PermissionResult {
        return SecurityProvider.PermissionResult.GRANTED
    }

}
