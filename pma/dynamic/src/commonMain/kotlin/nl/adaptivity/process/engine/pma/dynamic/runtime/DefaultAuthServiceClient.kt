package nl.adaptivity.process.engine.pma.dynamic.runtime

import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.util.multiplatform.PrincipalCompat

class DefaultAuthServiceClient(val originatingClientAuth: PmaAuthInfo, val authService: AuthService) :
    AuthServiceClient<PmaAuthInfo, PmaAuthToken, AuthorizationCode> {

    override val principal: PrincipalCompat get() = originatingClientAuth.principal

    override fun isTokenValid(token : PmaAuthToken): Boolean {
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
        return getAuthTokenDirect(authorizationTarget.serviceId, authorization)
/*

        val authCode = authService.getAuthorizationCode(originatingClientAuth, authorizationTarget.serviceId, authorization)
        return authService.exchangeAuthCode(originatingClientAuth, authCode)
*/
    }


    /**
     * Create an authorization code for a client to access the service with given scope
     * @param authorizedService The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param tokenTargetService The service being authorized
     * @param requestedScope The scope being authorized
     */
    override fun requestPmaAuthCode(
        authorizedService: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationCode {
        return authService.requestPmaAuthCode(
            requestorAuth = originatingClientAuth,
            authorizedService = authorizedService,
            nodeInstanceHandle = nodeInstanceHandle,
            tokenTargetService = tokenTargetService,
            requestedScope = requestedScope
        )
    }

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param identifiedUser The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param authorizedService The service that can exchange the auth code
     * @param tokenTargetService The service that is the target of the resulting token.
     * @param requestedScope The scope being authorized
     */
    override fun requestPmaAuthCode(
        identifiedUser: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        authorizedService: ServiceId<Service>,
        tokenTargetService: ServiceId<Service>,
        requestedScope: AuthScope
    ): AuthorizationCode {
        return authService.requestPmaAuthCode(originatingClientAuth, identifiedUser, nodeInstanceHandle, authorizedService, tokenTargetService, requestedScope)
    }

    /**
     * Request a PMA token for accessing a specific service with specific scope.
     * @param requestorAuth The authorization for the service making the request. This will also be the identity
     *          attached to the token.
     * @param nodeInstanceHandle The handle of the node instance to associate with the token
     * @param serviceId The service that the token is for
     * @param scope The requested authorization.
     */
    override fun requestPmaAuthToken(
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        scope: AuthScope
    ): PmaAuthToken {
        return authService.requestPmaAuthToken(originatingClientAuth, originatingClientAuth.principal.name, nodeInstanceHandle, serviceId, scope)
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
    override fun exchangeDelegateToken(
        exchangedToken: PmaAuthToken,
        service: ServiceId<*>,
        requestedScope: AuthScope,
    ): PmaAuthToken {
        return authService.exchangeDelegateToken(originatingClientAuth, exchangedToken, service, requestedScope)
    }

    override fun exchangeAuthCode(authorizationCode: AuthorizationCode): PmaAuthToken {
        return authService.exchangeAuthCode(originatingClientAuth, authorizationCode)
    }

    override fun getAuthTokenDirect(
        serviceId: ServiceId<Service>,
        reqScope: AuthScope
    ): PmaAuthToken {
        // TODO is this really safe?
        return authService.getAuthTokenDirect(originatingClientAuth, originatingClientAuth.principal.name, serviceId, reqScope)
    }

    /**
     * Register a global permission against a user/client
     */
    override fun registerGlobalPermission(
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
    override fun registerClient(serviceName: ServiceName<*>, secret: String): PmaIdSecretAuthInfo {
        return authService.registerClient(originatingClientAuth, serviceName, secret)
    }

    /**
     * Register a client to the authorization service. The username will be made unique (distinguishing [ServiceId] from
     * [ServiceName])
     * @param serviceName The "name" of the service
     * @param secret The password for the service
     * @return The actual userId for the service. This name is made to be unique.
     */
    override fun registerClient(principal: PrincipalCompat, secret: String): PmaIdSecretAuthInfo {
        return authService.registerClient(originatingClientAuth, principal, secret)
    }

    override fun userHasPermission(principal: PrincipalCompat, serviceId: ServiceId<*>, permission: SecurityProvider.Permission): Boolean {
        return authService.userHasPermission(originatingClientAuth, principal, serviceId, permission)
    }
}
