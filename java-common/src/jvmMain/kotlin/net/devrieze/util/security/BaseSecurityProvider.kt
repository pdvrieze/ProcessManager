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

import net.devrieze.util.security.SecurityProvider.PermissionResult.*
import java.security.Principal


/**
 * Security provider that allows everything.
 *
 * @author Paul de Vrieze
 */
abstract class BaseSecurityProvider : SecurityProvider {

    override fun ensurePermission(permission: SecurityProvider.Permission,
                                  subject: Principal?): SecurityProvider.PermissionResult {
        return ensurePermission(getPermission(permission, subject),
                                " denied permission to ${subject?.name}. to perform $permission To allow this set a security provider.")
    }

    private fun ensurePermission(permissionResult: SecurityProvider.PermissionResult,
                                 deniedMessage: String): SecurityProvider.PermissionResult {
        when (permissionResult) {
            GRANTED -> return GRANTED

            DENIED  -> throw PermissionDeniedException(javaClass.simpleName + deniedMessage)

            UNAUTHENTICATED
                    -> throw AuthenticationNeededException("For permissions to be available, authentication is needed")
        }
    }

    override fun ensurePermission(permission: SecurityProvider.Permission,
                                  subject: Principal?,
                                  objectPrincipal: Principal): SecurityProvider.PermissionResult {
        return ensurePermission(getPermission(permission, subject, objectPrincipal),
                                " denied permission to ${subject?.name} to perform $permission in relation to role ${objectPrincipal.name}. To allow this set a security provider.")
    }

    override fun ensurePermission(permission: SecurityProvider.Permission,
                                  subject: Principal?,
                                  secureObject: SecuredObject<*>): SecurityProvider.PermissionResult {
        return ensurePermission(getPermission(permission, subject, secureObject),
                                " denied permission to ${subject?.name} to perform $permission on $secureObject. To allow this set a security provider.")
    }

    override fun hasPermission(permission: SecurityProvider.Permission,
                               subject: Principal,
                               secureObject: SecuredObject<*>): Boolean {
        return getPermission(permission, subject, secureObject) === GRANTED
    }

    override fun hasPermission(permission: SecurityProvider.Permission, subject: Principal): Boolean {
        return getPermission(permission, subject) === GRANTED
    }

    override fun hasPermission(permission: SecurityProvider.Permission,
                               subject: Principal,
                               objectPrincipal: Principal): Boolean {
        return getPermission(permission, subject, objectPrincipal) === GRANTED
    }
}
