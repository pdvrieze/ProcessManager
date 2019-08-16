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

import java.lang.IllegalArgumentException

data class ExtScope<V>(val scope: AuthScope, val extraData: V) :
    PermissionScope, UseAuthScope {
    override val description: String get() = toString()

    override fun includes(useScope: UseAuthScope): Boolean {
        if (useScope !is ExtScope<*>) return false
        val myScope = this.scope
        if (myScope !is PermissionScope) return false
        val requestedSubScope = useScope.scope
        if (requestedSubScope !is UseAuthScope) throw IllegalArgumentException("the passed scope's subscope cannot be used on use site")
        return myScope.includes(requestedSubScope) && extraData == useScope.extraData
    }

    override fun intersect(otherScope: PermissionScope): PermissionScope? = when {
        otherScope is ExtScope<*> && includes(otherScope) -> otherScope
        else                                              -> null
    }

    override fun union(otherScope: PermissionScope): PermissionScope = when {
        otherScope is ExtScope<*> &&
            scope == otherScope.scope &&
            extraData == otherScope.extraData -> this

        else                                  -> UnionPermissionScope(listOf(this, otherScope))
    }

    override fun toString(): String {
        return "${scope.description}($extraData)"
    }
}

