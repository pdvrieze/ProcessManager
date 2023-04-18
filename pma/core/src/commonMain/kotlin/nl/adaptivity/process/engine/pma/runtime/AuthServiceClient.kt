package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface AuthServiceClient<InfoT: AuthorizationInfo, TokenT: AuthorizationInfo.Token, CodeT> {

    val principal: PrincipalCompat

    fun validateAuthInfo(
        authInfoToCheck: InfoT,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    )

    fun exchangeAuthCode(authorizationCode: CodeT): TokenT

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param authorizedService The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param tokenTargetService The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        authorizedService: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): CodeT

    /**
     * Create an authorization code for a client to access the service with given scope
     * @param client The client that is being authorized
     * @param nodeInstanceHandle The node instance related to this authorization
     * @param serviceId The service being authorized
     * @param requestedScope The scope being authorized
     */
    fun requestPmaAuthCode(
        identifiedUser: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        authorizedService: ServiceId<*>,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): CodeT

    fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): TokenT

    fun invalidateActivityTokens(
        hNodeInstance: PNIHandle
    )

    fun isTokenValid(token : TokenT): Boolean

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
    ): TokenT

    /**
     * Acquire a delegate token, based upon both client authentication and a token used to invoke the service.
     * @param clientAuth The authorization of the client (could be clientId/password).
     * @param exchangedToken The token to use as basis for the delegate token (allowing the client ot invoke a specific service)
     * @param service The service targeted by the token
     * @param requestedScope The scope needed
     */
    fun exchangeDelegateToken(
        exchangedToken: TokenT,
        service: ServiceId<*>,
        requestedScope: AuthScope,
    ): TokenT

    fun getAuthTokenDirect(
        serviceId: ServiceId<Service>,
        reqScope: AuthScope
    ): TokenT

    /**
     * Register a global permission against a user/client
     */
    fun registerGlobalPermission(
        principal: PrincipalCompat,
        service: Service,
        scope: AuthScope
    )

    /**
     * Register a client to the authorization service. The username will be made unique (distinguishing [ServiceId] from
     * [ServiceName])
     * @param serviceName The "name" of the service
     * @param secret The password for the service
     * @return The actual userId for the service. This name is made to be unique.
     */
    fun registerClient(serviceName: ServiceName<*>, secret: String): InfoT

    /**
     * Register a client to the authorization service. The username will be made unique (distinguishing [ServiceId] from
     * [ServiceName])
     * @param serviceName The "name" of the service
     * @param secret The password for the service
     * @return The actual userId for the service. This name is made to be unique.
     */
    fun registerClient(principal: PrincipalCompat, secret: String): InfoT

    fun userHasPermission(principal: PrincipalCompat, serviceId: ServiceId<*>, permission: UseAuthScope): Boolean
}

