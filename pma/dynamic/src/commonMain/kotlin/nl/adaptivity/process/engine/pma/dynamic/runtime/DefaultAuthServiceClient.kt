package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.util.multiplatform.PrincipalCompat

class DefaultAuthServiceClient(val originatingClientAuth: PmaAuthInfo, val authService: AuthService) :
    AuthServiceClient<PmaAuthInfo, PmaAuthToken, AuthorizationCode> {

    override val principal: PrincipalCompat get() = originatingClientAuth.principal

    fun isTokenValid(token : PmaAuthToken): Boolean {
        return authService.isTokenValid(originatingClientAuth, token)
    }

    override fun validateAuthInfo(authInfoToCheck: PmaAuthInfo, serviceId: ServiceId<Service>, scope: UseAuthScope) {
        authService.validateAuthInfo(originatingClientAuth, authInfoToCheck, serviceId, scope)
    }

    override fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): PmaAuthToken {
        val authorization = authorizations.reduce { l, r -> l.union(r) }

        val authCode = authService.getAuthorizationCode(originatingClientAuth, authorizationTarget.serviceId, authorization)
        return authService.exchangeAuthCode(originatingClientAuth, authCode)
    }


    /**
     * Create an authorization code for a client to access the service with given scope
     * @param client The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param serviceId The service being authorized
     * @param requestedScope The scope being authorized
     */
    override fun requestPmaAuthCode(
        client: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationCode {
        return authService.requestPmaAuthCode(originatingClientAuth, client, nodeInstanceHandle, serviceId, requestedScope)
    }

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param client The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param serviceId The service being authorized
     * @param requestedScope The scope being authorized
     */
    override fun requestPmaAuthCode(
        client: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationCode {
        return authService.requestPmaAuthCode(originatingClientAuth, client, nodeInstanceHandle, serviceId, requestedScope)
    }

    /**
     * Request a PMA token for accessing a specific service with specific scope.
     * @param requestorAuth The authorization for the service making the request. This will also be the identity
     *          attached to the token.
     * @param nodeInstanceHandle The handle of the node instance to associate with the token
     * @param serviceId The service that the token is for
     * @param scope The requested authorization.
     */
    fun requestPmaAuthToken(
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        return authService.requestPmaAuthToken(originatingClientAuth, nodeInstanceHandle, serviceId, scope)
    }

    override fun invalidateActivityTokens(hNodeInstance: PNIHandle) {
        return authService.invalidateActivityTokens(originatingClientAuth, hNodeInstance)
    }

    /**
     * Acquire a delegate token, based upon both client authentication and a token used to invoke the service.
     * @param clientAuth The authorization of the client (could be clientId/password).
     * @param exchangedToken The token to use as basis for the delegate token (allowing the client ot invoke a specific service)
     * @param service The service targeted by the token
     * @param requestedScope The scope needed
     */
    fun exchangeDelegateToken(
        exchangedToken: PmaAuthToken,
        service: ServiceId<*>,
        requestedScope: AuthScope,
    ): PmaAuthToken {
        return authService.exchangeDelegateToken(originatingClientAuth, exchangedToken, service, requestedScope)
    }

    override fun exchangeAuthCode(authorizationCode: AuthorizationCode): PmaAuthToken {
        return authService.exchangeAuthCode(originatingClientAuth, authorizationCode)
    }

    fun getAuthTokenDirect(
        serviceId: ServiceId<Service>,
        reqScope: AuthScope
    ): PmaAuthToken {
        return authService.getAuthTokenDirect(originatingClientAuth, serviceId, reqScope)
    }

    /**
     * Register a global permission against a user/client
     */
    fun registerGlobalPermission(
        principal: PrincipalCompat,
        service: Service,
        scope: AuthScope
    ) {
        authService.registerGlobalPermission(originatingClientAuth, principal, service, scope)
    }

    /**
     * Register a client to the authorization service. The username will be made unique (distinguishing [ServiceId] from
     * [ServiceName])
     * @param serviceName The "name" of the service
     * @param secret The password for the service
     * @return The actual userId for the service. This name is made to be unique.
     */
    fun registerClient(serviceName: ServiceName<*>, secret: String): PmaIdSecretAuthInfo {
        return authService.registerClient(originatingClientAuth, serviceName, secret)
    }

    /**
     * Register a client to the authorization service. The username will be made unique (distinguishing [ServiceId] from
     * [ServiceName])
     * @param serviceName The "name" of the service
     * @param secret The password for the service
     * @return The actual userId for the service. This name is made to be unique.
     */
    fun registerClient(principal: PrincipalCompat, secret: String): PmaIdSecretAuthInfo {
        return authService.registerClient(originatingClientAuth, principal, secret)
    }
}
