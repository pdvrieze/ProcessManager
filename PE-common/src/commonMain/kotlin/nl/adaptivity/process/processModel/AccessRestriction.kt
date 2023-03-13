package nl.adaptivity.process.processModel

import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AccessRestriction<in C> {
//    fun isAuthorized(principal: PrincipalCompat): Boolean
    fun hasAccess(context: C, principal: PrincipalCompat): Boolean
}
