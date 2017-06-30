/*
 * Copyright (c) 2017.
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

import java.security.SecureRandom

/**
 * Created by pdvrieze on 22/06/17.
 */
object SYSTEMPRINCIPAL : RolePrincipal {

  public const val NS = "urn:SYSTEMPRINCIPAL"
  public const val TAG = "systemPrincipal"

  public val KEY by lazy { SecureRandom.getInstance("NativePRNGNonBlocking").nextLong() }

  override fun hasRole(role: String): Boolean {
    return true
  }

  override fun getName(): String {
    return "<SYSTEM PRINCIPAL>"
  }

  override fun toString(): String {
    return name
  }
}