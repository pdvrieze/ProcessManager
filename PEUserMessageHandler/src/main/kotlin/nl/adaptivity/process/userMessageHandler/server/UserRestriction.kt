package nl.adaptivity.process.userMessageHandler.server

import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat

class UserRestriction(val allowedPrincipals: Set<String>): AccessRestriction {
    constructor(allowedPrincipal: String) : this(setOf(allowedPrincipal))

    override fun hasAccess(context: Any?, principal: PrincipalCompat): Boolean {
        return principal.name in allowedPrincipals
    }

    override fun serializeToString(): String = when (allowedPrincipals.size) {
        0 -> ""
        1 -> allowedPrincipals.single()
        else -> allowedPrincipals.joinToString(prefix = "[")
    }
}
