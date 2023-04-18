package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.security.*
import net.devrieze.util.security.SecurityProvider.Permission
import net.devrieze.util.security.SecurityProvider.PermissionResult
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.UseAuthScope
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.net.devrieze.util.security.hasPermission

class PmaSecurityProvider(
    private val serviceId: ServiceId<*>,
    private val authService: AuthServiceClient<*,*,*>,
    private val baseProvider: SecurityProvider = OwnerOrObjectSecurityProvider()
) : BaseSecurityProvider() {

    override fun getPermission(permission: Permission, subject: PrincipalCompat?): PermissionResult {
        return when {
            permission !is UseAuthScope || subject == null -> {
                baseProvider.ensurePermission(permission, subject)
            }
            authService.userHasPermission(subject, serviceId, permission) -> PermissionResult.GRANTED
            else -> PermissionResult.DENIED
        }
    }

    override fun getPermission(
        permission: Permission,
        subject: PrincipalCompat?,
        secureObject: SecuredObject<*>
    ): PermissionResult {
        return when {
            permission !is UseAuthScope || subject == null -> {
                baseProvider.ensurePermission(permission, subject, secureObject)
            }
            authService.userHasPermission(subject, serviceId, SecureObjectAuthScope(subject, secureObject, permission)) -> PermissionResult.GRANTED
            else -> PermissionResult.DENIED
        }
    }

    override fun getPermission(
        permission: Permission,
        subject: PrincipalCompat?,
        objectPrincipal: Principal
    ): PermissionResult {
        return when {
            permission !is UseAuthScope || subject == null -> {
                baseProvider.ensurePermission(permission, subject, objectPrincipal)
            }
            authService.userHasPermission(subject, serviceId, ObjectPrincipalAuthScope(objectPrincipal, permission)) -> PermissionResult.GRANTED
            else -> PermissionResult.DENIED
        }
    }

}

class ObjectPrincipalAuthScope(val objectPrincipal: Principal, val permission: Permission): UseAuthScope {
    override val description: String
        get() = "objectPrincipal($objectPrincipal, $permission)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObjectPrincipalAuthScope

        if (objectPrincipal != other.objectPrincipal) return false
        return permission == other.permission
    }

    override fun hashCode(): Int {
        var result = objectPrincipal.hashCode()
        result = 31 * result + permission.hashCode()
        return result
    }

}

class PermissionScope(val permission: Permission): AuthScope {
    override val description: String get() = permission.toString()
}

class SecureObjectAuthScope(val principal: PrincipalCompat, val secureObject: SecuredObject<*>, val permission: Permission): UseAuthScope {
    override val description: String
        get() = "secureObjectScope($permission)"

    override fun instantiateScope(context: ActivityInstanceContext): AuthScope? {
        return super.instantiateScope(context)
    }

    override fun includes(useScope: Permission): Boolean {
        return secureObject.hasPermission(principal, permission).toPermissionResult() == PermissionResult.GRANTED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureObjectAuthScope

        if (secureObject != other.secureObject) return false
        return permission == other.permission
    }

    override fun hashCode(): Int {
        var result = secureObject.hashCode()
        result = 31 * result + permission.hashCode()
        return result
    }


}
