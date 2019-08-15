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

import net.devrieze.util.Handle
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

class AuthService(val logger: Logger): Service {
    @UseExperimental(ExperimentalUnsignedTypes::class)
    val authServiceId = "AuthService:${Random.nextUInt().toString(16)}"
    override val serviceId: String
        get() = authServiceId

    private val registeredClients = mutableMapOf<String, ClientInfo>()
    private val authorizationCodes= mutableMapOf<AuthorizationCode, AuthToken>()
    private val activeTokens = mutableListOf<AuthToken>()

    private val tokenPermissions = mutableMapOf<String, MutableList<Permission>>()

    /**
     * Validate whether the authentication information will authenticate to the service with the given scope
     * @param serviceId The service that is being accessed
     * @param authInfo The authentication information being used
     * @param scope The scope of the access requested
     */
    fun validateAuthInfo(service: Service, authInfo: AuthInfo, scope: AuthScope) {
        val serviceId = service.serviceId
        when(authInfo) {
            is IdSecretAuthInfo -> validateUserPermission(serviceId, authInfo, scope)
            is AuthToken -> validateAuthTokenPermission(serviceId, authInfo, scope)
            else -> logger.log(Level.INFO, "validateAuthInfo(clientId = $serviceId, authInfo = $authInfo, scope = $scope)")
        }
    }

    private fun internalValidateAuthInfo(authInfo: AuthInfo, scope: AuthScope) {
        when(authInfo) {
            is IdSecretAuthInfo -> validateUserPermission(authServiceId, authInfo, scope)
            is AuthToken -> validateAuthTokenPermission(authServiceId, authInfo, scope)
            else -> logger.log(Level.INFO, "validateAuthInfo(authInfo = $authInfo, scope = $scope)")
        }

    }

    private fun validateAuthTokenPermission(serviceId: String, authToken: AuthToken, scope: AuthScope) {
        if(authToken !in activeTokens) throw AuthorizationException("Token not active: $authToken")
//        val tokenPermissions = tokenPermissions.get(authToken.tokenValue) ?:emptyList<Permission>()
        val hasPermissionDirectPermission = authToken.serviceId==serviceId && authToken.scope.includes(scope)
        if (!hasPermissionDirectPermission) {
            val additionalPermissions = tokenPermissions[authToken.tokenValue]?: emptyList<Permission>()
            val hasExtPermission = additionalPermissions.any { it.serviceId==serviceId && it.scope.includes(scope) }
            if (!hasExtPermission) {
                throw AuthorizationException("No permission found for token $authToken to $serviceId.$scope")
            }
        }
        logger.log(Level.INFO, "validateTokenPermissions(clientId = $serviceId, token = $authToken, scope = $scope)")
    }

    private fun validateUserPermission(serviceId: String, authInfo: IdSecretAuthInfo, scope: AuthScope) {
        if (serviceId!= authServiceId) throw AuthorizationException("Only authService allows password auth")
        logger.log(Level.INFO, "validateUserPermissions(clientId = $serviceId, authInfo = $authInfo, scope = $scope)")
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
        nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        service: Service,
        scope: AuthScope
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
        scope: AuthScope
                               ): AuthorizationCode {
        val nodeInstanceHandle = auth.getNodeInstanceHandle()
        return createAuthorizationCodeImpl(auth, clientId, nodeInstanceHandle, service, scope)
    }

    private fun AuthInfo.getNodeInstanceHandle(): Handle<SecureObject<ProcessNodeInstance<*>>> = when(this) {
        is AuthToken -> nodeInstanceHandle
        else -> getInvalidHandle()
    }

    private fun createAuthorizationCodeImpl(
        auth: AuthInfo,
        clientId: String,
        nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        service: Service,
        scope: AuthScope
                                           ): AuthorizationCode {
        internalValidateAuthInfo(auth, LoanPermissions.GRANT_PERMISSION.context(service.serviceId))

        val clientPrincipal = clientFromId(clientId)
        val token = AuthToken(clientPrincipal, nodeInstanceHandle, Random.nextString(), service.serviceId, scope)
        val authorizationCode = AuthorizationCode(Random.nextString())
        authorizationCodes[authorizationCode] = token
        activeTokens.add(token)
        logger.log(Level.INFO, "createAuthorizationCode(code = ${authorizationCode.code}, token = $token)")
        return authorizationCode
    }

    private fun clientFromId(clientId: String): Principal {
        // TODO look up actual users.
        return SimplePrincipal(clientId)
    }

