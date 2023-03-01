package nl.adaptivity.process.engine.test

import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.PermissionScope
import nl.adaptivity.process.engine.test.loanOrigination.auth.UseAuthScope

typealias ActivityInstance = ProcessNodeInstance<*>
typealias Authorization = PermissionScope
typealias PermissionDesc = (ActivityInstance) -> Authorization
typealias Action = UseAuthScope

fun PermissionDesc.instantiate(activityInstance: ActivityInstance): PermissionDesc {
    return TODO()
}


/*
fun Service.extractAuthToken(token: AuthToken): Set<Authorization> = when {
    token.serviceId != serviceId -> emptySet()
//    return token.
}
*/


fun verifyAuthorization(authorizations: Set<Authorization>, action: Action): Boolean =
    authorizations.any { it.includes(action) }
