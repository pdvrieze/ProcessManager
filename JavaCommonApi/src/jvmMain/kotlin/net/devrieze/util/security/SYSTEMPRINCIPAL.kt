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

import java.security.SecureRandom
import kotlin.jvm.JvmField

actual object SYSTEMPRINCIPAL : RolePrincipal {

    @JvmField
    actual val NS = "urn:SYSTEMPRINCIPAL"
    @JvmField
    actual val TAG = "systemPrincipal"

    val KEY by lazy { SecureRandom.getInstance("NativePRNGNonBlocking").nextLong() }

    actual override fun hasRole(role: String): Boolean {
        return true
    }

    actual override fun getName(): String {
        return "<SYSTEM PRINCIPAL>"
    }

    override fun toString(): String {
        return getName()
    }
}
