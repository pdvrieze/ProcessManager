package nl.adaptivity.process.processModel

import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AccessRestriction {
//    fun isAuthorized(principal: PrincipalCompat): Boolean
    fun hasAccess(context: Any?, principal: PrincipalCompat): Boolean
    fun serializeToString(): String
}
