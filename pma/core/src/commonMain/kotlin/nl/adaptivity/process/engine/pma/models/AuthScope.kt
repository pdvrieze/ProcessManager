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

package nl.adaptivity.process.engine.pma.models

import net.devrieze.util.security.SecurityProvider.Permission
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.AuthorizationException

interface AuthScopeTemplate<in C: ActivityInstanceContext> {
    fun instantiateScope(context: C): AuthScope?
}

interface AuthScope : AuthScopeTemplate<ActivityInstanceContext> {
    val description: String get() = toString()

    /**
     * All authscopes can be templates as well (they just don't need instantiation).
     */
    override fun instantiateScope(context: ActivityInstanceContext): AuthScope? = this

    /**
     * Determine whether this scope is larger than the passed one. In other words, whether the parameter scope
     * is allowed if this scope is allowed.
     */
    fun includes(useScope: Permission): Boolean  {
        return this == useScope
    }

    /**
     * This will return a scope (if any) that meets both scopes. This is a narrower permission,
     * and may very well be nothing.
     * @return `null` if there is no permission, otherwise a narrower permission.
     */
    fun intersect(otherScope: AuthScope): AuthScope {
        return when {
            otherScope == ANYSCOPE -> this
            otherScope is UnionPermissionScope -> otherScope.intersect(this)
            otherScope is UseAuthScope && includes(otherScope) -> otherScope
            else -> EMPTYSCOPE
        }
    }

    /**
     * This will return a scope that combines both permissions. This is a broader permission.
     */
    fun union(otherScope: AuthScope): AuthScope {
        return when (otherScope) {
            this -> this
            else -> UnionPermissionScope(listOf(this, otherScope))
        }
    }

}

class UnionPermissionScope(members: List<AuthScope>): AuthScope {

    val members: List<AuthScope> = members.filterNot { it == EMPTYSCOPE }.flatMap {
        (it as? UnionPermissionScope)?.members ?: listOf(it)
    }

    constructor(vararg members: AuthScope): this(members.toList())

    override fun includes(useScope: Permission): Boolean {
        return members.any { it.includes(useScope) }
    }

    override fun intersect(otherScope: AuthScope): AuthScope {
        if (otherScope==ANYSCOPE) return this

        val newMembers = members.map {
            it.intersect(otherScope)
        }.filterNot { it == EMPTYSCOPE }

        if (newMembers.isEmpty()) return EMPTYSCOPE
        if (newMembers.size == 1) return newMembers.single()
        return UnionPermissionScope(newMembers)
    }

    override fun union(otherScope: AuthScope): AuthScope =
        when (otherScope) {
            is UnionPermissionScope -> UnionPermissionScope(members + otherScope.members)
            else -> UnionPermissionScope(members + otherScope)
        }

    override val description: String
        get() = members.joinToString(" && ", prefix = "(", postfix = ")") { it.description }

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnionPermissionScope

        if (members.size != other.members.size) return false // compare size before comparing
        val sortedMembers = members.sortedBy { it.description }
        val sortedOtherMembers = other.members.sortedBy { it.description }
        if (sortedMembers != sortedOtherMembers) return false

        return true
    }

    override fun hashCode(): Int {
        return members.hashCode()
    }


}

/**
 * Scope for using permissions
 */
interface UseAuthScope: AuthScope, Permission {

}

object EMPTYSCOPE: AuthScope {
    override val description: String get() = "-"
    override fun includes(useScope: Permission): Boolean = false
    override fun intersect(otherScope: AuthScope): EMPTYSCOPE = EMPTYSCOPE
    override fun union(otherScope: AuthScope): AuthScope = otherScope
    override fun instantiateScope(context: ActivityInstanceContext): EMPTYSCOPE = EMPTYSCOPE
}

object ANYSCOPE: AuthScope {
    override val description get() = "*"
    override fun toString() = "AnyScope"
    override fun includes(useScope: Permission): Boolean {
        throw AuthorizationException("Using the any scope directly is not permitted")
    }

    override fun union(otherScope: AuthScope): AuthScope {
        return this
    }

    override fun intersect(otherScope: AuthScope): AuthScope {
        return otherScope
    }
}
