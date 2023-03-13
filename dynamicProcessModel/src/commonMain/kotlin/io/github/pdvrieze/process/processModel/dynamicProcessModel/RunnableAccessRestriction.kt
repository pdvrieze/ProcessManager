package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.security.RolePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat

sealed class RunnableAccessRestriction : AccessRestriction<ActivityInstanceContext?> {
    open infix fun and(other: RunnableAccessRestriction): RunnableAccessRestriction = when(other) {
        is ConjunctiveRestriction -> ConjunctiveRestriction(this, *other.elements)
        else -> ConjunctiveRestriction(this, other)
    }

    open infix fun or(other: RunnableAccessRestriction): RunnableAccessRestriction = when(other) {
        is DisjunctiveRestriction -> DisjunctiveRestriction(this, *other.elements)
        else -> DisjunctiveRestriction(this, other)
    }
}

class RoleRestriction(val allowedRoles: Collection<String>) : RunnableAccessRestriction() {
    constructor(allowedRole: String) : this(setOf(allowedRole))

    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        return principal is RolePrincipal &&
            allowedRoles.any { principal.hasRole(it) }
    }

    override fun or(other: RunnableAccessRestriction): RunnableAccessRestriction = when (other) {
        is RoleRestriction -> RoleRestriction(allowedRoles + other.allowedRoles)
        else -> super.or(other)
    }
}

class ConjunctiveRestriction internal constructor(vararg val elements: RunnableAccessRestriction) : RunnableAccessRestriction() {
    override infix fun and(other: RunnableAccessRestriction) = when(other) {
        is ConjunctiveRestriction -> ConjunctiveRestriction(*this.elements, *other.elements)
        else -> ConjunctiveRestriction(*this.elements, other)
    }

    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        return elements.all {
            it.hasAccess(context, principal)
        }
    }
}

class DisjunctiveRestriction internal constructor(vararg val elements: RunnableAccessRestriction) : RunnableAccessRestriction() {
    override infix fun or(other: RunnableAccessRestriction) = when(other) {
        is DisjunctiveRestriction -> DisjunctiveRestriction(*this.elements, *other.elements)
        else -> DisjunctiveRestriction(*this.elements, other)
    }

    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        return elements.any { it.hasAccess(context, principal) }
    }
}

class PrincipalRestriction(val allowedPrincipals: Set<String>) : RunnableAccessRestriction() {
    constructor(allowedPrincipal: String) : this(setOf(allowedPrincipal))

    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        return principal.name in allowedPrincipals
    }

    override fun or(other: RunnableAccessRestriction): RunnableAccessRestriction = when (other) {
        is PrincipalRestriction -> PrincipalRestriction(allowedPrincipals + other.allowedPrincipals)
        else -> super.or(other)
    }

    override fun and(other: RunnableAccessRestriction): RunnableAccessRestriction = when (other) {
        is PrincipalRestriction -> and(other)
        else -> super.and(other)
    }

    fun and(other: PrincipalRestriction): Nothing {
        throw IllegalArgumentException("A conjunctive restriction on multiple usernames is invalid")
    }
}

class BindingOfDutyRestriction(val referenceNode: Identified): RunnableAccessRestriction() {
    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        val refInst: IProcessNodeInstance = context?.processContext?.instancesForName(referenceNode)
            ?.singleOrNull { it.state == NodeInstanceState.Complete } ?: return false
        val referencePrincipal = refInst.assignedUser
        return principal.name == referencePrincipal?.name
    }
}

class SeparationOfDutyRestriction(val referenceNode: Identified) : RunnableAccessRestriction() {
    override fun hasAccess(context: ActivityInstanceContext?, principal: PrincipalCompat): Boolean {
        val refInsts: List<IProcessNodeInstance> = context?.processContext?.instancesForName(referenceNode)
            ?.filter { it.state == NodeInstanceState.Complete } ?: emptyList()

        return refInsts.none { it.assignedUser?.name == principal.name }
    }
}
