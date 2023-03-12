package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.security.RolePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.AuthRestriction
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat

sealed class RunnableAuthRestriction : AuthRestriction {
    open infix fun and(other: RunnableAuthRestriction): RunnableAuthRestriction = when(other) {
        is ConjunctiveRestriction -> ConjunctiveRestriction(this, *other.elements)
        else -> ConjunctiveRestriction(this, other)
    }

    open infix fun or(other: RunnableAuthRestriction): RunnableAuthRestriction = when(other) {
        is DisjunctiveRestriction -> DisjunctiveRestriction(this, *other.elements)
        else -> DisjunctiveRestriction(this, other)
    }

    abstract fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean
}

class RoleRestriction(val allowedRoles: Collection<String>) : RunnableAuthRestriction() {
    constructor(allowedRole: String) : this(setOf(allowedRole))

    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        return principal is RolePrincipal &&
            allowedRoles.any { principal.hasRole(it) }
    }

    override fun or(other: RunnableAuthRestriction): RunnableAuthRestriction = when (other) {
        is RoleRestriction -> RoleRestriction(allowedRoles + other.allowedRoles)
        else -> super.or(other)
    }
}

class ConjunctiveRestriction internal constructor(vararg val elements: RunnableAuthRestriction) : RunnableAuthRestriction() {
    override infix fun and(other: RunnableAuthRestriction) = when(other) {
        is ConjunctiveRestriction -> ConjunctiveRestriction(*this.elements, *other.elements)
        else -> ConjunctiveRestriction(*this.elements, other)
    }

    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        return elements.all {
            it.hasAccess(context, principal)
        }
    }
}

class DisjunctiveRestriction internal constructor(vararg val elements: RunnableAuthRestriction) : RunnableAuthRestriction() {
    override infix fun or(other: RunnableAuthRestriction) = when(other) {
        is DisjunctiveRestriction -> DisjunctiveRestriction(*this.elements, *other.elements)
        else -> DisjunctiveRestriction(*this.elements, other)
    }

    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        return elements.any { it.hasAccess(context, principal) }
    }
}

class PrincipalRestriction(val allowedPrincipals: Set<String>) : RunnableAuthRestriction() {
    constructor(allowedPrincipal: String) : this(setOf(allowedPrincipal))

    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        return principal.name in allowedPrincipals
    }

    override fun or(other: RunnableAuthRestriction): RunnableAuthRestriction = when (other) {
        is PrincipalRestriction -> PrincipalRestriction(allowedPrincipals + other.allowedPrincipals)
        else -> super.or(other)
    }

    override fun and(other: RunnableAuthRestriction): RunnableAuthRestriction = when (other) {
        is PrincipalRestriction -> and(other)
        else -> super.and(other)
    }

    fun and(other: PrincipalRestriction): Nothing {
        throw IllegalArgumentException("A conjunctive restriction on multiple usernames is invalid")
    }
}

class BindingOfDutyRestriction(val referenceNode: Identified): RunnableAuthRestriction() {
    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        val refInst: IProcessNodeInstance = context.processContext.instancesForName(referenceNode)
            .firstOrNull { it.state == NodeInstanceState.Complete } ?: return false
        val referencePrincipal = refInst

        TODO("not implemented")
    }
}

class SeparationOfDutyRestriction(val referenceNode: Identified) : RunnableAuthRestriction() {
    override fun hasAccess(context: ActivityInstanceContext, principal: PrincipalCompat): Boolean {
        TODO("not implemented")
    }
}
