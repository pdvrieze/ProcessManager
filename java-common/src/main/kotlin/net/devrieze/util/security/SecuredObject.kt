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

package net.devrieze.util.security

import java.security.Principal

/**
 * Interface for objects that will provide interface access with a permission.
 */

interface SecuredObject<out T:Any> {
  fun withPermission():T
}

inline fun <T:SecureObject<T>, R> SecureObject<T>.withPermission(securityProvider: SecurityProvider, permission: SecurityProvider.Permission, subject:Principal, body: (T)->R):R {
  val o = withPermission()
  securityProvider.ensurePermission(permission, subject, o)
  return body(o)
}