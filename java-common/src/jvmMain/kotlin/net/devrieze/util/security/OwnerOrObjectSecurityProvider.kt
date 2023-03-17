package net.devrieze.util.security

import net.devrieze.util.security.SecurityProvider.PermissionResult
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.net.devrieze.util.security.hasPermission

class OwnerOrObjectSecurityProvider(val adminRoles: Set<String> = emptySet()) : BaseSecurityProvider() {
    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: Principal?
    ): PermissionResult {
        return when {
            subject == null -> PermissionResult.UNAUTHENTICATED

            (subject is RolePrincipal) &&
                adminRoles.any { subject.hasRole(it) } -> PermissionResult.GRANTED

            else -> PermissionResult.DENIED
        }
    }

    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: Principal?,
        objectPrincipal: Principal
    ): PermissionResult {
        return when {
            subject == null -> PermissionResult.UNAUTHENTICATED

            (subject is RolePrincipal) &&
                adminRoles.any { subject.hasRole(it) } -> PermissionResult.GRANTED

            else -> PermissionResult.DENIED
        }
    }

    override fun getPermission(
        permission: SecurityProvider.Permission,
        subject: PrincipalCompat?,
        secureObject: SecuredObject<*>
    ): PermissionResult {
        if (subject == null) return PermissionResult.UNAUTHENTICATED
        if (subject == SYSTEMPRINCIPAL) return PermissionResult.GRANTED
        // shortcircuit admin
        if ((subject is RolePrincipal) &&
            adminRoles.any { subject.hasRole(it) }
        ) return PermissionResult.GRANTED

        return (secureObject.hasPermission(subject, permission) orBool {
            (secureObject as? SecureObject)?.let { it.owner.name == subject.name }
        }).toPermissionResult()
    }
}
