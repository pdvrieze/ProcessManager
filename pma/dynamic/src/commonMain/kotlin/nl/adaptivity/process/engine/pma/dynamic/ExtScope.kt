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

package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.EMPTYSCOPE
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.engine.pma.models.UseAuthScope

data class ExtScope<V>(val scope: AuthScope, val extraData: V) :
    AuthScope, UseAuthScope {
    override val description: String get() = toString()

    override fun includes(useScope: UseAuthScope): Boolean {
        return this == useScope
    }

    override fun intersect(otherScope: AuthScope): AuthScope =
        when {
            this == otherScope || otherScope == scope -> this

            otherScope is UnionPermissionScope -> otherScope.intersect(this)

            otherScope is ExtScope<*>
                && extraData == otherScope.extraData -> {

                when (val effectiveScope = scope.intersect(otherScope.scope)) {
                    EMPTYSCOPE -> EMPTYSCOPE
                    else -> ExtScope(effectiveScope, extraData)
                }
            }

            else -> super<AuthScope>.intersect(otherScope)
        }

    override fun union(otherScope: AuthScope): AuthScope =
        when {
            this == otherScope -> this
            else -> UnionPermissionScope(listOf(this, otherScope))
        }

    override fun toString(): String {
        return "${scope.description}($extraData)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtScope<*>

        if (scope != other.scope) return false
        if (extraData != other.extraData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scope.hashCode()
        result = 31 * result + (extraData?.hashCode() ?: 0)
        return result
    }
}

