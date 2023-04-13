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

package nl.adaptivity.process.engine.pma

import net.devrieze.util.Handle
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.impl.Level
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.*
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.nl.adaptivity.util.kotlin.removeIfTo
import kotlin.random.Random
import kotlin.random.nextUInt

class AuthService(
    serviceName: String,
    val logger: LoggerCompat,
    private val nodeLookup: Map<Handle<SecureProcessNodeInstance>, String>,
    private val random: Random
) : AutomatedService {
    override val serviceInstanceId: ServiceId<AuthService> = ServiceId("${serviceName}:${random.nextUInt().toString(16)}")

    val authServiceId get() = serviceInstanceId

    override val serviceName: ServiceName<AutomatedService> = ServiceName(serviceName)

    private val registeredClients = mutableMapOf<String, ClientInfo>()
    private val authorizationCodes = mutableMapOf<AuthorizationCode, PmaAuthToken>()
    private val activeTokens = mutableListOf<PmaAuthToken>()
    private val globalPermissions =
        mutableMapOf<String, MutableMap<String, AuthScope>>()

    private val tokenPermissions = mutableMapOf<String, MutableList<Permission>>()

    private inline fun doLog(authInfo: PmaAuthInfo?, message: String) {
        when (authInfo) {
            is PmaAuthToken -> {
                val processNodeInstance = authInfo.nodeInstanceHandle
                val nodeId = nodeLookup[processNodeInstance] ?: "<unknown node>"
                logger.log(
                    Level.INFO,
                    "[$nodeId:${processNodeInstance.handleValue}>${authInfo.principal.name}] - $message"
                )
            }

            is PmaIdSecretAuthInfo -> logger.log(Level.INFO, "[GLOBAL>${authInfo.principal.name}] - $message")
            null -> logger.log(Level.INFO, "[UNAUTH] - $message")
            else -> logger.log(Level.INFO, "[GLOBAL>${authInfo}] - $message")
        }
    }

    inline fun doLog(message: String) {
        logger.log(Level.INFO, "[UNAUTH] - $message")
    }

    /**
     * Validate whether the authentication information will authenticate to the service with the given scope
     * @param authInfo The authentication information being used
     * @param serviceId The service that is being accessed
     * @param scope The scope of the access requested
     */
    fun validateAuthInfo(
        authInfo: PmaAuthInfo,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    ) {
        when (authInfo) {
            is PmaIdSecretAuthInfo -> validateUserPermission(authInfo, serviceId, scope)
            is PmaAuthToken -> validateAuthTokenPermission(authInfo, serviceId, scope)
            else -> doLog(
                authInfo,
                "validateAuthInfo(clientId = $serviceId, authInfo = $authInfo, scope = $scope)"
            )
        }
    }

    private fun internalValidateAuthInfo(
        authInfo: PmaAuthInfo,
        scope: UseAuthScope
    ) {
        when (authInfo) {
            is PmaIdSecretAuthInfo -> validateUserPermission(authInfo, authServiceId, scope)
            is PmaAuthToken -> validateAuthTokenPermission(authInfo, authServiceId, scope)
            else -> doLog(authInfo, "validateAuthInfo(authInfo = $authInfo, scope = $scope)")
        }

    }

    private fun validateAuthTokenPermission(
        authToken: PmaAuthToken,
        serviceId: ServiceId<*>,
        useScope: UseAuthScope
    ) {
        if (authToken !in activeTokens) throw AuthorizationException("Token not active: $authToken")
        if (authToken.serviceId != serviceId) throw AuthorizationException("The token $authToken is not for the expected service $serviceId")
//        val tokenPermissions = tokenPermissions.get(authToken.tokenValue) ?:emptyList<Permission>()
        val hasTransparentPermission = authToken.scope.includes(useScope)
        if (!hasTransparentPermission) {
            val opaquePermissions = tokenPermissions[authToken.token] ?: emptyList()
            val hasExtPermission = opaquePermissions.any { it.scope.includes(useScope) }
            if (!hasExtPermission) {
                val hasGlobalPerm: Boolean = authToken.scope == IDENTIFY &&
                    (globalPermissions.get(authToken.principal.name)?.get(serviceId)?.includes(useScope)
                        ?: false)

                if (!hasGlobalPerm) {
                    throw AuthorizationException("No permission found for token $authToken to $serviceId.${useScope.description}")
                }
            }
        }
        doLog(
            authToken,
            "validateTokenPermissions(clientId = $serviceId, token = $authToken, scope = ${useScope.description})"
        )
    }

    private fun validateUserPermission(
        authInfo: PmaIdSecretAuthInfo,
        serviceId: ServiceId<*>,
        useScope: UseAuthScope
    ) {
        if (serviceId != authServiceId) throw AuthorizationException("Only authService allows password auth")
        if (registeredClients[authInfo.principal.name]?.secret != authInfo.secret) {
            throw AuthorizationException("Password mismatch for client ${authInfo.principal} (${registeredClients[authInfo.principal.name]?.secret} != ${authInfo.secret})")
        }

        val source =
            Throwable().stackTrace[2].let { "${it.className.substringAfterLast('.')}.${it.methodName.substringBefore('-')}" }
        doLog(
            authInfo,
            "validateUserPermissions(clientId = $serviceId, authInfo = $authInfo, scope = ${useScope.description}) from $source"
        )
        if (useScope != IDENTIFY) { // Identify by password is always allowed
            val hasGlobalPerms =
                globalPermissions.get(authInfo.principal.name)?.get(serviceId)?.includes(useScope) ?: false
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
        auth: PmaIdSecretAuthInfo,
        clientId: String,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        service: Service,
        scope: AuthScope
    ): AuthorizationCode {
        return createAuthorizationCodeImpl(auth, clientId, nodeInstanceHandle, service, scope)
    }

    fun requestPmaAuthToken(
        engineAuth: PmaAuthInfo,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        return createAuthTokenImpl(engineAuth, engineAuth.principal, nodeInstanceHandle, serviceId, scope)
    }

    fun exchangeDelegateToken(
        clientAuth: PmaAuthInfo,
        exchangedToken: PmaAuthToken,
        service: ServiceId<*>,
        requestedScope: AuthScope,
    ) : PmaAuthToken {
        val clientServiceId = clientAuth.principal.name
        validateAuthTokenPermission(
            exchangedToken,
            ServiceId<Service>(clientServiceId),
            DELEGATED_PERMISSION.context(clientServiceId, service, requestedScope)
        )
        val newToken = createAuthTokenNonValidated(clientAuth.principal, exchangedToken.nodeInstanceHandle, service, requestedScope)

        doLog(clientAuth, "exchangeDelegateToken(exchanged = ${exchangedToken}, token = $newToken)")
        return newToken
    }

    private fun createAuthTokenImpl(
        auth: PmaAuthInfo,
        client: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        targetServiceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        // We know the task handle so permission limited to the task handle is sufficient
        internalValidateAuthInfo(auth, GRANT_ACTIVITY_PERMISSION.context(nodeInstanceHandle, client.name, targetServiceId, scope))

        return createAuthTokenNonValidated(client, nodeInstanceHandle, targetServiceId, scope)
    }

    private fun createAuthTokenNonValidated(
        client: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        targetServiceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        val existingToken = activeTokens.lastOrNull {
            it.principal == client &&
                    it.nodeInstanceHandle == nodeInstanceHandle &&
                    it.serviceId == targetServiceId &&
                    it.scope == scope
        }

        val token = if (existingToken != null) {
    //            Random.nextString()
            existingToken
        } else {
            PmaAuthToken(client, nodeInstanceHandle, Random.nextString(), targetServiceId, scope)
        }
        activeTokens.add(token)
        return token
    }

    private fun createAuthorizationCodeImpl(
        auth: PmaAuthInfo,
        clientId: String,
        nodeInstanceHandle: PNIHandle,
        service: Service,
        scope: AuthScope
    ): AuthorizationCode {

        val clientPrincipal = clientFromId(clientId)

        // We know the task handle so permission limited to the task handle is sufficient
        internalValidateAuthInfo(auth, GRANT_ACTIVITY_PERMISSION.context(nodeInstanceHandle, clientPrincipal.name, service.serviceInstanceId, scope))

        val token = createAuthTokenNonValidated(clientPrincipal, nodeInstanceHandle, service.serviceInstanceId, scope)

        val authorizationCode = AuthorizationCode(Random.nextString(), clientPrincipal)
        authorizationCodes[authorizationCode] = token

        doLog(auth, "createAuthorizationCode(code = ${authorizationCode.code}, token = $token)")

        return authorizationCode
    }

    private fun clientFromId(clientId: String): PrincipalCompat {
        // TODO look up actual users.
        return SimplePrincipal(clientId)
    }

    fun getAuthToken(clientAuth: PmaAuthInfo, authorizationCode: AuthorizationCode): PmaAuthToken {
        internalValidateAuthInfo(clientAuth, IDENTIFY)
        val token =
            authorizationCodes.get(authorizationCode) ?: throw AuthorizationException("authorization code invalid")

        if (token !in activeTokens) activeTokens.add(token)

        if (authorizationCode.principal != clientAuth.principal)
            throw AuthorizationException("Invalid client for authorization code ${token.principal} != ${clientAuth.principal}")

        doLog(clientAuth, "authTokenFromAuthorization(authorization = $authorizationCode) = $token")
        authorizationCodes.remove(authorizationCode)
        return token
    }

    /** Common implementation that is used for authorization codes/authtokens but doesn't log*/
    private fun getAuthCommon(
        identityToken: PmaAuthInfo,
        serviceId: ServiceId<*>,
        reqScope: AuthScope
    ): PmaAuthToken {
        // TODO principal should be authorized
        internalValidateAuthInfo(identityToken, IDENTIFY)
        val userPermissions: AuthScope? =
            globalPermissions.get(identityToken.principal.name)?.get(serviceId)

        val effectiveScope: AuthScope
        val tokenAssociatedPermissions: Sequence<AuthScope> = if (identityToken is PmaAuthToken) {
            val scopes = (tokenPermissions[identityToken.token]
                ?.asSequence()
                ?: emptySequence())
                .map { it.scope }

            if (identityToken.nodeInstanceHandle.isValid) {
                scopes.mapNotNull {
                    when {
                        // Any child scopes for activity limited grants
                        it is GRANT_ACTIVITY_PERMISSION.ContextScope &&
                            it.taskInstanceHandle == identityToken.nodeInstanceHandle -> it.childScope ?: ANYSCOPE

                        // As well as global grants
                        it is GRANT_GLOBAL_PERMISSION.ContextScope ->
                            it.childScope ?: ANYSCOPE

                        else -> null
                    }
                }
            } else { // not restricted to a task, so activity permissions are not valid
                scopes.filterIsInstance<GRANT_GLOBAL_PERMISSION.ContextScope>()
                    .filter { it.serviceId == serviceId }
                    .map { it.childScope ?: ANYSCOPE }
            }
        } else {
            emptySequence()
        }
        val registeredPermissions = tokenAssociatedPermissions
            .plus(listOfNotNull(userPermissions).asSequence())
            .ifEmpty {
                throw AuthorizationException("The token $identityToken has no permission to create delegate tokens for ${serviceId}.${reqScope.description}")
            }
            .reduce<AuthScope?, AuthScope> { l, r -> l?.union(r) }
            ?: throw AuthorizationException("The token $identityToken permissions cancel to nothing")

        if (reqScope == ANYSCOPE) {
            effectiveScope = registeredPermissions
        } else {
            val intersection = registeredPermissions.intersect(reqScope)
            if (intersection != reqScope) throw AuthorizationException("The requested permission $reqScope is not contained within $registeredPermissions")
            effectiveScope = intersection
        }

        // TODO look up permissions for taskIdentityToken

        val nodeInstanceHandle = (identityToken as? PmaAuthToken)?.nodeInstanceHandle ?: Handle.invalid()

        val existingToken = activeTokens.firstOrNull {
            it.principal == identityToken.principal &&
                it.nodeInstanceHandle == nodeInstanceHandle &&
                it.serviceId == serviceId &&
                it.scope == effectiveScope
        }

        if (existingToken != null) {
            return existingToken
        }

        return PmaAuthToken(identityToken.principal, nodeInstanceHandle, Random.nextString(), serviceId, effectiveScope)

    }

    fun getAuthorizationCode(
        identityToken: PmaAuthInfo,
        serviceId: ServiceId<*>,
        reqScope: AuthScope
    ): AuthorizationCode {
        val token = getAuthCommon(identityToken, serviceId, reqScope)
        val authorizationCode = AuthorizationCode(Random.nextString(), identityToken.principal)
        authorizationCodes[authorizationCode] = token

        if (token in activeTokens) {
            doLog(identityToken, "getAuthorizationCode($identityToken) - reuse = $authorizationCode -> $token")
        } else {
            doLog(identityToken, "getAuthorizationCode($identityToken) = $authorizationCode -> $token")
        }
        return authorizationCode
    }

    fun getAuthTokenDirect(
        identityToken: PmaAuthInfo,
        service: Service,
        reqScope: AuthScope
    ): PmaAuthToken {
        return getAuthTokenDirect(identityToken, service.serviceInstanceId, reqScope)
    }

    fun getAuthTokenDirect(
        identityToken: PmaAuthInfo,
        serviceId: ServiceId<Service>,
        reqScope: AuthScope
    ): PmaAuthToken {
        val token = getAuthCommon(identityToken, serviceId, reqScope)
        if (token in activeTokens) {
            doLog(identityToken, "getAuthTokenDirect($identityToken) - reuse = $token")
        } else {
            activeTokens.add(token)
            doLog(identityToken, "getAuthTokenDirect($identityToken) = $token")
        }
        return token
    }

    fun loginDirect(auth: PmaIdSecretAuthInfo): PmaAuthToken {
        internalValidateAuthInfo(auth, IDENTIFY)
        return PmaAuthToken(
            auth.principal,
            Handle.invalid(),
            Random.nextString(),
            authServiceId,
            IDENTIFY
        ).also {
            activeTokens.add(it)
            doLog(it, "loginDirect($auth) = $it")
        }
    }

    fun grantPermission(
        auth: PmaAuthInfo,
        authorizationCode: AuthorizationCode,
        service: Service,
        scope: AuthScope
    ) {
        val authToken =
            authorizationCodes[authorizationCode] ?: throw AuthorizationException("Invalid authorization code")
        grantPermission(auth, authToken, service, scope)
    }

    fun grantPermission(
        auth: PmaAuthInfo,
        taskIdToken: PmaAuthToken,
        service: Service,
        scope: AuthScope
    ) {
        val serviceId = service.serviceInstanceId
        val neededClientId = auth.principal.name
        // If we are providing permission to an activity limited token, it is sufficient to have the ability to grant
        // permissions limited to that activity context (as the token receiving permission will not outlast the activity)
        val neededScope = when (taskIdToken.nodeInstanceHandle.isValid) {
            true -> GRANT_ACTIVITY_PERMISSION.context(taskIdToken.nodeInstanceHandle, neededClientId, service, scope)
            else -> GRANT_GLOBAL_PERMISSION.context(neededClientId, service, scope)
        }
        internalValidateAuthInfo(auth, neededScope)
        if (taskIdToken.serviceId != serviceId) throw AuthorizationException("Cannot grant permission for a token for one service to work against another service")
        assert(taskIdToken in activeTokens)
        val tokenPermissionList = tokenPermissions.getOrPut(taskIdToken.token) { mutableListOf() }
        doLog(auth, "grantPermission(token = ${taskIdToken.token}, serviceId = $serviceId, scope = $scope)")
        tokenPermissionList.add(Permission(scope))
    }

    fun invalidateActivityTokens(
        auth: PmaAuthInfo,
        hNodeInstance: Handle<SecureProcessNodeInstance>
    ) {
        doLog(auth, "invalidateActivityTokens($hNodeInstance)")
        internalValidateAuthInfo(auth, INVALIDATE_ACTIVITY.context(hNodeInstance))

        val invalidatedTokens = mutableListOf<PmaAuthToken>()
        authorizationCodes.values.removeIfTo(invalidatedTokens) { it.nodeInstanceHandle == hNodeInstance }
        activeTokens.removeIfTo(invalidatedTokens) { it.nodeInstanceHandle == hNodeInstance }

        for (token in invalidatedTokens) {
            doLog("invalidateActivityTokens/token($hNodeInstance, $token)")
            tokenPermissions.remove(token.token)
        }
    }

    fun registerClient(name: String, secret: String): PmaIdSecretAuthInfo {
        val clientId = "$name:${Random.nextUInt().toString(16)}"
        return registerClient(clientFromId(clientId), secret, name)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun registerClient(user: PrincipalCompat, secret: String, name: String = user.name): PmaIdSecretAuthInfo {
        doLog("registerClient($user)")
        val clientId = user.name
        if (registeredClients[clientId] != null) return registerClient(name, secret)
        registeredClients[clientId] = ClientInfo(clientId, name, secret)
        return PmaIdSecretAuthInfo(user, secret)
    }

    fun isTokenInvalid(token: PmaAuthToken): Boolean {
        return token !in activeTokens
    }

    fun registerGlobalPermission(
        authInfo: PmaAuthInfo?,
        principal: PrincipalCompat,
        service: Service,
        scope: AuthScope
    ) {
        doLog(authInfo, "registerGlobalPermissions($authInfo, $principal, ${service.serviceInstanceId}, $scope)")
        if (authInfo != null) {
            val clientId = authInfo.principal.name
            internalValidateAuthInfo(
                authInfo,
                CommonPMAPermissions.GRANT_GLOBAL_PERMISSION.context(clientId, service, scope)
            )
        }
        globalPermissions.compute(principal.name) { _, map ->
            when(map) {
                null -> mutableMapOf(service.serviceInstanceId.serviceId to scope)
                else -> map.apply {
                    compute(service.serviceInstanceId.serviceId) { k, oldScope ->
                        when (oldScope) {
                            null -> scope
                            else -> oldScope.union(scope)
                        }
                    }
                }
            }
        }
        globalPermissions.getOrPut(principal.name) { mutableMapOf() }
            .compute(service.serviceInstanceId.serviceId) { k, oldScope ->
                when (oldScope) {
                    null -> scope
                    else -> oldScope.union(scope)
                }
            }
    }


    private data class Permission(val scope: AuthScope) {
        override fun toString(): String {
            return "Permission(${scope.description})"
        }
    }

    override fun toString(): String {
        return "AuthService"
    }

    companion object {

        private operator fun Map<String, AuthScope>.get(key: ServiceId<*>) = get(key.serviceId)

        private operator fun MutableMap<String, AuthScope>.set(key: ServiceId<*>, value: AuthScope) = set(key.serviceId, value)

    }
}
