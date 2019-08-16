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
    val description: String get() = toString()
}

/**
 * Scope for permissions that are granted
 */
interface PermissionScope: AuthScope {
    /**
     * Determine whether this scope is larger than the passed one. In other words, whether the parameter scope
     * is allowed if this scope is allowed.
     */
    fun includes(useScope: UseAuthScope): Boolean

    fun intersect(otherScope: PermissionScope) : PermissionScope?
    fun union(otherScope: PermissionScope): PermissionScope
}

class UnionPermissionScope(members: List<PermissionScope>): PermissionScope {
    val members: List<PermissionScope> = members.flatMap {
        (it as? UnionPermissionScope)?.members ?: listOf(it)
    }

    override fun includes(useScope: UseAuthScope): Boolean {
        return members.any { it.includes(useScope) }
    }

    override fun intersect(otherScope: PermissionScope): PermissionScope? {
        val newMembers = members.mapNotNull { it.intersect(otherScope) }
        if(newMembers.isEmpty()) return null
        if (newMembers.size==1) return newMembers.single()
        return UnionPermissionScope(newMembers)
    }

    override fun union(otherScope: PermissionScope): PermissionScope = when (otherScope) {
        is UnionPermissionScope -> UnionPermissionScope(members + otherScope.members)
        else                -> UnionPermissionScope(members + otherScope)
    }
    override val description: String
        get() = members.joinToString(" && ",prefix = "(", postfix = ")") { it.description }

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnionPermissionScope

        if (members.size!= other.members.size) return false // compare size before comparing
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
interface UseAuthScope: AuthScope {

}

object ANYSCOPE: PermissionScope {
    override val description get() = "*"
    override fun toString() = "AnyScope"
    override fun includes(useScope: UseAuthScope): Boolean {
        throw AuthorizationException("Using the any scope directly is not permitted")
    }

    override fun union(otherScope: PermissionScope): PermissionScope {
        return this
    }

    override fun intersect(otherScope: PermissionScope): PermissionScope? {
        return otherScope
    }
}
