/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.test.loanOrigination.systems

import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.Random
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.GRANT_ACTIVITY_PERMISSION
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.GRANT_GLOBAL_PERMISSION
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.nextUInt
import kotlin.random.nextULong

class AuthService(
    val logger: Logger,
    private val nodeLookup: Map<PNIHandle, String>
                 ) : Service {
    @UseExperimental(ExperimentalUnsignedTypes::class)
    val authServiceId = "AuthService:${Random.nextUInt().toString(16)}"
    override val serviceId: String
        get() = authServiceId

    private val registeredClients = mutableMapOf<String, ClientInfo>()
    private val authorizationCodes = mutableMapOf<AuthorizationCode, AuthToken>()
    private val activeTokens = mutableListOf<AuthToken>()
    private val globalPermissions = mutableMapOf<Principal, MutableMap<String, MutableList<PermissionScope>>>()

    private val tokenPermissions = mutableMapOf<String, MutableList<Permission>>()

    private inline fun doLog(authInfo: AuthInfo?, message: String) {
        when (authInfo) {
            is AuthToken        -> {
                val processNodeInstance = authInfo.nodeInstanceHandle
                val nodeId = nodeLookup[processNodeInstance] ?: "<unknown node>"
                logger.log(Level.INFO, "[$nodeId:${processNodeInstance.handleValue}>${authInfo.principal.name}] - $message")
            }
            is IdSecretAuthInfo -> logger.log(Level.INFO, "[GLOBAL>${authInfo.principal.name}] - $message")
            null                -> logger.log(Level.INFO, "[UNAUTH] - $message")
            else                -> logger.log(Level.INFO, "[GLOBAL>${authInfo}] - $message")
        }
    }

    inline fun doLog(message: String) {
        logger.log(Level.INFO, "[UNAUTH] - $message")
    }

    /**
     * Validate whether the authentication information will authenticate to the service with the given scope
     * @param serviceId The service that is being accessed
     * @param authInfo The authentication information being used
     * @param scope The scope of the access requested
     */
    fun validateAuthInfo(service: Service, authInfo: AuthInfo, scope: UseAuthScope) {
        val serviceId = service.serviceId
        when (authInfo) {
            is IdSecretAuthInfo -> validateUserPermission(serviceId, authInfo, scope)
            is AuthToken -> validateAuthTokenPermission(serviceId, authInfo, scope)
            else -> doLog(authInfo, "validateAuthInfo(clientId = $serviceId, authInfo = $authInfo, scope = $scope)")
        }
    }

    private fun internalValidateAuthInfo(authInfo: AuthInfo, scope: UseAuthScope) {
        when (authInfo) {
            is IdSecretAuthInfo -> validateUserPermission(authServiceId, authInfo, scope)
            is AuthToken        -> validateAuthTokenPermission(authServiceId, authInfo, scope)
            else                -> doLog(authInfo, "validateAuthInfo(authInfo = $authInfo, scope = $scope)")
        }

    }

    private fun validateAuthTokenPermission(serviceId: String, authToken: AuthToken, useScope: UseAuthScope) {
        if (authToken !in activeTokens) throw AuthorizationException("Token not active: $authToken")
        if (authToken.serviceId != serviceId) throw AuthorizationException("The token $authToken is not for the expected service $serviceId")
//        val tokenPermissions = tokenPermissions.get(authToken.tokenValue) ?:emptyList<Permission>()
        val hasPermissionDirectPermission = authToken.serviceId == serviceId && authToken.scope.includes(useScope)
        if (!hasPermissionDirectPermission) {
            val additionalPermissions = tokenPermissions[authToken.tokenValue] ?: emptyList<Permission>()
            val hasExtPermission = additionalPermissions.any { it.scope.includes(useScope) }
            if (!hasExtPermission) {
                val hasGlobalPerm: Boolean = authToken.scope == LoanPermissions.IDENTIFY &&
                    (globalPermissions.get(authToken.principal)?.get(serviceId)?.any { it.includes(useScope) } ?: false)

                if (!hasGlobalPerm) {
                    throw AuthorizationException("No permission found for token $authToken to $serviceId.${useScope.description}")
                }
            }
        }
        doLog(authToken, "validateTokenPermissions(clientId = $serviceId, token = $authToken, scope = ${useScope.description})")
    }

    private fun validateUserPermission(serviceId: String, authInfo: IdSecretAuthInfo, useScope: UseAuthScope) {
        if (serviceId != authServiceId) throw AuthorizationException("Only authService allows password auth")
        if (registeredClients[authInfo.principal.name]?.secret != authInfo.secret) {
            throw AuthorizationException("Password mismatch for client ${authInfo.principal} (${registeredClients[authInfo.principal.name]?.secret} != ${authInfo.secret})")
        }

        val source = Throwable().stackTrace[2].let { "${it.className.substringAfterLast('.')}.${it.methodName}" }
        doLog(authInfo, "validateUserPermissions(clientId = $serviceId, authInfo = $authInfo, scope = ${useScope.description}) from $source")
        if (useScope!=LoanPermissions.IDENTIFY) { // Identify by password is always allowed
            val hasGlobalPerms =
                globalPermissions.get(authInfo.principal)?.get(serviceId)?.any { it.includes(useScope) } ?: false
            if (!hasGlobalPerms) {
                throw AuthorizationException("No permission found for user ${authInfo.principal} to $serviceId.${useScope.description}")
            }
        }
    }

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param auth Authorization for this action
     * @param clientId The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param service The service being authorized
     * @param scope The scope being authorized
     */
    fun createAuthorizationCode(
        auth: IdSecretAuthInfo,
        clientId: String,
        nodeInstanceHandle: PNIHandle,
        service: Service,
        scope: PermissionScope
                               ): AuthorizationCode {
        return createAuthorizationCodeImpl(auth, clientId, nodeInstanceHandle, service, scope)
    }

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param auth Authorization for this action
     * @param clientId The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param service The service being authorized
     * @param scope The scope being authorized
     */
    fun createAuthorizationCode(
        auth: AuthInfo,
        clientId: String,
        service: Service,
        scope: PermissionScope
                               ): AuthorizationCode {
        val nodeInstanceHandle = auth.getNodeInstanceHandle()
        return createAuthorizationCodeImpl(auth, clientId, nodeInstanceHandle, service, scope)
    }

    private fun AuthInfo.getNodeInstanceHandle(): PNIHandle = when (this) {
        is AuthToken -> nodeInstanceHandle
        else         -> getInvalidHandle()
    }

    private fun createAuthorizationCodeImpl(
        auth: AuthInfo,
        clientId: String,
        nodeInstanceHandle: PNIHandle,
        service: Service,
        scope: PermissionScope
                                           ): AuthorizationCode {
        internalValidateAuthInfo(auth, LoanPermissions.GRANT_PERMISSION.context(clientId, service, scope))

        val clientPrincipal = clientFromId(clientId)
        val existingToken = activeTokens.lastOrNull {
            it.principal == clientPrincipal &&
                it.nodeInstanceHandle == nodeInstanceHandle &&
                it.serviceId == serviceId &&
                it.scope == scope
        }

        val token = if (existingToken != null) {
            Random.nextString()
            existingToken
        } else {
            AuthToken(clientPrincipal, nodeInstanceHandle, Random.nextString(), service.serviceId, scope)
        }
        val authorizationCode = AuthorizationCode(Random.nextString())
        authorizationCodes[authorizationCode] = token
        activeTokens.add(token)
        doLog(auth, "createAuthorizationCode(code = ${authorizationCode.code}, token${if (existingToken != null) " - reused" else ""} = $token)")
        return authorizationCode
    }

    private fun clientFromId(clientId: String): Principal {
        // TODO look up actual users.
        return SimplePrincipal(clientId)
    }

    fun getAuthToken(clientAuth: AuthInfo, authorizationCode: AuthorizationCode): AuthToken {
        internalValidateAuthInfo(clientAuth, LoanPermissions.IDENTIFY)
        val token =
            authorizationCodes.get(authorizationCode) ?: throw AuthorizationException("authorization code invalid")

        if (token !in activeTokens) throw AuthorizationException("Authorization code expired")

        if (token.principal != clientAuth.principal) throw AuthorizationException("Invalid client for authentication code")

        doLog(clientAuth, "authTokenFromAuthorization(authorization = $authorizationCode) = $token")
        authorizationCodes.remove(authorizationCode)
        return token
    }

    /** Create a token that allows the identification of the client with the token */
    fun createTaskIdentityToken(clientAuth: IdSecretAuthInfo, processNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>, principal: Principal): AuthToken {
        internalValidateAuthInfo(clientAuth, LoanPermissions.CREATE_TASK_IDENTITY)
        val token = AuthToken(principal, processNodeInstance, Random.nextString(), authServiceId, LoanPermissions.IDENTIFY)
        activeTokens.add(token)
        doLog(clientAuth, "createTaskIdentityToken() = $token")
        return token
    }

    fun getAuthTokenDirect(identityToken: AuthInfo, service: Service, reqScope: PermissionScope): AuthToken {
        // TODO principal should be authorized
        val serviceId = service.serviceId
        internalValidateAuthInfo(identityToken, LoanPermissions.IDENTIFY)
        val userPermissions = globalPermissions.get(identityToken.principal)?.get(serviceId) ?: emptyList<PermissionScope>()

        val effectiveScope: PermissionScope
        val tokenAssociatedPermissions = if (identityToken is AuthToken) {
            (tokenPermissions[identityToken.tokenValue]
                ?.asSequence()
                ?: emptySequence())
                .map { it.scope }
                .filterIsInstance<LoanPermissions.GRANT_PERMISSION.ContextScope>()
                .filter { it.serviceId == serviceId }
                .map { it.childScope ?: ANYSCOPE }
        } else {
            emptySequence()
        }
        val registeredPermissions = tokenAssociatedPermissions
            .plus(userPermissions.asSequence())
            .ifEmpty {
                throw AuthorizationException("The token $identityToken has no permission to create delegate tokens for ${service.serviceId}.${reqScope.description}")
            }
            .reduce<PermissionScope?, PermissionScope> { l, r -> l?.union(r) }
            ?: throw AuthorizationException("The token $identityToken permissions cancel to nothing")

        if (reqScope == ANYSCOPE) {
            effectiveScope = registeredPermissions
        } else {
            val intersection = registeredPermissions.intersect(reqScope)
            if (intersection != reqScope) throw AuthorizationException("The requested permission $reqScope is not contained within $registeredPermissions")
            effectiveScope = intersection
        }

/*
        val tokenPermissions = tokenPermissions.get(taskIdentityToken.tokenValue) ?:emptyList<Permission>()
        val hasPermission = tokenPermissions.any { it.serviceId == serviceId && it.scope.includes(scope) }
        if (!hasPermission) {
            throw AuthorizationException("No permissions available for $taskIdentityToken to $serviceId.$scope")

        }
*/
        // TODO look up permissions for taskIdentityToken

        val nodeInstanceHandle = (identityToken as? AuthToken)?.nodeInstanceHandle ?: getInvalidHandle()

        val existingToken = activeTokens.firstOrNull {
            it.principal == identityToken.principal &&
                it.nodeInstanceHandle == nodeInstanceHandle &&
                it.serviceId == serviceId &&
                it.scope == effectiveScope
        }

        if (existingToken != null) {
            Random.nextString() // consume random number
            doLog(identityToken, "getAuthTokenDirect($identityToken) - reuse = $existingToken")
            return existingToken
        }

        return AuthToken(identityToken.principal, nodeInstanceHandle, Random.nextString(), serviceId, effectiveScope).also {
            activeTokens.add(it)
            doLog(identityToken, "getAuthTokenDirect($identityToken) = $it")
        }
    }

    fun loginDirect(auth: IdSecretAuthInfo): AuthToken {
        internalValidateAuthInfo(auth, LoanPermissions.IDENTIFY)
        return AuthToken(auth.principal, getInvalidHandle(), Random.nextString(), authServiceId, LoanPermissions.IDENTIFY).also {
            activeTokens.add(it)
            doLog(it, "loginDirect($auth) = $it")
        }
    }

    fun grantPermission(auth: AuthInfo, authorizationCode: AuthorizationCode, service: Service, scope: PermissionScope) {
        val authToken = authorizationCodes[authorizationCode] ?: throw AuthorizationException("Invalid authorization code")
        grantPermission(auth, authToken, service, scope)
    }

    fun grantPermission(auth: AuthInfo, taskIdToken: AuthToken, service: Service, scope: PermissionScope) {
        val serviceId = service.serviceId
        val clientId = auth.principal.name
        internalValidateAuthInfo(auth, LoanPermissions.GRANT_PERMISSION.context(clientId, service, scope))
        if (taskIdToken.serviceId != serviceId) throw AuthorizationException("Cannot grant permission for a token for one service to work againsta another service")
        assert(taskIdToken in activeTokens)
        val tokenPermissionList = tokenPermissions.getOrPut(taskIdToken.tokenValue) { mutableListOf() }
        doLog(auth, "grantPermission(token = ${taskIdToken.tokenValue}, serviceId = $serviceId, scope = $scope)")
        tokenPermissionList.add(Permission(scope))
    }

    fun invalidateActivityTokens(auth: AuthInfo, hNodeInstance: PNIHandle) {
        internalValidateAuthInfo(auth, LoanPermissions.INVALIDATE_ACTIVITY.context(hNodeInstance))
        val it = activeTokens.iterator()
        while (it.hasNext()) {
            val token = it.next()
            if (token.nodeInstanceHandle == hNodeInstance) {
                doLog(auth, "invalidateActivityTokens($hNodeInstance, $token)")
                it.remove()
                tokenPermissions.remove(token.tokenValue)
            }
        }
    }

    fun registerClient(name: String, secret: String): IdSecretAuthInfo {
        val clientId = "$name:${Random.nextUInt().toString(16)}"
        return registerClient(clientFromId(clientId), secret, name)
    }

    @UseExperimental(ExperimentalUnsignedTypes::class)
    fun registerClient(user: Principal, secret: String, name: String = user.name): IdSecretAuthInfo {
        doLog("registerClient($user)")
        val clientId = user.name
        if (registeredClients[clientId] != null) return registerClient(name, secret)
        registeredClients[clientId] = ClientInfo(clientId, name, secret)
        return IdSecretAuthInfo(user, secret)
    }

    fun isTokenInvalid(token: AuthToken): Boolean {
        return token !in activeTokens
    }

    fun registerGlobalPermission(authInfo: AuthInfo?, principal: Principal, service: Service, scope: PermissionScope) {
        doLog(authInfo, "registerGlobalPermissions($authInfo, $principal, ${service.serviceId}, $scope)")
        if (authInfo != null) {
            val clientId = authInfo.principal.name
            internalValidateAuthInfo(authInfo, LoanPermissions.GRANT_PERMISSION.context(clientId, service, scope))
        }
        globalPermissions.getOrPut(principal) { mutableMapOf() }
            .getOrPut(service.serviceId) { mutableListOf() }
            .add(scope)
    }


    private data class Permission(val scope: PermissionScope) {
        override fun toString(): String {
            return "Permission(${scope.description})"
        }
    }

    override fun toString(): String {
        return "AuthService"
    }
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun kotlin.random.Random.nextString() = nextULong().toString(16)
