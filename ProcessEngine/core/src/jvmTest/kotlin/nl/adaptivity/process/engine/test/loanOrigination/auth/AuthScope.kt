/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine.test.loanOrigination.auth

interface AuthScope {
    /**
     * Determine whether this scope is larger than the passed one. In other words, whether the parameter scope
     * is allowed if this scope is allowed.
     */
    fun includes(scope: AuthScope): Boolean

    val description: String
}

fun AuthScope(description: String) = object : AuthScope {
    override val description: String get() = description
    override fun includes(scope: AuthScope): Boolean {
        return scope===this
    }
}