    fun getAuthToken(clientAuth: IdSecretAuthInfo, authorizationCode: AuthorizationCode): AuthToken {
        internalValidateAuthInfo(clientAuth, LoanPermissions.IDENTIFY)
        val token =
            authorizationCodes.get(authorizationCode) ?: throw AuthorizationException("authorization code invalid")

        if(token !in activeTokens) throw AuthorizationException("Authorization code expired")

        if(token.principal!=clientAuth.principal) throw AuthorizationException("Invalid client for authentication code")

        logger.log(Level.INFO, "authTokenFromAuthorization(authorization = $authorizationCode) = $token")
        authorizationCodes.remove(authorizationCode)
        return token
    }

    /** Create a token that allows the identification of the client with the token */
    fun createTaskIdentityToken(clientAuth: IdSecretAuthInfo, processNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>, principal: Principal): AuthToken {
        internalValidateAuthInfo(clientAuth, LoanPermissions.CREATE_TASK_IDENTITY)
        val token = AuthToken(principal, processNodeInstance, Random.nextString(), authServiceId, LoanPermissions.IDENTIFY)
        activeTokens.add(token)
        logger.log(Level.INFO, "createTaskIdentityToken() = $token")
        return token
    }

    fun getAuthTokenDirect(
        principal: Principal,
        taskIdentityToken: AuthToken,
        serviceId: String,
        scope: AuthScope
                          ): AuthToken {
        internalValidateAuthInfo(taskIdentityToken, LoanPermissions.IDENTIFY)
        // Assume the principal has been logged in validly
        if (principal!=taskIdentityToken.principal) throw AuthorizationException("Mismatch between task identity user and task user $principal <> ${taskIdentityToken.principal}")
        val tokenPermissions = tokenPermissions.get(taskIdentityToken.tokenValue) ?:emptyList<Permission>()
        val hasPermission = tokenPermissions.any { it.serviceId == serviceId && it.scope.includes(scope) }
        if (!hasPermission) {
            throw AuthorizationException("No permissions available for $taskIdentityToken to $serviceId.$scope")

        }
        // TODO look up permissions for taskIdentityToken

        return AuthToken(principal, taskIdentityToken.nodeInstanceHandle, Random.nextString(), serviceId, scope).also {
            activeTokens.add(it)
            logger.log(Level.INFO, "getAuthTokenDirect($taskIdentityToken) = $it")
        }
    }

    fun grantPermission(auth: AuthInfo, authorizationCode: AuthorizationCode, service: Service, scope: AuthScope) {
        val authToken = authorizationCodes[authorizationCode] ?: throw AuthorizationException("Invalid authorization code")
        grantPermission(auth, authToken, service, scope)
    }

    fun grantPermission(auth: AuthInfo, taskIdToken: AuthToken, service: Service, scope: AuthScope) {
        val serviceId = service.serviceId
        internalValidateAuthInfo(auth, LoanPermissions.GRANT_PERMISSION.context(serviceId))
        assert(taskIdToken in activeTokens)
        val tokenPermissionList = tokenPermissions.getOrPut(taskIdToken.tokenValue) { mutableListOf() }
        logger.log(Level.INFO, "grantPermission(token = ${taskIdToken.tokenValue}, serviceId = $serviceId, scope = $scope)")
        tokenPermissionList.add(Permission(serviceId, scope))
    }

    fun invalidateActivityTokens(auth: AuthInfo, hNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>) {
        internalValidateAuthInfo(auth, LoanPermissions.INVALIDATE_ACTIVITY.context(hNodeInstance))
        val it = activeTokens.iterator()
        while (it.hasNext()) {
            val token = it.next()
            if (token.nodeInstanceHandle==hNodeInstance) {
                it.remove()
                tokenPermissions.remove(token.tokenValue)
            }
        }
    }

    @UseExperimental(ExperimentalUnsignedTypes::class)
    fun registerClient(name: String, secret: String): IdSecretAuthInfo {
        val clientId = "$name:${Random.nextUInt().toString(16)}"
        if (registeredClients[clientId]!=null) return registerClient(name, secret)
        registeredClients[clientId] = ClientInfo(clientId, name, secret)
        return IdSecretAuthInfo(SimplePrincipal(clientId), secret)
    }


    private data class Permission(val serviceId: String, val scope: AuthScope)

}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun Random.nextString() = nextULong().toString(16)
