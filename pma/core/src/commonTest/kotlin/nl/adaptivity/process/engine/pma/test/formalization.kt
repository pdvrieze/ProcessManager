package nl.adaptivity.process.engine.pma.test.nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.PermissionScope
import nl.adaptivity.process.engine.pma.UseAuthScope
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance

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
