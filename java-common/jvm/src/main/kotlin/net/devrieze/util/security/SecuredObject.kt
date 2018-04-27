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

import net.devrieze.util.security.SecurityProvider.Permission
import java.security.Principal

/**
 * Interface for objects that will provide interface access with a permission.
 */

interface SecuredObject<out T> {
  fun withPermission():T
}

private val defaultAlternate = fun  (securityProvider: SecurityProvider, permission: Permission, subject: Principal, o: SecureObject<*>) {securityProvider.ensurePermission(permission, subject, o); throw IllegalStateException() }

inline fun <T:SecureObject<T>, R> SecureObject<T>.withPermission(securityProvider: SecurityProvider, permission: Permission, subject:Principal, body: (T)->R):R {
  securityProvider.ensurePermission(permission, subject, this)
  return body(withPermission())
}

inline fun <T:SecureObject<T>, R> SecureObject<T>.withPermission(securityProvider: SecurityProvider, permission: Permission, subject:Principal, alternate: (SecurityProvider, Permission, Principal, SecureObject<T>)->R, body: (T)->R):R {
  return when {
    securityProvider.hasPermission(permission, subject, this) -> body(this.withPermission())
    else -> alternate(securityProvider, permission, subject, this)
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T:SecureObject<T>> SecureObject<T>.ifPermitted(securityProvider: SecurityProvider, permission: Permission, subject:Principal):T? {
  return when {
    securityProvider.hasPermission(permission, subject, this) -> withPermission()
    else -> null
  }
}
