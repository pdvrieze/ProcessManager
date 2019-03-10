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

import net.devrieze.util.security.SecurityProvider.PermissionResult
import java.security.Principal

/**
 * SecurityProvider that is sensible in allowing only owners and admins to perform operations.
 */
class OwnerOnlySecurityProvider(val adminRoles:Set<String>) : BaseSecurityProvider() {

  constructor(vararg adminRoles: String): this(setOf(*adminRoles))

  override fun getPermission(permission: SecurityProvider.Permission, subject: Principal?, secureObject: SecureObject<*>): PermissionResult {
    if (subject==null) return PermissionResult.UNAUTHENTICATED
    if (subject is SYSTEMPRINCIPAL) { return PermissionResult.GRANTED }
    val owner = secureObject.owner
    if (owner is SYSTEMPRINCIPAL) {
      if (subject is RolePrincipal && adminRoles.any { subject.hasRole(it) }) return PermissionResult.GRANTED

      return PermissionResult.DENIED
    }

    if (subject.name == owner.name) { return PermissionResult.GRANTED }
    return PermissionResult.DENIED
  }

  override fun getPermission(permission: SecurityProvider.Permission,
                             subject: Principal?): PermissionResult {
    return if (subject==null) PermissionResult.UNAUTHENTICATED else PermissionResult.GRANTED
  }

  override fun getPermission(permission: SecurityProvider.Permission, subject: Principal?, objectPrincipal: Principal): PermissionResult {
    if (subject==null) return PermissionResult.UNAUTHENTICATED
    if (subject== net.devrieze.util.security.SYSTEMPRINCIPAL) { return PermissionResult.GRANTED }
    if (subject == objectPrincipal) { return PermissionResult.GRANTED }
    if (subject is RolePrincipal) { if (adminRoles.any { subject.hasRole(it) }) return PermissionResult.GRANTED }
    return PermissionResult.DENIED
  }
}