package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.security.RolePrincipal
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat

class RoleRestriction(val allowedRoles: Collection<String>) : AccessRestriction {
    constructor(allowedRole: String) : this(setOf(allowedRole))

    override fun hasAccess(context: Any?, principal: PrincipalCompat, permission: SecurityProvider.Permission): Boolean {
        return principal is RolePrincipal &&
            allowedRoles.any { principal.hasRole(it) }
    }

    override fun serializeToString(): String = when (allowedRoles.size) {
        0 -> ""
        1 -> "@${allowedRoles.single()}"
        else -> allowedRoles.joinToString(prefix = "@[", postfix = "]")
    }
}
