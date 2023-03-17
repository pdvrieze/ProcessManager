package nl.adaptivity.util.net.devrieze.util.security

import net.devrieze.util.security.SecuredObject
import net.devrieze.util.security.SecurityProvider.IntermediatePermissionDecision
import net.devrieze.util.security.SecurityProvider.Permission
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface ActiveSecureObject<out T> : SecuredObject<T> {
    fun hasPermission(subject: PrincipalCompat, permission: Permission): IntermediatePermissionDecision
}

fun <T> SecuredObject<T>.hasPermission(subject: PrincipalCompat, permission: Permission): IntermediatePermissionDecision {
    return when(this) {
        is ActiveSecureObject<T> -> hasPermission(subject, permission)
        else -> IntermediatePermissionDecision.PASS
    }
}
