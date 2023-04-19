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
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.impl.Level
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.*
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.nl.adaptivity.util.kotlin.removeIfTo
import kotlin.random.Random
import kotlin.random.nextUInt

class AuthService(
    override val serviceName: ServiceName<AuthService>,
    adminUser: PmaIdSecretAuthInfo,
    val logger: LoggerCompat,
    private val nodeLookup: Map<Handle<SecureProcessNodeInstance>, String>,
    private val random: Random
) : AutomatedService {
    override val serviceInstanceId: ServiceId<AuthService> =
        ServiceId("${serviceName.serviceName}:${random.nextUInt().toString(16)}")

    private val registeredClients = mutableMapOf<String, ClientInfo>()
    private val authorizationCodes = mutableMapOf<AuthorizationCode, PmaAuthToken>()
    private val activeTokens = mutableListOf<PmaAuthToken>()
    private val globalPermissions =
        mutableMapOf<String, MutableMap<String, AuthScope>>()

    private val tokenPermissions = mutableMapOf<String, MutableList<PermissionScopeHolder>>()

    init {
        registeredClients[adminUser.id] = ClientInfo(adminUser.id, adminUser.id, adminUser.secret)
        globalPermissions[adminUser.id] = mutableMapOf(serviceInstanceId.serviceId to ADMIN)
    }

    private fun doLog(authInfo: PmaAuthInfo?, message: String) {
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

    private fun doLog(message: String) {
        logger.log(Level.INFO, "[UNAUTH] - $message")
    }

    private fun effectiveUserScope(principal: PrincipalCompat, targetService: ServiceId<*>): AuthScope {
        if (principal is PmaAuthToken.Principal) {
            return effectiveUserScope(principal.token, targetService)
        } else {
            return globalPermissions[principal.name]?.get(targetService) ?: EMPTYSCOPE
        }
    }

    private fun effectiveUserScope(pmaAuthInfo: PmaAuthInfo, targetService: ServiceId<*>): AuthScope {
        return when (pmaAuthInfo) {
            is PmaAuthToken -> effectiveUserScope(pmaAuthInfo, targetService)
            is PmaIdSecretAuthInfo -> effectiveUserScope(
                pmaAuthInfo.principal,
                targetService
            ).union(IDENTIFY) // username password always includes identify
            else -> effectiveUserScope(pmaAuthInfo.principal, targetService)
        }
    }

    private fun effectiveUserScope(authToken: PmaAuthToken, targetService: ServiceId<*>): AuthScope {
        when {
            authToken !in activeTokens -> throw AuthorizationException("The token $authToken is no longer valid")
            targetService != authToken.serviceId -> throw AuthorizationException("The token $authToken is not for service $targetService")
        }

        val tokenScope =
            (tokenPermissions[authToken.token]?.fold(authToken.scope) { l: AuthScope, r -> l.union(r.scope) }
                ?: authToken.scope)

        return globalPermissions[authToken.principal.name]?.get(targetService)?.union(tokenScope)
            ?: tokenScope
    }

    private fun validateAuthInfo(
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

    private fun validateAuthServiceAccess(
        authInfo: PmaAuthInfo,
        scope: UseAuthScope
    ) = validateAuthInfo(authInfo, serviceInstanceId, scope)

    private fun validateAuthTokenPermission(
        authToken: PmaAuthToken,
        serviceId: ServiceId<*>,
        useScope: UseAuthScope
    ) {
        val effectiveScope = effectiveUserScope(authToken, serviceId)
        if (!effectiveScope.includes(useScope)) {
            throw AuthorizationException("No permission found for token $authToken to $serviceId.${useScope.description}")
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
        val principal = authInfo.principal
        if (registeredClients[principal.name]?.secret != authInfo.secret) {
            throw AuthorizationException("Password mismatch for client $principal (${registeredClients[principal.name]?.secret} != ${authInfo.secret})")
        }
        if (useScope == IDENTIFY) return // Username/password always includes identification. Shortcircuit this case.

        val effectiveScope = effectiveUserScope(principal, serviceId)
        if (!effectiveScope.includes(useScope)) {
            throw AuthorizationException("No permission found for token $principal to $serviceId.${useScope.description}")
        }

        val source =
            Throwable().stackTrace[2].let { "${it.className.substringAfterLast('.')}.${it.methodName.substringBefore('-')}" }
        doLog(
            authInfo,
            "validateUserPermissions(serviceId = $serviceId, idSecret = $authInfo, scope = ${useScope.description}) from $source"
        )
    }

    private fun createPmaAuthTokenImpl(
        requestorAuth: PmaAuthInfo,
        nodeInstanceHandle: PNIHandle,
        clientId: String,
        targetServiceId: ServiceId<*>,
        scope: AuthScope,
        associatedUserName: String
    ): PmaAuthToken {
        // We know the task handle so permission limited to the task handle is sufficient
        validateAuthServiceAccess(
            requestorAuth,
            GRANT_ACTIVITY_PERMISSION.context(nodeInstanceHandle, clientId, targetServiceId, scope)
        )

        return unsecuredCreateOrReuseToken(clientId, nodeInstanceHandle, targetServiceId, scope, associatedUserName)
    }

    private fun unsecuredCreateOrReuseToken(
        authorizedClient: String,
        nodeInstanceHandle: PNIHandle,
        targetServiceId: ServiceId<*>,
        scope: AuthScope,
        associatedUserName: String
    ): PmaAuthToken {
        activeTokens.lastOrNull {
            it.principal.name == authorizedClient &&
                it.nodeInstanceHandle == nodeInstanceHandle &&
                it.serviceId == targetServiceId &&
                it.scope == scope
        }?.let { return it }

        return PmaAuthToken(authorizedClient, nodeInstanceHandle, Random.nextString(), targetServiceId, scope, associatedUserName).also {
            activeTokens.add(it)
        }
    }

    private fun requestPmaAuthCodeImpl(
        requestorAuth: PmaAuthInfo,
        identifiedUserName: String?,
        nodeInstanceHandle: PNIHandle,
        authorizedServiceOrUser: String,
        tokenTargetService: ServiceId<Service>,
        requestedScope: AuthScope
    ): AuthorizationCode {

        // We know the task handle so permission limited to the task handle is sufficient
        validateAuthServiceAccess(
            requestorAuth, GRANT_ACTIVITY_PERMISSION.context(
                nodeInstanceHandle, authorizedServiceOrUser,
                tokenTargetService, requestedScope
            )
        )

        val identifiedUser = identifiedUserName ?: authorizedServiceOrUser

        val token = unsecuredCreateOrReuseToken(identifiedUser, nodeInstanceHandle, tokenTargetService, requestedScope, identifiedUser)

        val authorizationCode = AuthorizationCode(
            code = Random.nextString(),
            authorizedServiceOrUser = authorizedServiceOrUser,
            identifiedUser = clientFromId(identifiedUserName ?: "<unknown user"),
        )
        authorizationCodes[authorizationCode] = token

        doLog(requestorAuth, "createAuthorizationCode(code = ${authorizationCode.code}, token = $token)")

        return authorizationCode
    }

    private fun clientFromId(clientId: String): PrincipalCompat {
        // TODO look up actual users.
        return SimplePrincipal(clientId)
    }

    /** Common implementation that is used for authorization codes/authtokens but doesn't log. */
    private fun getAuthCommon(
        identifiedUser: String,
        clientId: String,
        requestorAuth: PmaAuthInfo,
        serviceId: ServiceId<*>,
        reqScope: AuthScope
    ): PmaAuthToken {
        // TODO principal should be authorized
        validateAuthServiceAccess(requestorAuth, IDENTIFY) // TODO is this correct
//        validateAuthServiceAccess(requestorAuth, GRANT_GLOBAL_PERMISSION.context(clientId, serviceId, reqScope))

        val requestingUserPermissions: AuthScope = effectiveUserScope(requestorAuth, serviceInstanceId)
            globalPermissions[requestorAuth.principal.name]?.get(serviceInstanceId.serviceId)
                ?.takeIf { it.includes(ADMIN) } ?: globalPermissions.get(requestorAuth.principal.name)?.get(serviceId)

        val nodeInstanceHandle: PNIHandle = (requestorAuth as? PmaAuthToken)?.nodeInstanceHandle ?: Handle.invalid()

        if (requestingUserPermissions.includes(ADMIN)) { // Shortcircuit the case where the requestor has admin permissions
            return unsecuredCreateOrReuseToken(clientId, nodeInstanceHandle, serviceId, reqScope, identifiedUser)
        }

        val clientGlobalPermissions = globalPermissions[clientId]?.get(serviceId)

        val effectiveScope: AuthScope

        val requestorAssociatedPermissions = when (requestorAuth) {
            !is PmaAuthToken -> emptyList()

            else -> when (val s = effectiveUserScope(requestorAuth, requestorAuth.serviceId)) {
                is UnionPermissionScope -> s.members
                else -> listOf(s)
            }
        }

        val delegatingPermissions = when {
            ! nodeInstanceHandle.isValid -> {
                requestorAssociatedPermissions.filterIsInstance<GRANT_GLOBAL_PERMISSION.ContextScope>()
                    .filter { it.serviceId == serviceId }
                    .map { it.childScope ?: ANYSCOPE }
            }
            else -> {
                requestorAssociatedPermissions.mapNotNull { permission ->
                    when {
                        permission is DELEGATED_PERMISSION.DelegateContextScope &&
                            permission.serviceId == serviceId -> permission.childScope ?: ANYSCOPE
                        // Any child scopes for activity limited grants
                        permission is GRANT_ACTIVITY_PERMISSION.ContextScope &&
                            permission.taskInstanceHandle == nodeInstanceHandle -> permission.childScope ?: ANYSCOPE

                        // As well as global grants
                        permission is GRANT_GLOBAL_PERMISSION.ContextScope ->
                            permission.childScope ?: ANYSCOPE

                        else -> null
                    }
                }

            }
        }
        var delegatingScope = delegatingPermissions.fold(EMPTYSCOPE) { l: AuthScope, r -> l.union(r) }.union(clientGlobalPermissions ?: EMPTYSCOPE)

        if (delegatingScope == EMPTYSCOPE) {
            if (reqScope != ANYSCOPE && reqScope != IDENTIFY) {
                throw AuthorizationException("The token $requestorAuth has no permission to create delegate tokens for ${serviceId}.${reqScope.description}")
            }
            delegatingScope = IDENTIFY
        }

        val registeredPermissions = delegatingScope

        if (reqScope == ANYSCOPE) {
            effectiveScope = registeredPermissions
        } else {
            val intersection = registeredPermissions.intersect(reqScope)
            if (intersection != reqScope) throw AuthorizationException("The requested permission $reqScope is not contained within $registeredPermissions")
            effectiveScope = intersection
        }

        // TODO look up permissions for taskIdentityToken

        val existingToken = activeTokens.firstOrNull {
            it.principal.name == clientId &&
                it.nodeInstanceHandle == nodeInstanceHandle &&
                it.serviceId == serviceId &&
                it.scope == effectiveScope
        }

        if (existingToken != null) {
            return existingToken
        }

        return PmaAuthToken(clientId, nodeInstanceHandle, Random.nextString(), serviceId, effectiveScope, identifiedUser)

    }

    private fun unsecuredRegisterGlobalPermission(
        clientName: String,
        service: Service,
        scope: AuthScope
    ) {
        globalPermissions.compute(clientName) { _, map ->
            when (map) {
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
    }

    fun userHasPermission(
        clientAuth: PmaAuthInfo,
        principal: PrincipalCompat,
        serviceId: ServiceId<*>,
        permission: SecurityProvider.Permission
    ): Boolean {
        validateAuthServiceAccess(clientAuth, VALIDATE_AUTH.invoke(serviceId))

        return effectiveUserScope(principal, serviceId).includes(permission)
    }


    /**
     * Validate whether the authentication information will authenticate to the service with the given scope
     * @param clientAuth The authentication of the client verifying the permissions
     * @param authInfoToCheck The authentication information being used
     * @param serviceId The service that is being accessed
     * @param scope The scope of the access requested
     */
    fun validateAuthInfo(
        clientAuth: PmaAuthInfo,
        authInfoToCheck: PmaAuthInfo,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    ) {
        validateAuthServiceAccess(clientAuth, VALIDATE_AUTH(serviceId))
        validateAuthInfo(authInfoToCheck, serviceId, scope)
    }

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param requestorAuth Authorization for this action
     * @param clientId The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param service The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        requestorAuth: PmaAuthInfo,
        identifiedUser: PrincipalCompat,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        authorizedService: ServiceId<*>,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationCode = requestPmaAuthCodeImpl(
        requestorAuth = requestorAuth,
        identifiedUserName = identifiedUser.name,
        nodeInstanceHandle = nodeInstanceHandle,
        authorizedServiceOrUser = authorizedService.serviceId,
        tokenTargetService = tokenTargetService,
        requestedScope = requestedScope
    )

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param requestorAuth Authorization for this action
     * @param clientId The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param service The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        requestorAuth: PmaAuthInfo,
        authorizedService: ServiceId<*>,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationCode =
        requestPmaAuthCodeImpl(
            requestorAuth = requestorAuth,
            identifiedUserName = "<unknown username>",
            nodeInstanceHandle = nodeInstanceHandle,
            authorizedServiceOrUser = authorizedService.serviceId,
            tokenTargetService = tokenTargetService,
            requestedScope = requestedScope
        )

    /**
     * Request a PMA token for accessing a specific service with specific scope.
     * @param requestorAuth The authorization for the service making the request. This will also be the identity
     *          attached to the token.
     * @param nodeInstanceHandle The handle of the node instance to associate with the token
     * @param serviceId The service that the token is for
     * @param scope The requested authorization.
     */
    fun requestPmaAuthToken(
        requestorAuth: PmaAuthInfo,
        clientId: String,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        val associatedUserName = (requestorAuth as? PmaAuthToken)?.associatedUserName ?: requestorAuth.principal.name
        return createPmaAuthTokenImpl(requestorAuth, nodeInstanceHandle, clientId, serviceId, scope, associatedUserName)
    }

    /**
     * Exchange authorization code for a token.
     * @param requestorAuth The authorization of the client that exchanges the code
     * @param authorizationCode The code to eschange for a token
     */
    fun exchangeAuthCode(requestorAuth: PmaAuthInfo, authorizationCode: AuthorizationCode): PmaAuthToken {
        // We just want to check the client to identify itself.
        validateAuthServiceAccess(requestorAuth, IDENTIFY)

        val token = authorizationCodes[authorizationCode]
            ?: throw AuthorizationException("authorization code invalid")

        if (authorizationCode.authorizedServiceOrUser != requestorAuth.principal.name)
            throw AuthorizationException("Invalid client for authorization code (code target ${authorizationCode.authorizedServiceOrUser}) != (resolver ${requestorAuth.principal})\n    Token: $token")

        if (token !in activeTokens) throw AuthorizationException("Token ${token} expired before being exchanged")

        authorizationCodes.remove(authorizationCode)

        doLog(requestorAuth, "authTokenFromAuthorization(authorization = $authorizationCode) = $token")

        return token
    }

    /**
     * Acquire a delegate token, based upon both client authentication and a token used to invoke the service.
     * @param requestorAuth The authorization of the client (could be clientId/password).
     * @param exchangedToken The token to use as basis for the delegate token (allowing the client ot invoke a specific service)
     * @param service The service targeted by the token
     * @param requestedScope The scope needed
     */
    fun exchangeDelegateToken(
        requestorAuth: PmaAuthInfo,
        exchangedToken: PmaAuthToken,
        service: ServiceId<*>,
        requestedScope: AuthScope,
    ): PmaAuthToken {
        validateAuthServiceAccess(requestorAuth, IDENTIFY)

//        val clientServiceId = clientAuth.principal.name
        val clientName = requestorAuth.principal.name

        if (clientName != exchangedToken.serviceId.serviceId) {
            throw AuthorizationException("The exchanged token is not for the service that wishes to exchange it.")
        }

        // Get all scopes that can be exchanged for the service by the exchanged token
        val delegateScope = exchangedToken.scope.intersect(DELEGATED_PERMISSION.restrictTo(service, requestedScope))
            .dropDelegation(service)
        if (delegateScope == EMPTYSCOPE) throw AuthorizationException("The exchanged token cannot be delegated")

        // Add in the permissions that the token already has for the given service (likely nothing)
        val availableScope = delegateScope.union(effectiveUserScope(requestorAuth, service))

        // Now check that the request can be satisfied (anyscope is a wildcard), otherwise all requests must be met.
        if (requestedScope != ANYSCOPE && requestedScope.intersect(availableScope) != requestedScope) {
            throw AuthorizationException("The available scope for token $")
        }

        // TODO this should be invalid
        validateAuthTokenPermission(
            exchangedToken,
            exchangedToken.serviceId,
            DELEGATED_PERMISSION.context(service, requestedScope)
        )

        val newToken = unsecuredCreateOrReuseToken(
            clientName,
            exchangedToken.nodeInstanceHandle,
            service,
            availableScope,
            exchangedToken.associatedUserName
        )

        doLog(requestorAuth, "exchangeDelegateToken(exchanged = ${exchangedToken}, token = $newToken)")
        return newToken
    }

    /**
     * Get a global authorization code.
     * @param requestorAuth as basis.
     * @param clientId The client that will be able to exchange the token
     * @param tokenIdentity The user/service that is the principal for the token.
     * @param serviceId The service to be invoked
     * @param reqScope The requested scope.
     */
    fun getAuthorizationCode(
        requestorAuth: PmaAuthInfo,
        clientId: String,
        tokenIdentity: String,
        serviceId: ServiceId<*>,
        reqScope: AuthScope
    ): AuthorizationCode {
        val token = getAuthCommon(tokenIdentity, clientId, requestorAuth, serviceId, reqScope)
        val authorizationCode = AuthorizationCode(Random.nextString(), clientId, clientFromId(tokenIdentity))
        authorizationCodes[authorizationCode] = token

        if (token !in activeTokens) {
            activeTokens.add(token)
            doLog(requestorAuth, "getAuthorizationCode($requestorAuth) - reuse = $authorizationCode -> $token")
        }
        doLog(requestorAuth, "getAuthorizationCode($requestorAuth) = $authorizationCode -> $token")
        return authorizationCode
    }

    fun getAuthTokenDirect(
        requestorAuth: PmaAuthInfo,
        serviceId: ServiceId<*>,
        reqScope: AuthScope
    ): PmaAuthToken {
        return getAuthTokenDirect(requestorAuth, requestorAuth.principal.name, serviceId, reqScope)
    }

    fun getAuthTokenDirect(
        requestorAuth: PmaAuthInfo,
        tokenIdentity: String,
        serviceId: ServiceId<Service>,
        reqScope: AuthScope
    ): PmaAuthToken {
        val token = getAuthCommon(tokenIdentity, requestorAuth.principal.name, requestorAuth, serviceId, reqScope)
        if (token !in activeTokens) {
            doLog(requestorAuth, "getAuthTokenDirect($requestorAuth) - reuse = $token")
            activeTokens.add(token)
        }
        doLog(requestorAuth, "getAuthTokenDirect($requestorAuth) = $token")
        return token
    }

    /**
     * Get an authorization token that can be used just to identify a user by token rather than username/password.
     * The only scope is IDENTIFY or the registered global scope of the user.
     *
     * @param auth The id/secret authentication
     * @param serviceId The authorized service (defaults to the authService)
     */
    fun loginDirect(auth: PmaIdSecretAuthInfo, serviceId: ServiceId<*> = serviceInstanceId): PmaAuthToken {
        validateAuthInfo(auth, serviceId, IDENTIFY)
        val effectiveScope = effectiveUserScope(auth, serviceId)
        return PmaAuthToken(
            auth.principal.name,
            Handle.invalid(),
            Random.nextString(),
            serviceInstanceId,
            effectiveScope,
            auth.principal.name
        ).also {
            activeTokens.add(it)
            doLog(it, "loginDirect($auth) = $it")
        }
    }

    /**
     * Grant permission to an authorization code('s token).
     * @param requestorAuth The authorization for the requestor
     * @param authorizationCode The authorizationCode
     * @param service The service to be authorized. If not matching the code, this will result in delegate permissions
     * @param scope The needed scope against the service
     */
    fun grantPermission(
        requestorAuth: PmaAuthInfo,
        authorizationCode: AuthorizationCode,
        service: Service,
        scope: AuthScope
    ) {
        val authToken =
            authorizationCodes[authorizationCode] ?: throw AuthorizationException("Invalid authorization code")
        grantPermission(requestorAuth, authToken, service, scope)
    }

    /**
     * Grant permission to an authorization token.
     * @param requestorAuth The authorization for the requestor
     * @param taskIdToken The authorization token to augment (using opaque scopes)
     * @param service The service to be authorized. If not matching the code, this will result in delegate permissions
     * @param scope The needed scope against the service
     */
    fun grantPermission(
        requestorAuth: PmaAuthInfo,
        taskIdToken: PmaAuthToken,
        service: Service,
        scope: AuthScope
    ) {
        val serviceId = service.serviceInstanceId
        val neededClientId = requestorAuth.principal.name
        // If we are providing permission to an activity limited token, it is sufficient to have the ability to grant
        // permissions limited to that activity context (as the token receiving permission will not outlast the activity)
        val neededScope = when (taskIdToken.nodeInstanceHandle.isValid) {
            true -> GRANT_ACTIVITY_PERMISSION.context(taskIdToken.nodeInstanceHandle, neededClientId, service, scope)
            else -> GRANT_GLOBAL_PERMISSION.context(neededClientId, service, scope)
        }
        validateAuthServiceAccess(requestorAuth, neededScope)

        if (taskIdToken.serviceId != serviceId) throw AuthorizationException("Cannot grant permission for a token for one service to work against another service")
        assert(taskIdToken in activeTokens)
        val tokenPermissionList = tokenPermissions.getOrPut(taskIdToken.token) { mutableListOf() }
        doLog(requestorAuth, "grantPermission(token = ${taskIdToken.token}, serviceId = $serviceId, scope = $scope)")
        tokenPermissionList.add(PermissionScopeHolder(scope))
    }

    /**
     * Function that invalidates all auth tokens associated with a given activity instance.
     * @param requestorAuth The authorization for the requestor
     * @param hNodeInstance The handle to invalidate tokens for.
     */
    fun invalidateActivityTokens(
        requestorAuth: PmaAuthInfo,
        hNodeInstance: PNIHandle
    ) {
        doLog(requestorAuth, "invalidateActivityTokens($hNodeInstance)")
        validateAuthServiceAccess(requestorAuth, INVALIDATE_ACTIVITY.context(hNodeInstance))

        val invalidatedTokens = mutableListOf<PmaAuthToken>()
        authorizationCodes.values.removeIfTo(invalidatedTokens) { it.nodeInstanceHandle == hNodeInstance }
        activeTokens.removeIfTo(invalidatedTokens) { it.nodeInstanceHandle == hNodeInstance }

        for (token in invalidatedTokens) {
            doLog("invalidateActivityTokens/token($hNodeInstance, $token)")
            tokenPermissions.remove(token.token)
        }
    }

    /**
     * Register a client/user against the service.
     * @param requestorAuth The authorization for the action
     * @param serviceName The service name used as the basis for the auth info
     * @param secret The password to use
     * @return A new combination of id/secret. Note that this will return a unique id for the service (based on the name).
     */
    fun registerClient(requestorAuth: PmaAuthInfo, serviceName: ServiceName<*>, secret: String): PmaIdSecretAuthInfo {
        val clientId = clientFromId("${serviceName.serviceName}:${Random.nextUInt().toString(16)}")
        val clientAuthInfo = registerClient(requestorAuth, clientId, secret, serviceName.serviceName)

        unsecuredRegisterGlobalPermission(
            clientAuthInfo.principal.name,
            this,
            VALIDATE_AUTH(ServiceId<Service>(clientAuthInfo.id))
        )
        return clientAuthInfo
    }

    /**
     * Register a client/user against the service.
     * @param requestorAuth The authorization for the action
     * @param user The username to register
     * @param secret The password to use
     * @return A new combination of id/secret. If the user is not unique it will be different from the [user] parameter.
     */
    fun registerClient(
        requestorAuth: PmaAuthInfo,
        user: PrincipalCompat,
        secret: String,
        name: String = user.name.substringBeforeLast(':')
    ): PmaIdSecretAuthInfo {
        doLog("registerClient($user)")
        validateAuthServiceAccess(requestorAuth, REGISTER_CLIENT)

        var actualPrincipal = user
        while (actualPrincipal.name in registeredClients) {
            actualPrincipal = clientFromId("${name}:${random.nextUInt().toString(16)}")
        }

        registeredClients[actualPrincipal.name] = ClientInfo(actualPrincipal.name, name, secret)
        return PmaIdSecretAuthInfo(actualPrincipal, secret)
    }

    /**
     * Verify that the given token is still valid. Note that a service is always allowed to verify the tokens concerning it.
     * @param requestorAuth The user requesting the validation
     * @param token The token to verify for validity
     */
    fun isTokenValid(requestorAuth: PmaAuthInfo, token: PmaAuthToken): Boolean {
        if (token.principal.name != requestorAuth.principal.name) { // A user/service can always verify their own tokens
            validateAuthServiceAccess(requestorAuth, VALIDATE_AUTH(token.serviceId))
        }
        return token in activeTokens
    }

    /**
     * Register a global permission for a user against a given service
     * @param requestorAuth The authorization information to verify has permission
     * @param authorizedClient The client that will receive the permission
     * @param service The permission the authorization is for
     * @param scope The scope/permission to give
     */
    fun registerGlobalPermission(
        requestorAuth: PmaAuthInfo,
        authorizedClient: PrincipalCompat,
        service: Service,
        scope: AuthScope
    ) {
        doLog(requestorAuth, "registerGlobalPermissions($requestorAuth, $authorizedClient, ${service.serviceInstanceId}, $scope)")
        validateAuthServiceAccess(
            requestorAuth,
            GRANT_GLOBAL_PERMISSION.context(authorizedClient.name, service, scope)
        )
        unsecuredRegisterGlobalPermission(authorizedClient.name, service, scope)
    }


    private data class PermissionScopeHolder(val scope: AuthScope) {
        override fun toString(): String {
            return "Permission(${scope.description})"
        }
    }

    override fun toString(): String {
        return "AuthService"
    }

    companion object {

        private operator fun Map<String, AuthScope>.get(key: ServiceId<*>) = get(key.serviceId)

        private operator fun MutableMap<String, AuthScope>.set(key: ServiceId<*>, value: AuthScope) =
            set(key.serviceId, value)

    }
}

private fun AuthScope.dropDelegation(service: ServiceId<*>): AuthScope = when (this){
    is UnionPermissionScope -> members.fold(EMPTYSCOPE) { l: AuthScope, r -> l.union(r.dropDelegation(service)) }
    is DELEGATED_PERMISSION.DelegateContextScope -> if (service!=serviceId) EMPTYSCOPE else childScope
    else -> EMPTYSCOPE
}
