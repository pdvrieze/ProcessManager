package nl.adaptivity.process.processModel

import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AccessRestriction {
//    fun isAuthorized(principal: PrincipalCompat): Boolean
    fun hasAccess(context: Any?, principal: PrincipalCompat, permission: SecurityProvider.Permission): Boolean
    fun serializeToString(): String
}
