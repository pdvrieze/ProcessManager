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
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.random.nextULong

class AuthService(val logger: Logger) {
    val clientId = "AuthService:${Random.nextString()}"

    private val authorizationCodes= mutableMapOf<AuthorizationCode, AuthToken>()
    private val activeTokens = mutableListOf<AuthToken>()

    private val tokenPermissions = mutableMapOf<String, MutableList<Permission>>()

    /**
     * Validate whether the authentication information will authenticate to the service with the given scope
     * @param serviceId The service that is being accessed
     * @param authInfo The authentication information being used
     * @param scope The scope of the access requested
     */
    fun validateAuthInfo(serviceId: String, authInfo: AuthInfo, scope: AuthScope) {
        logger.log(Level.INFO, "validateAuthInfo(clientId = $serviceId, authInfo = $authInfo, scope = $scope)")
    }

    fun createAuthorizationCode(
        auth: IdSecretAuthInfo,
        serviceId: String,
        clientId: String,
        nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        scope: AuthScope
                               ): AuthorizationCode {
        validateAuthInfo(this.clientId, auth, scope)
        val clientPrincipal = clientFromId(clientId)
        val token = AuthToken(clientPrincipal, nodeInstanceHandle, Random.nextString(), serviceId, scope)
        val authorizationCode = AuthorizationCode(Random.nextString())
        authorizationCodes[authorizationCode] = token
        logger.log(Level.INFO, "createAuthorizationCode(code = ${authorizationCode.code}, token = $token)")
        return authorizationCode
    }

    private fun clientFromId(clientId: String): Principal {
        // TODO look up actual users.
        return SimplePrincipal(clientId)
    }

    fun getAuthToken(clientAuth: IdSecretAuthInfo, authorizationCode: AuthorizationCode): AuthToken {
        validateAuthInfo(clientId, clientAuth, LoanPermissions.IDENTIFY)
        val token =
            authorizationCodes.get(authorizationCode) ?: throw AuthorizationException("authorization code invalid")

        if(token.principal!=clientAuth.principal) throw AuthorizationException("Invalid client for authentication code")

        logger.log(Level.INFO, "authTokenFromAuthorization(authorization = $authorizationCode) = $token")
        authorizationCodes.remove(authorizationCode)
        activeTokens.add(token)
        return token
    }

    /** Create a token that allows the identification of the client with the token */
    fun createTaskIdentityToken(clientAuth: IdSecretAuthInfo, processNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>, principal: Principal): AuthToken {
        validateAuthInfo(clientId, clientAuth, LoanPermissions.CREATE_TASK_IDENTITY)
        val token = AuthToken(principal, processNodeInstance, Random.nextString(), clientId, LoanPermissions.IDENTIFY)
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
        // Assume the principal has been logged in validly
        if (principal!=taskIdentityToken.principal) throw AuthorizationException("Mismatch between task identity user and task user $principal <> ${taskIdentityToken.principal}")

        // TODO look up permissions for taskIdentityToken

        return AuthToken(principal, taskIdentityToken.nodeInstanceHandle, Random.nextString(), serviceId, scope)
    }

    fun grantPermission(auth: AuthInfo, taskIdToken: AuthToken, serviceId: String, scope: AuthScope) {
        validateAuthInfo(clientId, auth, LoanPermissions.GRANT_PERMISSION.context(serviceId))
        assert(taskIdToken in activeTokens)
        val tokenPermissionList = tokenPermissions.getOrPut(taskIdToken.tokenValue) { mutableListOf() }
        logger.log(Level.INFO, "grantPermission(token = ${taskIdToken.tokenValue}, serviceId = $serviceId, scope = $scope)")
        tokenPermissionList.add(Permission(serviceId, scope))
    }


    private data class Permission(val serviceId: String, val scope: AuthScope)

}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun Random.nextString() = nextULong().toString(16)
